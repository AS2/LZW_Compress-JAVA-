import com.java_polytech.pipeline_interfaces.RC;

public class LZW_ExProcessor {
    private static boolean wasStarted = false;
    private final static RC LZW_DECOMPRESS_BAD_WORD_INDEX = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Readen bad word index");

    static void InitExProcessor(int newMaxVocSize) {
        LZW_ExVocabularyManager.InitVocabulary(newMaxVocSize);
    }

    // LZW compress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    static public byte[] LZWCompress(byte[] data) {
        byte symbol;
        int searchRes;
        byte[] wordWithSymb;

        if (data == null) {
            // add last word
            if (LZW_ExVocabularyManager.wordSpacePos != 0) {
                searchRes = LZW_ExVocabularyManager.IsVocContainThisWord(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos);
                LZW_ExBufferWriterManager.AddToBuffer(searchRes, LZW_ExVocabularyManager.currentVocabularyBits);
            }

            return LZW_ExBufferWriterManager.GetWritterBufferAtEnding();
        }

        LZW_ExBufferReaderManager.InitBytesManager(data);

        // read first symbol
        if (!LZW_ExBufferReaderManager.isBufferEnded() && !wasStarted) {
            symbol = LZW_ExBufferReaderManager.ReadByte();
            LZW_ExVocabularyManager.AddByteToWordSpace(symbol);
            wasStarted = true;
        }

        // read new symbols
        while (!LZW_ExBufferReaderManager.isBufferEnded()) {
            symbol = LZW_ExBufferReaderManager.ReadByte();
            LZW_ExVocabularyManager.AddByteToWordSpace(symbol);

            searchRes = LZW_ExVocabularyManager.IsVocContainThisWord(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos);

            if (searchRes == -1) {
                // add old word code to write buffer
                searchRes = LZW_ExVocabularyManager.IsVocContainThisWord(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos - 1);

                LZW_ExBufferWriterManager.AddToBuffer(searchRes, LZW_ExVocabularyManager.currentVocabularyBits);

                // add new word code to vocabulary
                wordWithSymb = new byte[LZW_ExVocabularyManager.wordSpacePos];
                System.arraycopy(LZW_ExVocabularyManager.wordSpace, 0, wordWithSymb, 0, LZW_ExVocabularyManager.wordSpacePos);
                LZW_ExVocabularyManager.CoddingAddWord(wordWithSymb);

                // refresh word space
                LZW_ExVocabularyManager.wordSpacePos = 0;
                LZW_ExVocabularyManager.AddByteToWordSpace(symbol);
            }
        }

        return LZW_ExBufferWriterManager.SendBufferToConsumer();
    }

    // LZW decompress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    public static boolean decompressResult;

    static public byte[] LZWDecompress(byte[] data) {
        int wordIndex, searchRes;
        byte[] wordToAdd;

        if (data == null) {
            LZW_ExBufferWriterManager.WriteString(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos);
            return LZW_ExBufferWriterManager.GetWritterBufferAtEnding();
        }

        LZW_ExBufferReaderManager.InitBytesManager(data);

        // read first symbol
        if (!LZW_ExBufferReaderManager.isBufferEnded() && !wasStarted) {
            wordIndex = LZW_ExBufferReaderManager.ReadNBits(LZW_ExVocabularyManager.currentVocabularyBits);
            LZW_ExVocabularyManager.wordSpacePos = 0;
            //if read bad word index -> leave with error status
            if (wordIndex < 0 || wordIndex > LZW_ExVocabularyManager.lastNewWordIndex) {
                decompressResult = false;
                return null;
            }
            // add word in 'sandbox' array
            LZW_ExVocabularyManager.AddWordToWordSpace(LZW_ExVocabularyManager.vocabulary[wordIndex].wordInBytes);
            wasStarted = true;
        }

        // read new symbols
        while (!LZW_ExBufferReaderManager.isBufferEnded()) {
            // if trying read N bits on last iteration ended with no error -> read all N bits again
            if (LZW_ExBufferReaderManager.READNBITS_RESULT == LZW_ExBufferReaderManager.READNBITS_RESULT_TYPE.COMPLETED)
                wordIndex = LZW_ExBufferReaderManager.ReadNBits(LZW_ExVocabularyManager.currentVocabularyBits);
            // if trying read N bits on last iteration ended with error -> read only remained part from new buffer
            else
                wordIndex = LZW_ExBufferReaderManager.ReadRemainedPart();

            // if trying read N bits on last function call ended with error -> save result and leave 'while' loop
            if (LZW_ExBufferReaderManager.READNBITS_RESULT == LZW_ExBufferReaderManager.READNBITS_RESULT_TYPE.NOT_COMPLETED)
                break;

            // if unknown word -> make usual algorithm: try to add new letter to word, if exist -> continue searching, if not -> write word and read new index
            if (wordIndex < LZW_ExVocabularyManager.lastNewWordIndex) {
                LZW_ExVocabularyManager.AddByteToWordSpace(LZW_ExVocabularyManager.vocabulary[wordIndex].wordInBytes[0]);
                searchRes = LZW_ExVocabularyManager.IsVocContainThisWord(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos);

                if (searchRes == -1) {
                    // add old word code to write buffer
                    LZW_ExBufferWriterManager.WriteString(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos - 1);

                    // add new word code to vocabulary
                    wordToAdd = new byte[LZW_ExVocabularyManager.wordSpacePos];
                    System.arraycopy(LZW_ExVocabularyManager.wordSpace, 0, wordToAdd, 0, LZW_ExVocabularyManager.wordSpacePos);
                    LZW_ExVocabularyManager.DecodingAddWord(wordToAdd);

                    LZW_ExVocabularyManager.wordSpacePos = 0;
                    LZW_ExVocabularyManager.AddWordToWordSpace(LZW_ExVocabularyManager.vocabulary[wordIndex].wordInBytes);
                } else {
                    // increase word
                    LZW_ExVocabularyManager.AddWordToWordSpace(LZW_ExVocabularyManager.vocabulary[wordIndex].wordInBytes);
                }
            }
            // then code comes immediately -> last symbol = first symbol
            else if (wordIndex == LZW_ExVocabularyManager.lastNewWordIndex) {
                LZW_ExBufferWriterManager.WriteString(LZW_ExVocabularyManager.wordSpace, LZW_ExVocabularyManager.wordSpacePos);

                // add new word code to vocabulary
                LZW_ExVocabularyManager.AddByteToWordSpace(LZW_ExVocabularyManager.wordSpace[0]);
                wordToAdd = new byte[LZW_ExVocabularyManager.wordSpacePos];
                System.arraycopy(LZW_ExVocabularyManager.wordSpace, 0, wordToAdd, 0, LZW_ExVocabularyManager.wordSpacePos);
                LZW_ExVocabularyManager.DecodingAddWord(wordToAdd);

                LZW_ExVocabularyManager.wordSpacePos = 0;
                LZW_ExVocabularyManager.AddWordToWordSpace(LZW_ExVocabularyManager.vocabulary[wordIndex].wordInBytes);
            }
            // then code incorrect -> leave with error
            else {
                decompressResult = false;
                return null;
            }
        }

        decompressResult = true;
        return LZW_ExBufferWriterManager.SendBufferToConsumer();
    }
}