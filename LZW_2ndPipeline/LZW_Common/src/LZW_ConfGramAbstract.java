import com.java_polytech.pipeline_interfaces.RC;
import java.util.Arrays;

public class LZW_ConfGramAbstract {
    public final static String fieldsSplitSymbol = "=";
    public final static String argsSplitSymbol = ",";
    public final static byte commentSymbol = '#';
    public final static int fieldsPerLine = 2;

    public final static RC UnknownFieldName = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Unknown field");
    public final static RC UncorrectFieldsCnt = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Some of fields doesn't init");

    private final String[] fieldsNames;

    protected LZW_ConfGramAbstract (String[] newFieldsNames) {
        fieldsNames = newFieldsNames;
    }

    public boolean IsGrammarHasThisField(String Field) {
        return Arrays.asList(fieldsNames).contains(Field);
    }

    public int GetFieldsCnt() {
        return (fieldsNames == null ? 0 : fieldsNames.length);
    }
}
