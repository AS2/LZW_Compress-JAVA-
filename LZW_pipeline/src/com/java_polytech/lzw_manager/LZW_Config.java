package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

public class LZW_Config {
    private final static String splitSymbol = "=";
    private final static int fieldsPerLine = 2;

    private final static RC RC_BAD_CONFIG_STRING = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Line in config doesn't have 2 parts");
    private final static RC RC_BAD_CONFIG_STR_CONTAIN = new RC(RC.RCWho.UNKNOWN, RC.RCType.CODE_CUSTOM_ERROR, "Config line has empty parts");

    private final HashMap<String, String> fieldsValues = new HashMap<>();

    LZW_Config () {
    }

    RC Parse(FileReader configFile) {
        Scanner sc = new Scanner(configFile);
        String line;

        // read lines and save them
        while (sc.hasNext()) {
            line = sc.nextLine();

            String[] stringComponents = line.split(splitSymbol);
            if (stringComponents.length != fieldsPerLine)
                return RC_BAD_CONFIG_STRING;
            else {
                // delete all possibles spaces
                stringComponents[0] = stringComponents[0].replaceAll(" ", "");
                stringComponents[1] = stringComponents[1].replaceAll(" ", "");

                if (stringComponents[0].isBlank() || stringComponents[0].isEmpty() || stringComponents[1].isBlank() || stringComponents[1].isEmpty())
                    return RC_BAD_CONFIG_STR_CONTAIN;

                fieldsValues.put(stringComponents[0], stringComponents[1]);
            }
        }

        return RC.RC_SUCCESS;
    }

    String GetValue(String fieldName) {
        return fieldsValues.get(fieldName);
    }
}
