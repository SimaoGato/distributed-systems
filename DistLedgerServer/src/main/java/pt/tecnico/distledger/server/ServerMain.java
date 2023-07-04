package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.grpc.AdminServiceImpl;
import pt.tecnico.distledger.server.grpc.ServerServiceImpl;
import pt.tecnico.distledger.server.grpc.UserServiceImpl;
import pt.tecnico.distledger.server.grpc.ServerService;

import java.io.IOException;

public class ServerMain {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private static final ServerService serverService = new ServerService("localhost:5001", DEBUG_FLAG);

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerState serverState = new ServerState(serverService, args[1]);

        System.out.println(ServerMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
            return;
        }

        if (DEBUG_FLAG) { System.out.print("Debug Mode Activated!\n"); }

        final int port = Integer.parseInt(args[0]);
        final BindableService userImpl = new UserServiceImpl(serverState, DEBUG_FLAG);
        final BindableService adminImpl = new AdminServiceImpl(serverState, DEBUG_FLAG);
        final BindableService serverImpl = new ServerServiceImpl(serverState, DEBUG_FLAG);

        Server server = ServerBuilder.forPort(port).addService(userImpl).addService(adminImpl).addService(serverImpl).build();

        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Register in namingServer
        serverService.registerService("DistLedger", args[1], "localhost:" + args[0]);

        // Wait for shutdown
        System.out.println("Press enter to shutdown");
        System.in.read();
        serverService.deleteService("DistLedger", "localhost:" + args[0]);
        server.shutdown();
    }

}

