package pl.thelis3k.howmuchismyhotwheels.valuation.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.CarValuation;
import pl.thelis3k.howmuchismyhotwheels.valuation.model.ValuationSource;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarValuationRepository extends MongoRepository<CarValuation, String> {

    Optional<CarValuation> findFirstByHotWheelsCarIdAndSourceOrderByValuationDateDesc(String hotWheelsCarId, ValuationSource source);

    List<CarValuation> findAllByHotWheelsCarId(String hotWheelsCarId);

    void deleteByHotWheelsCarId(String hotWheelsCarId);

    Optional<CarValuation> findTopByOrderBySmartAveragePriceDesc();

    Optional<CarValuation> findTopByOrderBySmartAveragePriceAsc();

    @Aggregation(pipeline = {
            "{ '$match': { 'source': 'EBAY' } }",
            "{ '$group': { '_id': '$hotWheelsCarId' } }",
            "{ '$count': 'total' }"
    })
    Long countDistinctCarsValuedByEbay();
}