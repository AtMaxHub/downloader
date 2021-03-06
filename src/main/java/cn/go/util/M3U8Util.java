package cn.go.util;

import cn.go.entity.M3U8;
import cn.go.entity.ReqDto;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8Util {
    private static Logger logger = LoggerFactory.getLogger(M3U8Util.class);

    private static int tryCount = 30;
    private static final String question_mark = "?";

    private static String prefix = "D:/all.download/java_download_m3u8/" ;
    private static String TEMP_DIR = prefix + "temp" + File.separator;
    private static String mergeTsPath ;

    private static volatile String fileName= null;
    private static String mp4File ;
    public static ConcurrentHashMap<String,String> urlRefererMap = new ConcurrentHashMap<>();


    private static final BlockingQueue<byte[]> ENCRYPT_BLOCKING_QUEUE = new LinkedBlockingQueue<>();
    // private static ConcurrentSkipListSet<Integer> seqList = new ConcurrentSkipListSet<>();
    private static final ConcurrentSkipListMap<Integer, byte[]> bytesMap = new ConcurrentSkipListMap<>();
    // 使用此map 会使用单线程
    private static final LinkedHashMap<String, byte[]> bytesLinkedMap = new LinkedHashMap<>();
    private static final Map<String, byte[]> conBytesMap = new ConcurrentHashMap<>();
    private static volatile boolean USE_LINKED_MAP = false;
    private static final ConcurrentSkipListMap<Integer, File> filesMap = new ConcurrentSkipListMap<>();


    public static String download(ReqDto reqDto){
        String fileName = reqDto.getFileName();
        if(org.apache.commons.lang3.StringUtils.isNotBlank(fileName)){
            String fileDir = TEMP_DIR + fileName;
            File file = new File(TEMP_DIR);
            File[] files = file.listFiles();
            int count = 0;
            if(files != null && files.length > 1){
                for (File fileEle : files) {
                    if(fileEle.isDirectory()){
                        continue;
                    }
                    String name = fileEle.getName();
                    if(name.contains(fileName)){
                        count++;
                    }
                }
            }
            count++;
            fileName = fileName + "-" + count;
            reqDto.setFileName(fileName);
        }
        return download(reqDto, false);
    }

    /**
     * 大文件暂存磁盘; 小文件放内存
     */
    public static String download(ReqDto reqDto,  boolean bigFile){
        String url = reqDto.getM3u8Url();

        if(org.apache.commons.lang3.StringUtils.isBlank(url)){
            logger.error("url_is_blank");
            return "url_is_blank";
        }
        String referer = reqDto.getReferer();

        // 1：解析m3u8 文件提取 aes-key 和 tsUrl; 同时日志记录 文件内容
        int tryTimes = 0;
        M3U8 m3U8 = getM3U8ByURL(url, referer);
        while (true){
            if(m3U8 == null && tryTimes < tryCount){
                try {
                    TimeUnit.SECONDS.sleep( tryTimes);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("tryTimes=[{}]; m3u8_url=[{}]", tryTimes, url);
                m3U8 = getM3U8ByURL(url,referer);
                tryTimes++;
            }else{
                break;
            }
        }
        if(m3U8 == null){
            logger.error("url = {}, getM3U8ByURL_is_null", url);
            return "从 url 解析 m3u8 失败，url=" + url;
        }

        // 2：下载 aes-key
        if(m3U8.getAES_KEY_URL() != null){
            byte[] AES_KEY = new HttpClientUtil(referer).getAESKey(m3U8.getAES_KEY_URL());
            m3U8.setAES_KEY(AES_KEY);
        }
        String mergeTsPathTempLate = TEMP_DIR  + "${FILE_NAME}_" + DateFormatUtils.format(new Date(), "yyyy.MM.dd.HH.mm.ss") +".ts";
        if(bigFile){
            File tfile = new File(TEMP_DIR);
            if (!tfile.exists()) {
                tfile.mkdirs();
            }
            downloadTs2File(m3U8);
            boolean mergeFiles = mergeFiles(tfile.listFiles(), mergeTsPathTempLate);
            logger.info("merge_file: {} , return= {}" , mergeTsPathTempLate, mergeFiles);
        }else {
            // 3：并行下载 ts， 并解密 放入 bytesMap
            gainTs2Memory(m3U8);
            // 4：合并 bytesMap，输出到 MP4 文件
            mergeMemBytes(m3U8.getUrlList(), reqDto.getFileName(), mergeTsPathTempLate);
        }
        convert(reqDto.getBitRate());
        return null;
    }


    private static byte[] downloadTs(String  tsUrl, M3U8 m3u8Entity){
        byte[] decryptedBytes = null;
        int tmpTryCount = tryCount;
        int tmpTryTimes = 0;
        while (true){
            if(decryptedBytes == null && tmpTryTimes <= tmpTryCount){
                try {
                    try {
                        TimeUnit.SECONDS.sleep( tmpTryTimes);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    HttpEntity httpEntity = new HttpClientUtil(m3u8Entity.getReferer()).get(tsUrl);
                    if(httpEntity == null){
                        tmpTryTimes ++;
                        continue;
                    }
                    byte[] inputBytes = IOUtils.toByteArray(httpEntity.getContent());
                    if(inputBytes != null){
                        logger.info("url={} ; inputStream.read [{}] bytes", tsUrl, inputBytes.length);
                    }else{
                        logger.info("url={} ; inputStream.read_null", tsUrl);
                    }
//                    byte[] inputBytes = new HttpClientUtil(m3u8Entity.getReferer()).httpUrl(tsUrl);

                    if(m3u8Entity.getAES_KEY() != null){
                        decryptedBytes = AESFileUtil.decryptedBytes(m3u8Entity.getAES_KEY(), m3u8Entity.getAES_IV(), inputBytes);
                    }else {
                        decryptedBytes = inputBytes;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    if(e.getMessage() != null){
                        boolean unknownHost = e.getMessage().contains("UnknownHost");
                        if(unknownHost){
                            tmpTryCount = 500;
                        }
                    }
                }
                if(decryptedBytes == null){
                    logger.error("decryptedBytes_is_null, tmpTryTimes=[{}], tsUrl=[{}]",tmpTryTimes, tsUrl);
                }
                tmpTryTimes++;
            }else {
                break;
            }
        }

        return decryptedBytes;
    }

    private static void gainTs2Memory(M3U8 m3u8Entity){
        String basePath = m3u8Entity.getBasepath();

        if(USE_LINKED_MAP){
            m3u8Entity.getTsList().forEach(m3U8Ts -> {
                String tsUrl = basePath + m3U8Ts.getUrl();
                byte[] decryptedBytes = downloadTs(tsUrl, m3u8Entity);
                if(decryptedBytes == null){
                    return;
                }
                bytesLinkedMap.put(tsUrl, decryptedBytes);
            });
        }else {
            m3u8Entity.getTsList().stream().parallel().forEach(m3U8Ts -> {
                try {
                    String tsUrl = m3U8Ts.getUrl();
                    byte[] decryptedBytes = downloadTs(tsUrl, m3u8Entity);
                    if(decryptedBytes == null){
                        return;
                    }
                    conBytesMap.put(tsUrl, decryptedBytes);

                    /*byte[] inputBytes = new HttpClientUtil(m3u8Entity.getReferer()).httpUrl(tsUrl);
                    byte[] decryptedBytes;
                    if(AES_KEY != null){
                        decryptedBytes = AESFileUtil.decryptedBytes(AES_KEY, AES_IV, inputBytes);
                    }else {
                        decryptedBytes = inputBytes;
                    }
                    if(decryptedBytes == null){
                        logger.error("decryptedBytes_is_null, tsUrl= {}", tsUrl);
                        return;
                    }
                    Integer tsSeq = Integer.valueOf(getSeq(tsUrl));
                    bytesMap.put(tsSeq, decryptedBytes);*/

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
        }
        logger.info( "[{}] 文件下载完毕!", fileName);
    }

    public static void downloadTs2File(M3U8 m3u8Entity){
        String basePath = m3u8Entity.getBasepath();
        // .stream().parallel()
        m3u8Entity.getTsList().forEach(m3U8Ts ->
        {
            File file = new File(TEMP_DIR + m3U8Ts.getFile());
            if(file.exists()){
                // 下载过的就不管了
                logger.info("{} : exists",  m3U8Ts.getFile());
                return;
            }

            FileOutputStream fos = null;
            InputStream inputStream = null;
            try {
                String tsUrl = basePath + m3U8Ts.getUrl();
                byte[] bytes = new HttpClientUtil(m3u8Entity.getReferer()).getBytes(tsUrl);
                byte[] decryptedBytes;
                if(m3u8Entity.getAES_KEY() != null){
                    decryptedBytes = AESFileUtil.decryptedBytes(m3u8Entity.getAES_KEY(), m3u8Entity.getAES_IV(), bytes);
                }else {
                    decryptedBytes = bytes;
                }
                if(decryptedBytes == null){
                    logger.error("decryptedBytes_is_null, tsUrl= {} \n", tsUrl);
                    return;
                }
                // 会自动创建文件
                fos = new FileOutputStream(file);
                IOUtils.write(decryptedBytes, fos);
                Integer tsSeq = Integer.valueOf(getSeq(tsUrl));
                filesMap.put(tsSeq, file);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                // 关流
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        logger.info( "{} 文件下载完毕!", fileName);
    }

    private static String buildFileName(String m3u8URL){
        if(!m3u8URL.contains("?")){
            return System.currentTimeMillis()+"";
        }
        String tmpFileName = m3u8URL.substring(m3u8URL.lastIndexOf("/") + 1, m3u8URL.indexOf("?"));
        String fileNameRegex = "^[a-z0-9A-Z]+";
        Pattern pattern = Pattern.compile(fileNameRegex);
        Matcher matcher = pattern.matcher(tmpFileName);
        if(matcher.find()){
            return matcher.group();
        }
        return tmpFileName;
    }

    public static M3U8 getM3U8ByURL(String m3u8URL, String  referer) {

        if(org.apache.commons.lang3.StringUtils.isBlank(m3u8URL)){
            logger.error("m3u8URL_is_null");
            return null;
        }
        try {
            String basePath = m3u8URL.substring(0, m3u8URL.lastIndexOf("/") + 1);
            fileName = buildFileName(m3u8URL);
            M3U8 m3U8 = new M3U8();
            m3U8.setBasepath(basePath);
            m3U8.setReferer(referer);

            HttpEntity httpEntity = new HttpClientUtil(referer).get(m3u8URL);
            if(httpEntity == null){
                logger.error("m3u8URL=[{}], get_m3u8_txt_return_null", m3u8URL);
                return null;
            }
            boolean needResetBasePath = false;

            String m3u8Txt = IOUtils.toString(httpEntity.getContent(), Charset.defaultCharset());
            logger.info("m3u8URL={}; m3u8Txt={}",m3u8URL, m3u8Txt);
            String[] splitTxt = m3u8Txt.split("\n");

            float seconds = 0;
            int mIndex;
            for (String line: splitTxt){
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
                                String aes_key_url = s1.split("=", 2)[1];
                                if(aes_key_url != null){
                                    aes_key_url = aes_key_url.replaceAll("\"","");
                                    if(aes_key_url.startsWith("/") && !(aes_key_url.contains("http"))){
                                        aes_key_url = HttpClientUtil.getDomain(m3u8URL) + aes_key_url;
                                    }
                                    m3U8.setAES_KEY_URL(aes_key_url);
                                }

                                continue;
                            }
                            if (s1.contains("IV")){
                                String aes_iv = s1.split("=", 2)[1];
                                m3U8.setAES_IV(aes_iv);
                            }

                        }
                    }
                    continue;
                }
                if (line.endsWith("m3u8")) {
                    return getM3U8ByURL(basePath + line, referer);
                }
                if(line.startsWith("/") && !needResetBasePath ){
                    needResetBasePath = true;
                    java.net.URL  url = new java.net.URL(basePath);
                    basePath =  url.getProtocol() +"://" + url.getAuthority();
                    m3U8.setBasepath(basePath);
                }
                /*if(!USE_LINKED_MAP){
                    boolean contains = line.contains("-");
                    if(!contains){
                        USE_LINKED_MAP = true;
                    }else {
                        String seqStr = line.substring(line.lastIndexOf("-") + 1);
                        String numReg = "^[0-9]+";
                        boolean matches = seqStr.matches(numReg);
                        if(!matches){
                            USE_LINKED_MAP = true;
                        }
                    }
                }*/
                else if(line.startsWith("http")){
                    m3U8.addUrl(line);
                }else{
                    m3U8.addUrl(basePath + line);
                }


                if(line.contains(question_mark)){
                    String file = line.substring(0, line.indexOf(question_mark));
                    m3U8.addTs(new M3U8.Ts(line, file, seconds));
                } else {
                    m3U8.addTs(new M3U8.Ts(line, line, seconds));
                }
                seconds = 0;
            }


            return m3U8;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }


    private static void mergeMemBytes(List<String> urlList, String customFileName, String resultPath) {
        String  diskFilePath =  resultPath.replace("${FILE_NAME}", fileName);
        if(org.apache.commons.lang3.StringUtils.isNotBlank(customFileName)){
            diskFilePath = resultPath.replace("${FILE_NAME}", customFileName);
        }
        mergeTsPath = diskFilePath;
        mp4File = diskFilePath.replace(".ts", ".mp4");

        File resultFile = new File(diskFilePath);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(resultFile, true);
           /* if(USE_LINKED_MAP){
                bytesLinkedMap.forEach((seqNo, bytes) -> {
                    try {
                        fileOutputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                bytesLinkedMap.clear();
            }else {
                bytesMap.forEach((seqNo, bytes) -> {
                    try {
                        fileOutputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                bytesMap.clear();
            }*/

            urlList.forEach((urlKey)->{
                try {
                    byte[] bytes = conBytesMap.get(urlKey);
                    fileOutputStream.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            conBytesMap.clear();

            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            logger.error("==>> {} 文件合并异常", fileName);
            return ;
        }
        logger.info("[{}] 文件合并完毕", fileName);
    }

    /**
     * 合并并删除临时 ts 文件
     */
    private static boolean mergeFiles(File[] files, String resultPath) {
        if(fileName != null){
            resultPath = resultPath.replace("${FILE_NAME}", fileName);
            mergeTsPath = resultPath;
            mp4File = resultPath.replace(".ts", ".mp4");
        }
        File resultFile = new File(resultPath);
        List<File> tempFileList = new ArrayList<>();
        try {
            FileOutputStream outputStream = new FileOutputStream(resultFile, true);
            FileChannel resultFileChannel = outputStream.getChannel();
            for (Map.Entry<Integer, File> fileEntry : filesMap.entrySet()) {
                FileInputStream tfs  = new FileInputStream(fileEntry.getValue());
                FileChannel blk = tfs.getChannel();
                resultFileChannel.transferFrom(blk, resultFileChannel.size(), blk.size());
                blk.close();
                tfs.close();
                tempFileList.add(fileEntry.getValue());
            }
            resultFileChannel.close();
            outputStream.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        for (File file : tempFileList) {
            boolean delete = file.delete();
            logger.info("{} deleted= {}" , file.getName(), delete);
        }
        logger.info("[{}] 文件合并完毕", fileName);
        return true;
    }


    private static String parseFFmepgPath(URL resource){
        if(resource == null){
            return null;
        }
        String absolutePath = null;
        try {
            absolutePath = new File(resource.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            logger.error("ffmpeg_not_found" + e.getMessage(),e);
            logger.error("try execute command [ffmpeg -y -i xx\\input.ts -vcodec copy -acodec copy -vbsf h264_mp4toannexb xx\\out.mp4] to complete the code conversion" );
        }
        return absolutePath;
    }

    private static void convert(String bitRate){
        URL resource = ClassLoader.getSystemResource("ffmpeg-3.4.1.exe");
        String absolutePath  = parseFFmepgPath(resource);

        if(absolutePath == null){
            resource = ClassLoader.getSystemResource("/ffmpeg-3.4.1.exe");
            absolutePath = parseFFmepgPath(resource);
        }

        if(absolutePath == null){
            InputStream inputStream = M3U8Util.class.getResourceAsStream("/ffmpeg-3.4.1.exe");
            absolutePath = parseFFmepgPath(resource);

            String userDir = System.getProperty("user.dir");
            String ffmpegDir = userDir +"/ffmpeg-3.4.1.exe";
            File file = new File(ffmpegDir);
            if(!file.exists()){
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(ffmpegDir);
                    int copy = IOUtils.copy(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();
                    logger.info("copy=[{}]", copy);
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage(), e);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(file.exists()){
                boolean canExecute = file.canExecute();
                logger.info("[{}],canExecute=[{}]",file.getAbsolutePath(), canExecute);
                absolutePath = ffmpegDir;
            }
        }

        if(absolutePath == null){
            throw  new RuntimeException("ffmpeg_not_found, ts_file=" + mergeTsPath);
        }

        logger.info("absolutePath=[{}]", absolutePath);
        String h264 = absolutePath + " -y -i ${TS_FILE} -vcodec copy -acodec copy -vbsf h264_mp4toannexb ${MP4_FILE}";

        // 这个方式转换慢但是生成的 mp4 文件小
        String aac = absolutePath +" -y -i ${TS_FILE} -c:v libx264 -c:a copy -bsf:a aac_adtstoasc ${MP4_FILE}";
        String bitStr = aac;
        String h264Str = "h264";
        String aacStr = "aac";

        if(mp4File == null){
            throw new RuntimeException("mp4File_is_null");
        }

        if(h264Str.equalsIgnoreCase(bitRate)){
            bitStr = h264.replace("${TS_FILE}", mergeTsPath).replace("${MP4_FILE}", mp4File);
        }else{
            bitStr = aac.replace("${TS_FILE}", mergeTsPath).replace("${MP4_FILE}", mp4File);
        }

        logger.info("ffmpeg_cmd=[{}]", bitStr);

        StringTokenizer st = new StringTokenizer(bitStr);
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
        File file = new File(mergeTsPath);
        if(file.exists()){
            boolean delete = file.delete();
            logger.info("[{}], delete=[{}]", mergeTsPath, delete);
        }

        logger.info("[{}] 转换完成", mp4File);
    }


    private static String getSeq(String tsUrl){
        if(tsUrl == null){
            return null;
        }
        String tmp = tsUrl.substring(0, tsUrl.indexOf(".ts"));
        return tmp.substring(tmp.lastIndexOf("-") + 1);
    }

}
