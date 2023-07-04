package pt.tecnico.distledger.server.domain.operation;

public interface OperationVisitor {
    public void visitOperation(Operation operation);
    public void visitCreateOp(CreateOp operation);
    public void visitTransferOp(TransferOp operation);
}
