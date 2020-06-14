package cn.go;

//import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AppRun {

    public static void main(String[] args) {
//        SpringApplication.run(AppRun.class, args);
//        System.out.println(System.getProperty("spring.output.ansi.enabled"));
//        System.setProperty("spring.output.ansi.enabled", AnsiOutput.Enabled.NEVER.toString());
        SpringApplicationBuilder builder = new SpringApplicationBuilder(AppRun.class);
        builder.headless(false).run(args);
    }
}

