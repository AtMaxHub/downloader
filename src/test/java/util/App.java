package util;

import cn.go.entity.M3U8;
import cn.go.util.AESFileUtil;
import cn.go.util.HttpClientUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 1：解析m3u8 文件提取 aes-key 和 tsUrl; 同时日志记录 文件内容
 * 2：下载 aes-key
 * 3：并行下载 ts， 并解密 放入 bytesMap; 小文件使用内存;大文件落盘
 * 4：合并 bytesMap，输出到 MP4 文件
 * 5：ffmpeg 转换
 */
public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static String AES_KEY_URL ;
    private static byte[] AES_KEY ;
    private static String AES_IV ;

    private static final BlockingQueue<byte[]> ENCRYPT_BLOCKING_QUEUE = new LinkedBlockingQueue<>();
    private static final ConcurrentSkipListMap<Integer, byte[]> bytesMap = new ConcurrentSkipListMap();
    // private ConcurrentSkipListSet<File> fileList = new ConcurrentSkipListSet<>(Comparator.comparingInt(file -> Integer.parseInt(file.getName().replace(".xyz", ""))));
    private static ConcurrentSkipListSet<Integer> seqList = new ConcurrentSkipListSet<>();

    private static final String question_mark = "?";
    private static final String referer = "https://course.xxx.cn/course/";

    private static String prefix = "D:/all.download/java_download_m3u8/" ;
    private static String TEMP_DIR = prefix + "temp";
    private static String mergeFilePath = TEMP_DIR + File.separator  + "${FILE_NAME}_" + UUID.randomUUID().toString() +".ts";
    private static int connTimeout = 30 * 60 * 1000;
    private static int readTimeout = 30 * 60 * 1000;
    public static String url = "http://playertest.longtailvideo.com/adaptive/bipbop/gear4/prog_index.m3u8";
    private static volatile String fileName= null;
    private static String mp4File ;


    static String loadStream(InputStream in) {
        int ptr = 0;
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            while ((ptr = reader.read()) != -1) {
                buffer.append((char) ptr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public static void main(String[] args) {
        download();
    }

    private static void download(){
        File tfile = new File(TEMP_DIR);
        if (!tfile.exists()) {
            tfile.mkdirs();
        }

        // 1：解析m3u8 文件提取 aes-key 和 tsUrl; 同时日志记录 文件内容
        M3U8 m3U8 = getM3U8ByURL(url);
        if(m3U8 == null){
            logger.error("getM3U8ByURL_is_null");
            return;
        }

        // 2：下载 aes-key
        if(AES_KEY_URL != null){
             AES_KEY = HttpClientUtil.getAESKey(AES_KEY_URL);
        }
        // 3：并行下载 ts， 并解密 放入 bytesMap
        //downloadM3U82File(m3U8);
        gainM3U82Memory(m3U8);
        // 4：合并 bytesMap，输出到 MP4 文件
//        mergeFiles(tfile.listFiles(), mergeFilePath);
        mergeMemBytes(mergeFilePath);

        convert();
    }



    // 9740748badc5120793485285d3a889a7-sd-encrypt-stream-00001.ts?auth_key=1589600860-448a8b9cb58644678667f033a61ad013-0-54b59c473968c5a2e6ec5c2b5d144a8a
    //System.out.println(getSeq("9740748badc5120793485285d3a889a7-sd-encrypt-stream-00001.ts?auth_key=1589600860-448a8b9cb58644678667f033a61ad013-0-54b59c473968c5a2e6ec5c2b5d144a8a"));
    private static String getSeq(String tsUrl){
        if(tsUrl == null){
            return null;
        }
        String tmp = tsUrl.substring(0, tsUrl.indexOf(".ts"));
        return tmp.substring(tmp.lastIndexOf("-") + 1);
    }

    private static void gainM3U82Memory(M3U8 m3U8Eneity){
        String basePath = m3U8Eneity.getBasepath();
        // parallel()
        m3U8Eneity.getTsList().stream().parallel().forEach(m3U8Ts -> {

            InputStream inputStream = null;
            try {
                String tsUrl = basePath + m3U8Ts.getUrl();
                URL url = new URL(tsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Referer", referer);
                conn.setConnectTimeout(connTimeout);
                conn.setReadTimeout(readTimeout);
                if (conn.getResponseCode() == 200) {

                    inputStream = conn.getInputStream();
                    byte[] inputBytes = IOUtils.toByteArray(inputStream);
                    logger.info("ts_url={} , inputStream.read [{}] bytes", tsUrl, inputBytes.length);
                    byte[] decryptedBytes = AESFileUtil.decryptedBytes(AES_KEY, AES_IV, inputBytes);
                    if(decryptedBytes == null){
                        logger.error("decryptedBytes_is_null, tsUrl= {}", tsUrl);
                        System.exit(100);
                    }
                    Integer tsSeq = Integer.valueOf(getSeq(tsUrl));
                    seqList.add(tsSeq);
                    bytesMap.put(tsSeq, decryptedBytes);
                }else{
                    logger.info("ts_url={} : http_code= {}" ,  tsUrl, conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
            } finally {// 关流
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        logger.info( "{} 文件下载完毕!", fileName);
    }

    public static void downloadM3U82File(M3U8 m3U8Eneity){
        String basePath = m3U8Eneity.getBasepath();
        m3U8Eneity.getTsList().stream().parallel().forEach(m3U8Ts -> {
            File file = new File(TEMP_DIR + File.separator + m3U8Ts.getFile());
            if(file.exists()){
                // 下载过的就不管了
                logger.info("{} : exists",  m3U8Ts.getFile());
                return;
            }

            FileOutputStream fos = null;
            InputStream inputStream = null;
            try {
                String tsUrl = basePath + m3U8Ts.getUrl();
                URL url = new URL(tsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Referer", referer);
                conn.setConnectTimeout(connTimeout);
                conn.setReadTimeout(readTimeout);
                if (conn.getResponseCode() == 200) {
                    logger.info("ts_url={} : ok" ,  tsUrl);
                    inputStream = conn.getInputStream();
                    fos = new FileOutputStream(file);// 会自动创建文件
                    int len = 0;
                    byte[] buf = new byte[1024];
                    while ((len = inputStream.read(buf)) != -1) {
                        fos.write(buf, 0, len);// 写入流中
                    }
                }else{
                    logger.info("ts_url={} : http_code= {}" ,  tsUrl, conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {// 关流
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {e.printStackTrace();}
            }
        });
        logger.info( "{} 文件下载完毕!", fileName);
    }

    public static M3U8 getM3U8ByURL(String m3u8URL) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(m3u8URL).openConnection();
            conn.setRequestProperty("Referer", referer);
            if(conn.getResponseCode() != 200){
                logger.info("from_m3u8_url_code= {}, m3u8_url={}", conn.getResponseCode(), m3u8URL);
                return null;
            }

            String realUrl = conn.getURL().toString();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String basePath = realUrl.substring(0, realUrl.lastIndexOf("/") + 1);
            fileName = realUrl.substring(realUrl.lastIndexOf("/") + 1, realUrl.indexOf("?"));
            M3U8 ret = new M3U8();
            ret.setBasepath(basePath);

            String line;
            float seconds = 0;
            int mIndex;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
                if (line.startsWith("#")) {
                    if (line.startsWith("#EXTINF:")) {
                        line = line.substring(8);
                        if ((mIndex = line.indexOf(",")) != -1) {
                            line = line.substring(0, mIndex);
                        }
                        try {
                            logger.info("line={}", line);
                            seconds = Float.parseFloat(line);
                        } catch (Exception e) {
                            seconds = 0;
                        }
                    }else if(line.startsWith("#EXT-X-KEY:")){
                        String[] split1 = line.split(",");
                        for (String s1 : split1) {
                            if (s1.contains("METHOD")) {
                                String method = s1.split("=", 2)[1];
                                continue;
                            }
                            if (s1.contains("URI")) {
                                AES_KEY_URL = s1.split("=", 2)[1];
                                if(AES_KEY_URL != null){
                                    AES_KEY_URL = AES_KEY_URL.replaceAll("\"","");
                                }
                                continue;
                            }
                            if (s1.contains("IV"))
                                AES_IV = s1.split("=", 2)[1];
                        }
                    }
                    continue;
                }
                if (line.endsWith("m3u8")) {
                    return getM3U8ByURL(basePath + line);
                }
                if(line.contains(question_mark)){
                    String file = line.substring(0, line.indexOf(question_mark));
                    ret.addTs(new M3U8.Ts(line, file, seconds));
                } else {
                    ret.addTs(new M3U8.Ts(line, line, seconds));
                }
                seconds = 0;
            }
            reader.close();
            return ret;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    private static boolean mergeMemBytes( String resultPath) {
        if(fileName != null){
            resultPath = resultPath.replace("${FILE_NAME}", fileName);
            mergeFilePath = resultPath;
            mp4File = resultPath.replace(".ts", ".mp4");
        }
        File resultFile = new File(resultPath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(resultFile, true);
            FileChannel resultFileChannel = fileOutputStream.getChannel();
            bytesMap.forEach((seqNo, bytes) -> {
                try {
                    fileOutputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fileOutputStream.flush();
            fileOutputStream.close();
            resultFileChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        logger.info("==>> {} 文件合并完毕", fileName);
        return true;
    }

    private static boolean mergeFiles(File[] fpaths, String resultPath) {
        if (fpaths == null || fpaths.length < 1) {
            return false;
        }

        if (fpaths.length == 1) {
            return fpaths[0].renameTo(new File(resultPath));
        }
        for (int i = 0; i < fpaths.length; i++) {
            if (!fpaths[i].exists() || !fpaths[i].isFile()) {
                logger.error("fpaths[{}], not_exist", i );
                return false;
            }
        }
        File resultFile = new File(resultPath);

        try {
            FileOutputStream fs = new FileOutputStream(resultFile, true);
            FileChannel resultFileChannel = fs.getChannel();
            FileInputStream tfs;
            for (int i = 0; i < fpaths.length; i++) {
                tfs = new FileInputStream(fpaths[i]);
                FileChannel blk = tfs.getChannel();
                resultFileChannel.transferFrom(blk, resultFileChannel.size(), blk.size());
                tfs.close();
                blk.close();
            }
            fs.close();
            resultFileChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        for (File fpath : fpaths) {
            fpath.delete();
        }
        logger.info("==>> {} 文件合并完毕", fileName);
        return true;
    }

    private static void convert(){

        URL resource = ClassLoader.getSystemResource("ffmpeg-3.4.1.exe");
        String absolutePath = null;
        if(resource != null){
            try {
                absolutePath = new File(resource.toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        // ffmpeg -y -i D:\all.download\java_download_m3u8\temp\ok.ts -vcodec copy -acodec copy -vbsf h264_mp4toannexb D:\all.download\java_download_m3u8\temp\out.mp4
        String h264 = absolutePath + " -y -i ${TS_FILE} -vcodec copy -acodec copy -vbsf h264_mp4toannexb ${MP4_FILE}";

        // 这个方式转换慢但是生成的MP4文件小
        String acc = absolutePath +" -y -i ${TS_FILE} -c:v libx264 -c:a copy -bsf:a aac_adtstoasc ${MP4_FILE}";

        if(mp4File != null){
            acc = acc.replace("${TS_FILE}" ,mergeFilePath).replace("${MP4_FILE}", mp4File);
        }else{
            throw new RuntimeException("mp4File_is_null");
        }
        StringTokenizer st = new StringTokenizer(acc);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++){
            cmdarray[i] = st.nextToken();
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder().redirectErrorStream(true);
            Process process = processBuilder.command(cmdarray).start();
            InputStream inputStream = process.getInputStream();
            String inputStr = IOUtils.toString(inputStream, Charset.defaultCharset());
            logger.info(inputStr);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }


}
