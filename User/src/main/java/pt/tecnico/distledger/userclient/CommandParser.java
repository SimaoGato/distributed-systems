package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final UserService userService;

    public CommandParser(UserService userService) {
        this.userService = userService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try{
                switch (cmd) {
                    case CREATE_ACCOUNT -> this.createAccount(line);
                    case TRANSFER_TO -> this.transferTo(line);
                    case BALANCE -> this.balance(line);
                    case HELP -> this.printUsage();
                    case EXIT -> exit = true;
                    default -> {
                    }
                }
            }
            catch (Exception e){
                System.err.println(e.getMessage());
            }
        }

        userService.closeAllChannels();
    }

    private void createAccount(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        System.out.println(userService.createAccountService(username, server));
    }

    private void balance(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        System.out.println(userService.getBalanceService(username, server));
    }

    private void transferTo(String line){
        String[] split = line.split(SPACE);

        if (split.length != 5){
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        System.out.println(userService.transferToService(from, dest, amount, server));
    }

    private void printUsage() {
        System.out.println("""
                Usage:
                - createAccount <server> <username>
                - balance <server> <username>
                - transferTo <server> <username_from> <username_to> <amount>
                - exit
                """);
    }
}
