// singleton "error" type
public class error {
    enum ErrorCode {
        NO_ERR(0),
        CONF_ERR(1),
        LZW_PROC_ERR(2),
        LZW_COMPRESS_ERR(3),
        LZW_DECOMPRESS_ERR(4);

        private int errCode;
        ErrorCode(int code) {
            errCode = code;
        }
    }

    public static ErrorCode errNo = ErrorCode.NO_ERR;        // error code
    public static String errMessage;                         // error message

    // Update error method
    // ARGS: - no - error number type
    //       - message - error message
    public static void UpdateError(ErrorCode no, String message) {
        errNo = no;
        errMessage = message;
    }
}
