package pt.tecnico.distledger.server.serverExceptions;

public class ServerUnavailableException extends Exception {
    public ServerUnavailableException() {
        super();
    }

    @Override
    public String getMessage() {
        return "UNAVAILABLE";
    }
}
