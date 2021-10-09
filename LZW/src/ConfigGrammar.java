public class ConfigGrammar {
    public final static String splitSymbol = "=";     // splitting symbol
    public final static int fieldsPerLine = 2;        // count of fields per line
    public final String[] fieldsTypes;                // fields types array

    ConfigGrammar(String[] newFieldsTypes) {
        fieldsTypes = newFieldsTypes;
    }
}
