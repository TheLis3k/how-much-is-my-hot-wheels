package pl.thelis3k.howmuchismyhotwheels.valuation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ValuationResponse {
    private Double smartPrice;
    private Double minPrice;
    private Double maxPrice;
    private String maxPriceLink;
    private Integer offersCount;
    private String currency;

    private LocalDateTime valuationDate;

    private ValuationStatus status;
}