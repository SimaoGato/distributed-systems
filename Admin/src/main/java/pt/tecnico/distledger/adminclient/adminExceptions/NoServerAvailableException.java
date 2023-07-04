package pt.tecnico.distledger.adminclient.adminExceptions;

public class NoServerAvailableException extends Exception {

    public NoServerAvailableException() {super();}

    @Override
    public String getMessage() {return "There is no server available"; }
}
