package pt.tecnico.distledger.server.serverExceptions;

public class BalanceIsntZeroException extends Exception {
    public BalanceIsntZeroException() {
        super();
    }

    @Override
    public String getMessage() {
        return "Balance not zero";
    }
}
