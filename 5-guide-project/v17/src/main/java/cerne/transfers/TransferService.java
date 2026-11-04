package cerne.transfers;

/** Interface shared by the real service and the decorators (session 7). One method: a lambda works as a test double. */
@FunctionalInterface
public interface TransferService {

    TransferReceipt transfer(TransferRequest request);
}
