import java.util.Arrays;

public class LZW_ReaderGrammar extends LZW_ConfGramAbstract {
    enum LZW_ReaderConfFields {
        BUFFER_SIZE
    }

    protected LZW_ReaderGrammar() {
        super(Arrays.stream(LZW_ReaderConfFields.values()).map(Enum::toString).toArray(String[]::new));
    }
}
