import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

public class LZW_Config {
    private final static RC RC_BAD_CONFIG_STRING = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Line in config doesn't have 2 parts");
    private final static RC RC_BAD_CONFIG_STR_CONTAIN = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Config line has empty parts");

    private final HashMap<String, String> fieldsValues = new HashMap<>();
    private final LZW_ConfGramAbstract grammar;

    LZW_Config (LZW_ConfGramAbstract newGrammar) {
        grammar = newGrammar;
    }

    RC Parse(FileReader configFile) {
        Scanner sc = new Scanner(configFile);
        String line;

        // read lines and save them
        while (sc.hasNext()) {
            line = sc.nextLine();

            String[] stringComponents = line.split(LZW_ConfGramAbstract.splitSymbol);
            if (stringComponents.length != LZW_ConfGramAbstract.fieldsPerLine)
                return RC_BAD_CONFIG_STRING;
            else {
                // delete all possibles spaces
                stringComponents[0] = stringComponents[0].replaceAll(" ", "");
                stringComponents[1] = stringComponents[1].replaceAll(" ", "");

                if (stringComponents[0].isEmpty() || stringComponents[1].isEmpty())
                    return RC_BAD_CONFIG_STR_CONTAIN;
                else if (!grammar.IsGrammarHasThisField(stringComponents[0]))
                    return LZW_ConfGramAbstract.UnknownFieldName;

                fieldsValues.put(stringComponents[0], stringComponents[1]);
            }
        }

        if (fieldsValues.size() != grammar.GetFieldsCnt())
            return LZW_ConfGramAbstract.UncorrectFieldsCnt;

        return RC.RC_SUCCESS;
    }

    String GetValue(String fieldName) {
        return fieldsValues.get(fieldName);
    }
}
