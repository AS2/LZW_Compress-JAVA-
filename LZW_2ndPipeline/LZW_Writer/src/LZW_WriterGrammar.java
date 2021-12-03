import java.util.Arrays;

public class LZW_WriterGrammar extends LZW_ConfGramAbstract {
    enum LZW_WriterConfFields {
        BUFFER_SIZE
    }

    protected LZW_WriterGrammar() {
        super(Arrays.stream(LZW_WriterConfFields.values()).map(Enum::toString).toArray(String[]::new));
    }
}
