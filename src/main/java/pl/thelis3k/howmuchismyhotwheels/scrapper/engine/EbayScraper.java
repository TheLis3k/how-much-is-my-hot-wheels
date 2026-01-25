package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.util.PriceUtil;
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

    public CarValuation valuateCar(HotWheelsCar car) {
        List<Offer> offers = new ArrayList<>();
        String query = URLEncoder.encode("Hot Wheels " + car.getName(), StandardCharsets.UTF_8);
        String url = "https://www.ebay.pl/sch/i.html?_nkw=" + query + "&_sacat=222&_sop=15";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true) // Wracamy do trybu ukrytego
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            try {
                log.info("🔍 [eBay] Szukam: {}", car.getName());
                page.navigate(url);

                try {
                    page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(8000));
                } catch (TimeoutError e) {
                    // Ignorujemy timeout sieci, jeśli elementy się załadowały
                }

                String containerSelector = ".s-item, .s-card";
                Locator items = page.locator(containerSelector);

                int count = items.count();
                if (count == 0) {
                    try {
                        page.waitForSelector(containerSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                        items = page.locator(containerSelector);
                        count = items.count();
                    } catch (Exception e) {
                        log.warn("⚠️ Brak wyników na eBay (timeout).");
                        return emptyValuation(car);
                    }
                }

                for (int i = 0; i < count; i++) {
                    Locator item = items.nth(i);
                    try {
                        Locator priceLocator = item.locator(".s-item__price, .s-card__price");
                        if (priceLocator.count() == 0) continue;

                        String priceText = priceLocator.first().innerText();

                        Locator linkLocator = item.locator("a.s-item__link, a.s-card__link");
                        String link = linkLocator.count() > 0 ? linkLocator.first().getAttribute("href") : "";

                        if (priceText.contains("do")) {
                            priceText = priceText.split("do")[0];
                        }

                        int priceCents = PriceUtil.toCents(priceText);

                        if (priceCents > 100 && priceCents < 200000) {
                            offers.add(new Offer(priceCents, link));
                        }
                    } catch (Exception e) {
                        // ignore broken items
                    }
                }
            } catch (Exception e) {
                log.error("❌ eBay scrap error: {}", e.getMessage());
            } finally {
                browser.close();
            }
        }

        ValuationMetrics metrics = priceCalculator.calculate(offers);
        log.info("✅ [eBay] Przetworzono {} ofert. Smart Price: {} PLN", metrics.getCount(), metrics.getSmartAverage());

        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.EBAY)
                .lowestPrice(metrics.getMin())
                .highestPrice(metrics.getMax())
                .highestPriceLink(metrics.getMaxLink())
                .averagePrice(metrics.getAverage())
                .smartAveragePrice(metrics.getSmartAverage())
                .offersCount(metrics.getCount())
                .currency("PLN")
                .build();
    }

    private CarValuation emptyValuation(HotWheelsCar car) {
        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.EBAY)
                .lowestPrice(0.0)
                .highestPrice(0.0)
                .averagePrice(0.0)
                .smartAveragePrice(0.0)
                .offersCount(0)
                .currency("PLN")
                .build();
    }
}