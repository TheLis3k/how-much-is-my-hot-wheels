package pl.thelis3k.howmuchismyhotwheels.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.FandomScraper;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final HotWheelsCarRepository repository;

    private final FandomScraper scraper;

    @Override
    public void run(String... args) throws Exception {
        scraper.scrapeCurrentYear();
    }
}