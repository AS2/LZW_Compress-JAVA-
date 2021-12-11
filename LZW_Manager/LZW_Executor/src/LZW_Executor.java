import com.java_polytech.pipeline_interfaces.IConsumer;
import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

public class LZW_Executor implements IExecutor {
    private static final RC RC_NULL_CONSUMER = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Executor taken 'null' IConsumer");
    private static final RC RC_BAD_WORD_INDEX_IN_FILE = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Decompiler read bad wordIndex - maybe you trying to decompress origin file");

    private final LZW_ConfGramAbstract grammar = new LZW_ExGrammar();

    private enum Mode {
        ENCODE,
        DECODE
    }

    private IConsumer executorComsumer;
    private Mode executorMode;
    private int minConsumeredBufferSize;

    private byte[] remainedData;

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
            return RC.RC_EXECUTOR_CONFIG_FILE_ERROR;
        }

        String executorWorkMode = config.GetValue(LZW_ExGrammar.LZW_ExConfFields.EXECUTOR_MODE.toString());
        if (executorWorkMode == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;

        if (executorWorkMode.equals(Mode.ENCODE.toString()))
            executorMode = Mode.ENCODE;
        else if (executorWorkMode.equals(Mode.DECODE.toString()))
            executorMode = Mode.DECODE;
        else
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;

        String maxBitsStr = config.GetValue(LZW_ExGrammar.LZW_ExConfFields.MAX_BITS.toString());
        if (maxBitsStr == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;

        int maxBitsVocabulary;
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

        LZW_ExProcessor.InitExProcessor(maxBitsVocabulary);

        return RC.RC_SUCCESS;
    }

    private RC ProcessBuffer(byte[] buffer) {
        byte[] newBytes;

        if (executorMode == Mode.ENCODE)
            newBytes = LZW_ExProcessor.LZWCompress(buffer);
        else {
            newBytes = LZW_ExProcessor.LZWDecompress(buffer);
            if (newBytes == null && !LZW_ExProcessor.decompressResult)
                return RC_BAD_WORD_INDEX_IN_FILE;
        }

        RC consumePipeline = executorComsumer.consume(newBytes);
        if (!consumePipeline.isSuccess())
            return consumePipeline;

        return RC.RC_SUCCESS;
    }

    @Override
    public RC consume(byte[] buff) {
        if (buff == null) {
            RC rc = ProcessBuffer(remainedData);
            if (!rc.isSuccess())
                return rc;

            rc = ProcessBuffer(null);
            if (!rc.isSuccess())
                return rc;

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
                RC rc = ProcessBuffer(remainedData);
                if (!rc.isSuccess())
                    return rc;

                remainedData = new byte[0];
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
