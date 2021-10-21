import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

class ConfigInfo {
    // config grammar
    private final static ConfigGrammar grammar = new ConfigGrammar(new String[]{"SRC_FILE", "DIR_FILE", "MAX_BITS", "MODE", "BUFFER_SIZE"});
    // config values
    private final HashMap<String, String> fieldsValues = new HashMap<>();

    // Config info constructor
    public ConfigInfo(String configPath) {
        int isFieldTypeRecognized;

        try (FileReader fr = new FileReader(configPath)) {
            BufferedReader br = new BufferedReader(fr);

            // read lines and save them
            String line;
            while ((line = br.readLine()) != null) {
                String[] stringComponents = line.split(grammar.splitSymbol);
                if (stringComponents.length != grammar.fieldsPerLine) {
                    error.UpdateError(error.ErrorCode.CONF_ERR, "LE: Bad config parameter line");
                    return;
                }
                else {
                    // delete all possibles spaces
                    stringComponents[0] = stringComponents[0].replaceAll(" ", "");
                    stringComponents[1] = stringComponents[1].replaceAll(" ", "");
                    isFieldTypeRecognized = 0;

                    for (String fieldValue : grammar.fieldsTypes)
                        if (fieldValue.equals(stringComponents[0])) {
                            isFieldTypeRecognized = 1;
                            break;
                        }

                    if (isFieldTypeRecognized != 1) {
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
