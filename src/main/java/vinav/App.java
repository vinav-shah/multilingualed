package vinav;

import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class App implements CommandLineRunner {

    @Resource
    FilesStorageService storageService;

    public static void main(String[] args) {

        SpringApplication.run(App.class, args);
        System.setProperty("jasypt.encryptor.password", "mleditions");

    }

    @Override
    public void run(String... arg) throws Exception {
        storageService.init();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}