package pl.thelis3k.howmuchismyhotwheels.valuation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "valuations")
public class CarValuation {

    @Id
    private String id;

    @Indexed
    private String hotWheelsCarId;

    private ValuationSource source;
    private Double lowestPrice;
    private Double highestPrice;
    private String highestPriceLink;
    private Double averagePrice;
    private Double smartAveragePrice;
    private Integer offersCount;
    private String currency;

    @Builder.Default
    private LocalDateTime valuationDate = LocalDateTime.now();
}