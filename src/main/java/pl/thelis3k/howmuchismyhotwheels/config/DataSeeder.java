package pl.thelis3k.howmuchismyhotwheels.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.FandomScraper;
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
        log.info("🧪 ROZPOCZYNAM TEST INTEGRACYJNY: 2026 & eBay Price Check");

        List<HotWheelsCar> cars2026 = carRepository.findAll().stream()
                .filter(c -> c.getReleaseYear() != null && c.getReleaseYear() == 2026)
                .toList();

        if (cars2026.isEmpty()) {
            log.info("📥 Pobieram auta z roku 2026 (wymagane do testu)...");
            fandomScraper.scrapeRecentYears();
            // Odśwież listę
            cars2026 = carRepository.findAll().stream()
                    .filter(c -> c.getReleaseYear() != null && c.getReleaseYear() == 2026)
                    .toList();
        } else {
            log.info("✅ Auta z 2026 już są w bazie.");
        }

        HotWheelsCar testCar = cars2026.stream().findFirst().orElse(null);

        if (testCar != null) {
            log.info("🚗 Wybrano auto do testu: {}", testCar.getName());

            log.info("🔎 Zapytanie #1 (Oczekiwane: NOT_FOUND_UPDATING)...");
            ValuationResponse response1 = valuationService.getValuationForCar(testCar.getId());
            log.info("👉 Wynik #1: Status={}", response1.getStatus());

            if (response1.getStatus() == ValuationStatus.NOT_FOUND_UPDATING || response1.getStatus() == ValuationStatus.STALE_UPDATING) {
                log.info("⏳ Czekam 15 sekund na robota eBay...");
                Thread.sleep(15000);

                log.info("🔎 Zapytanie #2 (Oczekiwane: FRESH)...");
                ValuationResponse response2 = valuationService.getValuationForCar(testCar.getId());
                log.info("👉 Wynik #2: Status={}, Cena=${}", response2.getStatus(), response2.getSmartPrice());

                if (response2.getStatus() == ValuationStatus.FRESH) {
                    log.info("✅ TEST ZALICZONY! System działa poprawnie.");
                } else {
                    log.error("❌ TEST NIEZALICZONY.");
                }
            } else {
                log.info("⚠️ Auto miało już cenę (FRESH). Test pominięty.");
            }
        } else {
            log.error("❌ Nie znaleziono żadnego auta z 2023 roku!");
        }
    }
}