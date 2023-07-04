package pt.tecnico.distledger.server.serverExceptions;

public class DestAccountDoesntExistException extends Exception {
    public DestAccountDoesntExistException() {
        super();
    }

    public String getMessage(String user) {
        return "AccountTo " + user + " not found";
    }
}
