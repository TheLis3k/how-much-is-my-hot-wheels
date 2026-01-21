package pl.thelis3k.howmuchismyhotwheels.valuation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValuationResponse {
    private ValuationStatus globalStatus;

    private PlatformValuation ebay;
    private PlatformValuation etsy;
}