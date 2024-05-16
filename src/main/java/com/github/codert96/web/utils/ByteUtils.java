package com.github.codert96.web.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ByteUtils {

    public byte[] middle(byte[] input, int length) {
        int startIndex = (input.length - length) / 2;
        byte[] result = new byte[length];
        System.arraycopy(input, startIndex, result, 0, length);
        return result;
    }

    public byte[] reverse(byte[] bytes) {
        int length = bytes.length;
        byte[] reversedArray = new byte[length];
        for (int i = 0; i < length; i++) {
            reversedArray[i] = bytes[length - 1 - i];
        }
        return reversedArray;
    }
}
