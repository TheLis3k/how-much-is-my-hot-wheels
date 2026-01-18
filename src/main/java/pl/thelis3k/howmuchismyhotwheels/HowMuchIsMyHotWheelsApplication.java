package pl.thelis3k.howmuchismyhotwheels;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HowMuchIsMyHotWheelsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HowMuchIsMyHotWheelsApplication.class, args);
    }

}
