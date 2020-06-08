package util;

import cn.go.util.AESFileUtil;
import cn.go.util.HttpClientUtil;
import cn.go.util.M3U8Util;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Encoder;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonTest {

    private static void httpDownloadKey(){
//        byte[] arrOutput = { 0xA7, 0xBF, 0x85, 0xFB, 0x78, 0xA4, 0xBE, 0x92, 0xCB, 0x41, 0x41, 0x1D, 0xE9, 0xEE, 0x1C, 0x32 };
        // p7+F+3ikvpLLQUEd6e4cMg==
        String url = "xxx";
        byte[] aesKey = HttpClientUtil.getAESKey(url);
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
        HttpClientUtil.refererThreadLocal.set("https://course.xxx.cn/course/");
        String result =  M3U8Util.download("", true);
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

    public static void main(String[] args) {
        regex();
    }
}
