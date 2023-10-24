package uk.tw.energy.service;

import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.PricePlan;
import uk.tw.energy.exceptions.NoReadingsExistForMeterId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static uk.tw.energy.exceptions.Constants.*;

@Service
public class PricePlanService {

    private final List<PricePlan> pricePlans;
    private final MeterReadingService meterReadingService;

    /**
     * Added the accountService class object in the constructor for initialization and injection.
     */
    private final AccountService accountService;

    public PricePlanService(List<PricePlan> pricePlans, MeterReadingService meterReadingService, AccountService accountService) {
        this.pricePlans = pricePlans;
        this.meterReadingService = meterReadingService;
        this.accountService = accountService;
    }

    private Optional<Map<String, BigDecimal>> getConsumptionCostOfElectricityReadingsForEachPricePlan(String smartMeterId) {
        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);
        if (!electricityReadings.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(pricePlans.stream().collect(
                Collectors.toMap(PricePlan::getPlanName, t -> calculateCost(electricityReadings.get(), t))));
    }

    private BigDecimal calculateCost(List<ElectricityReading> electricityReadings, PricePlan pricePlan) {
        BigDecimal average = calculateAverageReading(electricityReadings);
        BigDecimal timeElapsed = calculateTimeElapsed(electricityReadings);

        BigDecimal averagedCost = average.divide(timeElapsed, RoundingMode.HALF_UP);
        return averagedCost.multiply(pricePlan.getUnitRate());
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> electricityReadings) {
        BigDecimal summedReadings = electricityReadings.stream()
                .map(ElectricityReading::reading)
                .reduce(BigDecimal.ZERO, (reading, accumulator) -> reading.add(accumulator));

        return summedReadings.divide(BigDecimal.valueOf(electricityReadings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTimeElapsed(List<ElectricityReading> electricityReadings) {
        ElectricityReading first = electricityReadings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .get();

        ElectricityReading last = electricityReadings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .get();

        return BigDecimal.valueOf(Duration.between(first.time(), last.time()).getSeconds() / 3600.0);
    }

    /**
     * refactored below code extracted from controller and made methods private to achieve abstraction and encapsulation.
     *
     * @param smartMeterId
     * @return
     */
    public Map<String, Object> getCostForEachPricePlan(String smartMeterId) {
        String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans = getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);
        if (consumptionsForPricePlans.isPresent()) {
            return createResponseMapForComparisons(pricePlanId, consumptionsForPricePlans.get());
        } else {
            //preparing the response map which can be used in creating response entity in controller class.
            return (Map<String, Object>) new HashMap<>()
                    .put(PRICE_PLAN_ID_KEY, new NoReadingsExistForMeterId(NO_METER_READING_FOUND + pricePlanId));
        }
    }

    //private method for cleaner code.
    private Map<String, Object> createResponseMapForComparisons(String pricePlanId, Map<String, BigDecimal> consumptions) {
        Map<String, Object> pricePlanComparisons = new HashMap<>();
        pricePlanComparisons.put(PRICE_PLAN_ID_KEY, pricePlanId);
        pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptions);
        return pricePlanComparisons;
    }

    public List<Map.Entry<String, BigDecimal>> getRecommendations(String smartMeterId, Integer limit) {
        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                getConsumptionCostOfElectricityReadingsForEachPricePlan(smartMeterId);

        List<Map.Entry<String, BigDecimal>> recommendations = null;
        if (!consumptionsForPricePlans.isPresent()) {
            //setting the response when consumptionsForPricePlans is empty.
            recommendations = new ArrayList<>();
            recommendations.add(new AbstractMap.SimpleEntry<>(ERROR, BigDecimal.ZERO));
            return recommendations;
        }
        recommendations = new ArrayList<>(consumptionsForPricePlans.get().entrySet());
        recommendations.sort(Comparator.comparing(Map.Entry::getValue));

        if (limit != null && limit < recommendations.size()) {
            recommendations = recommendations.subList(0, limit);
        }
        return recommendations;
    }
}
