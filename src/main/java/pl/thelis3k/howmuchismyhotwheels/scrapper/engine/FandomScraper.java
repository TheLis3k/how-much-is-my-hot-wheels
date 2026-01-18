package pl.thelis3k.howmuchismyhotwheels.scrapper.engine;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Year;
import java.util.stream.IntStream;

@Slf4j
@Service
public class FandomScraper {

    private static final String BASE_URL = "https://hotwheels.fandom.com/wiki/List_of_%d_Hot_Wheels";
    private static final int START_YEAR = 1968;

    public void scrapeAllYears() {
        int currentYear = Year.now().getValue();
        int endYear = currentYear + 1;

        log.info("🚀 Rozpoczynam pełny scraping lat: {} - {}", START_YEAR, endYear);

        IntStream.rangeClosed(START_YEAR, endYear)
                .forEach(year -> {
                    scrapeYear(year);
                    sleep(2000); // 2 seconds to avoid rate limiting
                });
    }

    public void scrapeCurrentYear() {
        int currentYear = Year.now().getValue();
        int nextYear = currentYear + 1;
        log.info("📅 Sprawdzam bieżący i następny rok: {}, {}", currentYear, nextYear);
        scrapeYear(currentYear);
        sleep(2000); // 2 seconds to avoid rate limiting
        scrapeYear(nextYear);
    }

    private void scrapeYear(int year) {
        String url = String.format(BASE_URL, year);
        try {
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Referer", "https://www.google.com/")
                    .timeout(10000);

            Document doc = connection.get();

            if (doc.title().contains("Cloudflare") || doc.title().contains("Challenge")) {
                log.error("⛔ BLOKADA CLOUDFLARE dla roku {}. Jsoup nie da rady.", year);
                return;
            }

            log.info("✅ Pobrano rok {}. Tytuł: {}", year, doc.title());

            // TU BĘDZIE LOGIKA WYCIĄGANIA TABELI (Kolejny krok)

        } catch (IOException e) {
            log.error("❌ Błąd pobierania roku {}: {}", year, e.getMessage());
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}