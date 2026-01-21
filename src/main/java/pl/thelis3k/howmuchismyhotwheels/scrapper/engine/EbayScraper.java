package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.microsoft.playwright.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
public class EbayScraper {

    private final PriceCalculator priceCalculator;
    private static final String SEARCH_URL = "https://www.ebay.com/sch/i.html?_nkw=%s&_sacat=0&LH_Sold=1&LH_Complete=1";

    public synchronized CarValuation valuateCar(HotWheelsCar car) {
        log.info("💰 [eBay Playwright] Wyceniam: {}", car.getName());
        String query = "Hot Wheels " + car.getName();
        List<Offer> extractedOffers = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            boolean isHeadless = Boolean.parseBoolean(System.getenv().getOrDefault("HEADLESS_MODE", "true"));

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(isHeadless)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            page.navigate(String.format(SEARCH_URL, encodedQuery));

            try {
                page.waitForSelector(".s-item__price", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                log.warn("⚠️ [eBay] Timeout oczekiwania na ceny.");
            }

            String html = page.content();
            Document doc = Jsoup.parse(html);
            Elements items = doc.select(".s-item");

            for (Element item : items) {
                if (item.text().contains("Shop on eBay")) continue;

                String priceText = item.select(".s-item__price").text();
                String link = item.select("a.s-item__link").attr("href");

                if (priceText.contains(" to ")) {
                    priceText = priceText.split(" to ")[0];
                }

                Double price = parsePrice(priceText);

                if (price != null && price > 1.0 && link != null) {
                    extractedOffers.add(new Offer(price, link));
                }
            }

            log.info("✅ [eBay] Znaleziono {} ofert.", extractedOffers.size());

        } catch (Exception e) {
            log.error("❌ Błąd eBay: {}", e.getMessage());
        }

        ValuationMetrics metrics = priceCalculator.calculate(extractedOffers);

        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.EBAY)
                .lowestPrice(metrics.min())
                .highestPrice(metrics.max())
                .highestPriceLink(metrics.maxLink())
                .averagePrice(metrics.average())
                .smartAveragePrice(metrics.smartAverage())
                .offersCount(metrics.count())
                .currency("USD")
                .build();
    }

    private Double parsePrice(String text) {
        if (text == null || text.isEmpty()) return null;
        String clean = text.replaceAll("[^0-9.,]", "");
        if (clean.contains(",") && clean.contains(".")) clean = clean.replace(",", "");
        else if (clean.contains(",")) clean = clean.replace(",", ".");
        try { return Double.parseDouble(clean); } catch (Exception e) { return null; }
    }
}