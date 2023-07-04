package pt.tecnico.distledger.server.serverExceptions;

public class UserIsAheadOfServerException extends Exception {

    public UserIsAheadOfServerException() { super(); }

    @Override
    public String getMessage() { return "User is ahead of server"; }
}
