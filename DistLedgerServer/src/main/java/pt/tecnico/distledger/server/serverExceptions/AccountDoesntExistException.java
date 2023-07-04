package pt.tecnico.distledger.server.serverExceptions;

public class AccountDoesntExistException extends Exception {
    public AccountDoesntExistException() {
        super();
    }

    public String getMessage(String user) {
        return "User " + user + " not found";
    }
}
