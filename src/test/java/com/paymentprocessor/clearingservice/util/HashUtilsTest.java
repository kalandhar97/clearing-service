package com.paymentprocessor.clearingservice.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void sha256HexReturns64CharacterHexString() {
        String hash = HashUtils.sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-fA-F]+");
    }
}
