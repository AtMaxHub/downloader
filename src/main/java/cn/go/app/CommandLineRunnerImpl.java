package cn.go.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Component
public class CommandLineRunnerImpl implements CommandLineRunner {

    private static Logger logger = LoggerFactory.getLogger(CommandLineRunnerImpl.class);

    @Override
    public void run(String... args) throws Exception {
        logger.info("通过实现CommandLineRunner接口，在spring boot项目启动后打开浏览器");
        logger.info("如果没有自动打开浏览器，请自行开启浏览器并输入地址：[http://127.0.0.1:8080/index.html]");

        openDefaultBrowser("http://127.0.0.1:8080/index.html");
    }

    private static void openDefaultBrowser(String url) throws Exception {
        Desktop desktop = Desktop.getDesktop();
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            URI uri = new URI(url);
            desktop.browse(uri);
        }
    }

}
