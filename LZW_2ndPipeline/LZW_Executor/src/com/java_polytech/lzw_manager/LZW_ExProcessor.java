package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.RC;

public class LZW_ExProcessor {
    private static boolean wasStarted = false;
    private final static RC LZW_DECOMPRESS_BAD_WORD_INDEX = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Readen bad word index");

    private LZW_ExVocabularyManager LZWVocabularyManager = new LZW_ExVocabularyManager();
    private LZW_ExBufferReaderManager LZWReaderManager = new LZW_ExBufferReaderManager();
    private LZW_ExBufferWriterManager LZWWriterManager = new LZW_ExBufferWriterManager();

    void InitExProcessor(int newMaxVocSize) {
        LZWVocabularyManager.InitVocabulary(newMaxVocSize);
    }

    // LZW compress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    public byte[] LZWCompress(byte[] data) {
        byte symbol;
        int searchRes;
        byte[] wordWithSymb;

        if (data == null) {
            // add last word
            if (LZWVocabularyManager.wordSpacePos != 0) {
                searchRes = LZWVocabularyManager.IsVocContainThisWord(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos);
                LZWWriterManager.AddToBuffer(searchRes, LZWVocabularyManager.currentVocabularyBits);
            }

            return LZWWriterManager.GetWritterBufferAtEnding();
        }

        LZWReaderManager.InitBytesManager(data);

        // read first symbol
        if (!LZWReaderManager.isBufferEnded() && !wasStarted) {
            symbol = LZWReaderManager.ReadByte();
            LZWVocabularyManager.AddByteToWordSpace(symbol);
            wasStarted = true;
        }

        // read new symbols
        while (!LZWReaderManager.isBufferEnded()) {
            symbol = LZWReaderManager.ReadByte();
            LZWVocabularyManager.AddByteToWordSpace(symbol);

            searchRes = LZWVocabularyManager.IsVocContainThisWord(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos);

            if (searchRes == -1) {
                // add old word code to write buffer
                searchRes = LZWVocabularyManager.IsVocContainThisWord(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos - 1);

                LZWWriterManager.AddToBuffer(searchRes, LZWVocabularyManager.currentVocabularyBits);

                // add new word code to vocabulary
                wordWithSymb = new byte[LZWVocabularyManager.wordSpacePos];
                System.arraycopy(LZWVocabularyManager.wordSpace, 0, wordWithSymb, 0, LZWVocabularyManager.wordSpacePos);
                LZWVocabularyManager.CoddingAddWord(wordWithSymb);

                // refresh word space
                LZWVocabularyManager.wordSpacePos = 0;
                LZWVocabularyManager.AddByteToWordSpace(symbol);
            }
        }

        return LZWWriterManager.SendBufferToConsumer();
    }

    // LZW decompress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    public boolean decompressResult;

    public byte[] LZWDecompress(byte[] data) {
        int wordIndex, searchRes;
        byte[] wordToAdd;

        if (data == null) {
            LZWWriterManager.WriteString(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos);
            return LZWWriterManager.GetWritterBufferAtEnding();
        }

        LZWReaderManager.InitBytesManager(data);

        // read first symbol
        if (!LZWReaderManager.isBufferEnded() && !wasStarted) {
            wordIndex = LZWReaderManager.ReadNBits(LZWVocabularyManager.currentVocabularyBits);
            LZWVocabularyManager.wordSpacePos = 0;
            //if read bad word index -> leave with error status
            if (wordIndex < 0 || wordIndex > LZWVocabularyManager.lastNewWordIndex) {
                decompressResult = false;
                return null;
            }
            // add word in 'sandbox' array
            LZWVocabularyManager.AddWordToWordSpace(LZWVocabularyManager.vocabulary[wordIndex].wordInBytes);
            wasStarted = true;
        }

        // read new symbols
        while (!LZWReaderManager.isBufferEnded()) {
            // if trying read N bits on last iteration ended with no error -> read all N bits again
            if (LZWReaderManager.READNBITS_RESULT == LZW_ExBufferReaderManager.READNBITS_RESULT_TYPE.COMPLETED)
                wordIndex = LZWReaderManager.ReadNBits(LZWVocabularyManager.currentVocabularyBits);
            // if trying read N bits on last iteration ended with error -> read only remained part from new buffer
            else
                wordIndex = LZWReaderManager.ReadRemainedPart();

            // if trying read N bits on last function call ended with error -> save result and leave 'while' loop
            if (LZWReaderManager.READNBITS_RESULT == LZW_ExBufferReaderManager.READNBITS_RESULT_TYPE.NOT_COMPLETED)
                break;

            // if unknown word -> make usual algorithm: try to add new letter to word, if exist -> continue searching, if not -> write word and read new index
            if (wordIndex < LZWVocabularyManager.lastNewWordIndex) {
                LZWVocabularyManager.AddByteToWordSpace(LZWVocabularyManager.vocabulary[wordIndex].wordInBytes[0]);
                searchRes = LZWVocabularyManager.IsVocContainThisWord(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos);

                if (searchRes == -1) {
                    // add old word code to write buffer
                    LZWWriterManager.WriteString(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos - 1);

                    // add new word code to vocabulary
                    wordToAdd = new byte[LZWVocabularyManager.wordSpacePos];
                    System.arraycopy(LZWVocabularyManager.wordSpace, 0, wordToAdd, 0, LZWVocabularyManager.wordSpacePos);
                    LZWVocabularyManager.DecodingAddWord(wordToAdd);

                    LZWVocabularyManager.wordSpacePos = 0;
                    LZWVocabularyManager.AddWordToWordSpace(LZWVocabularyManager.vocabulary[wordIndex].wordInBytes);
                } else {
                    // increase word
                    LZWVocabularyManager.AddWordToWordSpace(LZWVocabularyManager.vocabulary[wordIndex].wordInBytes);
                }
            }
            // then code comes immediately -> last symbol = first symbol
            else if (wordIndex == LZWVocabularyManager.lastNewWordIndex) {
                LZWWriterManager.WriteString(LZWVocabularyManager.wordSpace, LZWVocabularyManager.wordSpacePos);

                // add new word code to vocabulary
                LZWVocabularyManager.AddByteToWordSpace(LZWVocabularyManager.wordSpace[0]);
                wordToAdd = new byte[LZWVocabularyManager.wordSpacePos];
                System.arraycopy(LZWVocabularyManager.wordSpace, 0, wordToAdd, 0, LZWVocabularyManager.wordSpacePos);
                LZWVocabularyManager.DecodingAddWord(wordToAdd);

                LZWVocabularyManager.wordSpacePos = 0;
                LZWVocabularyManager.AddWordToWordSpace(LZWVocabularyManager.vocabulary[wordIndex].wordInBytes);
            }
            // then code incorrect -> leave with error
            else {
                decompressResult = false;
                return null;
            }
        }

        decompressResult = true;
        return LZWWriterManager.SendBufferToConsumer();
    }
}