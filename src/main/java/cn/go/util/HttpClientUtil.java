package cn.go.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpClientUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    public static ThreadLocal<String> refererThreadLocal = new ThreadLocal<>();
    public static volatile String referer = "";
    private static int time_out = 30 * 60 * 1000;

    public static HttpEntity get(String url) {
        if (url != null) {
            url = url.trim();
        }
        HttpEntity result = null;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        //String referer = refererThreadLocal.get();
        if(referer != null){
            httpGet.setHeader("Referer", referer);
        }
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            result = response.getEntity();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public static byte[] getBytes(String url) {
        if (url != null) {
            url = url.trim();
        }
        byte[] result = null;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(time_out).setConnectTimeout(time_out).build();//设置请求和传输超时时间
        httpGet.setConfig(requestConfig);

        //String referer = refererThreadLocal.get();
        if(referer != null){
            httpGet.setHeader("Referer", referer);
        }
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            result = IOUtils.toByteArray(response.getEntity().getContent());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    private static HttpEntity post(String url, HttpEntity entity) {
        HttpEntity result = null;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        post.setEntity(entity);
        try {
            CloseableHttpResponse response = client.execute(post);
            result = response.getEntity();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public static byte[] httpUrl(String urlStr, Map<String,String> headerMap, int connTimeout, int readTimeout){
        InputStream inputStream = null;
        byte[] inputBytes = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if(headerMap != null && headerMap.size() > 0){
                Set<String> keySet = headerMap.keySet();
                for (String key : keySet) {
                    conn.setRequestProperty(key, headerMap.get(key));
                }
            }
            conn.setConnectTimeout(connTimeout);
            conn.setReadTimeout(readTimeout);
            int responseCode = conn.getResponseCode();
            if(HttpURLConnection.HTTP_OK != responseCode){
                logger.info("url={}; HttpURLConnection,http_code= {}" ,  url, responseCode);
            }
            inputStream = conn.getInputStream();
            inputBytes = IOUtils.toByteArray(inputStream);
            logger.info("url={} ; HttpURLConnection.inputStream.read [{}] bytes", url, inputBytes.length);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {// 关流
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return inputBytes;
    }

    public static byte[] httpUrl(String urlStr){
        //String referer = refererThreadLocal.get();
        Map<String,String> headerMap = new HashMap<>();
        int connTimeout = 30 * 60 * 1000;
        int readTimeout = 30 * 60 * 1000;
        headerMap.put("Referer", referer);
        return httpUrl(urlStr, headerMap, connTimeout, readTimeout );
    }

    public static String doPost(String url, HttpEntity entity) {
        String result = null;
        String defaultCharset = "utf-8";
        HttpEntity res = post(url, entity);
        try {
            result = EntityUtils.toString(res, defaultCharset);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] getAESKey(String url) {
        byte[] result = null;
        InputStream inputStream = null;
        HttpEntity res = get(url);
        try {
            inputStream = res.getContent();
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            byte[] buff = new byte[1024 * 1024];
            int len;
            while ((len = inputStream.read(buff)) != -1) {
                byteArray.write(buff, 0, len);
                //byteArray.flush();
            }
            result = byteArray.toByteArray();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }


    private static void m3u8() {
        // https://fangdaoproduct.yaocaiwuziyou.com/00a98404c80a4bb6bcbd0affa93af57d/09bb78f29c6de8011cd02ed27ebff719-sd-encrypt-stream.m3u8?auth_key=1589600860-448a8b9cb58644678667f033a61ad013-0-356159d8555842f5381498f1d55510b0
        String url = "https://fangdaoproduct.yaocaiwuziyou.com/00a98404c80a4bb6bcbd0affa93af57d/09bb78f29c6de8011cd02ed27ebff719-sd-encrypt-stream.m3u8?auth_key=1589600860-448a8b9cb58644678667f033a61ad013-0-356159d8555842f5381498f1d55510b0";
        HttpEntity httpEntity = get(url);
        try {
            String res = EntityUtils.toString(httpEntity);
            System.out.println(res);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

