import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

class ConfigInfo {
    // 'config field type' enum
    public enum FieldType {
        SRC_FILE,
        DIR_FILE,
        MODE,
        MAX_BITS,
        BUFFER_SIZE
    }

    // config grammar
    private final static ConfigGrammar grammar = new ConfigGrammar(Arrays.stream(FieldType.values()).map(FieldType::toString).toArray(String[]::new));
    // config values
    private final HashMap<String, String> fieldsValues = new HashMap<>();

    // Config info constructor
    public ConfigInfo(String configPath) {
        try (FileReader fr = new FileReader(configPath)) {
            BufferedReader br = new BufferedReader(fr);

            // read lines and save them
            String line;
            while ((line = br.readLine()) != null) {
                String[] stringComponents = line.split(ConfigGrammar.splitSymbol);
                if (stringComponents.length != ConfigGrammar.fieldsPerLine) {
                    error.UpdateError(error.ErrorCode.CONF_ERR, "LE: Bad config parameter line");
                    return;
                }
                else {
                    // delete all possibles spaces
                    stringComponents[0] = stringComponents[0].replaceAll(" ", "");
                    stringComponents[1] = stringComponents[1].replaceAll(" ", "");

                    if (stringComponents[0].isEmpty() || stringComponents[1].isEmpty())  {
                        error.UpdateError(error.ErrorCode.CONF_ERR, "LE: some config's stroke is empty");
                        return;
                    }

                    if (!Arrays.asList(grammar.fieldsTypes).contains(stringComponents[0])) {
                        error.UpdateError(error.ErrorCode.CONF_ERR, "LE: Unrecognized config parameter type '" + stringComponents[0] + "'");
                        return;
                    }

                    fieldsValues.put(stringComponents[0], stringComponents[1]);
                }
            }
        }
        catch (IOException ex) {
            error.UpdateError(error.ErrorCode.CONF_ERR, "RE: Cannot open config to read");
        }
    }

    public String[] GetFieldTypes() {
        return grammar.fieldsTypes;
    }

    public HashMap<String, String> GetFieldValues() {
        return fieldsValues;
    }
}
