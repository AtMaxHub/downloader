package util;

import cn.go.util.AESFileUtil;
import cn.go.util.HttpClientUtil;
import cn.go.util.M3U8Util;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiString;
import org.fusesource.jansi.HtmlAnsiOutputStream;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonTest {


    private static void httpDownloadKey(){
//        byte[] arrOutput = { 0xA7, 0xBF, 0x85, 0xFB, 0x78, 0xA4, 0xBE, 0x92, 0xCB, 0x41, 0x41, 0x1D, 0xE9, 0xEE, 0x1C, 0x32 };
        // p7+F+3ikvpLLQUEd6e4cMg==
        String url = "xxx";
        byte[] aesKey = new HttpClientUtil().getAESKey(url);
        BASE64Encoder encoder = new BASE64Encoder();
        System.out.println(encoder.encode(aesKey));
        System.out.println(DatatypeConverter.printHexBinary(aesKey));
    }

    private static void testFile(){
        /*String prefix = "D:/workspace/";
        String fileName = prefix + "ellipse.mp4";
        String encryptedFileName = prefix + "ellipse_encrypt.mp4";
        String decryptedFileName = prefix + "ellipse_decrypt.mp4";
        encryptFile(fileName, encryptedFileName);*/
        //decryptedFile(encryptedFileName, decryptedFileName);
        String prefix = "D:/all.download/java_download_m3u8/temp/";
        String fileName = "9740748badc5120793485285d3a889a7-sd-encrypt-stream-00001.ts";
        String encryptedFileName = prefix + fileName;
        String decryptedFileName = prefix + "decrypt/" + fileName;

        String aesKey = "p7+F+3ikvpLLQUEd6e4cMg==";
        AESFileUtil.decryptedFile(aesKey, encryptedFileName, decryptedFileName);
    }

    private static void binHexTest() throws UnsupportedEncodingException {
        byte[] bytes = "HELLO WORLD".getBytes();
        String helloHex = DatatypeConverter.printHexBinary(bytes);
        System.out.printf("Hello hex: 0x%s\n" ,helloHex);

        byte[] decodedHex = DatatypeConverter.parseHexBinary(helloHex);
        String decodedString = new String(decodedHex, "UTF-8");
        System.out.printf("Hello decoded : %s\n", decodedString);


        String str = "p7+F+3ikvpLLQUEd6e4cMg==";
        Base64 base64 = new Base64();
        byte[] decode = base64.decode(str);
        System.out.println(DatatypeConverter.printHexBinary(decode));
        System.out.println(new String(decode));
    }

    private static void m3u8(){
        HttpClientUtil.refererThreadLocal.set("https://xxxxx/");
        String result =  M3U8Util.download(null, true);
        System.out.println(result);
    }

    private static void regex(){
        String fileNameRegex= "^[a-z0-9A-Z]+";
        String str = "6cee98f82bc9814b6ad3b4dcbb53adb4-sd-encrypt-stream.m3u8_da38f36d-25a0-4a16-a860-ada9c44ce8ee";
        Pattern pattern = Pattern.compile(fileNameRegex);
        Matcher matcher = pattern.matcher(str);
        // check all occurance
        while (matcher.find()) {
            System.out.println(matcher.group());
        }
    }

    private static void out(){


        System.out.println(System.getProperty("spring.output.ansi.enabled"));

//        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes); \u001b[2m2020-06-14
        try {
            String tm = "\u001B[2m2020-06-14 10:45:34.768\u001B[0;39m \u001B[32m INFO\u001B[0;39m \u001B[35m12860\u001B[0;39m \u001B[2m---\u001B[0;39m \u001B[2m[onPool-worker-3]\u001B[0;39m \u001B[36mcn.go.util.HttpClientUtil               \u001B[0;39m \u001B[2m:\u001B[0;39m ";
            System.out.println(tm);
            AnsiString ansiString = new AnsiString(tm);
            CharSequence plain = ansiString.getPlain();
            System.out.println(plain);

            System.out.println(Ansi.ansi().a(tm).reset());
            String s = StringEscapeUtils.escapeHtml4(tm);
            System.out.println(s);

            String replace = tm.replace("/\\u([0-9a-f]{3,4})/i", "&#x\\1;");
            System.out.println("replace: "+ replace);
            System.exit(0);
            PrintStream out = System.out;
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream pipedOutputStream1 = new PipedOutputStream(in);
            PrintStream printStream = new PrintStream(pipedOutputStream1);
            System.setOut(printStream);
            System.out.println("123123");

            byte[] bytes = new byte[1024];
            ByteBuffer buffer= ByteBuffer.allocate(256);
            in.read(bytes) ;
            String str =new String(bytes);
            System.err.println(str);
            java.util.Arrays.fill(bytes, 0, bytes.length ,(byte)0);
            out.println(str);
            System.out.println("123");
            in.read(bytes) ;
            System.err.println(new String(bytes));

           /* while (in.read(bytes) != -1){
                String str =new String(bytes);
                 System.out.println(str);
            }*/
          //  int read = in.read(bytes);
            //out.write(bytes);
            //向OutPutStream中写入，如 message.writeTo(baos);
           // System.out.println(str);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void jarFile (){
        File file = new File("D:/workspace/Idea/m3u8_download/target/m3u8-download-0.0.1-SNAPSHOT.jar!/BOOT-INF/classes!/ffmpeg-3.4.1.exe");
        InputStream resourceAsStream = CommonTest.class.getResourceAsStream("/ffmpeg-3.4.1.exe");
        try {
            String userDir = System.getProperty("user.dir");
            String ffmpegDir = userDir +"/ffmpeg-3.4.1.exe";
            FileOutputStream fileOutputStream = new FileOutputStream(ffmpegDir);
            FileInputStream fileInputStream = new FileInputStream(file);
            int copy = IOUtils.copy(fileInputStream, fileOutputStream);
            System.out.println(copy);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private static void str(){
        String numReg = "^[0-9]+";
        String str="2131-0";
        String str1="2131";
        System.out.println(str1.matches(numReg));
        System.out.println(numReg.matches(str1));
        System.out.println("java.net.UnknownHostException".contains("UnknownHost"));
    }

    public static String encrypt(String str, String key) {
        String KEY_ALGORITHM = "AES";
        // 加密模式为ECB，填充模式为NoPadding
//        String CIPHER_ALGORITHM = "AES/CBC/NoPadding";
        String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
//        String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
        // 字符集
        String ENCODING = "UTF-8";
        // 向量
        //String IV_SEED = "1234567812345678";
        String IV_SEED = "0000000000000000";
        try {
            if (str == null) {
                throw  new RuntimeException ("AES加密出错:Key为空null");
            }
            // 判断Key是否为16位
            if (key.length() != 16) {
                throw  new RuntimeException ("AES加密出错:Key长度不是16位");
            }
            byte[] raw = key.getBytes(ENCODING);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(IV_SEED.getBytes(ENCODING));
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);


            byte[] srawt = str.getBytes(ENCODING);
            /*int len = srawt.length;
            *//* 计算补空格后的长度 *//*
            while (len % 16 != 0)
                len++;
            byte[] sraw = new byte[len];
            *//* 在最后空格 *//*
            for (int i = 0; i < len; ++i) {
                if (i < srawt.length) {
                    sraw[i] = srawt[i];
                } else {
                    sraw[i] = 32;
                }
            }*/
            //byte[] encrypted = cipher.doFinal(sraw);
            byte[] encrypted = cipher.doFinal(srawt);
            return (new String(Base64.encodeBase64(encrypted), "UTF-8"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static SecretKey generateKey(String salt, String passphrase) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), hex(salt), 1000, 128);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            return key;
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return null;
        }
    }

    public static byte[] hex(String str) {
        try {
            return Hex.decodeHex(str.toCharArray());
        }
        catch (DecoderException e) {
            throw new IllegalStateException(e);
        }
    }


    public static String byteToHex(byte[] bytes){
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }

    private static void clietTest(){
        String m3u8URL = "";
        HttpClientUtil.setORIGIN("");
        HttpEntity httpEntity = new HttpClientUtil("").get(m3u8URL);
        String m3u8Txt = null;
        try {
            m3u8Txt = IOUtils.toString(httpEntity.getContent(), Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(m3u8Txt);
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    public static void main(String[] args) {
        byte[] bytes = "9fc8ce2a8ceadfaa".getBytes();

        System.out.println(bytes);
        java.net.URL  url = null;
        try {
            url = new java.net.URL("https://lajiao-bo.com/20190504/HsfS4Dpd/800kb/hls/index.m3u8 ");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.out.println(url.getProtocol());
        System.out.println(url.getAuthority());
    }

}
