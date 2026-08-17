package io.github.kottzi.gamepicker.lobby.domain.service;

import java.security.SecureRandom;

public final class InviteCodeGenerator {

    // без 0/O/1/I/L, чтобы код было легко продиктовать голосом или прочитать с экрана
    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int DEFAULT_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodeGenerator() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return code.toString();
    }
}
