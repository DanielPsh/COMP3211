import controller.PIMController;
import model.PIRRepository;
import view.ConsoleView;

public class Main {
    public static void main(String[] args) {
        new ConsoleView(new PIMController(new PIRRepository())).run();
    }
}
