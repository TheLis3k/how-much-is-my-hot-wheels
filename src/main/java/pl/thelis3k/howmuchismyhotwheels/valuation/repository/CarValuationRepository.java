package pl.thelis3k.howmuchismyhotwheels.valuation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;

import java.util.List;

@Repository
public interface CarValuationRepository extends MongoRepository<CarValuation, String> {

    List<CarValuation> findAllByHotWheelsCarIdOrderByValuationDateDesc(String hotWheelsCarId);
}