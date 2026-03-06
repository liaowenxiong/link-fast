package cn.linkfast.utils;

import org.junit.jupiter.api.Test;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class AESCBCTest {

    @Test
    void testEncryptAndDecrypt() throws IllegalBlockSizeException, InvalidKeyException, 
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, 
            InvalidAlgorithmParameterException {
        
        String data = "TestMessage"; // 待加密的原文
        String key = "qwertyuiop123456asdfghjk"; // key 长度只能是 16、24 或 32 字节
        String iv = key.substring(0, 16); // CBC 模式需要用到初始向量参数

        // 测试加密
        byte[] ciphertext = AESCBC.encryptCBC(data.getBytes(), key.getBytes(), iv.getBytes());
        assertNotNull(ciphertext);
        assertTrue(ciphertext.length > 0);
        System.out.println("CBC 模式加密结果（Base64）：" + Base64.getEncoder().encodeToString(ciphertext));

        // 测试解密
        byte[] plaintext = AESCBC.decryptCBC(ciphertext, key.getBytes(), iv.getBytes());
        assertNotNull(plaintext);
        assertEquals(data, new String(plaintext));
        System.out.println("解密结果：" + new String(plaintext));
    }

    @Test
    void testEncryptDecryptWithDifferentData() throws Exception {
        String[] testData = {
            "Hello World",
            "中文测试数据",
            "1234567890",
            "Special chars: !@#$%^&*()",
            ""
        };

        String key = "qwertyuiop123456asdfghjk";
        String iv = key.substring(0, 16);

        for (String data : testData) {
            byte[] ciphertext = AESCBC.encryptCBC(data.getBytes(), key.getBytes(), iv.getBytes());
            byte[] plaintext = AESCBC.decryptCBC(ciphertext, key.getBytes(), iv.getBytes());
            assertEquals(data, new String(plaintext));
        }
    }

    @Test
    void testInvalidKeyLength() {
        String data = "test";
        String key = "short"; // 长度不足16字节
        String iv = "1234567890123456";

        assertThrows(InvalidKeyException.class, () -> {
            AESCBC.encryptCBC(data.getBytes(), key.getBytes(), iv.getBytes());
        });
    }

    @Test
    void testInvalidIvLength() {
        String data = "test";
        String key = "qwertyuiop123456asdfghjk";
        String iv = "short"; // 长度不足16字节

        assertThrows(InvalidAlgorithmParameterException.class, () -> {
            AESCBC.encryptCBC(data.getBytes(), key.getBytes(), iv.getBytes());
        });
    }
}