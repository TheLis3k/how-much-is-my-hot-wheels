package pl.thelis3k.howmuchismyhotwheels.hotwheels.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cars")
@CompoundIndex(name = "unique_car_idx", def = "{'toyId': 1}", unique = true, sparse = true)
public class HotWheelsCar {

    @Id
    private String id;

    private String name;
    private String series;
    private Integer releaseYear;
    private String toyId;
    private String collectionNumber;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}