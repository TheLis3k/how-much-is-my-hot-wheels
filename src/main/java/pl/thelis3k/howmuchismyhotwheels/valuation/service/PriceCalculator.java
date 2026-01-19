package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import org.springframework.stereotype.Service;
import pl.thelis3k.howmuchismyhotwheels.valuation.service.ValuationMetrics.Offer;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@Service
public class PriceCalculator {

    public ValuationMetrics calculate(List<Offer> offers) {
        if (offers == null || offers.isEmpty()) {
            return new ValuationMetrics(0.0, 0.0, null, 0.0, 0.0, 0);
        }

        List<Offer> sortedOffers = new ArrayList<>(offers);
        Collections.sort(sortedOffers);

        int size = sortedOffers.size();

        Double min = sortedOffers.getFirst().price();
        Offer maxOffer = sortedOffers.getLast();
        Double max = maxOffer.price();
        String maxLink = maxOffer.url();

        Double avg = sortedOffers.stream().mapToDouble(Offer::price).average().orElse(0.0);

        Double smartAvg;
        if (size < 5) {
            smartAvg = avg;
        } else {
            int cutOff = (int) (size * 0.40);

            if (size - (2 * cutOff) <= 0) {
                smartAvg = avg;
            } else {
                List<Offer> trimmedList = sortedOffers.subList(cutOff, size - cutOff);
                smartAvg = trimmedList.stream().mapToDouble(Offer::price).average().orElse(0.0);
            }
        }

        return new ValuationMetrics(
                round(min),
                round(max),
                maxLink,
                round(avg),
                round(smartAvg),
                size
        );
    }

    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}