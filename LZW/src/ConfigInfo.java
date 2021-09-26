import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class ConfigInfo {
    public enum InfoType {
        srcFile("SRC_FILE"),
        dirFile("DIR_FILE"),
        maxVocBits("MAX_BITS"),
        mode("MODE"),
        bufferSize("BUFFER_SIZE");

        private String type;
        InfoType(String infoType) {
            type = infoType;
        }
        public String asString() {
            return type;
        }
    }

    String sourceFilePath;      // file path to compress/decompress
    String newFilePath;         // file path to save result
    String maxVocabularyBits;   // vocabulary size: >= 8 bits && <= 30 bits
    String lzwWorkType;         // 0 - compress, 1 - decompress
    String bufSize;             // buffer size (in bytes)

    final String splitSymbol = "=";

    public ConfigInfo(String configPath) {
        try (FileReader fr = new FileReader(configPath)) {
            BufferedReader br = new BufferedReader(fr);
            sourceFilePath = newFilePath = maxVocabularyBits = lzwWorkType = bufSize = null;

            // read lines and save them
            String line;
            while ((line = br.readLine()) != null) {
                String[] stringComponents = line.split(splitSymbol);
                if (stringComponents.length != 2) {
                    error.UpdateError(1, "LE: Bad config parameter line");
                    return;
                }
                else {
                    // delete all possibles spaces
                    stringComponents[0] = stringComponents[0].replaceAll(" ", "");
                    stringComponents[1] = stringComponents[1].replaceAll(" ", "");

                    if (stringComponents[0].equalsIgnoreCase(InfoType.srcFile.asString()))
                        sourceFilePath = stringComponents[1];
                    else if (stringComponents[0].equalsIgnoreCase(InfoType.dirFile.asString()))
                        newFilePath = stringComponents[1];
                    else if (stringComponents[0].equalsIgnoreCase(InfoType.maxVocBits.asString()))
                        maxVocabularyBits = stringComponents[1];
                    else if (stringComponents[0].equalsIgnoreCase(InfoType.mode.asString()))
                        lzwWorkType = stringComponents[1];
                    else if (stringComponents[0].equalsIgnoreCase(InfoType.bufferSize.asString()))
                        bufSize = stringComponents[1];
                    else {
                        error.UpdateError(1, "LE: Unknown type");
                        return;
                    }
                }
            }

            if (sourceFilePath == null || newFilePath == null || maxVocabularyBits == null ||
                lzwWorkType == null ||  bufSize == null) {
                error.UpdateError(1, "LE: In config matches not all fields");
            }
        }
        catch (IOException ex) {
            error.UpdateError(1, "RE: Cannot open config to read");
        }
    }
}
