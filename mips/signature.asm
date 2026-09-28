# MiniLang pipeline, stage 3: verification signature.
#
# Loads resultado.txt into memory, walks it line by line and writes firma.txt:
#   operations = trace lines before RESULT=
#   acc        = 0, then acc = acc * 31 + value for each value after RESULT=
#   checksum   = (acc XOR operations) + 17
# With RESULT=60 and 3 operations: 60 XOR 3 = 63, 63 + 17 = 80, as in the assignment.
# Arithmetic is 32-bit modulo 2^32 (mul, addu and addiu do not trap on overflow).
#
# Run from the project folder: java -jar Mars45.jar nc sm ae1 se1 mips/signature.asm
#
# Registers kept by main:
#   $s0 text cursor          $s1 end of the loaded text   $s2 operations
#   $s3 acc / checksum       $s4 current line, from 1     $s5 1 after RESULT=
#   $s6 report write cursor  $s7 file descriptor

        .data
input_name:     .asciiz "resultado.txt"
output_name:    .asciiz "firma.txt"
result_tag:     .asciiz "RESULT="
operations_tag: .asciiz "OPERATIONS="
checksum_tag:   .asciiz "CHECKSUM="
eol:            .asciiz "\n"
msg_ok:         .asciiz "Firma calculada. Se genero firma.txt.\n"
msg_no_input:   .asciiz "No se encontro resultado.txt. Ejecute primero la etapa Python desde la carpeta del proyecto.\n"
msg_read:       .asciiz "No se pudo leer resultado.txt.\n"
msg_too_big:    .asciiz "resultado.txt es demasiado grande (maximo 65536 bytes).\n"
msg_no_result:  .asciiz "resultado.txt no tiene la linea RESULT=.\n"
msg_write:      .asciiz "No se pudo escribir firma.txt.\n"
msg_line:       .asciiz "resultado.txt, linea "
msg_separator:  .asciiz ": "
msg_bad_char:   .asciiz "caracter invalido en RESULT="
msg_bad_number: .asciiz "numero invalido en RESULT="
msg_no_value:   .asciiz "RESULT= no tiene un valor"
msg_after:      .asciiz "RESULT= debe ser la ultima linea"

text:           .space 65537    # one byte more than allowed, to detect bigger files
text_limit:
report:         .space 256      # firma.txt or an error message
number_text:    .space 12       # decimal digits, written from right to left
number_end:

        .text
        .globl main

main:
        jal     load_input
        li      $s2, 0
        li      $s3, 0
        li      $s4, 1
        li      $s5, 0

scan_lines:
        bgeu    $s0, $s1, lines_done
        bnez    $s5, after_result_line
        la      $a0, result_tag
        move    $a1, $s0
        jal     starts_with
        bnez    $v0, result_line
        jal     skip_line
        beqz    $v0, next_line          # empty lines are ignored
        addiu   $s2, $s2, 1             # each trace line is one executed operation
        b       next_line

result_line:
        addiu   $s0, $s0, 7             # skip "RESULT="
        jal     parse_result
        b       next_line

after_result_line:
        jal     skip_line
        bnez    $v0, fail_after_result

next_line:
        addiu   $s4, $s4, 1
        b       scan_lines

lines_done:
        beqz    $s5, fail_no_result
        xor     $s3, $s3, $s2           # checksum = acc XOR operations
        addiu   $s3, $s3, 17            # checksum = checksum + 17
        jal     build_report
        jal     save_report
        li      $v0, 4
        la      $a0, msg_ok
        syscall
        li      $v0, 17
        li      $a0, 0
        syscall

# Reads the whole resultado.txt into text; $s0 = first byte, $s1 = byte after the last one.
load_input:
        li      $v0, 13
        la      $a0, input_name
        li      $a1, 0                  # read only
        li      $a2, 0
        syscall
        bltz    $v0, fail_no_input
        move    $s7, $v0
        la      $s0, text
        move    $s1, $s0
read_more:
        la      $t0, text_limit
        subu    $a2, $t0, $s1           # free space left in text
        beqz    $a2, fail_too_big
        li      $v0, 14
        move    $a0, $s7
        move    $a1, $s1
        syscall
        bltz    $v0, fail_read
        beqz    $v0, read_done
        addu    $s1, $s1, $v0
        b       read_more
read_done:
        li      $v0, 16
        move    $a0, $s7
        syscall
        jr      $ra

# $v0 = 1 when the text at $a1 starts with the NUL-terminated tag at $a0.
starts_with:
        lbu     $t0, 0($a0)
        beqz    $t0, starts_yes
        bgeu    $a1, $s1, starts_no
        lbu     $t1, 0($a1)
        bne     $t0, $t1, starts_no
        addiu   $a0, $a0, 1
        addiu   $a1, $a1, 1
        b       starts_with
starts_yes:
        li      $v0, 1
        jr      $ra
starts_no:
        li      $v0, 0
        jr      $ra

# Moves $s0 past the current line; $v0 = characters in it, without '\r' or '\n'.
skip_line:
        li      $v0, 0
skip_loop:
        bgeu    $s0, $s1, skip_done
        lbu     $t0, 0($s0)
        addiu   $s0, $s0, 1
        li      $t1, 10
        beq     $t0, $t1, skip_done
        li      $t1, 13
        beq     $t0, $t1, skip_loop
        addiu   $v0, $v0, 1
        b       skip_loop
skip_done:
        jr      $ra

# Reads the values after "RESULT=" up to the end of the line and mixes them into $s3.
# The number being read is kept in $t0 (value), $t1 (has digits) and $t2 (negative);
# $t3 becomes 1 once the line has a digit or '['.
parse_result:
        addiu   $sp, $sp, -4
        sw      $ra, 0($sp)
        li      $t0, 0
        li      $t1, 0
        li      $t2, 0
        li      $t3, 0
value_loop:
        bgeu    $s0, $s1, value_end
        lbu     $t4, 0($s0)
        addiu   $s0, $s0, 1
        li      $t5, 10
        beq     $t4, $t5, value_end
        li      $t5, 13
        beq     $t4, $t5, value_loop
        addiu   $t5, $t4, -48           # digit value when $t4 is '0'..'9'
        sltiu   $t6, $t5, 10
        bnez    $t6, add_digit
        li      $t5, 45                 # '-'
        beq     $t4, $t5, negative_sign
        li      $t5, 91                 # '['
        beq     $t4, $t5, list_start
        li      $t5, 44                 # ',', ']' and ' ' end the current number
        beq     $t4, $t5, value_separator
        li      $t5, 93
        beq     $t4, $t5, value_separator
        li      $t5, 32
        beq     $t4, $t5, value_separator
        b       fail_bad_char
add_digit:
        li      $t6, 10
        mul     $t0, $t0, $t6           # value = value * 10 + digit
        addu    $t0, $t0, $t5
        li      $t1, 1
        li      $t3, 1
        b       value_loop
negative_sign:
        or      $t6, $t1, $t2           # '-' is valid only once, before the digits
        bnez    $t6, fail_bad_number
        li      $t2, 1
        b       value_loop
list_start:
        li      $t3, 1
        b       value_loop
value_separator:
        jal     mix_value
        b       value_loop
value_end:
        jal     mix_value
        beqz    $t3, fail_no_value
        li      $s5, 1
        lw      $ra, 0($sp)
        addiu   $sp, $sp, 4
        jr      $ra

# acc = acc * 31 + value for the number in $t0..$t2, then clears it. Uses $t7.
mix_value:
        beqz    $t1, mix_nothing
        beqz    $t2, mix_add
        subu    $t0, $zero, $t0         # negative value
mix_add:
        li      $t7, 31
        mul     $s3, $s3, $t7
        addu    $s3, $s3, $t0
        li      $t0, 0
        li      $t1, 0
        li      $t2, 0
        jr      $ra
mix_nothing:
        bnez    $t2, fail_bad_number    # '-' without digits
        jr      $ra

# Writes "OPERATIONS=<n>\nCHECKSUM=<n>\n" into report.
build_report:
        addiu   $sp, $sp, -4
        sw      $ra, 0($sp)
        la      $s6, report
        la      $a0, operations_tag
        jal     put_text
        move    $a0, $s2
        jal     put_number
        la      $a0, eol
        jal     put_text
        la      $a0, checksum_tag
        jal     put_text
        move    $a0, $s3
        jal     put_number
        la      $a0, eol
        jal     put_text
        lw      $ra, 0($sp)
        addiu   $sp, $sp, 4
        jr      $ra

# Creates or replaces firma.txt with the report; it is reached only with a valid input.
save_report:
        addiu   $sp, $sp, -4
        sw      $ra, 0($sp)
        li      $v0, 13
        la      $a0, output_name
        li      $a1, 1                  # write, create or replace
        li      $a2, 0
        syscall
        bltz    $v0, fail_write
        move    $s7, $v0
        move    $a0, $s7
        jal     send_report
        move    $t0, $v0
        li      $v0, 16
        move    $a0, $s7
        syscall
        bltz    $t0, fail_write
        lw      $ra, 0($sp)
        addiu   $sp, $sp, 4
        jr      $ra

# Appends the NUL-terminated text at $a0 to report at $s6. Uses $t8.
put_text:
        lbu     $t8, 0($a0)
        beqz    $t8, put_text_done
        sb      $t8, 0($s6)
        addiu   $a0, $a0, 1
        addiu   $s6, $s6, 1
        b       put_text
put_text_done:
        jr      $ra

# Appends $a0 to report at $s6 as an unsigned decimal number. Uses $t7..$t9.
put_number:
        la      $t8, number_end
        li      $t9, 10
digit_loop:
        divu    $a0, $t9
        mfhi    $t7                     # next digit, from right to left
        mflo    $a0
        addiu   $t7, $t7, 48
        addiu   $t8, $t8, -1
        sb      $t7, 0($t8)
        bnez    $a0, digit_loop
        la      $t9, number_end
copy_digit:
        lbu     $t7, 0($t8)
        sb      $t7, 0($s6)
        addiu   $t8, $t8, 1
        addiu   $s6, $s6, 1
        bne     $t8, $t9, copy_digit
        jr      $ra

# Writes report up to $s6 to the file descriptor in $a0; $v0 < 0 on failure.
send_report:
        la      $a1, report
        subu    $a2, $s6, $a1
        li      $v0, 15
        syscall
        jr      $ra

fail_bad_char:
        la      $a0, msg_bad_char
        b       fail_at_line
fail_bad_number:
        la      $a0, msg_bad_number
        b       fail_at_line
fail_no_value:
        la      $a0, msg_no_value
        b       fail_at_line
fail_after_result:
        la      $a0, msg_after
        b       fail_at_line

# Writes "resultado.txt, linea <$s4>: <text at $a0>" to stderr and exits with code 1.
fail_at_line:
        move    $s2, $a0
        la      $s6, report
        la      $a0, msg_line
        jal     put_text
        move    $a0, $s4
        jal     put_number
        la      $a0, msg_separator
        jal     put_text
        move    $a0, $s2
        jal     put_text
        la      $a0, eol
        jal     put_text
        b       fail_send

fail_no_input:
        la      $a0, msg_no_input
        b       fail_message
fail_read:
        la      $a0, msg_read
        b       fail_message
fail_too_big:
        la      $a0, msg_too_big
        b       fail_message
fail_no_result:
        la      $a0, msg_no_result
        b       fail_message
fail_write:
        la      $a0, msg_write

# Writes the text at $a0 to stderr and exits with code 1.
fail_message:
        la      $s6, report
        jal     put_text
fail_send:
        li      $a0, 2                  # stderr
        jal     send_report
        li      $v0, 17
        li      $a0, 1
        syscall
