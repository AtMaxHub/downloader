package cn.go.controller;

import cn.go.entity.ReqDto;
import cn.go.util.AESFileUtil;
import cn.go.util.HttpClientUtil;
import cn.go.util.M3U8Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.Map;

@Controller
public class M3u8Controller {
    private static Logger logger = LoggerFactory.getLogger(M3u8Controller.class);

    @RequestMapping("/ctrl")
    @ResponseBody
    public String ctrl() {
        return "Hello World";
    }

    @RequestMapping("/index")
    public String index(Map<String, Object> map) {
        map.put("name","zhongwen_中文..");
        return "m3u8";
    }

    @RequestMapping("/download")
    @ResponseBody
    public String download(@Valid ReqDto req, Map<String, Object> map) {
        logger.info(req.toString());
        if(org.apache.commons.lang3.StringUtils.isNotBlank(req.getReferer()) ){
            boolean httpNotExists = !req.getReferer().startsWith("http");
            if(httpNotExists){
                boolean keyBlank = StringUtils.isBlank(req.getKey());
                if(keyBlank){
                    return "请输入key";
                }

                String key = req.getKey();
                boolean notTimesOf8 = key.length() % 8 != 0;
                if(notTimesOf8){
                    return "key 长度不是 8 的倍数，请检查！";
                }
                String referer = AESFileUtil.decryptStr(key, null, req.getReferer());
                if(!referer.startsWith("http")){
                    return "referer="+req.getReferer();
                }
                req.setReferer(referer);
            }
        }
        String result = null;
        String referer = req.getReferer();
        try {
            java.net.URL  url = new java.net.URL(referer);
            String origin = url.getProtocol() +"://" + url.getAuthority();
            HttpClientUtil.setORIGIN(origin);
            HttpClientUtil.default_referer= req.getReferer();
            result = M3U8Util.download(req);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }
}
