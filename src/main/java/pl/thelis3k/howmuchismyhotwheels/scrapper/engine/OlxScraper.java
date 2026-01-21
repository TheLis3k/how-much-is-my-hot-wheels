package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
public class OlxScraper {

    private final PriceCalculator priceCalculator;

    public CarValuation valuateCar(HotWheelsCar car) {
        List<Offer> offers = new ArrayList<>();
        String query = URLEncoder.encode("Hot Wheels " + car.getName(), StandardCharsets.UTF_8);
        // Kategoria: Zabawki, Sortowanie: Najtańsze
        String url = "https://www.olx.pl/dla-dzieci/zabawki/q-" + query + "/?search%5Border%5D=filter_float_price%3Aasc";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            try {
                log.info("🔍 [OLX] Szukam: {}", car.getName());
                page.navigate(url);

                try {
                    page.locator("button#onetrust-accept-btn-handler").click(new Locator.ClickOptions().setTimeout(2000));
                } catch (Exception e) {}

                try {
                    page.waitForSelector("div[data-cy='l-card']", new Page.WaitForSelectorOptions().setTimeout(8000));
                } catch (Exception e) {
                    log.warn("⚠️ Brak wyników na OLX (timeout).");
                    return emptyValuation(car);
                }

                List<ElementHandle> items = page.querySelectorAll("div[data-cy='l-card']");
                log.info("📄 [OLX] Znaleziono {} ogłoszeń.", items.size());

                int limit = Math.min(items.size(), 40);

                for (int i = 0; i < limit; i++) {
                    ElementHandle item = items.get(i);
                    try {
                        String title = "Brak tytułu";
                        String link = "";

                        ElementHandle linkEl = item.querySelector("a");
                        if (linkEl != null) {
                            link = "https://www.olx.pl" + linkEl.getAttribute("href");
                            // OLX czasem daje linki bezwzględne, czasem względne
                            if (link.contains("https://www.olx.plhttps")) {
                                link = link.replace("https://www.olx.plhttps", "https");
                            }
                        }

                        // Pobieranie ceny
                        ElementHandle priceEl = item.querySelector("[data-testid='ad-price']");
                        if (priceEl == null) continue;

                        String priceText = priceEl.innerText();

                        // Odsiewamy "Zamienię", "Za darmo"
                        if (!priceText.contains("zł")) continue;

                        priceText = priceText.replaceAll("[^0-9,.]", "").replace(",", ".");

                        if (!priceText.isEmpty()) {
                            double price = Double.parseDouble(priceText);
                            if (price > 1.0 && price < 1000.0) {
                                offers.add(new Offer(price, link));
                            }
                        }
                    } catch (Exception e) {
                        // ignore parsing errors
                    }
                }
            } catch (Exception e) {
                log.error("❌ OLX scrap error: {}", e.getMessage());
            }
        }

        ValuationMetrics metrics = priceCalculator.calculate(offers);
        log.info("✅ [OLX] Przetworzono {} ofert. Smart Price: {} PLN", metrics.count(), metrics.smartAverage());

        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.OLX)
                .lowestPrice(metrics.min())
                .highestPrice(metrics.max())
                .highestPriceLink(metrics.maxLink())
                .averagePrice(metrics.average())
                .smartAveragePrice(metrics.smartAverage())
                .offersCount(metrics.count())
                .currency("PLN")
                .build();
    }

    private CarValuation emptyValuation(HotWheelsCar car) {
        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.OLX)
                .offersCount(0)
                .currency("PLN")
                .build();
    }
}