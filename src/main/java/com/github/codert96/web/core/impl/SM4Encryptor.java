package com.github.codert96.web.core.impl;

import com.github.codert96.web.core.Encryptor;
import com.github.codert96.web.utils.ByteUtils;
import com.tencent.kona.crypto.KonaCryptoProvider;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Objects;

public class SM4Encryptor implements Encryptor {
    private static final String transformation = "SM4/CBC/PKCS7Padding";
    private static final String provider = "KonaCrypto";
    private static final String algorithm = "SM4";
    private final static KeyGenerator KEY_GENERATOR;

    static {
        if (Objects.isNull(Security.getProvider(provider))) {
            Security.addProvider(new KonaCryptoProvider());
        }
        try {
            KEY_GENERATOR = KeyGenerator.getInstance(algorithm, provider);
            KEY_GENERATOR.init(128);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SneakyThrows
    public byte[] encrypt(byte[] key, byte[] plaintext) {
        return cipher(key, Cipher.ENCRYPT_MODE).doFinal(plaintext);
    }

    @Override
    @SneakyThrows
    public byte[] decrypt(byte[] key, byte[] ciphertext) {
        return cipher(key, Cipher.DECRYPT_MODE).doFinal(ciphertext);
    }

    @Override
    public String algorithm() {
        return algorithm;
    }

    private Cipher cipher(byte[] key, int mode) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation, provider);
        byte[] iv = ByteUtils.reverse(key);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(mode, new SecretKeySpec(key, algorithm), ivParameterSpec);
        return cipher;

    }

    @SneakyThrows
    public byte[] genKey() {
        SecretKey secretKey = KEY_GENERATOR.generateKey();
        return secretKey.getEncoded();
    }
}
