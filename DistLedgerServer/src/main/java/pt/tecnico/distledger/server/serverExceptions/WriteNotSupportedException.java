package pt.tecnico.distledger.server.serverExceptions;

public class WriteNotSupportedException extends Exception {

    public WriteNotSupportedException() { super(); }

    @Override
    public String getMessage() {
        return "Writing is not supported on this server";
    }
}
