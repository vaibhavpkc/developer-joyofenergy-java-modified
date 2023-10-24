package uk.tw.energy.controller;

import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.exceptions.InvalidParametersException;
import uk.tw.energy.exceptions.NoReadingsExistForMeterId;
import uk.tw.energy.service.MeterReadingService;

import java.util.List;
import java.util.Optional;

import static uk.tw.energy.exceptions.Constants.*;

@RestController
@RequestMapping("/readings")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    public MeterReadingController(MeterReadingService meterReadingService) {
        this.meterReadingService = meterReadingService;
    }

    /**
     * Refactored controller class
     * @param meterReadings
     * @return
     * @throws InvalidParametersException
     */
    @PostMapping("/store")
    public ResponseEntity<String> storeMeterReadings(@RequestBody MeterReadings meterReadings)throws InvalidParametersException {
        String response = "";
        try {
           response = meterReadingService.storeReadings(meterReadings);
            if(response.equals(SUCCESS))
                return ResponseEntity.ok(READINGS_STORED_SUCCESS);
            else
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InvalidParametersException(INPUT_IS_INVALID).getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ERROR);
        }
    }

    //Incorporating pattern check and having Optional List being returned for the generic ResponseEntity object.

    @GetMapping("/read/{smartMeterId}")
    @Pattern(regexp = SMART_METER_ID_REGEX, message = INVALID_SMART_METER_ID)
    public ResponseEntity<Optional<List<ElectricityReading>>>  readReadings(@PathVariable String smartMeterId) {

        try{
            Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);
            return readings.isPresent()
                    ? ResponseEntity.ok(readings)
                    : ResponseEntity.status(HttpStatus.NO_CONTENT).body(Optional.empty());
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Optional.empty());
        }
    }
}
