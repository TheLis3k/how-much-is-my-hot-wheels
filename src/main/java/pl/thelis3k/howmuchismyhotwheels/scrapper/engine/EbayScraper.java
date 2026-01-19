package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
    private static final String SEARCH_URL = "https://www.ebay.com/sch/i.html?_nkw=%s&LH_Sold=1&LH_Complete=1&_ipg=60";

    public CarValuation valuateCar(HotWheelsCar car) {
        log.info("💰 [eBay] Wyceniam: {}", car.getName());

        String query = "Hot Wheels " + car.getName();
        List<Offer> extractedOffers = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));

            Page page = browser.newPage();

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            page.navigate(String.format(SEARCH_URL, encodedQuery));

            try {
                page.waitForSelector(".s-item, .s-card", new Page.WaitForSelectorOptions().setTimeout(5000));
            } catch (Exception e) {
                log.debug("⚠️ Timeout czekania na elementy (może brak wyników).");
            }

            String html = page.content();
            Document doc = Jsoup.parse(html);
            Elements items = doc.select(".s-item, .s-card");

            for (Element item : items) {
                if (item.text().contains("Shop on eBay") || item.text().contains("Results matching fewer words")) continue;

                boolean isCard = item.hasClass("s-card");
                String priceText = "";
                String shippingText = "";
                String link = "";

                if (isCard) {
                    priceText = item.select(".s-card__price").text();
                    link = item.select(".s-card__link").attr("href");
                    for (Element attr : item.select(".su-styled-text")) {
                        if (attr.text().contains("delivery") || attr.text().contains("shipping")) {
                            shippingText = attr.text();
                            break;
                        }
                    }
                } else {
                    if (item.select(".s-item__price").isEmpty()) continue;
                    priceText = item.select(".s-item__price").text();
                    shippingText = item.select(".s-item__shipping").text();
                    link = item.select(".s-item__link").attr("href");
                }

                Double itemPrice = parsePrice(priceText);
                Double shippingPrice = parsePrice(shippingText);

                if (itemPrice != null) {
                    double totalPrice = itemPrice + (shippingPrice != null ? shippingPrice : 0.0);
                    extractedOffers.add(new Offer(totalPrice, link));
                }
            }
        } catch (Exception e) {
            log.error("❌ Błąd eBay scrapera: {}", e.getMessage());
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
        if (text == null || text.isEmpty() || text.toLowerCase().contains("free")) return 0.0;
        if (text.contains(" to ")) text = text.split(" to ")[0];
        String cleanNumber = text.replaceAll("[^0-9.,]", "");
        if (cleanNumber.contains(",") && cleanNumber.contains(".")) cleanNumber = cleanNumber.replace(",", "");
        else if (cleanNumber.contains(",")) cleanNumber = cleanNumber.replace(",", ".");

        try { return Double.parseDouble(cleanNumber); } catch (Exception e) { return null; }
    }
}