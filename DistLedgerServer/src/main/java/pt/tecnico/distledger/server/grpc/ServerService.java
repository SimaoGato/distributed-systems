package pt.tecnico.distledger.server.grpc;

import io.grpc.*;
import pt.tecnico.distledger.server.domain.operation.DistLedgerOperationVisitor;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.serverExceptions.NoServerAvailableException;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ServerService {

    private final ManagedChannel namingChannel;

    private Map<String, DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub> stubCache;

    private Map<String, ManagedChannel> channelCache;

    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;

    private static boolean DEBUG_FLAG;

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            logger.info(debugMessage);
    }

    public ServerService(String target, final boolean DEBUG_FLAG) {
        namingChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        namingServerStub = NamingServerServiceGrpc.newBlockingStub(namingChannel);
        this.stubCache = new HashMap<>();
        this.channelCache = new HashMap<>();
        ServerService.DEBUG_FLAG = DEBUG_FLAG;
    }

    public void closeChannel() {
        this.namingChannel.shutdownNow();
    }

    public void registerService(String service, String qualifier, String target) {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setServiceName(service)
                .setQualifier(qualifier)
                .setServerAddress(target)
                .build();
        RegisterResponse response = namingServerStub.register(request);
    }

    public void deleteService(String service, String target) {
        DeleteRequest request = DeleteRequest.newBuilder()
                .setServiceName(service)
                .setServerAddress(target)
                .build();
        DeleteResponse response = namingServerStub.delete(request);

        channelCache.forEach((s, channel) -> {
            channel.shutdownNow();
        });
        channelCache.clear();

        ManagedChannel channel = (ManagedChannel) namingServerStub.getChannel();
        channel.shutdownNow();
    }

    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub lookupService(String service, String qualifier)
            throws NoServerAvailableException {
        LookupRequest request = LookupRequest.newBuilder()
                .setServiceName(service)
                .setQualifier(qualifier)
                .build();
        LookupResponse response = namingServerStub.lookup(request);

        if (response.getServerListList().isEmpty()) {
            throw new NoServerAvailableException();
        }
        else {
            return addStub(response.getServerList(0));
        }
    }

    public boolean propagateStateService(List<Operation> ledger, String destQualifier, List<Integer> replicaTS, List<Integer> otherReplicaTS) {
        ManagedChannel serverChannel;
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub serverStub;

        // Creating LedgerState object
        DistLedgerOperationVisitor visitor = new DistLedgerOperationVisitor();
        for (Operation operation : ledger) {
            if(isLess(otherReplicaTS, operation.getTS()))
                operation.accept(visitor);
        }
        List<DistLedgerCommonDefinitions.Operation> distLedgerOperations = visitor.getDistLedgerOperations();
        DistLedgerCommonDefinitions.LedgerState ledgerState = DistLedgerCommonDefinitions.LedgerState.newBuilder().addAllLedger(distLedgerOperations).build();

        CrossServerDistLedger.PropagateStateRequest request = CrossServerDistLedger.PropagateStateRequest.newBuilder()
                .setState(ledgerState).addAllReplicaTS(replicaTS)
                .build();

        // Propagate the operation to every server found
        try {
            serverStub = getStub(destQualifier);
            CrossServerDistLedger.PropagateStateResponse response = serverStub.propagateState(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getDescription().equals("UNAVAILABLE")) {
                debug("No other servers are available");
                return false;
            }
            else if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                debug("other server either doesnt exist or port changed");
                try {
                    serverStub = lookupService("DistLedger", destQualifier);
                    CrossServerDistLedger.PropagateStateResponse response = serverStub.propagateState(request);
                } catch (NoServerAvailableException exp) {
                    return false;
                }
            }
        } catch (NoServerAvailableException exp) {
            debug("There's no other servers");
            return false;
        }

        return true;
    }

    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub getStub(String serverQualifier) throws NoServerAvailableException {
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = stubCache.get(serverQualifier);
        if (stub == null) {
            try {
                stub = lookupService("DistLedger", serverQualifier);
            } catch (NoServerAvailableException e) {
                throw e;
            }
            stubCache.put(serverQualifier, stub);
        }
        return stub;
    }

    public DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub addStub(NamingServerDistLedger.ServerEntry serverEntry) {
        ManagedChannel newChannel = ManagedChannelBuilder.forTarget(serverEntry.getTarget()).usePlaintext().build();
        debug("channel created: " + newChannel.toString());

        ManagedChannel channel = channelCache.get("B");
        if (channel != null) {
            channel.shutdownNow();
            channelCache.remove(channel);
        }

        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub newStub =
                DistLedgerCrossServerServiceGrpc.newBlockingStub(newChannel);

        debug("stub created" + newStub.toString());

        stubCache.put(serverEntry.getQualifier(), newStub);
        channelCache.put(serverEntry.getQualifier(), newChannel);

        return newStub;
    }

    private boolean isLess(List<Integer> TS1, List<Integer> TS2) {
        boolean var = false;

        for (int i = 0; i < TS1.size(); i++) {
            if (TS1.get(i) < TS2.get(i)) {
                var = true;
            }
            else if(TS1.get(i) > TS2.get(i)) {
                return false;
            }
        }
        return var;
    }
}