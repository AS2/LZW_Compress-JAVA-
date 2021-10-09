// singleton "error" type
public class error {
    public static int errNo;            // error number: 0 - no error, 1 - config error,
                                        //               2 - lzw process, 3 - LZW compress err,
                                        //               4 - lzw decompress err
    public static String errMessage;    // error message

    // Update error method
    // ARGS: - no - error number type
    //       - message - error message
    public static void UpdateError(int no, String message) {
        errNo = no;
        errMessage = message;
    }
}
