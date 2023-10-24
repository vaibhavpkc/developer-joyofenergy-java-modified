package uk.tw.energy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.tw.energy.exceptions.NoReadingsExistForMeterId;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.tw.energy.exceptions.Constants.PRICE_PLAN_COMPARISONS_KEY;
import static uk.tw.energy.exceptions.Constants.PRICE_PLAN_ID_KEY;

@ExtendWith(SpringExtension.class)
public class PricePlanComparatorControllerTestNew {

    @InjectMocks
    private PricePlanComparatorController controller;

    @Mock
    private PricePlanService pricePlanService;

    private static final String VALID_METER_ID = "smart-meter-0";
    private static final String INVALID_METER_ID = "smart-meter";
    private static final int VALID_LIMIT = 10;
    private static final int INVALID_LIMIT = 101;
    private static final String ERROR = "error";

    @BeforeEach
    void setUp() {
        controller = new PricePlanComparatorController(pricePlanService);
    }

    /**
     * {
     * "pricePlanComparisons": {
     * "price-plan-2": 13.6516,
     * "price-plan-1": 27.3032,
     * "price-plan-0": 136.5160
     * },
     * "pricePlanId": "price-plan-0"
     * }
     *
     * @throws NoReadingsExistForMeterId
     */
    @Test
    void testCalculatedCostForEachPricePlanWithValidData() throws NoReadingsExistForMeterId {
        String smartMeterId = VALID_METER_ID;
        Map<String, Object> pricePlanComparisons = createSamplePricePlanComparisons();
        when(pricePlanService.getCostForEachPricePlan(smartMeterId)).thenReturn(pricePlanComparisons);

        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan(smartMeterId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pricePlanComparisons, response.getBody());
    }

    @Test
    void testCalculatedCostForEachPricePlanWithException() throws NoReadingsExistForMeterId {
        String smartMeterId = INVALID_METER_ID;
        when(pricePlanService.getCostForEachPricePlan(smartMeterId)).thenThrow(NoReadingsExistForMeterId.class);

        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan(smartMeterId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Collections.singletonMap(ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value() + "null"), response.getBody());
    }

    @Test
    void testCalculatedCostForEachPricePlanWithEmptyData() throws NoReadingsExistForMeterId {
        String smartMeterId = "smart-meter-6";
        Map<String, Object> responseMap = Collections.emptyMap();

        when(pricePlanService.getCostForEachPricePlan(smartMeterId)).thenReturn(responseMap);

        ResponseEntity<Map<String, Object>> response = controller.calculatedCostForEachPricePlan(smartMeterId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals(responseMap, response.getBody());
    }

    /**
     * [
     * {
     * "price-plan-2": 13.6516
     * },
     * {
     * "price-plan-1": 27.3032
     * }
     * ]
     */
    @Test
    public void testValidRecommendation() throws NoReadingsExistForMeterId {
        List<Map.Entry<String, BigDecimal>> recommendationList = createSampleRecommendationList();
        when(pricePlanService.getRecommendations(VALID_METER_ID, VALID_LIMIT)).thenReturn(recommendationList);

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendCheapestPricePlans(VALID_METER_ID, VALID_LIMIT);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(recommendationList, response.getBody());
    }

    @Test
    public void testValidRecommendationWithDefaultLimit() throws NoReadingsExistForMeterId {
        List<Map.Entry<String, BigDecimal>> recommendationList = createSampleRecommendationList();
        when(pricePlanService.getRecommendations(VALID_METER_ID, null)).thenReturn(recommendationList);

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendCheapestPricePlans(VALID_METER_ID, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(recommendationList, response.getBody());
    }

    @Test
    public void testNoContentResponse() throws NoReadingsExistForMeterId {
        when(pricePlanService.getRecommendations(VALID_METER_ID, VALID_LIMIT)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendCheapestPricePlans(VALID_METER_ID, VALID_LIMIT);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void testInternalError() throws NoReadingsExistForMeterId {
        when(pricePlanService.getRecommendations(VALID_METER_ID, VALID_LIMIT)).thenThrow(new RuntimeException("Test error"));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendCheapestPricePlans(VALID_METER_ID, VALID_LIMIT);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        Map.Entry<String, BigDecimal> errorEntry = response.getBody().get(0);
        assertEquals(ERROR, errorEntry.getKey());
        assertEquals(BigDecimal.ZERO, errorEntry.getValue());
    }

    private List<Map.Entry<String, BigDecimal>> createSampleRecommendationList() {
        List<Map.Entry<String, BigDecimal>> recommendations = new ArrayList<>();
        recommendations.add(new AbstractMap.SimpleEntry<>("price-plan-1", new BigDecimal("10.0")));
        recommendations.add(new AbstractMap.SimpleEntry<>("price-plan-2", new BigDecimal("20.0")));
        return recommendations;
    }

    private Map<String, Object> createSamplePricePlanComparisons() {
        Map<String, Object> pricePlanComparisons = new HashMap<>();
        Map<String, BigDecimal> consumptions = new HashMap<>();
        consumptions.put("price-plan-0", BigDecimal.valueOf(136.5160));
        consumptions.put("price-plan-1", BigDecimal.valueOf(27.3032));
        consumptions.put("price-plan-2", BigDecimal.valueOf(13.6516));
        pricePlanComparisons.put(PRICE_PLAN_COMPARISONS_KEY, consumptions);
        pricePlanComparisons.put(PRICE_PLAN_ID_KEY, "price-plan-0");
        return pricePlanComparisons;
    }


}
