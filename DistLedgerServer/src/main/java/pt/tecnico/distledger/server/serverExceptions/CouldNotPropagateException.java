package pt.tecnico.distledger.server.serverExceptions;

public class CouldNotPropagateException extends Exception {

    public CouldNotPropagateException() { super(); }

    @Override
    public String getMessage() {
        return "Primary server could not propagate to secondary server";
    }
}
