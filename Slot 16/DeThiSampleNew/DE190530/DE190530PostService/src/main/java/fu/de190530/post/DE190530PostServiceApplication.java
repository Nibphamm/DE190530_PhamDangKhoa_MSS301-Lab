package fu.de190530.post;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190530PostServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DE190530PostServiceApplication.class, args);
    }
}
