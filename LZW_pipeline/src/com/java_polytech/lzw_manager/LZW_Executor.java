package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class LZW_Executor implements IExecutor {
    private final static String EXECUTOR_MODE = "MODE";
    private final static String MAX_BITS = "MAX_BITS";

    private static final RC RC_NULL_CONSUMER = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Executor taken 'null' IConsumer");

    private enum Mode {
        ENCODE("ENCODE"),
        DECODE("DECODE");

        private final String type;
        Mode(String t) {
            type = t;
        }

        public String toStr() {
            return type;
        }
    }

    private IConsumer executorComsumer;
    private Mode executorMode;
    private int maxBitsVocabulary, minConsumeredBufferSize;
    private LZW_ExProcessor lzwProcessor;

    private byte[] remainedData;

    @Override
    public RC setConfig(String cfgFileName) {
        LZW_Config config = new LZW_Config();
        FileReader configFile;

        try {
            configFile = new FileReader(cfgFileName);
            RC configParserRC = config.Parse(configFile);

            if (!configParserRC.isSuccess()) {
                return configParserRC;
            }
        } catch (FileNotFoundException ex) {
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;
        }

        String executorWorkMode = config.GetValue(EXECUTOR_MODE);
        if (executorWorkMode == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;

        if (executorWorkMode.equals(Mode.ENCODE.toStr()))
            executorMode = Mode.ENCODE;
        else if (executorWorkMode.equals(Mode.DECODE.toStr()))
            executorMode = Mode.DECODE;
        else
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;

        String maxBitsStr = config.GetValue(MAX_BITS);
        if (maxBitsStr == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;

        try {
            maxBitsVocabulary = Integer.parseInt(maxBitsStr);
            if (maxBitsVocabulary <= 8 || maxBitsVocabulary > 32)
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        catch (NumberFormatException ex) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        minConsumeredBufferSize = (int)Math.ceil((double) maxBitsVocabulary / 8);
        remainedData = new byte[0];

        lzwProcessor = new LZW_ExProcessor();
        lzwProcessor.InitExProcessor(maxBitsVocabulary);

        return RC.RC_SUCCESS;
    }

    @Override
    public RC consume(byte[] buff) {
        if (buff == null) {
            byte[] lastBytes;

            if (executorMode == Mode.ENCODE)
                lastBytes = lzwProcessor.LZWCompress(remainedData);
            else
                lastBytes = lzwProcessor.LZWDecompress(remainedData);
            RC sendLastBytes = executorComsumer.consume(lastBytes);
            if (!sendLastBytes.isSuccess())
                return sendLastBytes;

            if (executorMode == Mode.ENCODE)
                lastBytes = lzwProcessor.LZWCompress(null);
            else
                lastBytes = lzwProcessor.LZWDecompress(null);

            sendLastBytes = executorComsumer.consume(lastBytes);
            if (!sendLastBytes.isSuccess())
                return sendLastBytes;

            RC stopPipeline = executorComsumer.consume(null);
            if (!stopPipeline.isSuccess())
                return stopPipeline;
            return RC.RC_SUCCESS;
        }
        else {
            int remainedDataPart = remainedData.length;
            remainedData = Arrays.copyOf(remainedData, remainedDataPart + buff.length);
            System.arraycopy(buff, 0, remainedData, remainedDataPart, buff.length);

            if (remainedData.length >= minConsumeredBufferSize) {
                byte[] newBytes;

                if (executorMode == Mode.ENCODE)
                    newBytes = lzwProcessor.LZWCompress(remainedData);
                else
                    newBytes = lzwProcessor.LZWDecompress(remainedData);

                remainedData = new byte[0];

                RC consumePipeline = executorComsumer.consume(newBytes);
                if (!consumePipeline.isSuccess())
                    return consumePipeline;
            }
            return RC.RC_SUCCESS;
        }
    }

    @Override
    public RC setConsumer(IConsumer consumer) {
        if (consumer == null)
            return RC_NULL_CONSUMER;
        executorComsumer = consumer;
        return RC.RC_SUCCESS;
    }
}
