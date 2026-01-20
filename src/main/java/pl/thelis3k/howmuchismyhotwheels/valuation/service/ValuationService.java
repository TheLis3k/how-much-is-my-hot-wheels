package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.EbayScraper;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationStatus;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.repository.CarValuationRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationService {

    private final HotWheelsCarRepository carRepository;
    private final CarValuationRepository valuationRepository;
    private final EbayScraper ebayScraper;

    public ValuationResponse getValuationForCar(String carId) {
        HotWheelsCar car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found: " + carId));

        Optional<CarValuation> lastValuationOpt = valuationRepository.findFirstByHotWheelsCarIdOrderByValuationDateDesc(carId);

        if (lastValuationOpt.isPresent()) {
            CarValuation val = lastValuationOpt.get();
            boolean isFresh = val.getValuationDate().isAfter(LocalDateTime.now().minusDays(7));

            if (isFresh) {
                log.info("✅ Zwracam świeżą wycenę z cache dla: {}", car.getName());
                return mapToResponse(val, ValuationStatus.FRESH);
            } else {
                log.info("cw Zwracam starą wycenę dla: {} i uruchamiam UPDATE w tle.", car.getName());
                triggerBackgroundUpdate(car);
                return mapToResponse(val, ValuationStatus.STALE_UPDATING);
            }
        } else {
            log.info("🤷‍♂️ Brak wyceny dla: {}. Uruchamiam pierwszą wycenę w tle.", car.getName());
            triggerBackgroundUpdate(car);

            return ValuationResponse.builder()
                    .status(ValuationStatus.NOT_FOUND_UPDATING)
                    .build();
        }
    }

    @Async
    public void triggerBackgroundUpdate(HotWheelsCar car) {
        log.info("🔄 [Async] Rozpoczynam aktualizację ceny w tle: {}", car.getName());
        try {
            CarValuation newValuation = ebayScraper.valuateCar(car);

            if (newValuation.getOffersCount() > 0) {
                valuationRepository.save(newValuation);
                log.info("✅ [Async] Zaktualizowano cenę dla: {} (Nowy SmartAvg: ${})", car.getName(), newValuation.getSmartAveragePrice());
            } else {
                log.warn("⚠️ [Async] eBay nie zwrócił wyników dla: {}", car.getName());
            }
        } catch (Exception e) {
            log.error("❌ [Async] Błąd wyceny: {}", e.getMessage());
        }
    }

    private ValuationResponse mapToResponse(CarValuation val, ValuationStatus status) {
        return ValuationResponse.builder()
                .smartPrice(val.getSmartAveragePrice())
                .minPrice(val.getLowestPrice())
                .maxPrice(val.getHighestPrice())
                .maxPriceLink(val.getHighestPriceLink())
                .offersCount(val.getOffersCount())
                .currency(val.getCurrency())
                .valuationDate(val.getValuationDate())
                .status(status)
                .build();
    }
}