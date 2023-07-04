package pt.tecnico.distledger.server.serverExceptions;

public class AccountAlreadyExistsException extends Exception {

    public AccountAlreadyExistsException() {
        super();
    }

    public String getMessage(String user) {
        return "Username " + user + " already taken";
    }
}
