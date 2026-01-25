package pl.thelis3k.howmuchismyhotwheels.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseStatsResponse {
    private long totalCars;
    private long valuedCars;
}