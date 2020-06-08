package cn.go.entity;

import javax.validation.constraints.NotBlank;

public class ReqDto {
    @NotBlank(message = "m3u8Url 不能为空")
    private String m3u8Url;
    private String referer;

    public String getM3u8Url() {
        return m3u8Url;
    }

    public void setM3u8Url(String m3u8Url) {
        this.m3u8Url = m3u8Url;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    @Override
    public String toString() {
        return "ReqDto{" +
                "m3u8Url='" + m3u8Url + '\'' +
                ", referer='" + referer + '\'' +
                '}';
    }
}