package uk.tw.energy.exceptions;

public class InvalidParametersException extends RuntimeException {
    public InvalidParametersException(String message) {
        super(message);
    }
}
