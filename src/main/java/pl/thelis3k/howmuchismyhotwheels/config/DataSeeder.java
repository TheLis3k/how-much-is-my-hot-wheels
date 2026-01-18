package pl.thelis3k.howmuchismyhotwheels.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final HotWheelsCarRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            HotWheelsCar car = HotWheelsCar.builder()
                    .name("Twin Mill")
                    .series("Legends of Speed")
                    .releaseYear(2024)
                    .build();

            repository.save(car);
            System.out.println("--------------------------------------");
            System.out.println("🏎️ SUCCESS! Pierwszy Hot Wheels w bazie: " + car.getName());
            System.out.println("--------------------------------------");
        }
    }
}