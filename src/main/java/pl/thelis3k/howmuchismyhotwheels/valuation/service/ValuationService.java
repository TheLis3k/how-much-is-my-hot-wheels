package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.EbayScraper;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.EtsyApiService;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.PlatformValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationStatus;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.ValuationSource;
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

    private final EtsyApiService etsyApiService;

    @Autowired
    @Lazy
    private ValuationService self;

    public ValuationResponse getValuationForCar(String carId) {
        HotWheelsCar car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found: " + carId));

        Optional<CarValuation> ebayVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.EBAY);
        Optional<CarValuation> etsyVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.ETSY);

        boolean ebayFresh = isFresh(ebayVal);
        boolean etsyFresh = isFresh(etsyVal);

        ValuationStatus globalStatus;

        if (ebayFresh) {
            globalStatus = ValuationStatus.FRESH;
        } else if (ebayVal.isPresent()) {
            globalStatus = ValuationStatus.STALE_UPDATING;
            self.triggerBackgroundUpdate(car);
        } else {
            globalStatus = ValuationStatus.NOT_FOUND_UPDATING;
            self.triggerBackgroundUpdate(car);
        }

        return ValuationResponse.builder()
                .globalStatus(globalStatus)
                .ebay(ebayVal.map(v -> mapToPlatform(v, ebayFresh)).orElse(null))
                .etsy(etsyVal.map(v -> mapToPlatform(v, etsyFresh)).orElse(null))
                // TODO: Tu w przyszłości dojdzie .allegro(...)
                .build();
    }

    private boolean isFresh(Optional<CarValuation> val) {
        return val.isPresent() && val.get().getValuationDate().isAfter(LocalDateTime.now().minusDays(7));
    }

    @Async
    public void triggerBackgroundUpdate(HotWheelsCar car) {
        log.info("🔄 [Async] Aktualizacja cen dla: {}", car.getName());

        // 1. EBAY (Działa)
        try {
            CarValuation val = ebayScraper.valuateCar(car);
            if (val.getOffersCount() > 0) {
                valuationRepository.save(val);
                log.info("✅ eBay zaktualizowany: {} ofert", val.getOffersCount());
            }
        } catch (Exception e) {
            log.error("❌ eBay Error: {}", e.getMessage());
        }

        // 2. ETSY (WYŁĄCZONE TYMCZASOWO)
        /*
        try {
            Thread.sleep(2000); // Odczekaj chwilę, żeby nie spamować
            CarValuation val = etsyApiService.valuateCar(car);
            if (val.getOffersCount() > 0) {
                valuationRepository.save(val);
                log.info("✅ Etsy zaktualizowane: {} ofert", val.getOffersCount());
            }
        } catch (Exception e) {
            log.error("❌ Etsy Error: {}", e.getMessage());
        }
        */

        // 3. TODO: ALLEGRO (Tu będzie miejsce na Twój nowy serwis)

    }

    private PlatformValuation mapToPlatform(CarValuation val, boolean isFresh) {
        return PlatformValuation.builder()
                .smartPrice(val.getSmartAveragePrice())
                .minPrice(val.getLowestPrice())
                .maxPrice(val.getHighestPrice())
                .maxPriceLink(val.getHighestPriceLink())
                .offersCount(val.getOffersCount())
                .currency(val.getCurrency())
                .valuationDate(val.getValuationDate())
                .isFresh(isFresh)
                .build();
    }
}