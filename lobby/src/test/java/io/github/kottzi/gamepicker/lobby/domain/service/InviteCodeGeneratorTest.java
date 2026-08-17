package io.github.kottzi.gamepicker.lobby.domain.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteCodeGeneratorTest {

    private static final String FORBIDDEN_CHARS = "0O1IL";

    @Test
    void generatesCodeOfDefaultLength() {
        assertEquals(6, InviteCodeGenerator.generate().length());
    }

    @Test
    void generatesCodeOfCustomLength() {
        assertEquals(10, InviteCodeGenerator.generate(10).length());
    }

    @Test
    void neverContainsAmbiguousCharacters() {
        for (int i = 0; i < 200; i++) {
            String code = InviteCodeGenerator.generate();
            for (char forbidden : FORBIDDEN_CHARS.toCharArray()) {
                assertFalse(code.indexOf(forbidden) >= 0, "код содержит запрещённый символ: " + code);
            }
        }
    }

    @Test
    void generatesDistinctCodesWithHighProbability() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            codes.add(InviteCodeGenerator.generate());
        }
        assertTrue(codes.size() > 490, "слишком много коллизий для 6-символьного алфавита из 31 символа");
    }
}
