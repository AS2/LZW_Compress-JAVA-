import java.util.Arrays;

public class LZW_ExVocabularyManager {
    final static private int DEFAULT_MAX_ARR_SIZE = 100;

    // Convert "unsigned" char to int method
    // Args: - Byte - Byte to parse
    // Return: int - parsed Byte
    private int ConvertToInt(byte Byte) {
        if (Byte < 0)
            return (int)(Byte) + 256;
        else
            return (Byte);
    }

    int CountArrHash(byte[] arr, int arrLength) {
        int res = 0, cnt = 0;

        for (int i = arrLength - 1; i >= 0 & cnt < 4; i--) {
            res = (res << 8) | ConvertToInt(arr[i]);
            cnt++;
        }

        return res;
    }

    // vocabulary parameters
    public class wordStruct {
        byte[] wordInBytes;
        int wordHash;

        wordStruct(byte[] newWordInBytes) {
            wordInBytes = newWordInBytes;
            wordHash = CountArrHash(wordInBytes, wordInBytes.length);
        }
    }

    private int maxVocSize;                             // Maximum bits per vocabulary index

    public wordStruct[] vocabulary;                     // vocabulary
    public int arraySize, lastNewWordIndex;             // vocabulary information
    public int currentVocabularyBits;

    public byte[] wordSpace;                            // word space for reader
    public int wordSpaceLength, wordSpacePos;           // word space parameters

    // Init vocabulary
    public void InitVocabulary(int newMaxVocSize) {
        maxVocSize = newMaxVocSize;

        vocabulary = new wordStruct[512];
        byte[] byteCode;

        for (int i = 0; i < 256; i++) {
            byteCode = new byte[1];
            byteCode[0] = (byte)i;
            vocabulary[i] = new wordStruct(byteCode);
        }

        wordSpaceLength = DEFAULT_MAX_ARR_SIZE;
        wordSpace = new byte[DEFAULT_MAX_ARR_SIZE];

        currentVocabularyBits = 9;
        arraySize = 512;
        lastNewWordIndex = 256;
    }

    // Compare words:
    // Arguments: byte[] arr1, byte[] arr2 - words to compare
    // Returns: boolean - false if words don't same, true - if same
    public boolean CompareWords(wordStruct wordToCompare, byte[] arr, int lArr, int arrHash) {
        if (wordToCompare.wordInBytes.length != lArr)
            return false;
        else {
            if (wordToCompare.wordHash != arrHash)
                return false;

            for (int i = 0; i < lArr; i++)
                if (wordToCompare.wordInBytes[i] != arr[i])
                    return false;

            return true;
        }
    }

    // Find word in vocabulary:
    // Arguments: byte[] word - word to find
    // Returns: int - '-1' if voc doesn't contain word, not negative number - word index
    public int IsVocContainThisWord(byte[] word, int wordLength) {
        int wordHash = CountArrHash(word, wordLength);

        if (wordLength == 1) {
            for (int i = 0; i < 256; i++)
                if (CompareWords(vocabulary[i], word, wordLength, wordHash))
                    return i;
        }
        else if (word.length > 1) {
            for (int i = 256; i < lastNewWordIndex; i++)
                if (CompareWords(vocabulary[i], word, wordLength, wordHash))
                    return i;
        }
        return -1;
    }

    // Resize vocabulary function:
    // Arguments: none
    // Returns: none
    public void ResizeVoc() {
        vocabulary = Arrays.copyOf(vocabulary,arraySize * 2);
        arraySize *= 2;
        currentVocabularyBits++;
    }

    // Add word to vocabulary in compress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    public void CoddingAddWord(byte[] word) {
        if (lastNewWordIndex >= arraySize && currentVocabularyBits <= maxVocSize) {
            ResizeVoc();
            vocabulary[lastNewWordIndex++] = new wordStruct(word);
        }
        else if (lastNewWordIndex < arraySize)
            vocabulary[lastNewWordIndex++] = new wordStruct(word);
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    public void DecodingAddWord(byte[] word) {
        if (lastNewWordIndex < arraySize) {
            vocabulary[lastNewWordIndex++] = new wordStruct(word);
            if (lastNewWordIndex == arraySize && currentVocabularyBits <= maxVocSize)
                ResizeVoc();
        }
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    public void AddByteToWordSpace(byte newByte) {
        if (wordSpacePos == wordSpaceLength) {
            wordSpaceLength *= 2;
            wordSpace = Arrays.copyOf(wordSpace, wordSpaceLength);
        }
        wordSpace[wordSpacePos++] = newByte;
    }

    // Add word to vocabulary in decompress type work
    // Arguments: byte[] word - word to add
    // Returns: none
    public void AddWordToWordSpace(byte[] newWord) {
        for (byte ch : newWord)
            AddByteToWordSpace(ch);
    }
}
