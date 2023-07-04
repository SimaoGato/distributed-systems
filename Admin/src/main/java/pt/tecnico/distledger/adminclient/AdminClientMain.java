package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public static void main(String[] args) {

        System.out.println(AdminClientMain.class.getSimpleName());

        if(DEBUG_FLAG) { System.out.printf("Debug Mode Activated!\n"); }

        CommandParser parser = new CommandParser(new AdminService(DEBUG_FLAG));

        parser.parseInput();

    }
}
