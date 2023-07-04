package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;
import pt.tecnico.distledger.adminclient.adminExceptions.NoServerAvailableException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AdminService {

    private static boolean DEBUG_FLAG;

    private static final Logger logger = Logger.getLogger(AdminService.class.getName());

    private Map<String, AdminServiceGrpc.AdminServiceBlockingStub> stubCache;

    private Map<String, ManagedChannel> channelCache;

    private static final String namingServerTarget = "localhost:5001";

    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            logger.info(debugMessage);
    }

    public AdminService(final boolean DEBUG_FLAG) {

        AdminService.DEBUG_FLAG = DEBUG_FLAG;

        this.stubCache = new HashMap<>();
        this.channelCache = new HashMap<>();

        ManagedChannel channel = ManagedChannelBuilder.forTarget(namingServerTarget).usePlaintext().build();
        debug("channel created: " + channel.toString());
        channelCache.put("NamingServer", channel);

        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(channel);
        debug("stub created" + namingServerStub.toString());
    }

    public AdminServiceGrpc.AdminServiceBlockingStub addStub(NamingServerDistLedger.ServerEntry serverEntry) {
        ManagedChannel newChannel = ManagedChannelBuilder.forTarget(serverEntry.getTarget()).usePlaintext().build();
        debug("channel created: " + newChannel.toString());

        ManagedChannel channel = channelCache.get(serverEntry.getQualifier());
        if (channel != null) {
            channel.shutdownNow();
            channelCache.remove(channel);
        }

        channelCache.put(serverEntry.getQualifier(), newChannel);

        AdminServiceGrpc.AdminServiceBlockingStub newStub = AdminServiceGrpc.newBlockingStub(newChannel);
        debug("stub created" + newStub.toString());

        stubCache.put(serverEntry.getQualifier(), newStub);

        return newStub;
    }

    private AdminServiceGrpc.AdminServiceBlockingStub getStub(String serverQualifier) throws NoServerAvailableException {
        AdminServiceGrpc.AdminServiceBlockingStub stub = stubCache.get(serverQualifier);
        if (stub == null) {
            stub = lookupService(serverQualifier);
            stubCache.put(serverQualifier, stub);
        }
        debug("getStub returned: " + stub.toString());
        return stub;
    }

    public AdminServiceGrpc.AdminServiceBlockingStub lookupService(String qualifier) throws NoServerAvailableException {
        NamingServerDistLedger.LookupRequest request = NamingServerDistLedger.LookupRequest.newBuilder()
                .setServiceName("DistLedger")
                .setQualifier(qualifier)
                .build();

        NamingServerDistLedger.LookupResponse response = namingServerStub.lookup(request);
        debug("lookupService response: " + response.toString());

        if (response.getServerListList().isEmpty()) {
            throw new NoServerAvailableException();
        }
        return addStub(response.getServerList(0));
    }

    public void closeAllChannels() {
        channelCache.forEach((namingServer, channel) -> {
            channel.shutdownNow();
        });
        channelCache.clear();

        debug("All channels shutdown");
    }

    public String activateServer(String serverQualifier) {
        AdminServiceGrpc.AdminServiceBlockingStub stub;
        try {
            stub = getStub(serverQualifier);
            return activateRequest(stub);
        } catch (StatusRuntimeException e) {
            debug("admin received activateServer error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    stub = lookupService(serverQualifier);
                    return activateRequest(stub);
                } catch (NoServerAvailableException exp) {
                    return "Caught exception with description: " + exp.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException exp) {
            return "Caught exception with description: " + exp.getMessage() + "\n";
        }
    }

    public String deactivateServer(String serverQualifier) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = getStub(serverQualifier);
            return deactivateRequest(stub);
        } catch (StatusRuntimeException e) {
            debug("admin received deactivateServer error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    AdminServiceGrpc.AdminServiceBlockingStub stub = lookupService(serverQualifier);
                    return deactivateRequest(stub);
                } catch (NoServerAvailableException exp) {
                    return exp.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException exp) {
            return exp.getMessage() + "\n";
        }
    }

    public String getLedgerState(String serverQualifier) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = getStub(serverQualifier);
            return getLedgerStateRequest(stub);
        } catch (StatusRuntimeException e) {
            debug("admin received getLedgerState error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    AdminServiceGrpc.AdminServiceBlockingStub stub = lookupService(serverQualifier);
                    return getLedgerStateRequest(stub);
                } catch (NoServerAvailableException exp) {
                    return exp.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException exp) {
            return exp.getMessage() + "\n";
        }
    }

    public String gossip(String serverQualifier) {
        try {
            AdminServiceGrpc.AdminServiceBlockingStub stub = getStub(serverQualifier);
            return gossipRequest(stub);
        } catch (StatusRuntimeException e) {
            debug("admin received gossip error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    AdminServiceGrpc.AdminServiceBlockingStub stub = lookupService(serverQualifier);
                    return gossipRequest(stub);
                } catch (NoServerAvailableException exp) {
                    return exp.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException exp) {
            return exp.getMessage() + "\n";
        }
    }

    public String activateRequest(AdminServiceGrpc.AdminServiceBlockingStub stub) {
        AdminDistLedger.ActivateRequest request = AdminDistLedger.ActivateRequest.newBuilder().build();
        AdminDistLedger.ActivateResponse response = stub.activate(request);
        return "OK\n";
    }

    public String deactivateRequest(AdminServiceGrpc.AdminServiceBlockingStub stub) {
        AdminDistLedger.DeactivateRequest request = AdminDistLedger.DeactivateRequest.newBuilder().build();
        AdminDistLedger.DeactivateResponse response = stub.deactivate(request);
        return "OK\n";
    }
    public String getLedgerStateRequest(AdminServiceGrpc.AdminServiceBlockingStub stub) {
        AdminDistLedger.getLedgerStateRequest request = AdminDistLedger.getLedgerStateRequest.newBuilder().build();
        AdminDistLedger.getLedgerStateResponse response = stub.getLedgerState(request);
        return "OK\n" + response.toString();
    }
    public String gossipRequest(AdminServiceGrpc.AdminServiceBlockingStub stub) {
        AdminDistLedger.GossipRequest request = AdminDistLedger.GossipRequest.newBuilder().build();
        AdminDistLedger.GossipResponse response = stub.gossip(request);
        return "OK\n" + response.toString();
    }
}
