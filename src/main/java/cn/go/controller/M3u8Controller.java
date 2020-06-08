package cn.go.controller;

import cn.go.entity.ReqDto;
import cn.go.util.HttpClientUtil;
import cn.go.util.M3U8Util;
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
        logger.info(map.toString());
        logger.info(req.toString());
        HttpClientUtil.referer= req.getReferer();
        String result = null;
        try {
            result = M3U8Util.download(req.getM3u8Url());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }
}
