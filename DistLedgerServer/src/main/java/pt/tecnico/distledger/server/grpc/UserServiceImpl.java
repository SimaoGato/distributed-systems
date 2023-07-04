package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.serverExceptions.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.server.domain.ServerState;
import static io.grpc.Status.NOT_FOUND;
import java.util.logging.Logger;

import static io.grpc.Status.*;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final ServerState serverState;

    private static boolean DEBUG_FLAG;

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG) logger.info(debugMessage);
    }

    public UserServiceImpl(ServerState serverState, final boolean DEBUG_FLAG) {
        this.serverState = serverState;
        UserServiceImpl.DEBUG_FLAG = DEBUG_FLAG;
    }

    @Override
    public void createAccount(UserDistLedger.CreateAccountRequest request, StreamObserver<UserDistLedger.CreateAccountResponse> responseObserver) {

        debug("Received create account request with username " + request.getUserId());

        try {
            CreateOp createOp = serverState.createAccount(request.getUserId(), request.getPrevTSList(), false);

            UserDistLedger.CreateAccountResponse response = UserDistLedger.CreateAccountResponse.newBuilder()
                    .addAllTS(serverState.getReplicaTS())
                    .build();
            debug("Sending create account response for user " + request.getUserId());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            serverState.checkCreateStability(createOp, false);

        } catch (ServerUnavailableException e) {
            debug(e.getMessage());
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void balance(UserDistLedger.BalanceRequest request, StreamObserver<UserDistLedger.BalanceResponse> responseObserver) {

        debug("Received balance request for user " + request.getUserId());

        try {
            int value = serverState.getBalanceById(request.getUserId(), request.getPrevTSList());

            UserDistLedger.BalanceResponse response = UserDistLedger.BalanceResponse.newBuilder()
                    .setValue(value)
                    .addAllValueTS(serverState.getReplicaTS())
                    .build();
            debug("Sending balance response for user " + request.getUserId() + " : " + value);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (AccountDoesntExistException e) {
            debug(e.getMessage(request.getUserId()));
            responseObserver.onError(NOT_FOUND.withDescription(e.getMessage(request.getUserId())).asRuntimeException());

        } catch (ServerUnavailableException | UserIsAheadOfServerException e) {
            debug(e.getMessage());
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void transferTo(UserDistLedger.TransferToRequest request, StreamObserver<UserDistLedger.TransferToResponse> responseObserver) {

        debug("Received transfer request from user " + request.getAccountFrom() +
                    " to user " + request.getAccountTo());

        try {
            TransferOp transferOp = serverState.transferTo(request.getAccountFrom(), request.getAccountTo(), request.getAmount(), request.getPrevTSList(), false);

            UserDistLedger.TransferToResponse response = UserDistLedger.TransferToResponse.newBuilder()
                    .addAllTS(serverState.getReplicaTS())
                    .build();
            debug("Sending transferTo response from user " + request.getAccountFrom() +
                        " to user " + request.getAccountTo() + " of an amount of " + request.getAmount());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            serverState.checkTransferStability(transferOp, false);

        } catch (ServerUnavailableException e) {
            debug(e.getMessage());
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}