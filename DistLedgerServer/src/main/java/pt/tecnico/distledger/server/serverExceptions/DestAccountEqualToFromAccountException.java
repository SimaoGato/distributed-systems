package pt.tecnico.distledger.server.serverExceptions;

public class DestAccountEqualToFromAccountException extends Exception {
    public DestAccountEqualToFromAccountException() {
        super();
    }

    public String getMessage(String userFrom, String userTo) {
        return "Destination account " + userFrom + " is equal to from account " + userTo;
    }
}
