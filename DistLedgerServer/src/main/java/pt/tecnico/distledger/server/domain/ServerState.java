package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.grpc.ServerService;
import pt.tecnico.distledger.server.serverExceptions.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.*;

public class ServerState {

    private static final int ACTIVE = 1;

    private static final int INACTIVE = 0;

    private final List<Operation> ledger;

    private final Map<String, Integer> accounts;

    private final List<Integer> replicaTS;

    private final List<Integer> valueTS;

    private List<Integer> otherReplicaTS;

    private int status;

    private final String qualifier;

    private final ServerService serverService;

    public ServerState(ServerService serverService, String qualifier) {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.replicaTS = Arrays.asList(0, 0);
        this.valueTS = Arrays.asList(0, 0);
        this.otherReplicaTS = Arrays.asList(0, 0);
        this.status = ACTIVE;
        this.accounts.put("broker", 1000);
        this.qualifier = qualifier;
        this.serverService = serverService;
    }

    public synchronized CreateOp createAccount(String userId, List<Integer> prevTS, boolean gossip) throws ServerUnavailableException {
        
        if (isInactive()) { throw new ServerUnavailableException(); }

        incrementTS(replicaTS, gossip);

        CreateOp createOp = new CreateOp(userId, prevTS, updateOpTS(prevTS, replicaTS), false);

        ledger.add(createOp);

        return createOp;
    }
    
    public synchronized void checkCreateStability(CreateOp createOp, boolean gossip) {

        String userId = createOp.getAccount();

        List<Integer> prevTS = createOp.getPrevTS();

        if (accounts.containsKey(userId)) {
            ledger.remove(createOp);
            merge(this.valueTS, this.replicaTS);
            return;
        }

        if (isLessOrEqual(prevTS, this.valueTS)) {
            merge(this.valueTS, this.replicaTS);
            createOp.setStable(true);
            accounts.put(userId, 0);
        }
    }

    public synchronized int getBalanceById(String userId, List<Integer> prevTS) throws AccountDoesntExistException,
            ServerUnavailableException, UserIsAheadOfServerException {
        if (isInactive()) { throw new ServerUnavailableException(); }

        Integer balance = accounts.get(userId);

        if (balance == null) { throw new AccountDoesntExistException(); }

        if (!isLessOrEqual(prevTS, valueTS)) { throw new UserIsAheadOfServerException(); }

        return balance;
    }

    public synchronized TransferOp transferTo(String userId, String destAccount, int amount, List<Integer> prevTS, boolean gossip) throws
            ServerUnavailableException {

        if (isInactive()) { throw new ServerUnavailableException(); }

        incrementTS(replicaTS, gossip);

        TransferOp transferOp = new TransferOp(userId, destAccount, amount, prevTS, updateOpTS(prevTS, replicaTS), false);

        ledger.add(transferOp);

        return transferOp;
    }

    public synchronized void checkTransferStability(TransferOp transferOp, boolean gossip) {
        String userId = transferOp.getAccount();
        String destAccount = transferOp.getDestAccount();
        int amount = transferOp.getAmount();
        List<Integer> prevTS = transferOp.getPrevTS();

        if (userId.equals(destAccount)) {
            ledger.remove(transferOp);
            merge(this.valueTS, this.replicaTS);
            return;
        }

        Integer senderBalance = accounts.get(userId);
        Integer receiverBalance = accounts.get(destAccount);

        if ((senderBalance == null) || (receiverBalance == null) || (amount <= 0) || (senderBalance < amount)) {
            ledger.remove(transferOp);
            merge(this.valueTS, this.replicaTS);
            return;
        }

        if (isLessOrEqual(prevTS, this.valueTS)) {
            merge(this.valueTS, this.replicaTS);
            transferOp.setStable(true);
            accounts.put(userId, senderBalance - amount);
            accounts.put(destAccount, receiverBalance + amount);
        }
    }

    public void activateServer() { this.status = ACTIVE; }

    public void deactivateServer() { this.status = INACTIVE; }

    public List<Operation> getLedgerState() { return ledger; }

    public void gossip() {
        String destQualifier = "A";
        if (Objects.equals(this.qualifier, "A")) destQualifier = "B";
        serverService.propagateStateService(this.ledger, destQualifier, this.replicaTS, this.otherReplicaTS);
    }

    public void applyGossip(List<DistLedgerCommonDefinitions.Operation> otherLedger, List<Integer> otherReplicaTS) {
        this.otherReplicaTS = otherReplicaTS;

        // Add to updateLog
        for(DistLedgerCommonDefinitions.Operation op : otherLedger) {
            DistLedgerCommonDefinitions.OperationType operationType = op.getType();
            if (operationType.equals(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT)) {
                createAccountGossip(op.getUserId(), op.getPrevTSList(), op.getTSList(), true);
            }
            else if (operationType.equals(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO)) {
                transferToGossip(op.getUserId(), op.getDestUserId(), op.getAmount(), op.getPrevTSList(), op.getTSList(), true);
            }
        }

        boolean updates = true;

        while(updates) {
            updates = false;
            for(Operation op : this.ledger) {
                if(!op.isStable()) {
                    if (op instanceof CreateOp) {
                        if(checkCreateStabilityGossip((CreateOp) op, true))
                            updates = true;
                    } else if (op instanceof TransferOp) {
                        if (checkTransferStabilityGossip((TransferOp) op, true))
                            updates = true;
                    }
                }
            }
        }

    }

    public List<Integer> getReplicaTS() {
        return replicaTS;
    }

    public List<Integer> getValueTS() {
        return valueTS;
    }

    public synchronized void createAccountGossip(String userId, List<Integer> prevTS, List<Integer> TS, boolean gossip) {
        incrementTS(replicaTS, gossip);

        CreateOp createOp = new CreateOp(userId, prevTS, TS, false);

        ledger.add(createOp);
    }

    public synchronized boolean checkCreateStabilityGossip(CreateOp createOp, boolean gossip) {

        List<Integer> prevTS = createOp.getPrevTS();

        if (isLessOrEqual(prevTS, this.valueTS)) {
            String userId = createOp.getAccount();

            if (accounts.containsKey(userId)) {
                ledger.remove(createOp);
                merge(this.valueTS, this.replicaTS);
                return true;
            }
            merge(this.valueTS, this.replicaTS);
            createOp.setStable(true);
            accounts.put(userId, 0);
            return true;
        }
        return false;
    }

    public synchronized void transferToGossip(String userId, String destAccount, int amount, List<Integer> prevTS, List<Integer> TS, boolean gossip) {
        incrementTS(replicaTS, gossip);

        TransferOp transferOp = new TransferOp(userId, destAccount, amount, prevTS, TS, false);

        ledger.add(transferOp);
    }

    public synchronized boolean checkTransferStabilityGossip(TransferOp transferOp, boolean gossip) {
        List<Integer> prevTS = transferOp.getPrevTS();

        if (isLessOrEqual(prevTS, this.valueTS)) {
            String userId = transferOp.getAccount();
            String destAccount = transferOp.getDestAccount();
            int amount = transferOp.getAmount();

            if (userId.equals(destAccount)) {
                ledger.remove(transferOp);
                merge(this.valueTS, this.replicaTS);
                return true;
            }

            Integer senderBalance = accounts.get(userId);
            Integer receiverBalance = accounts.get(destAccount);

            if ((senderBalance == null) || (receiverBalance == null) || (amount <= 0) || (senderBalance < amount)) {
                ledger.remove(transferOp);
                merge(this.valueTS, this.replicaTS);
                return true;
            }

            merge(this.valueTS, this.replicaTS);
            transferOp.setStable(true);
            accounts.put(userId, senderBalance - amount);
            accounts.put(destAccount, receiverBalance + amount);
            return true;
        }
        return false;
    }

    private boolean isInactive() { return status == INACTIVE; }

    private boolean isLessOrEqual(List<Integer> TS1, List<Integer> TS2) {
        for (int i = 0; i < TS1.size(); i++) {
            if (TS1.get(i) > TS2.get(i)) {
                return false;
            }
        }
        return true;
    }

    private void incrementTS(List<Integer> TS, boolean gossip) {
        if (!gossip) {
            if (qualifier.charAt(0) == 'A') {
                TS.set(0, TS.get(0) + 1);
            } else {
                TS.set(1, TS.get(1) + 1);
            }
        } else {
            if (qualifier.charAt(0) == 'A') {
                TS.set(1, TS.get(1) + 1);
            } else {
                TS.set(0, TS.get(0) + 1);
            }
        }
    }

    private List<Integer> updateOpTS(List<Integer> prevTS, List<Integer> replicaTS) {
        List<Integer> TS = Arrays.asList(0, 0);

        if (qualifier.charAt(0) == 'A') {
            TS.set(0, replicaTS.get(0));
            TS.set(1, prevTS.get(1));
        } else {
            TS.set(0, prevTS.get(0));
            TS.set(1, replicaTS.get(1));
        }

        return TS;
    }

    private void merge(List<Integer> TS1, List<Integer> TS2) {
        for(int i = 0; i< TS1.size(); i++) {
            TS1.set(i, Math.max(TS1.get(i), TS2.get(i)));
        }
    }

    private boolean isBroker(String userId) { return "broker".equals(userId); }

}