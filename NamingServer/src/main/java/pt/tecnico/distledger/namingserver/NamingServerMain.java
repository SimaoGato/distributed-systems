package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.grpc.NamingServerServiceImpl;

import java.io.IOException;

public class NamingServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {

        NamingServerState namingServerState = new NamingServerState();

        System.out.println(NamingServerState.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", NamingServerMain.class.getName());
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final BindableService namingServerImpl = new NamingServerServiceImpl();

        Server namingServer = ServerBuilder.forPort(port).addService(namingServerImpl).build();

        // Start the naming server
        namingServer.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        namingServer.awaitTermination();

    }

}
