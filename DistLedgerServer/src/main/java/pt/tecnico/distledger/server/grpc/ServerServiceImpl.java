package pt.tecnico.distledger.server.grpc;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.DistLedgerOperationVisitor;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.serverExceptions.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import java.util.List;
import java.util.logging.Logger;

import static io.grpc.Status.*;
import static io.grpc.Status.PERMISSION_DENIED;

public class ServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    ServerState serverState;
    private static boolean DEBUG_FLAG;

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            logger.info(debugMessage);
    }

    public ServerServiceImpl(ServerState serverState, final boolean DEBUG_FLAG) {
        this.serverState = serverState;
        ServerServiceImpl.DEBUG_FLAG = DEBUG_FLAG;
    }

    @Override
    public void propagateState(CrossServerDistLedger.PropagateStateRequest request,
                               StreamObserver<CrossServerDistLedger.PropagateStateResponse> responseObserver) {

        serverState.applyGossip(request.getState().getLedgerList(), request.getReplicaTSList());
        CrossServerDistLedger.PropagateStateResponse response = CrossServerDistLedger.PropagateStateResponse.getDefaultInstance();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
