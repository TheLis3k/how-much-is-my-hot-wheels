package pl.thelis3k.howmuchismyhotwheels.valuation.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValuationMetrics {
    private Double min;
    private Double max;
    private String maxLink;
    private Double average;
    private Double smartAverage;
    private int count;

    public record Offer(int priceCents, String link) {}
}