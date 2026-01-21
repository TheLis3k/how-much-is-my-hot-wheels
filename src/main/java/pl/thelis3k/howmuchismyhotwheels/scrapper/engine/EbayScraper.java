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
public class EbayScraper {

    private final PriceCalculator priceCalculator;

    public CarValuation valuateCar(HotWheelsCar car) {
        List<Offer> offers = new ArrayList<>();
        String query = URLEncoder.encode("Hot Wheels " + car.getName(), StandardCharsets.UTF_8);
        String url = "https://www.ebay.pl/sch/i.html?_nkw=" + query + "&_sacat=222&_sop=15";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            try {
                log.info("🔍 [eBay] Szukam: {}", car.getName());
                page.navigate(url);
                page.waitForSelector(".s-item__price", new Page.WaitForSelectorOptions().setTimeout(10000));

                List<ElementHandle> items = page.querySelectorAll(".s-item");
                int limit = Math.min(items.size(), 40);

                for (int i = 0; i < limit; i++) {
                    ElementHandle item = items.get(i);
                    try {
                        String priceText = item.querySelector(".s-item__price").innerText();
                        ElementHandle linkEl = item.querySelector("a.s-item__link");
                        String link = linkEl != null ? linkEl.getAttribute("href") : "";

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
}