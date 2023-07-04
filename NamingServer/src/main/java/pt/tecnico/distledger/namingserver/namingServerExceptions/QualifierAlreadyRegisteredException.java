package pt.tecnico.distledger.namingserver.namingServerExceptions;

public class QualifierAlreadyRegisteredException extends Exception{

    public QualifierAlreadyRegisteredException() { super(); }

    @Override
    public String getMessage() {return "Not possible to register the server"; }

}
