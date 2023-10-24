package uk.tw.energy.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(InvalidParametersException.class)
    public ResponseEntity<String> handleInvalidParameterException(InvalidParametersException exception) {
        //ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), exception.getErrorCode());
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoReadingsExistForMeterId.class)
    public ResponseEntity<String> handleNoReadingsExistForMeterId(NoReadingsExistForMeterId exception) {
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.NO_CONTENT);
    }
}
