package cn.go.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.*;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpClientUtil {
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
    public static ThreadLocal<String> refererThreadLocal = new ThreadLocal<>();
    public static volatile String default_referer ;
    private  String referer ;

    private static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36";
    private static volatile String ORIGIN = "";

    public static String getORIGIN() {
        return ORIGIN;
    }

    public static void setORIGIN(String ORIGIN) {
        HttpClientUtil.ORIGIN = ORIGIN;
    }

    public String getReferer() {
        if(this.referer != null){
           return this.referer;
        }
        return default_referer;
    }

    public HttpClientUtil(String referer) {
        this.referer = referer;
    }

    public HttpClientUtil() {
    }

    public HttpEntity get(String url) {
        if (url != null) {
            url = url.trim();
        }
        HttpEntity result = null;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        if(getReferer() != null){
            httpGet.setHeader("Referer", getReferer());
        }
        // Sec-Fetch-Dest: empty
        //Accept: */*
        //Origin: https://course.weimiao.cn
        //Sec-Fetch-Site: cross-site
        //Sec-Fetch-Mode: cors
        //Referer: https://course.weimiao.cn/course/3/video/269
        //Accept-Encoding: gzip, deflate, br
        //Accept-Language: zh-CN,zh;q=0.9
        /*HttpHost proxy = new HttpHost("127.0.0.1", 8888);
        RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(proxy)
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(3000)
                .build();
        httpGet.setConfig(requestConfig);*/

        httpGet.setHeader("Sec-Fetch-Dest", "empty");
        httpGet.setHeader("Accept", "cors");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9");
        httpGet.setHeader("Sec-Fetch-Mode", "*/*");
        httpGet.setHeader("Sec-Fetch-Site", "cross-site");

        httpGet.setHeader("User-Agent", USER_AGENT);
        httpGet.setHeader("Origin", ORIGIN);
        try {
            CloseableHttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            logger.info("url={} ; statusCode= [{}] ", url, statusCode);
            if(HttpStatus.SC_FORBIDDEN == statusCode){
                throw new RuntimeException("403");
            }else if(HttpStatus.SC_MOVED_PERMANENTLY == statusCode || HttpStatus.SC_MOVED_TEMPORARILY == statusCode ){
                Header location = response.getFirstHeader("Location");
                String value = location.getValue();
                return get(value);
            }

            result = response.getEntity();
        } catch (IOException e) {
            boolean unknownHost = false;
            if(e.getClass() != null){
                unknownHost = e.getClass().toString().contains("UnknownHost");
            }
            if(unknownHost){
                logger.error(e.getClass().toString() + "," + url);
            }else{
                logger.error(e.getMessage(), e);
            }
        }
        return result;
    }

    public  byte[] getBytes(String url) {
        if (url != null) {
            url = url.trim();
        }
        byte[] result = null;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        int time_out = 30 * 60 * 1000;
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(time_out).setConnectTimeout(time_out).build();//设置请求和传输超时时间
        httpGet.setConfig(requestConfig);

        if(getReferer() != null){
            httpGet.setHeader("Referer", getReferer());
        }
        httpGet.setHeader("User-Agent", USER_AGENT);
        httpGet.setHeader("Origin", ORIGIN);
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

    public  byte[] httpUrl(String urlStr){
        //String referer = refererThreadLocal.get();
        Map<String,String> headerMap = new HashMap<>();
        int connTimeout = 30 * 60 * 1000;
        int readTimeout = 30 * 60 * 1000;

        if(getReferer() != null){
            headerMap.put("Referer", getReferer());
        }
        headerMap.put("User-Agent", USER_AGENT);
        headerMap.put("Origin", ORIGIN);
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

    public  byte[] getAESKey(String url) {
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

    public static String getDomain(String urlStr){
        URL  url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return  url.getProtocol() +"://" + url.getAuthority();
    }

}

