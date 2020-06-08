package cn.go;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AppRun {

    public static void main(String[] args) {
       // SpringApplication.run(AppRun.class, args);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AppRun.class);
        builder.headless(false).run(args);
    }
}

