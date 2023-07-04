package pt.tecnico.distledger.server.serverExceptions;

public class InvalidAmountException extends Exception {
    public InvalidAmountException() {
        super();
    }

    public String getMessage(int amount) {
        return "Amount has to be greater than zero: " + amount;
    }
}
