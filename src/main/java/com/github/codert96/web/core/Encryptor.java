package com.github.codert96.web.core;

public interface Encryptor {

    byte[] encrypt(byte[] key, byte[] plaintext);

    byte[] decrypt(byte[] key, byte[] ciphertext);

    String algorithm();

    byte[] genKey();
}
