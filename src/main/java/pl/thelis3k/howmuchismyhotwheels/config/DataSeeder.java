package pl.thelis3k.howmuchismyhotwheels.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.FandomScraper;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final HotWheelsCarRepository carRepository;
    private final FandomScraper fandomScraper;
    private final ValuationService valuationService;

    @Override
    public void run(String... args) throws Exception {
        long carCount = carRepository.count();

//        if (carCount == 0) {
//            log.info("📭 Baza danych jest pusta. Rozpoczynamy PEŁNĄ inicjalizację (Cold Start)...");
//
//            log.info("📚 KROK 1/2: Pobieranie katalogu aut (1968 - Obecnie)...");
//            fandomScraper.scrapeAllYears();

            carCount = carRepository.count();
            log.info("✅ Katalog pobrany. Mamy {} aut w bazie.", carCount);

            if (carCount > 0) {
                log.info("💰 KROK 2/2: Rozpoczynamy wycenę wszystkich aut. To może potrwać parę godzin...");
                valuationService.updateAllValuations();
            }

            log.info("🏁 Inicjalizacja zakończona sukcesem! Aplikacja jest gotowa.");

//        } else {
            log.info("💾 Wykryto dane w bazie ({} aut). Pomijam inicjalizację startową.", carCount);
            log.info("⏰ Czekam na harmonogram Scheduler'a (Poniedziałek/Wtorek).");
//        }
    }
}