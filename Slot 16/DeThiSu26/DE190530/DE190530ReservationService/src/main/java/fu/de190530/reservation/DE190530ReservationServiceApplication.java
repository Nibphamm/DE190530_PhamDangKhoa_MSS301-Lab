package fu.de190530.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DE190530ReservationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DE190530ReservationServiceApplication.class, args);
    }
}
