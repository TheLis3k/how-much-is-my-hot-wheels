package pl.thelis3k.howmuchismyhotwheels.hotwheels.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotWheelsCarRepository extends MongoRepository<HotWheelsCar, String> {
    Optional<HotWheelsCar> findByName(String name);
    boolean existsByToyId(String toyId);
    boolean existsByNameAndReleaseYear(String name, Integer releaseYear);


    List<HotWheelsCar> findByReleaseYearOrderByNameAsc(Integer releaseYear);

    @Aggregation(pipeline = {
            "{ '$group': { '_id': '$releaseYear' } }",
            "{ '$sort': { '_id': -1 } }"
    })
    List<Integer> findDistinctReleaseYears();
}