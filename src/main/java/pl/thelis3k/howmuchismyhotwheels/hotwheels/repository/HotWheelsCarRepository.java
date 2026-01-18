package pl.thelis3k.howmuchismyhotwheels.hotwheels.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.thelis3k.howmuchismyhotwheels.hotwheels.model.HotWheelsCar;

import java.util.Optional;

@Repository
public interface HotWheelsCarRepository extends MongoRepository<HotWheelsCar, String> {
    Optional<HotWheelsCar> findByName(String name);
    boolean existsByToyId(String toyId);
    boolean existsByNameAndReleaseYear(String name, Integer releaseYear);
}