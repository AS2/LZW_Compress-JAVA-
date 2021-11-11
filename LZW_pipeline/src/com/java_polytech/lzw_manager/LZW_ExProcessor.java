package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.RC;

public class LZW_ExProcessor {
    private final static RC RC_LZW_BAD_VOC_SIZE = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "LZW processor taken bad");
    private final static RC RC_LZW_READER_MANAGER_CANT_READ_BYTE = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "LZW processor cannot take byte");


    private LZW_ExBuffersManager.BytesReaderManager bytesReaderManager;
    private LZW_ExBuffersManager.BytesWriterManager bytesWriterManager;

    private LZW_ExVocabularyManager vocabulary;
    private boolean wasStarted;

    LZW_ExProcessor() {
        vocabulary = new LZW_ExVocabularyManager();
        bytesReaderManager = new LZW_ExBuffersManager.BytesReaderManager();
        bytesWriterManager = new LZW_ExBuffersManager.BytesWriterManager();
        wasStarted = false;
    }

    RC InitExProcessor(int newMaxVocSize) {
        if (newMaxVocSize <= 8 || newMaxVocSize > 32)
            return RC_LZW_BAD_VOC_SIZE;
        vocabulary.InitVocabulary(newMaxVocSize);
        return RC.RC_SUCCESS;
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
            if (vocabulary.wordSpacePos != 0) {
                searchRes = vocabulary.IsVocContainThisWord(vocabulary.wordSpace, vocabulary.wordSpacePos);
                bytesWriterManager.AddToBuffer(searchRes, vocabulary.currentVocabularyBits);
            }

            return bytesWriterManager.GetWritterBufferAtEnding();
        }

        bytesReaderManager.InitBytesManager(data);

        // read first symbol
        if (!bytesReaderManager.isBufferEnded() && !wasStarted) {
            symbol = bytesReaderManager.ReadByte();
            vocabulary.AddByteToWordSpace(symbol);
            wasStarted = true;
        }

        // read new symbols
        while (!bytesReaderManager.isBufferEnded()) {
            symbol = bytesReaderManager.ReadByte();
            vocabulary.AddByteToWordSpace(symbol);

            searchRes = vocabulary.IsVocContainThisWord(vocabulary.wordSpace, vocabulary.wordSpacePos);

            if (searchRes == -1) {
                // add old word code to write buffer
                searchRes = vocabulary.IsVocContainThisWord(vocabulary.wordSpace, vocabulary.wordSpacePos - 1);

                bytesWriterManager.AddToBuffer(searchRes, vocabulary.currentVocabularyBits);

                // add new word code to vocabulary
                wordWithSymb = new byte[vocabulary.wordSpacePos];
                System.arraycopy(vocabulary.wordSpace, 0, wordWithSymb, 0, vocabulary.wordSpacePos);
                vocabulary.CoddingAddWord(wordWithSymb);

                // refresh word space
                vocabulary.wordSpacePos = 0;
                vocabulary.AddByteToWordSpace(symbol);
            }
        }

        return bytesWriterManager.SendBufferToConsumer();
    }

    // LZW decompress function.
    // Arguments: none (used data from global parameters from 'LZW' class)
    // Returns: none.
    public byte[] LZWDecompress(byte[] data) {
        int wordIndex, searchRes;
        byte[] wordToAdd;

        if (data == null) {
            bytesWriterManager.WriteString(vocabulary.wordSpace, vocabulary.wordSpacePos);
            return bytesWriterManager.GetWritterBufferAtEnding();
        }

        bytesReaderManager.InitBytesManager(data);

        // read first symbol
        if (!bytesReaderManager.isBufferEnded() && !wasStarted) {
            wordIndex = bytesReaderManager.ReadNBits(vocabulary.currentVocabularyBits);
            vocabulary.wordSpacePos = 0;
            vocabulary.AddWordToWordSpace(vocabulary.vocabulary[wordIndex].wordInBytes);
            wasStarted = true;
        }

        // read new symbols
        while (!bytesReaderManager.isBufferEnded()) {
            if (vocabulary.currentVocabularyBits == 11)
                System.out.println("11 bits");

            if (bytesReaderManager.READNBITS_RESULT == LZW_ExBuffersManager.BytesReaderManager.READNBITS_RESULT_TYPE.COMPLETED)
                wordIndex = bytesReaderManager.ReadNBits(vocabulary.currentVocabularyBits);
            else
                wordIndex = bytesReaderManager.ReadRemainedPart();

            if (bytesReaderManager.READNBITS_RESULT == LZW_ExBuffersManager.BytesReaderManager.READNBITS_RESULT_TYPE.NOT_COMPLETED)
                break;

            if (wordIndex < vocabulary.lastNewWordIndex) {
                vocabulary.AddByteToWordSpace(vocabulary.vocabulary[wordIndex].wordInBytes[0]);
                searchRes = vocabulary.IsVocContainThisWord(vocabulary.wordSpace, vocabulary.wordSpacePos);

                if (searchRes == -1) {
                    // add old word code to write buffer
                    bytesWriterManager.WriteString(vocabulary.wordSpace, vocabulary.wordSpacePos - 1);

                    // add new word code to vocabulary
                    wordToAdd = new byte[vocabulary.wordSpacePos];
                    System.arraycopy(vocabulary.wordSpace, 0, wordToAdd, 0, vocabulary.wordSpacePos);
                    vocabulary.DecodingAddWord(wordToAdd);

                    vocabulary.wordSpacePos = 0;
                    vocabulary.AddWordToWordSpace(vocabulary.vocabulary[wordIndex].wordInBytes);
                } else {
                    // increase word
                    vocabulary.AddWordToWordSpace(vocabulary.vocabulary[wordIndex].wordInBytes);
                }
            }
            // then code comes immediately -> last symbol = first symbol
            else if (wordIndex == vocabulary.lastNewWordIndex) {
                bytesWriterManager.WriteString(vocabulary.wordSpace, vocabulary.wordSpacePos);

                // add new word code to vocabulary
                vocabulary.AddByteToWordSpace(vocabulary.wordSpace[0]);
                wordToAdd = new byte[vocabulary.wordSpacePos];
                System.arraycopy(vocabulary.wordSpace, 0, wordToAdd, 0, vocabulary.wordSpacePos);
                vocabulary.DecodingAddWord(wordToAdd);

                vocabulary.wordSpacePos = 0;
                vocabulary.AddWordToWordSpace(vocabulary.vocabulary[wordIndex].wordInBytes);
            }
        }

        return bytesWriterManager.SendBufferToConsumer();
    }
}
