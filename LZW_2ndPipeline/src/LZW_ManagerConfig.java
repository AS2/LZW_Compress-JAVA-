import java.util.Arrays;

public class LZW_ManagerConfig extends LZW_ConfGramAbstract {
    enum LZW_ManagerConfFields {
        SRC_FILE,
        DIR_FILE,
        READER_CLASS,
        EXECUTOR_CLASSES,
        WRITER_CLASS,
        EXECUTOR_CONFIGS,
        READER_CONFIG,
        WRITER_CONFIG,
        LOG_PATH
    }

    protected LZW_ManagerConfig() {
        super(Arrays.stream(LZW_ManagerConfFields.values()).map(Enum::toString).toArray(String[]::new));
    }
}
