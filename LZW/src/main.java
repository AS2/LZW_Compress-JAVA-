public class main {
    public static void main(String[] args) {
        if (args.length == 1) {
            ConfigInfo ci = new ConfigInfo(args[0]);

            if (error.errNo != error.ErrorCode.NO_ERR)
                System.out.println(error.errMessage);
            else {
                LZW lzw = new LZW(ci);
                lzw.LZWFunction();

                if (error.errNo != error.ErrorCode.NO_ERR)
                    System.out.println(error.errMessage);
                else
                    System.out.println("Success: LZW process completed!");
            }
        }
        else
            System.out.println("LE: Incorrect count of parameters");
    }
}
