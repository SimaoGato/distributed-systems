package pt.tecnico.distledger.userclient.userExceptions;

public class NoServerAvailableException extends Exception {

    public NoServerAvailableException() {
        super();
    }

    @Override
    public String getMessage() {
        return "Naming Server has no server addresses available";
    }
}
