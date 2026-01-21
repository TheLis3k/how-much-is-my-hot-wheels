package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.ValuationSource;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.PriceCalculator;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationMetrics;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationMetrics.Offer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtsyApiService {

    @Value("${etsy.api.key}")
    private String apiKey;

    private final PriceCalculator priceCalculator;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SEARCH_URL = "https://openapi.etsy.com/v3/application/listings/active?keywords=%s&limit=20&sort_on=score";

    public CarValuation valuateCar(HotWheelsCar car) {
        log.info("📡 [Etsy API] Pobieram dane dla: {}", car.getName());
        List<Offer> extractedOffers = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode("Hot Wheels " + car.getName(), StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, encodedQuery);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode results = root.path("results");

                if (results.isArray()) {
                    for (JsonNode item : results) {
                        try {
                            double price = item.path("price").path("amount").asDouble();
                            int divisor = item.path("price").path("divisor").asInt(100);
                            String currency = item.path("price").path("currency_code").asText();
                            String link = item.path("url").asText();
                            double finalPrice = price / divisor;

                            if ("USD".equalsIgnoreCase(currency) && finalPrice > 1.0) {
                                extractedOffers.add(new Offer(finalPrice, link));
                            }
                        } catch (Exception e) {
                            log.warn("⚠️ Błąd parsowania oferty: {}", e.getMessage());
                        }
                    }
                }
            }

            log.info("✅ [Etsy API] Znaleziono {} ofert.", extractedOffers.size());

        } catch (HttpClientErrorException e) {
            log.error("❌ Błąd Etsy API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Błąd ogólny Etsy: {}", e.getMessage());
        }

        ValuationMetrics metrics = priceCalculator.calculate(extractedOffers);

        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.ETSY)
                .lowestPrice(metrics.min())
                .highestPrice(metrics.max())
                .highestPriceLink(metrics.maxLink())
                .averagePrice(metrics.average())
                .smartAveragePrice(metrics.smartAverage())
                .offersCount(metrics.count())
                .currency("USD")
                .build();
    }
}