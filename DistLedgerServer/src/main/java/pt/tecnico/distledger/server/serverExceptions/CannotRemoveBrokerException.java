package pt.tecnico.distledger.server.serverExceptions;

public class CannotRemoveBrokerException extends Exception {
    public CannotRemoveBrokerException() {
        super();
    }

    @Override
    public String getMessage() {
        return "Cannot delete broker account";
    }
}
