package uk.tw.energy.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.exceptions.InvalidParametersException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static uk.tw.energy.exceptions.Constants.*;

@Service
public class MeterReadingService {

    //The Meter Reading Service class could have implemented an interface in order to use the methods.
    private static final Logger logger = Logger.getLogger(MeterReadingService.class.getName());
    private final Map<String, List<ElectricityReading>> meterAssociatedReadings;

    public MeterReadingService(Map<String, List<ElectricityReading>> meterAssociatedReadings) {
        this.meterAssociatedReadings = meterAssociatedReadings;
    }

    public Optional<List<ElectricityReading>> getReadings(String smartMeterId) {
        return Optional.ofNullable(meterAssociatedReadings.get(smartMeterId));
    }

    /**
     * Extracted the business logic from the controller to service layer and
     * changed the below method and incorporated new abstraction layer to have separate methods.
     * @param meterReadings
     * @return
     */
    public String storeReadings(MeterReadings meterReadings) {
        boolean response;
        logger.info("Going for the validation of input parameters.");
        response = validateMeterReadings(meterReadings);
        if(!response)
            return INPUT_IS_INVALID;
        // Appending all the new entries to the existing key value pair.
        if (meterAssociatedReadings.containsKey(meterReadings.smartMeterId())) {
            meterAssociatedReadings.get(meterReadings.smartMeterId()).addAll(meterReadings.electricityReadings());
        }
        //Adding the entry if it's not already present.
        meterAssociatedReadings.put(meterReadings.smartMeterId(), new ArrayList<>());
        return SUCCESS;
    }

    private boolean validateMeterReadings(MeterReadings meterReadings) {
        //correction implemented below: null check was not present so incorporated the null check.
        if (meterReadings != null) {
            String smartMeterId = meterReadings.smartMeterId();
            List<ElectricityReading> electricityReadings = meterReadings.electricityReadings();
            return smartMeterId != null && !smartMeterId.isEmpty()
                    && electricityReadings != null && !electricityReadings.isEmpty()
                    && isMeterReadingsValid(smartMeterId)
                    &&isValidElectricityReadings(electricityReadings);
        }
        return false;
    }

    /**
     * The below methods check for the pattern and validity of the data coming in for the paramters present.
     * @param smartMeterId
     * @return
     */
    private boolean isMeterReadingsValid(String smartMeterId) {
        logger.info("Checking whether the smartMeterId is valid"+ smartMeterId);
        if(!smartMeterId.matches(SMART_METER_ID_REGEX))
            return false;
        return true;
    }
    public static boolean isValidElectricityReadings(List<ElectricityReading> electricityReadings) {
        logger.info("Checking whether the electricityReadings are valid");
        for (ElectricityReading reading : electricityReadings) {
            if (isValidElectricityReading(reading)) {
            } else {
                logger.info("Data types are incorrect: " + reading);
                return false;
            }
        }
        return true;
    }

    private static boolean isValidElectricityReading(ElectricityReading electricityReading) {
        // Check the data types of the parameters
        boolean isTimeValid = electricityReading.time() instanceof Instant;
        boolean isReadingValid = electricityReading.reading() instanceof BigDecimal;

        return isTimeValid && isReadingValid;
    }
}
