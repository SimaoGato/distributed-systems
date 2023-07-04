package pt.tecnico.distledger.namingserver.namingServerExceptions;

public class TargetAlreadyRegisteredException extends Exception{

    public TargetAlreadyRegisteredException() { super(); }

    @Override
    public String getMessage() {return "Not possible to register the server"; }

}
