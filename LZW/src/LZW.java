import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LZW {
    final private static int maxINT = 2147483647;       // max int value
    final static private int MaxBufSize = 1073741824;   // max buffer size in config (=1Gb)

    final static private int DEFAULT_MAX_ARR_SIZE = 100;

    // 'work mode' enum
    enum Mode {
        ENCODE(0),
        DECODE(1),
        UNKNOWN(2);

        private final int type;
        Mode(int t) {
            type = t;
        }

        static Mode ToMode(int t) {
            if (t == 0)
                return ENCODE;
            else if (t == 1)
                return DECODE;
            else
                return UNKNOWN;
        }
    }

    // 'config field type' enum
    private enum InfoType {
        srcFile("SRC_FILE"),
        dirFile("DIR_FILE"),
        mode("MODE"),
        maxVocBits("MAX_BITS"),
        bufferSize("BUFFER_SIZE");

        private final String type;
        InfoType(String str) {
            type = str;
        }
        String GetType() {
            return type;
        }
    }

    // config file
    private final ConfigInfo lzwCi;

    // data for work initialize
    private String srcFile;                             // source file path
    private String dirFile;                             // direction file path
    private int maxVocSize;                             // Maximum bits per vocabulary index
    private Mode modeWork;                              // LZW work type: 0 - compress, 1 - decompress
    private int bufSize;                                // buffer size

    // buffers managers
    private BufReader reader;                           // buffer reader manager
    private BufWritter writter;                         // buffer writter manager

    // vocabulary parameters
    private byte[][] vocabulary;                                        // vocabulary
    private int currentVocabularyBits, arraySize, lastNewWordIndex;     // vocabulary information

    private byte[] wordSpace;                                           // word space for reader
    private int wordSpaceLength, wordSpacePos;                          // word space parameters

    // Init vocabulary
    private void InitVocabulary() {
        vocabulary = new byte[512][];
        byte[] byteCode;

        for (int i = 0; i < 256; i++) {
            byteCode = new byte[1];
            byteCode[0] = (byte)i;
            vocabulary[i] = byteCode;
        }

        wordSpace = new byte[DEFAULT_MAX_ARR_SIZE];

        currentVocabularyBits = 9;
        arraySize = 512;
        lastNewWordIndex = 256;
    }

    // Compare words:
    // Arguments: byte[] arr1, byte[] arr2 - words to compare
    // Returns: boolean - false if words don't same, true - if same
    private boolean CompareWords(byte[] arr1, int lArr1, byte[] arr2, int lArr2) {
        if (lArr1 != lArr2)
            return false;
        else {
            for (int i = 0; i < lArr1; i++)
                if (arr1[i] != arr2[i])
                    return false;

            return true;
        }
    }

    // Find word in vocabulary:
    // Arguments: byte[] word - word to find
    // Returns: int - '-1' if voc doesn't contain word, not negative number - word index
    private int IsVocContainThisWord(byte[] word, int wordLength) {
        if (wordLength == 1) {
            for (int i = 0; i < 256; i++)
                if (CompareWords(vocabulary[i], vocabulary[i].length, word, wordLength))
                    return i;
        }
        else if (word.length > 1) {
            for (int i = 256; i < lastNewWordIndex; i++)
                if (CompareWords(vocabulary[i], vocabulary[i].length, word, wordLength))
                    return i;
        }
        return -1;
    }

    // Resize vocabulary function:
    // Arguments: none
    // Returns: none
    private void ResizeVoc() {
        vocabulary = Arrays.copyOf(vocabulary,arraySize * 2);
        arraySize *= 2;
        currentVocabularyBits++;
    }

    // Add word to vocabulary in compress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    private void CoddingAddWord(byte[] word) {
        if (lastNewWordIndex >= arraySize && currentVocabularyBits <= maxVocSize) {
            ResizeVoc();
            vocabulary[lastNewWordIndex++] = word;
        }
        else if (lastNewWordIndex < arraySize)
            vocabulary[lastNewWordIndex++] = word;
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    private void DecodingAddWord(byte[] word) {
        if (lastNewWordIndex < arraySize) {
            vocabulary[lastNewWordIndex++] = word;
            if (lastNewWordIndex == arraySize && currentVocabularyBits <= maxVocSize)
                ResizeVoc();
        }
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    private void AddByteToWordSpace(byte newByte) {
        wordSpace[wordSpacePos++] = newByte;
        if (wordSpacePos == wordSpaceLength) {
            wordSpaceLength *= 2;
            wordSpace = Arrays.copyOf(wordSpace, wordSpaceLength);
        }
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    private void AddWordToWordSpace(byte[] newWord) {
        for (byte ch : newWord)
            AddByteToWordSpace(ch);
    }

    // LZW compress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    private void LZWCompress() {
        // reread buffer
        try {
            reader.RereadBuffer();
        }
        catch (IOException e) {
            error.UpdateError(error.ErrorCode.LZW_COMPRESS_ERR, "Cannot reread buffer");
            return;
        }

        InitVocabulary();

        byte symbol;
        int searchRes;
        byte[] word, wordWithSymb;

        // read first symbol
        if (!reader.isFileRead()) {
            try {
                symbol = reader.ReadByte();
            } catch (IOException e) {
                error.UpdateError(error.ErrorCode.LZW_COMPRESS_ERR, "Cannot reread buffer");
                return;
            }
            AddByteToWordSpace(symbol);
        }

        // read new symbols
        while (!reader.isFileRead()) {
            try {
                symbol = reader.ReadByte();
            } catch (IOException e) {
                error.UpdateError(error.ErrorCode.LZW_COMPRESS_ERR, "Cannot reread buffer");
                return;
            }
            AddByteToWordSpace(symbol);

            searchRes = IsVocContainThisWord(wordSpace, wordSpacePos);

            if (searchRes == -1) {
                // add old word code to write buffer
                searchRes = IsVocContainThisWord(wordSpace, wordSpacePos - 1);
                try {
                    writter.AddToBuffer(searchRes, currentVocabularyBits);
                } catch (IOException e) {
                    error.UpdateError(error.ErrorCode.LZW_COMPRESS_ERR, "RE: Cannot write buffer");
                    return;
                }

                // add new word code to vocabulary
                wordWithSymb = new byte[wordSpacePos];
                System.arraycopy(wordSpace, 0, wordWithSymb, 0, wordSpacePos);
                CoddingAddWord(wordWithSymb);

                // refresh word space
                wordSpacePos = 0;
                AddByteToWordSpace(symbol);
            }
        }

        // add last word
        if (wordSpacePos != 0) {
            searchRes = IsVocContainThisWord(wordSpace, wordSpacePos);
            try {
                writter.AddToBuffer(searchRes, currentVocabularyBits);
            } catch (IOException e) {
                error.UpdateError(error.ErrorCode.LZW_COMPRESS_ERR, "RE: Cannot write buffer");
            }
        }
    }

    // LZW decompress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    private void LZWDecompress() {
        try {
            reader.RereadBuffer();
        } catch (IOException e) {
            error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "Cannot reread buffer");
            return;
        }

        InitVocabulary();

        int wordIndex, searchRes;
        byte[] wordToAdd;

        // read first symbol
        if (!reader.isFileRead()) {
            try {
                wordIndex = reader.ReadNBits(currentVocabularyBits);
            } catch (IOException e) {
                error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "Cannot reread buffer");
                return;
            }
            wordSpacePos = 0;
            AddWordToWordSpace(vocabulary[wordIndex]);
        }

        // read new symbols
        while (!reader.isFileRead()) {
            try {
                wordIndex = reader.ReadNBits(currentVocabularyBits);
            } catch (IOException e) {
                error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "Cannot reread buffer");
                return;
            }

            if (wordIndex < lastNewWordIndex) {
                AddByteToWordSpace(vocabulary[wordIndex][0]);
                searchRes = IsVocContainThisWord(wordSpace, wordSpacePos);

                if (searchRes == -1) {
                    // add old word code to write buffer
                    try {
                        writter.WriteString(wordSpace, wordSpacePos - 1);
                    } catch (IOException e) {
                        error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "RE: Cannot write buffer");
                        return;
                    }
                    // add new word code to vocabulary
                    wordToAdd = new byte[wordSpacePos];
                    System.arraycopy(wordSpace, 0, wordToAdd, 0, wordSpacePos);
                    DecodingAddWord(wordToAdd);

                    wordSpacePos = 0;
                    AddWordToWordSpace(vocabulary[wordIndex]);
                } else {
                    // increase word
                    AddWordToWordSpace(vocabulary[wordIndex]);
                }
            }
            // then code comes immediately -> last symbol = first symbol
            else if (wordIndex == lastNewWordIndex) {
                try {
                    writter.WriteString(wordSpace, wordSpacePos);
                } catch (IOException e) {
                    error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "RE: Cannot write buffer");
                    return;
                }
                // add new word code to vocabulary
                AddByteToWordSpace(wordSpace[0]);
                wordToAdd = new byte[wordSpacePos];
                System.arraycopy(wordSpace, 0, wordToAdd, 0, wordSpacePos);
                DecodingAddWord(wordToAdd);

                wordSpacePos = 0;
                AddWordToWordSpace(vocabulary[wordIndex]);
            }
        }

        try {
            writter.WriteString(wordSpace, wordSpacePos);
        } catch (IOException e) {
            error.UpdateError(error.ErrorCode.LZW_DECOMPRESS_ERR, "RE: Cannot write buffer");
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
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "RE: Cannot open file to read");
        }

        try {
            writter = new BufWritter(dirFile, bufSize);
        }
        catch (FileNotFoundException ex) {
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "RE: Cannot open file to write");
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
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "RE: Cannot close reader stream");
        }

        try {
            writter.CloseWritter();
        }
        catch (IOException e) {
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "RE: Cannot close writter stream");
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
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Number is too big for counting");
                    return 0;
                }
            }
            else {
                error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: String, which must contains number, contain character");
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

        // check how many fields are fill
        if (fieldsValues.size() != 5) {
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: In config matches not all fields");
            return;
        }

        // parse fields
        for (String fieldType : fieldsTypes) {
            // parse SRC_FILE path
            if (fieldType.equals(InfoType.srcFile.GetType())) {
                srcFile = fieldsValues.get(fieldType);
                if (srcFile == null) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: No 'SRC_FILE' field in conf");
                    return;
                }
            }
            // parse DIR_FILE path
            if (fieldType.equals(InfoType.dirFile.GetType())) {
                dirFile = fieldsValues.get(fieldType);
                if (dirFile == null) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: No 'DIR_FILE' field in conf");
                    return;
                }
            }
            // parse max vocabulary bits path
            if (fieldType.equals(InfoType.maxVocBits.GetType())) {
                String tmpStr = fieldsValues.get(fieldType);
                if (tmpStr == null) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: No 'MAX_BITS' field in conf");
                    return;
                }
                maxVocSize = ParseStrToInt(tmpStr);

                if (error.errNo != error.ErrorCode.NO_ERR)
                    return;
                // let vocabulary be not very big, but let him store, minimum, 256 symbols
                if (maxVocSize < 9 || maxVocSize > 31) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Bits for per word must be between [9; 31]");
                    return;
                }
            }
            // parse mode
            if (fieldType.equals(InfoType.mode.GetType())) {
                String tmpStr = fieldsValues.get(fieldType);
                if (tmpStr == null) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: No 'MODE' field in conf");
                    return;
                }
                modeWork = Mode.ToMode(ParseStrToInt((fieldsValues.get("MODE"))));

                if (error.errNo != error.ErrorCode.NO_ERR)
                    return;
                else if (modeWork != Mode.ENCODE && modeWork != Mode.DECODE) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Incorrect mode work: 0 - for compress, 1 - for decompress");
                    return;
                }
            }
            // parse buffer size
            if (fieldType.equals(InfoType.bufferSize.GetType())) {
                String tmpStr = fieldsValues.get(fieldType);
                if (tmpStr == null) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: No 'BUFFER_SIZE' field in conf");
                    return;
                }
                bufSize = ParseStrToInt(fieldsValues.get("BUFFER_SIZE"));

                if (error.errNo != error.ErrorCode.NO_ERR)
                    return;
                else if ((double) bufSize < Math.ceil((double) maxVocSize / 8)) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Buffer size can't fit index to write");
                    return;
                }
                // let buffer to store not more than 1Gb
                else if (bufSize > MaxBufSize) {
                    error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Too big size for buffer");
                    return;
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
        if (error.errNo != error.ErrorCode.NO_ERR)
            return;

        OpenBuffers();
        if (error.errNo != error.ErrorCode.NO_ERR)
            return;

        if (modeWork == Mode.ENCODE) {
            System.out.println("Going compress...");
            LZWCompress();
        }
        else if (modeWork == Mode.DECODE) {
            System.out.println("Going decompress...");
            LZWDecompress();
        }
        else {
            error.UpdateError(error.ErrorCode.LZW_PROC_ERR, "LE: Too big size for buffer");
            return;
        }
        CloseBuffers();
    }
}
