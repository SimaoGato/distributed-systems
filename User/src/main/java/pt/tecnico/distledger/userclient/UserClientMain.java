package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    public static void main(String[] args) {

        System.out.println(UserClientMain.class.getSimpleName());

        if(DEBUG_FLAG) { System.out.print("Debug Mode Activated!\n"); }

        CommandParser parser = new CommandParser(new UserService(DEBUG_FLAG));

        parser.parseInput();

    }
}
