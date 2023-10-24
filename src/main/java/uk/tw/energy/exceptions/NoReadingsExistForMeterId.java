package uk.tw.energy.exceptions;

public class NoReadingsExistForMeterId extends RuntimeException {

    public NoReadingsExistForMeterId(String message) {
        super(message);
    }
}
