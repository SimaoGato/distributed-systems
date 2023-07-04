package pt.tecnico.distledger.server.serverExceptions;

public class TransferBiggerThanBalanceException extends Exception {
    public TransferBiggerThanBalanceException() {
        super();
    }

    public String getMessage(int amount) {
        return "Balance lower than amount " + amount + " to send";
    }
}
