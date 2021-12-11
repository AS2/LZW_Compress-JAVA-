import com.java_polytech.pipeline_interfaces.RC;

public class main {
    public static void main(String[] args) {
        if (args.length == 1) {
            RC managerRC;
            LZW_Manager manager = new LZW_Manager();
            System.out.println("LZW process started!");
            managerRC = manager.Run(args[0]);

            if (managerRC.isSuccess())
                System.out.println("LZW process completed!");
            else
                System.out.println(managerRC.info);
        }
        else
            System.out.println("LE: Incorrect count of parameters");
    }
}
