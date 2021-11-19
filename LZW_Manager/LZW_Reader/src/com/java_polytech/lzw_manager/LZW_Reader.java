package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class LZW_Reader implements IReader {
    private static final RC RC_NULL_CONSUMER = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Reader taken 'null' IConsumer");
    private static final RC RC_NULL_ISTREAM = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Reader taken 'null' input stream");
    private static final RC RC_ISTREAM_ERR = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Input stream error");

    private final LZW_ConfGramAbstract grammar = new LZW_ReaderGrammar();

    private IConsumer readerConsumer;
    private InputStream readerInputStream;
    private int bufferSize;

    @Override
    public RC setConfig(String cfgFileName) {
        LZW_Config config = new LZW_Config(grammar);
        FileReader configFile;

        try {
            configFile = new FileReader(cfgFileName);
            RC configParserRC = config.Parse(configFile);

            if (!configParserRC.isSuccess()) {
                return configParserRC;
            }
        } catch (FileNotFoundException ex) {
            return RC.RC_READER_CONFIG_FILE_ERROR;
        }

        String bufferSizeStr = config.GetValue(LZW_ReaderGrammar.LZW_ReaderConfFields.BUFFER_SIZE.toString());
        if (bufferSizeStr == null)
            return RC.RC_READER_CONFIG_GRAMMAR_ERROR;

        try {
            bufferSize = Integer.parseInt(bufferSizeStr);
            if (bufferSize <= 0)
                return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }
        catch (NumberFormatException ex) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }

        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer consumer) {
        if (consumer == null)
            return RC_NULL_CONSUMER;
        readerConsumer = consumer;
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setInputStream(InputStream input) {
        if (input == null)
            return RC_NULL_ISTREAM;
        readerInputStream = input;
        return RC.RC_SUCCESS;
    }

    @Override
    public RC run() {
        byte[] byteToConsumer;
        byte[] bytesTmp = new byte[bufferSize];
        int readenPart;

        do {
            try {
                readenPart = readerInputStream.read(bytesTmp, 0, bufferSize);
            } catch (IOException e) {
                return RC_ISTREAM_ERR;
            }

            // if read 0 bytes or less -> exit and dont try to copy arrays
            if (readenPart <= 0)
                break;

            byteToConsumer = Arrays.copyOf(bytesTmp, readenPart);
            RC consumerSend = readerConsumer.consume(byteToConsumer);
            if (!consumerSend.isSuccess())
                return consumerSend;
        } while (true);

        // end translation of data
        RC endConsumerTranslation = readerConsumer.consume(null);
        if (!endConsumerTranslation.isSuccess())
            return endConsumerTranslation;

        try {
            readerInputStream.close();
        } catch (IOException e) {
            return RC_ISTREAM_ERR;
        }

        return RC.RC_SUCCESS;
    }
}
