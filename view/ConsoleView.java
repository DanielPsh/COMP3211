package view;

import controller.PIMController;
import java.util.Scanner;

/** The only class that reads input and prints output. */
public class ConsoleView {

    private final PIMController controller;
    private final Scanner in = new Scanner(System.in);

    public ConsoleView(PIMController controller) {
        this.controller = controller;
    }

    public void run() {
        System.out.println("Personal Information Manager. Type 'help' for commands.");
        while (controller.isRunning()) {
            System.out.print("pim> ");
            if (!in.hasNextLine()) {
                break;
            }
            String line = in.nextLine();
            if (line.trim().isEmpty()) {
                continue;
            }
            System.out.println(controller.execute(line));
        }
    }
}
