package pl.thelis3k.howmuchismyhotwheels.valuation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.ValuationSource;

import java.util.Optional;

@Repository
public interface CarValuationRepository extends MongoRepository<CarValuation, String> {
    Optional<CarValuation> findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(String hotWheelsCarId, ValuationSource source);}