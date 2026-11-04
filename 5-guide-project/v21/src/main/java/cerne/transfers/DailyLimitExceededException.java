package cerne.transfers;

public class DailyLimitExceededException extends RuntimeException {

    public DailyLimitExceededException(String account) {
        super("Daily limit exceeded on account " + account);
    }
}
