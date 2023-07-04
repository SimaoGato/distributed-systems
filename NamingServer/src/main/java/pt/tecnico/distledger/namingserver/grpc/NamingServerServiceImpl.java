package pt.tecnico.distledger.namingserver.grpc;


import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.tecnico.distledger.namingserver.namingServerExceptions.QualifierAlreadyRegisteredException;
import pt.tecnico.distledger.namingserver.namingServerExceptions.TargetAlreadyRegisteredException;
import pt.tecnico.distledger.namingserver.namingServerExceptions.NotPossibleToRemoveServerException;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.namingserver.NamingServerServiceGrpc;

import java.util.List;

import static io.grpc.Status.ALREADY_EXISTS;
import static io.grpc.Status.NOT_FOUND;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private NamingServerState namingServerState = new NamingServerState();

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {

        try {
            namingServerState.register(request.getServiceName(), request.getQualifier(), request.getServerAddress());

            RegisterResponse response = RegisterResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (QualifierAlreadyRegisteredException e) {
            responseObserver.onError(ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (TargetAlreadyRegisteredException e) {
            responseObserver.onError(ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
            namingServerState.delete(request.getServiceName(), request.getServerAddress());

            DeleteResponse response = DeleteResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NotPossibleToRemoveServerException e) {
            responseObserver.onError(NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        List<ServerEntry> serverEntryList = namingServerState.lookup(request.getServiceName(), request.getQualifier());

        LookupResponse response = LookupResponse.newBuilder()
                .addAllServerList(serverEntryList.stream().map(ServerEntry::proto).toList())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
