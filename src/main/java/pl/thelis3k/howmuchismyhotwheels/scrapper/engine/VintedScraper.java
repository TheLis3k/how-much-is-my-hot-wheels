package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.microsoft.playwright.*;
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
public class VintedScraper {

    private final PriceCalculator priceCalculator;

    public CarValuation valuateCar(HotWheelsCar car) {
        List<Offer> offers = new ArrayList<>();
        String query = URLEncoder.encode("HotWheels " + car.getName(), StandardCharsets.UTF_8);
        String url = "https://www.vinted.pl/catalog?search_text=" + query + "&order=price_low_to_high&catalog[]=1499";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            try {
                log.info("🔍 [Vinted] Szukam: {}", car.getName());
                page.navigate(url);

                try {
                    page.waitForSelector("[data-testid='grid-item']", new Page.WaitForSelectorOptions().setTimeout(10000));
                } catch (Exception e) {
                    log.warn("⚠️ Brak wyników na Vinted (timeout).");
                    return emptyValuation(car);
                }

                try {
                    page.locator("#onetrust-accept-btn-handler").click(new Locator.ClickOptions().setTimeout(1000));
                } catch (Exception e) {}

                List<ElementHandle> items = page.querySelectorAll("[data-testid='grid-item']");
                int limit = Math.min(items.size(), 40);

                for (int i = 0; i < limit; i++) {
                    ElementHandle item = items.get(i);
                    try {
                        String title = "Brak tytułu";
                        String link = "";

                        ElementHandle linkEl = item.querySelector("a");
                        if (linkEl != null) {
                            link = linkEl.getAttribute("href");
                            title = linkEl.getAttribute("title");
                        }

                        if (title == null || title.equals("Brak tytułu") || link.contains("/member/")) {
                            continue;
                        }

                        String fullText = item.innerText();
                        String priceText = fullText.lines()
                                .filter(line -> line.contains("zł") || line.contains("PLN"))
                                .findFirst()
                                .orElse("0");

                        int priceCents = PriceUtil.toCents(priceText);

                        if (priceCents > 100 && priceCents < 100000) {
                            offers.add(new Offer(priceCents, link));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                log.error("❌ Vinted scrap error: {}", e.getMessage());
            } finally {
                browser.close();
            }
        }

        ValuationMetrics metrics = priceCalculator.calculate(offers);
        log.info("✅ [Vinted] Przetworzono {} ofert. Smart Price: {} PLN", metrics.getCount(), metrics.getSmartAverage());

        return CarValuation.builder()
                .hotWheelsCarId(car.getId())
                .source(ValuationSource.VINTED)
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
                .source(ValuationSource.VINTED)
                .lowestPrice(0.0)
                .highestPrice(0.0)
                .averagePrice(0.0)
                .smartAveragePrice(0.0)
                .offersCount(0)
                .currency("PLN")
                .build();
    }
}