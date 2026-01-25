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
import pl.thelis3k.howmuchismyhotwheels.hotwheels.repository.HotWheelsCarRepository;

import java.time.Year;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FandomScraper {

    private final HotWheelsCarRepository repository;
    private static final String BASE_URL = "https://hotwheels.fandom.com/wiki/List_of_%d_Hot_Wheels";
    private static final int START_YEAR = 1968;

    public void scrapeAllYears() {
        int nextYear = Year.now().getValue() + 1;
        runScraper(START_YEAR, nextYear);
    }

    public void scrapeRecentYears() {
        int currentYear = Year.now().getValue();
        int nextYear = currentYear + 1;
        log.info("📅 Uruchamiam update dla lat: {} - {}", currentYear, nextYear);
        runScraper(currentYear, nextYear);
    }

    private void runScraper(int startYear, int endYear) {
        try (Playwright playwright = Playwright.create()) {
            log.info("🚀 Uruchamiam Playwright...");

            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled"));

            Browser browser = playwright.chromium().launch(options);
            Page page = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080)).newPage();

            IntStream.rangeClosed(startYear, endYear).forEach(year -> {
                scrapeYear(page, year);
                sleepRandom(3000, 7000);
            });

            browser.close();
        } catch (Exception e) {
            log.error("❌ Krytyczny błąd Playwright: ", e);
        }
    }

    private void scrapeYear(Page page, int year) {
        String url = String.format(BASE_URL, year);
        log.info("📂 Przetwarzam rok: {}", year);

        try {
            page.navigate(url);

            try {
                page.mouse().move(100, 200);
                page.mouse().wheel(0, 500);
                page.waitForTimeout(2000);
            } catch (Exception e) {
                log.warn("⚠️ Problem z symulacją ruchu myszy dla roku {}: {}", year, e.getMessage());
            }

            String htmlContent = page.content();
            Document doc = Jsoup.parse(htmlContent);

            if (doc.title().contains("Cloudflare") || doc.title().contains("Challenge") || doc.title().contains("Just a moment")) {
                log.error("⛔ BLOKADA CLOUDFLARE dla roku {}! Czekam dłużej...", year);
                sleepRandom(10000, 15000);
                return;
            }

            parseAndSave(doc, year);

        } catch (Exception e) {
            log.error("❌ Błąd pobierania roku {}: {}", year, e.getMessage());
        }
    }

    private void parseAndSave(Document doc, int year) {
        Elements tables = doc.select("table.wikitable");
        if (tables.isEmpty()) {
            log.warn("⚠️ Brak tabeli 'wikitable' w roku {}", year);
            return;
        }

        int carsSaved = 0;
        int versionsUpdated = 0;

        for (Element table : tables) {
            Map<String, Integer> colMap = analyzeHeaders(table);
            if (!colMap.containsKey("NAME")) continue;

            Elements rows = table.select("tr");
            int nameIdx = colMap.get("NAME");

            for (int i = 1; i < rows.size(); i++) {
                Element row = rows.get(i);
                Elements cols = row.select("td");

                if (cols.size() > nameIdx) {
                    try {
                        String name = cols.get(nameIdx).text().trim();
                        String toyId = getColumnValue(cols, colMap.get("TOY_ID"));
                        String colNum = getColumnValue(cols, colMap.get("COL_NUM"));
                        String series = getColumnValue(cols, colMap.get("SERIES"));

                        if (series == null) series = "Mainline / Unknown";

                        if (isValidCarName(name)) {
                            Optional<HotWheelsCar> existingCarOpt = Optional.empty();

                            if (toyId != null && !toyId.isEmpty()) {
                                existingCarOpt = repository.findByToyId(toyId);
                            }

                            if (existingCarOpt.isEmpty()) {
                                existingCarOpt = repository.findByName(name);
                            }

                            if (existingCarOpt.isPresent()) {
                                HotWheelsCar existing = existingCarOpt.get();
                                existing.setVersions(existing.getVersions() + 1);
                                repository.save(existing);
                                versionsUpdated++;
                            } else {
                                HotWheelsCar car = HotWheelsCar.builder()
                                        .name(name)
                                        .series(series)
                                        .releaseYear(year)
                                        .toyId(toyId)
                                        .collectionNumber(colNum)
                                        .versions(1)
                                        .build();
                                repository.save(car);
                                carsSaved++;
                            }
                        }
                    } catch (Exception e) {
                        log.error("Błąd przetwarzania wiersza: ", e);
                    }
                }
            }
        }
        log.info("💾 Rok {}: Nowych aut: {}, Zaktualizowanych wersji: {}", year, carsSaved, versionsUpdated);
    }

    private String getColumnValue(Elements cols, Integer index) {
        if (index != null && cols.size() > index) {
            String text = cols.get(index).text().trim();
            return !text.isEmpty() ? text : null;
        }
        return null;
    }

    private boolean isValidCarName(String name) {
        return !name.isEmpty() && !name.equalsIgnoreCase("N/A") && !name.equalsIgnoreCase("TBA");
    }

    private Map<String, Integer> analyzeHeaders(Element table) {
        Map<String, Integer> map = new HashMap<>();
        Elements headers = table.select("th");
        if (headers.isEmpty()) {
            Element firstRow = table.select("tr").first();
            if (firstRow != null) headers = firstRow.select("td");
        }

        for (int i = 0; i < headers.size(); i++) {
            String text = headers.get(i).text().toUpperCase().trim();
            if (text.contains("NAME") || text.contains("MODEL") || text.contains("CAR")) map.put("NAME", i);
            else if (text.contains("SERIES")) {
                if (!text.contains("#") && !text.contains("NUM") && !text.contains("NO.")) {
                    map.put("SERIES", i);
                }
            }
            else if (text.contains("TOY") || text.contains("SKU")) map.put("TOY_ID", i);
            else if (text.contains("COL") || text.contains("NUMBER")) map.put("COL_NUM", i);
        }
        return map;
    }

    private void sleepRandom(int min, int max) {
        try {
            int delay = ThreadLocalRandom.current().nextInt(min, max + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}