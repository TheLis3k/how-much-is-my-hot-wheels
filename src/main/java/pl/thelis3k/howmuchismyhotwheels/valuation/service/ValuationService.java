package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.EbayScraper;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.repository.CarValuationRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationService {

    private final HotWheelsCarRepository carRepository;
    private final CarValuationRepository valuationRepository;
    private final EbayScraper ebayScraper;

    public void updateAllValuations() {
        List<HotWheelsCar> allCars = carRepository.findAll();
        log.info("🏁 Rozpoczynam masową wycenę. Liczba aut do sprawdzenia: {}", allCars.size());

        int counter = 0;
        for (HotWheelsCar car : allCars) {
            try {
                counter++;
                CarValuation valuation = ebayScraper.valuateCar(car);

                if (valuation.getOffersCount() > 0) {
                    valuationRepository.save(valuation);
                    log.info("[{}/{}] ✅ Zapisano wycenę dla: {} (SmartAvg: ${})",
                            counter, allCars.size(), car.getName(), valuation.getSmartAveragePrice());
                } else {
                    log.info("[{}/{}] ⚠️ Brak ofert dla: {}", counter, allCars.size(), car.getName());
                }

                sleepRandom(2000, 5000);

            } catch (Exception e) {
                log.error("❌ Błąd przy przetwarzaniu auta {}: {}", car.getName(), e.getMessage());
            }
        }
        log.info("🏁 Zakończono proces masowej wyceny.");
    }

    private void sleepRandom(int min, int max) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(min, max + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}