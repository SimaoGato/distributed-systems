package pt.tecnico.distledger.namingserver.namingServerExceptions;

public class NotPossibleToRemoveServerException extends Exception {

    public NotPossibleToRemoveServerException() { super(); }

    @Override
    public String getMessage() {return "Not possible to remove the server"; }

}
