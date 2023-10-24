package uk.tw.energy.controller;

import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.exceptions.NoReadingsExistForMeterId;
import uk.tw.energy.service.PricePlanService;

import java.math.BigDecimal;
import java.util.*;

import static uk.tw.energy.exceptions.Constants.*;

@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    /**
     * Constants which are public can be declared in a separate constant class.
     */

    private final PricePlanService pricePlanService;
    private static final String LIMIT = "limit";
    private static final String INVALID_LIMIT = "Limit is not correct.";
    private static final String LIMIT_REGEX = "^[0-9]{2}$";


    public PricePlanComparatorController(PricePlanService pricePlanService) {
        this.pricePlanService = pricePlanService;
    }

    /**
     * Added @pattern to match the string coming in from the input request. It should not contain numeric or special characters
     * Made the below public method more abstract so that the internal implementations
     * are not visible for modification easily to the outside classes.
     * Also, have the ResponseEntity generic class return Map as ResponseEntity object.
     */
    @GetMapping("/compare-all/{smartMeterId}")
    @Pattern(regexp = SMART_METER_ID_REGEX, message = INVALID_SMART_METER_ID)
    public ResponseEntity<Map<String, Object>> calculatedCostForEachPricePlan(@PathVariable String smartMeterId) throws NoReadingsExistForMeterId {
        try {
            Map<String, Object> responseMap = pricePlanService.getCostForEachPricePlan(smartMeterId);
            if (!responseMap.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).body(responseMap);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseMap);
            }
        } catch (Exception e) {
            // Handle other exceptions, logging the error and returning an appropriate response.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).
                    body(Collections.singletonMap(ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value() + e.getMessage()));
        }
    }

    /**
     * Suggestion Optional List can also be used if the response is not mandatory.
     * @param smartMeterId
     * @param limit
     * @return
     * @throws NoReadingsExistForMeterId
     */
    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendCheapestPricePlans(
            @PathVariable @Pattern(regexp = SMART_METER_ID_REGEX, message = INVALID_SMART_METER_ID) String smartMeterId,
            @RequestParam(value = LIMIT, required = false) @Pattern(regexp = LIMIT_REGEX, message = INVALID_LIMIT) Integer limit)
            throws NoReadingsExistForMeterId {
        List<Map.Entry<String, BigDecimal>> responseList;
        try {
            responseList = pricePlanService.getRecommendations(smartMeterId, limit);
            if (responseList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(responseList);
            }
        } catch (Exception e) {
            // Handle other exceptions, logging the error and returning an appropriate response.
            List<Map.Entry<String, BigDecimal>> errorResponse = new ArrayList<>();
            errorResponse.add(new AbstractMap.SimpleEntry<>(ERROR, BigDecimal.ZERO));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseList);
    }
}
