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
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.OlxScraper;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.VintedScraper;
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
    private final VintedScraper vintedScraper;
    private final OlxScraper olxScraper;

    @Autowired
    @Lazy
    private ValuationService self;

    public ValuationResponse getValuationForCar(String carId) {
        HotWheelsCar car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Car not found: " + carId));

        Optional<CarValuation> ebayVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.EBAY);
        Optional<CarValuation> etsyVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.ETSY);
        Optional<CarValuation> vintedVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.VINTED);
        Optional<CarValuation> olxVal = valuationRepository.findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(carId, ValuationSource.OLX);

        boolean ebayFresh = isFresh(ebayVal);
        boolean etsyFresh = isFresh(etsyVal);
        boolean vintedFresh = isFresh(vintedVal);
        boolean olxFresh = isFresh(olxVal);

        ValuationStatus globalStatus;

        if (ebayFresh && vintedFresh && olxFresh) {
            globalStatus = ValuationStatus.FRESH;
        } else if (ebayVal.isPresent() || vintedVal.isPresent() || olxVal.isPresent()) {
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
                .vinted(vintedVal.map(v -> mapToPlatform(v, vintedFresh)).orElse(null))
                .olx(olxVal.map(v -> mapToPlatform(v, olxFresh)).orElse(null))
                .build();
    }

    private boolean isFresh(Optional<CarValuation> val) {
        return val.isPresent() && val.get().getValuationDate().isAfter(LocalDateTime.now().minusDays(7));
    }

    @Async
    public void triggerBackgroundUpdate(HotWheelsCar car) {
        log.info("🔄 [Async] Aktualizacja cen dla: {}", car.getName());

        try {
            CarValuation val = ebayScraper.valuateCar(car);
            if (val.getOffersCount() > 0) {
                valuationRepository.save(val);
                log.info("✅ eBay zaktualizowany: {} ofert", val.getOffersCount());
            }
        } catch (Exception e) {
            log.error("❌ eBay Error: {}", e.getMessage());
        }

        try {
            CarValuation val = vintedScraper.valuateCar(car);
            if (val.getOffersCount() > 0) {
                valuationRepository.save(val);
                log.info("✅ Vinted zaktualizowany: {} ofert", val.getOffersCount());
            }
        } catch (Exception e) {
            log.error("❌ Vinted Error: {}", e.getMessage());
        }

        try {
            CarValuation val = olxScraper.valuateCar(car);
            if (val.getOffersCount() > 0) {
                valuationRepository.save(val);
                log.info("✅ OLX zaktualizowany: {} ofert", val.getOffersCount());
            }
        } catch (Exception e) {
            log.error("❌ OLX Error: {}", e.getMessage());
        }
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