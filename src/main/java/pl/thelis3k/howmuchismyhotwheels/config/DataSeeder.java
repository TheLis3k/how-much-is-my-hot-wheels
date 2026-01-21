package pl.thelis3k.howmuchismyhotwheels.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.FandomScraper;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.PlatformValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationStatus;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final HotWheelsCarRepository carRepository;
    private final FandomScraper fandomScraper;
    private final ValuationService valuationService;

    @Override
    public void run(String... args) throws Exception {
        // empty
    }
}