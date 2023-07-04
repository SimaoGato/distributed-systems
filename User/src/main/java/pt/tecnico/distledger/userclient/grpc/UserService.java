package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.userExceptions.NoServerAvailableException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import java.util.*;
import java.util.logging.Logger;

public class UserService {

    private static boolean DEBUG_FLAG;

    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    private final Map<String, UserServiceGrpc.UserServiceBlockingStub> stubCache;

    private final Map<String, ManagedChannel> channelCache;

    private final List<Integer> prevTS;

    private static final String namingServerTarget = "localhost:5001";

    private final NamingServerServiceGrpc.NamingServerServiceBlockingStub namingServerStub;

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG) logger.info(debugMessage);
    }

    public UserService(final boolean DEBUG_FLAG) {
        UserService.DEBUG_FLAG = DEBUG_FLAG;

        this.stubCache = new HashMap<>();
        this.channelCache = new HashMap<>();
        this.prevTS = Arrays.asList(0, 0);

        ManagedChannel channel = ManagedChannelBuilder.forTarget(namingServerTarget).usePlaintext().build();
        debug("naming channel created: " + channel.toString());
        channelCache.put("NamingServer", channel);

        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(channel);
        debug("naming stub created: " + namingServerStub.toString());
    }

    public UserServiceGrpc.UserServiceBlockingStub addStub(NamingServerDistLedger.ServerEntry serverEntry) {
        ManagedChannel newChannel = ManagedChannelBuilder.forTarget(serverEntry.getTarget()).usePlaintext().build();
        debug("addStub - channel created: " + newChannel.toString());

        ManagedChannel channel = channelCache.get(serverEntry.getQualifier());
        if (channel != null) {
            channel.shutdownNow();
            channelCache.remove(channel);
        }

        channelCache.put(serverEntry.getQualifier(), newChannel);

        UserServiceGrpc.UserServiceBlockingStub newStub = UserServiceGrpc.newBlockingStub(newChannel);
        debug("addStub - stub created" + newStub.toString());

        stubCache.put(serverEntry.getQualifier(), newStub);

        return newStub;
    }

    private UserServiceGrpc.UserServiceBlockingStub getStub(String serverQualifier) throws NoServerAvailableException {
        UserServiceGrpc.UserServiceBlockingStub stub = stubCache.get(serverQualifier);
        if (stub == null) {
            stub = lookupService(serverQualifier);
            stubCache.put(serverQualifier, stub);
        }
        debug("getStub returned: " + stub.toString());
        return stub;
    }

    public UserServiceGrpc.UserServiceBlockingStub lookupService(String qualifier) throws NoServerAvailableException {
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
        channelCache.forEach((namingServer, channel) -> channel.shutdownNow());
        channelCache.clear();

        debug("All channels shutdown");
    }

    public String createAccountService(String username, String serverQualifier) {
        UserServiceGrpc.UserServiceBlockingStub stub;
        try {
            stub = getStub(serverQualifier);
            return createAccountRequest(stub, username);
        } catch (StatusRuntimeException e) {
            debug("user received createAccount error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    stub = lookupService(serverQualifier);
                    return createAccountRequest(stub, username);
                } catch (NoServerAvailableException noServer) {
                    return e.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException e) {
            return e.getMessage() + "\n";
        }
    }

    public String getBalanceService(String username, String serverQualifier) {
        UserServiceGrpc.UserServiceBlockingStub stub;
        try {
            stub = getStub(serverQualifier);
            return getBalanceRequest(stub, username);
        } catch (StatusRuntimeException e) {
            debug("user received getBalance error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    stub = lookupService(serverQualifier);
                    return getBalanceRequest(stub, username);
                } catch (NoServerAvailableException noServer) {
                    return e.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException e) {
            return e.getMessage() + "\n";
        }
    }

    public String transferToService(String fromUsername, String toUsername, Integer amount, String serverQualifier) {
        UserServiceGrpc.UserServiceBlockingStub stub;
        try {
            stub = getStub(serverQualifier);
            return transferToRequest(stub, fromUsername, toUsername, amount);
        } catch (StatusRuntimeException e) {
            debug("user received transferTo error status: " + e.getStatus());
            if (e.getStatus().getCode() == Status.UNAVAILABLE.getCode()) {
                try {
                    stub = lookupService(serverQualifier);
                    return transferToRequest(stub, fromUsername, toUsername, amount);
                } catch (NoServerAvailableException noServer) {
                    return e.getMessage() + "\n";
                } catch (StatusRuntimeException exception) {
                    return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
                }
            } else {
                return "Caught exception with description: " + e.getStatus().getDescription() + "\n";
            }
        } catch (NoServerAvailableException e) {
            return e.getMessage() + "\n";
        }
    }

    public String createAccountRequest(UserServiceGrpc.UserServiceBlockingStub stub, String username) {
        UserDistLedger.CreateAccountRequest request = UserDistLedger.CreateAccountRequest.newBuilder()
                .setUserId(username)
                .addAllPrevTS(this.prevTS)
                .build();

        UserDistLedger.CreateAccountResponse response = stub.createAccount(request);
        updatePrevTS(response.getTSList());
        debug("User prevTS " + prevTS);
        return "OK\n" + response.toString() + "\n";
    }

    public String getBalanceRequest(UserServiceGrpc.UserServiceBlockingStub stub, String username) {
        UserDistLedger.BalanceRequest request = UserDistLedger.BalanceRequest.newBuilder()
                .setUserId(username)
                .addAllPrevTS(this.prevTS)
                .build();

        UserDistLedger.BalanceResponse response = stub.balance(request);
        updatePrevTS(response.getValueTSList());
        debug("User prevTS " + prevTS);
        return "OK\n" + response.getValue() + "\n";
    }

    public String transferToRequest(UserServiceGrpc.UserServiceBlockingStub stub, String fromUsername, String toUsername, int amount) {
        UserDistLedger.TransferToRequest request = UserDistLedger.TransferToRequest.newBuilder()
                .setAccountFrom(fromUsername)
                .setAccountTo(toUsername)
                .setAmount(amount)
                .addAllPrevTS(this.prevTS)
                .build();

        UserDistLedger.TransferToResponse response = stub.transferTo(request);
        updatePrevTS(response.getTSList());
        debug("User prevTS " + prevTS);
        return "OK\n" + response.toString() + "\n";
    }

    public void updatePrevTS(List<Integer> TS) {
        for (int i = 0; i < prevTS.size(); i++) {
            if (TS.get(i) > prevTS.get(i))
                prevTS.set(i, TS.get(i));
        }
    }

}
