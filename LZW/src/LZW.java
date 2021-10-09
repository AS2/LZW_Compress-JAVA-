import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class LZW {
    final private static int maxINT = 2147483647;

    private final ConfigInfo lzwCi;

    private String srcFile;                          // source file path
    private String dirFile;                          // direction file path
    private int maxVocSize;                          // Maximum bits per vocabulary index
    private int modeWork;                            // LZW work type: 0 - compress, 1 - decompress
    private int bufSize;                             // buffer size

    // buffers managers
    private BufReader reader;       // buffer reader manager
    private BufWritter writter;     // buffer writter manager

    // vocabulary parameters
    private String[] vocabulary;        // vocabulary
    private int currentVocabularyBits, arraySize, lastNewWordIndex;     // vocabulary information;

    // Init vocabulary
    private void InitVocabulary() {
        vocabulary = new String[512];

        for (int i = 0; i < 256; i++)
            vocabulary[i] = String.valueOf((char)i);

        currentVocabularyBits = 9;
        arraySize = 512;
        lastNewWordIndex = 256;
    }

    // Find word in vocabulary:
    // Arguments: String str - word to find
    // Returns: int - '-1' if voc doesnt contains word, not negative number - word index
    private int IsVocContainThisWord(String str) {
        if (str.length() == 1) {
            for (int i = 0; i < 256; i++)
                if (vocabulary[i].equals(str))
                    return i;
        }
        else if (str.length() > 1) {
            for (int i = 256; i < lastNewWordIndex; i++)
                if (vocabulary[i].equals(str))
                    return i;
        }
        return -1;
    }

    // Resize vocabulary function:
    // Arguments: none
    // Returns: none
    private void ResizeVoc() {
        vocabulary = Arrays.copyOf(vocabulary, arraySize * 2);
        arraySize *= 2;
        currentVocabularyBits++;
    }

    // Add word to vocabulary in compress type work
    // Arguments: String str - word to add
    // Returns: none
    private void CoddingAddWord(String Str) {
        if (lastNewWordIndex >= arraySize && currentVocabularyBits <= maxVocSize) {
            ResizeVoc();
            vocabulary[lastNewWordIndex++] = Str;
        }
        else if (lastNewWordIndex < arraySize)
            vocabulary[lastNewWordIndex++] = Str;
    }

    // Add word to vocabulary in decompress type work
    // Arguments: String str - word to add
    // Returns: none
    private void DecodingAddWord(String Str) {
        if (lastNewWordIndex < arraySize) {
            vocabulary[lastNewWordIndex++] = Str;
            if (lastNewWordIndex == arraySize && currentVocabularyBits <= maxVocSize)
                ResizeVoc();
        }
    }

    // LZW compress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    private void LZWCompress() {
        try {
            reader.RereadBuffer();
        }
        catch (IOException e) {
            error.UpdateError(3, "Cannot reread buffer");
            return;
        }

        InitVocabulary();

        int symbol, searchRes;
        String word = "";

        // read first symbol
        if (!reader.isFileRead()) {
            try {
                symbol = reader.ReadNBits(8);
            } catch (IOException e) {
                error.UpdateError(3, "Cannot reread buffer");
                return;
            }
            word = String.valueOf((char)symbol);
        }

        // read new symbols
        while (!reader.isFileRead()) {
            try {
                symbol = reader.ReadNBits(8);
            } catch (IOException e) {
                error.UpdateError(3, "Cannot reread buffer");
                return;
            }

            searchRes = IsVocContainThisWord(word + (char)symbol);
            if (searchRes == -1) {
                // add old word code to write buffer
                searchRes = IsVocContainThisWord(word);
                try {
                    writter.AddToBuffer(searchRes, currentVocabularyBits);
                } catch (IOException e) {
                    error.UpdateError(3, "RE: Cannot write buffer");
                    return;
                }
                // add new word code to vocabulary
                CoddingAddWord(word + (char)symbol);
                word = String.valueOf((char)symbol);
            }
            else {
                // increase word
                word += (char)symbol;
            }
        }

        // add last word
        searchRes = IsVocContainThisWord(word);
        try {
            writter.AddToBuffer(searchRes, currentVocabularyBits);
        } catch (IOException e) {
            error.UpdateError(3, "RE: Cannot write buffer");
        }
    }

    // LZW decompress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    private void LZWDecompress() {
        try {
            reader.RereadBuffer();
        } catch (IOException e) {
            error.UpdateError(4, "Cannot reread buffer");
            return;
        }

        InitVocabulary();

        int wordIndex, searchRes;
        String word = "";

        // read first symbol
        if (!reader.isFileRead()) {
            try {
                wordIndex = reader.ReadNBits(currentVocabularyBits);
            } catch (IOException e) {
                error.UpdateError(4, "Cannot reread buffer");
                return;
            }
            word = vocabulary[wordIndex];
        }

        // read new symbols
        while (!reader.isFileRead()) {
            try {
                wordIndex = reader.ReadNBits(currentVocabularyBits);
            } catch (IOException e) {
                error.UpdateError(3, "Cannot reread buffer");
                return;
            }

            if (wordIndex < lastNewWordIndex) {
                searchRes = IsVocContainThisWord(word + vocabulary[wordIndex].charAt(0));

                if (searchRes == -1) {
                    // add old word code to write buffer
                    try {
                        writter.WriteString(word);
                    } catch (IOException e) {
                        error.UpdateError(3, "RE: Cannot write buffer");
                        return;
                    }
                    // add new word code to vocabulary
                    DecodingAddWord(word + vocabulary[wordIndex].charAt(0));
                    word = vocabulary[wordIndex];
                } else {
                    // increase word
                    word += vocabulary[wordIndex];
                }
            }
            // then code comes immediately -> last symbol = first symbol
            else if (wordIndex == lastNewWordIndex) {
                try {
                    writter.WriteString(word);
                } catch (IOException e) {
                    error.UpdateError(3, "RE: Cannot write buffer");
                    return;
                }
                // add new word code to vocabulary
                DecodingAddWord(word + word.charAt(0));
                word = vocabulary[wordIndex];
            }
        }

        try {
            writter.WriteString(word);
        } catch (IOException e) {
            error.UpdateError(3, "RE: Cannot write buffer");
        }
    }

    // Open writter and reader buffers function.
    // Arguments: none
    // Returns: none; fill global error type if something went wrong
    private void OpenBuffers() {
        try {
            reader = new BufReader(srcFile, bufSize);
        }
        catch (FileNotFoundException ex) {
            error.UpdateError(2, "RE: Cannot open file to read");
        }

        try {
            writter = new BufWritter(dirFile, bufSize);
        }
        catch (FileNotFoundException ex) {
            error.UpdateError(2, "RE: Cannot open file to write");
        }
    }

    // Close writter and reader buffers function.
    // Arguments: none
    // Returns: none; fill global error type if something went wrong
    private void CloseBuffers() {
        try {
            reader.CloseReader();
        }
        catch (IOException e) {
            error.UpdateError(2, "RE: Cannot close reader stream");
        }

        try {
            writter.CloseWritter();
        }
        catch (IOException e) {
            error.UpdateError(2, "RE: Cannot close writter stream");
        }
    }

    // Parser string to int.
    // Arguments: String str - string to parse
    // Returns: int - "number" if you can parse str to int or return -1 with error
    //                if number too big or string contains non-numbers characters
    private static int ParseStrToInt(String str) {
        char[] stringsAtChar = str.toCharArray();
        int res = 0;

        for (char ch : stringsAtChar) {
            if (ch >= '0' && ch <= '9') {
                if (maxINT - res * 10 >= (ch - '0'))
                    res = res * 10 + (ch - '0');
                else {
                    error.UpdateError(2, "LE: Number is too big for counting");
                    return 0;
                }
            }
            else {
                error.UpdateError(2, "LE: String, which must contains number, contain character");
                return 0;
            }
        }

        return res;
    }

    // Config info parser and checker
    // Returns: none; fill global error type if something went wrong
    private void LZWParseConfig() {
        HashMap<String, String> fieldsValues = lzwCi.GetFieldValues();
        String[] fieldsTypes = lzwCi.GetFieldTypes();

        if (fieldsValues.size() != 5) {
            error.UpdateError(1, "LE: In config matches not all fields");
            return;
        }

        for (String fieldType : fieldsTypes) {
            switch (fieldType) {
                case "SRC_FILE" -> {
                    srcFile = fieldsValues.get("SRC_FILE");
                    if (srcFile == null) {
                        error.UpdateError(2, "LE: No 'SRC_FILE' field in conf");
                        return;
                    }
                }
                case "DIR_FILE" -> {
                    dirFile = fieldsValues.get("DIR_FILE");
                    if (dirFile == null) {
                        error.UpdateError(2, "LE: No 'DIR_FILE' field in conf");
                        return;
                    }
                }
                case "MAX_BITS" -> {
                    String tmpStr = fieldsValues.get("MAX_BITS");
                    if (tmpStr == null) {
                        error.UpdateError(2, "LE: No 'MAX_BITS' field in conf");
                        return;
                    }
                    maxVocSize = ParseStrToInt(tmpStr);

                    if (error.errNo != 0)
                        return;
                    // let vocabulary be not very big, but let him store, minimum, 256 symbols
                    if (maxVocSize < 8 || maxVocSize > 31) {
                        error.UpdateError(2, "LE: Bits for per word must be between [8; 31]");
                        return;
                    }
                }
                case "MODE" -> {
                    String tmpStr = fieldsValues.get("MODE");
                    if (tmpStr == null) {
                        error.UpdateError(2, "LE: No 'MODE' field in conf");
                        return;
                    }
                    modeWork = ParseStrToInt(fieldsValues.get("MODE"));

                    if (error.errNo != 0)
                        return;
                    else if (modeWork != 0 && modeWork != 1) {
                        error.UpdateError(2, "LE: Incorrect mode work: 0 - for compress, 1 - for decompress");
                        return;
                    }
                }
                case "BUFFER_SIZE" -> {
                    String tmpStr = fieldsValues.get("BUFFER_SIZE");
                    if (tmpStr == null) {
                        error.UpdateError(2, "LE: No 'BUFFER_SIZE' field in conf");
                        return;
                    }
                    bufSize = ParseStrToInt(fieldsValues.get("BUFFER_SIZE"));

                    if (error.errNo != 0)
                        return;
                    else if ((double) bufSize < Math.ceil((double) maxVocSize / 8)) {
                        error.UpdateError(2, "LE: Buffer size can't fit index to write");
                        return;
                    }
                    // let buffer to store not more than 1Mb
                    else if (bufSize > 1073741824) {
                        error.UpdateError(2, "LE: Too big size for buffer");
                        return;
                    }
                }
            }
        }
    }

    public LZW(ConfigInfo ci) {
        lzwCi = ci;
    }

    // General LZW compress/decompress function.
    // Arguments: ConfigInfo ci - config information about work process
    // Returns: none; fill global error type if something went wrong
    public void LZWFunction() {
        LZWParseConfig();
        if (error.errNo != 0)
            return;

        OpenBuffers();
        if (error.errNo != 0)
            return;

        if (modeWork == 0) {
            System.out.println("Going compress...");
            LZWCompress();
        }
        else if (modeWork == 1) {
            System.out.println("Going decompress...");
            LZWDecompress();
        }
        CloseBuffers();
    }
}
