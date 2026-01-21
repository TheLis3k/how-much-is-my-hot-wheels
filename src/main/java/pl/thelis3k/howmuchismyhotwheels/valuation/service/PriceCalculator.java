package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import org.springframework.stereotype.Component;
import pl.thelis3k.howmuchismyhotwheels.util.PriceUtil;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationMetrics.Offer;

import java.util.Comparator;
import java.util.List;

@Component
public class PriceCalculator {

    public ValuationMetrics calculate(List<Offer> offers) {
        if (offers.isEmpty()) {
            return ValuationMetrics.builder()
                    .min(0.0)
                    .max(0.0)
                    .maxLink("")
                    .average(0.0)
                    .smartAverage(0.0)
                    .count(0)
                    .build();
        }

        offers.sort(Comparator.comparingInt(Offer::priceCents));
        int minCents = offers.getFirst().priceCents();
        int maxCents = offers.getLast().priceCents();
        String maxLink = offers.getLast().link();

        double avgCents = offers.stream().mapToInt(Offer::priceCents).average().orElse(0);

        double smartAvgCents = avgCents;
        if (offers.size() >= 5) {
            int skip = Math.max(1, (int) (offers.size() * 0.1));
            smartAvgCents = offers.stream()
                    .skip(skip)
                    .limit(offers.size() - (2L * skip))
                    .mapToInt(Offer::priceCents)
                    .average()
                    .orElse(avgCents);
        }

        return ValuationMetrics.builder()
                .min(PriceUtil.toMajor(minCents).doubleValue())
                .max(PriceUtil.toMajor(maxCents).doubleValue())
                .maxLink(maxLink)
                .average(PriceUtil.toMajor((int) avgCents).doubleValue())
                .smartAverage(PriceUtil.toMajor((int) smartAvgCents).doubleValue())
                .count(offers.size())
                .build();
    }
}