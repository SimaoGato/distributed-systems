package pt.tecnico.distledger.server.domain.operation;

import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.ArrayList;
import java.util.List;

public class DistLedgerOperationVisitor implements OperationVisitor {
    private List<DistLedgerCommonDefinitions.Operation> distLedgerOperations = new ArrayList<>();

    @Override
    public void visitOperation(Operation operation) {
    }

    @Override
    public void visitCreateOp(CreateOp operation) {
        DistLedgerCommonDefinitions.Operation distLedgerOperation = DistLedgerCommonDefinitions.Operation.newBuilder()
                .setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT)
                .setUserId(operation.getAccount())
                .addAllPrevTS(operation.getPrevTS())
                .addAllTS(operation.getTS())
                .build();
        distLedgerOperations.add(distLedgerOperation);
    }

    @Override
    public void visitTransferOp(TransferOp operation) {
        DistLedgerCommonDefinitions.Operation distLedgerOperation = DistLedgerCommonDefinitions.Operation.newBuilder()
                .setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO)
                .setUserId(operation.getAccount())
                .setDestUserId(operation.getDestAccount())
                .setAmount(operation.getAmount())
                .addAllPrevTS(operation.getPrevTS())
                .addAllTS(operation.getTS())
                .build();
        distLedgerOperations.add(distLedgerOperation);
    }

    public List<DistLedgerCommonDefinitions.Operation> getDistLedgerOperations() {
        return distLedgerOperations;
    }
}
