package pl.thelis3k.howmuchismyhotwheels.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.thelis3k.howmuchismyhotwheels.controller.dto.DatabaseStatsResponse;
import pl.thelis3k.howmuchismyhotwheels.controller.dto.FullCarDetailsResponse;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;
import pl.thelis3k.howmuchismyhotwheels.valuation.repository.CarValuationRepository;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotWheelsController {

    private final HotWheelsCarRepository carRepository;
    private final ValuationService valuationService;
    private final CarValuationRepository carValuationRepository;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/stats")
    public ResponseEntity<DatabaseStatsResponse> getDatabaseStats() {
        Long valuedCount = carValuationRepository.countDistinctCarsValuedByEbay();
        return ResponseEntity.ok(DatabaseStatsResponse.builder()
                .totalCars(carRepository.count())
                .valuedCars(valuedCount != null ? valuedCount : 0L)
                .build());
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        return ResponseEntity.ok(carRepository.findDistinctReleaseYears());
    }

    @GetMapping("/cars/{year}")
    public ResponseEntity<?> getCarsByYear(@PathVariable Integer year) {
        List<HotWheelsCar> cars = carRepository.findByReleaseYearOrderByNameAsc(year);
        if (cars.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Nie znaleziono samochodzików dla roku: " + year));
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/namevaluation/{hotWheelsName}")
    public ResponseEntity<?> getCarsByName(@PathVariable String hotWheelsName) {
        List<HotWheelsCar> cars = valuationService.findCarsByName(hotWheelsName);
        if (cars.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Nie znaleziono samochodzików pasujących do nazwy: " + hotWheelsName));
        }
        return ResponseEntity.ok(cars);
    }

    @GetMapping("/valuation/{identifier}")
    public ResponseEntity<?> getValuation(@PathVariable String identifier) {
        if ("max".equalsIgnoreCase(identifier) || "min".equalsIgnoreCase(identifier)) {
            Optional<FullCarDetailsResponse> extremeValuation = valuationService.getExtremeValuation(identifier);
            if (extremeValuation.isPresent()) {
                return ResponseEntity.ok(extremeValuation.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Nie można ustalić wyceny typu: " + identifier));
            }
        }

        Optional<HotWheelsCar> carOpt = carRepository.findById(identifier);
        if (carOpt.isPresent()) {
            ValuationResponse valuation = valuationService.getValuationForCar(identifier);
            FullCarDetailsResponse response = FullCarDetailsResponse.builder()
                    .car(carOpt.get())
                    .valuation(valuation)
                    .build();
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Nie znaleziono samochodzika o ID: " + identifier));
        }
    }
}