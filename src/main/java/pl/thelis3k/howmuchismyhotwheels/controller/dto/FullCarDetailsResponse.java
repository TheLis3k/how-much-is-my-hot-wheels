package pl.thelis3k.howmuchismyhotwheels.controller.dto;

import lombok.Builder;
import lombok.Data;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;
import pl.thelis3k.howmuchismyhotwheels.valuation.dto.ValuationResponse;

@Data
@Builder
public class FullCarDetailsResponse {
    private HotWheelsCar car;
    private ValuationResponse valuation;
}