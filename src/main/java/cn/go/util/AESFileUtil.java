package cn.go.util;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Random;

public class AESFileUtil {

    private static Logger logger = LoggerFactory.getLogger(AESFileUtil.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static String aesKey;

    public static void setKey(String key) {
        aesKey = key;
    }

    public static void encryptFile(String fileName, String encryptedFileName) {
        try {
            FileInputStream fis = new FileInputStream(fileName);
            FileOutputStream fos = new FileOutputStream(encryptedFileName);
            //秘钥自动生成
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            byte[] keyValue = key.getEncoded();
            fos.write(keyValue);//记录输入的加密密码的消息摘要
            SecretKeySpec encryKey = new SecretKeySpec(keyValue, "AES");//加密秘钥
            byte[] ivValue = new byte[16];
            Random random = new Random(System.currentTimeMillis());
            random.nextBytes(ivValue);
            IvParameterSpec iv = new IvParameterSpec(ivValue);//获取系统时间作为IV
            fos.write("MyFileEncryptor".getBytes());//文件标识符
            fos.write(ivValue);    //记录IV
            Cipher cipher = Cipher.getInstance("AES/CFB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, encryKey, iv);
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            byte[] buffer = new byte[1024];
            int n = 0;
            while ((n = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, n);
            }
            cis.close();
            fos.close();
            logger.info("{}: encrypt_ok", fileName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }


    public static byte[] decryptedBytes(byte[] aesKey, String iv, byte[] encryptBytes) {
        if(aesKey == null || encryptBytes == null){
            logger.error("encryptBytes= {}, aesKey ={}", encryptBytes, aesKey);
            throw new RuntimeException("encryptBytes_or_aesKey_is_null");
        }
        byte[] decryptBytes = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey, "AES");
            if(iv == null){
                iv = "0000000000000000";
            }
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv.getBytes());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, paramSpec);
            logger.info("encryptBytes.length= [{}] bytes", encryptBytes.length);
            decryptBytes = cipher.doFinal(encryptBytes);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return decryptBytes;
    }

    public static void decryptedFile(String aesKey, String encryptedFileName, String decryptedFileName) {
        File file = new File(decryptedFileName);
        if (!file.exists()) {
            String parent = file.getParent();
            new File(parent).mkdirs();
        }else{
            file.delete();
        }
        try {
            FileInputStream fis = new FileInputStream(encryptedFileName);
            FileOutputStream fos = new FileOutputStream(decryptedFileName);
            byte[] decode = new Base64().decode(aesKey);
            logger.info("key_length={}", decode.length);
            SecretKeySpec secretKeySpec = new SecretKeySpec(new Base64().decode(aesKey), "AES");

//            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            String iv = "0000000000000000";
//          AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv.getBytes());
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(new byte[16]);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, paramSpec);

            CipherInputStream cis = new CipherInputStream(fis, cipher);

            byte[] buffer = new byte[fis.available()];
            int read = fis.read(buffer);
            logger.info("input_stream_read [{}] bytes", read);
            byte[] decryptBytes = cipher.doFinal(buffer);
            fos.write(decryptBytes);
            cis.close();
            fos.close();
            logger.info("{}: decrypt_ok", encryptedFileName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}



