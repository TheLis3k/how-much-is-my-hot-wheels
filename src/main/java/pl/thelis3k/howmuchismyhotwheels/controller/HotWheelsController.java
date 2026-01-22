package pl.thelis3k.howmuchismyhotwheels.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.thelis3k.howmuchismyhotwheels.controller.dto.FullCarDetailsResponse;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotWheelsController {

    private final HotWheelsCarRepository carRepository;
    private final ValuationService valuationService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        return ResponseEntity.ok(carRepository.findDistinctReleaseYears());
    }

    @GetMapping("/cars/{year}")
    public ResponseEntity<List<HotWheelsCar>> getCarsByYear(@PathVariable Integer year) {
        return ResponseEntity.ok(carRepository.findByReleaseYearOrderByNameAsc(year));
    }

    @GetMapping("/valuation/{carId}")
    public ResponseEntity<FullCarDetailsResponse> getCarValuation(@PathVariable String carId) {
        return carRepository.findById(carId)
                .map(car -> {
                    ValuationResponse valuation = valuationService.getValuationForCar(carId);

                    FullCarDetailsResponse response = FullCarDetailsResponse.builder()
                            .car(car)
                            .valuation(valuation)
                            .build();

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}