package pl.thelis3k.howmuchismyhotwheels.scrapper.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.scrapper.engine.FandomScraper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final FandomScraper fandomScraper;

    @Scheduled(cron = "0 0 4 * * MON")
    public void weeklyCheck() {
        log.info("⏰ Uruchamiam tygodniowe sprawdzanie nowości...");
        fandomScraper.scrapeCurrentYear();
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void monthlyFullScan() {
        log.info("⏰ Uruchamiam miesięczny pełny skan wszystkich lat...");
        fandomScraper.scrapeAllYears();
    }
}