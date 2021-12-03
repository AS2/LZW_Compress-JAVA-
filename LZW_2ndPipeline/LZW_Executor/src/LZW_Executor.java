import com.java_polytech.pipeline_interfaces.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class LZW_Executor implements IExecutor {
    private static final RC RC_NULL_CONSUMER = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Executor taken 'null' IConsumer");
    private static final RC RC_NULL_PROVIDER = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Executor taken 'null' IProvider");
    private static final RC RC_EMPTY_SAME_TYPES_SET = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "IProvider and LZW_Executor don't have same supported times");
    private static final RC RC_BAD_WORD_INDEX_IN_FILE = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Decompiler read bad wordIndex - check correct params to decompress");
    private static final RC RC_CONFIG_LZW_EXECUTOR_LOTS_ARGS = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Some field in 'LZW_EXECUTOR' config, which must have 1 argument, has more than 1 argument");


    private final LZW_ConfGramAbstract grammar = new LZW_ExGrammar();
    //private final TYPE[] supportedTypes = {TYPE.CHAR_ARRAY, TYPE.BYTE_ARRAY};
    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY};
    private TYPE currentType = null;

    private IProvider executorProvider;
    private IMediator mediator;
    private IConsumer executorComsumer;

    private enum Mode {
        ENCODE,
        DECODE
    }
    private Mode executorMode;

    private LZW_ExProcessor exProcessor = new LZW_ExProcessor();

    private int bufferOutSize;
    private byte[] bufferOut;

    private int minConsumeredBufferSize;
    private byte[] remainedData;

    private class ByteBufferMediator implements IMediator {
        @Override
        public Object getData() {
            if (bufferOutSize < 0)
                return null;

            byte[] dataToSend = new byte[bufferOutSize];
            System.arraycopy(bufferOut, 0, dataToSend, 0, bufferOutSize);
            return dataToSend;
        }
    }

    /*
    private class CharBufferMediator implements IMediator {
        @Override
        public Object getData() {
            if (bufferOutSize < 0)
                return null;

            assert (bufferOutSize % 2) == 0;
            byte[] dataTmp = new byte[bufferOutSize];
            System.arraycopy(bufferOut, 0, dataTmp, 0, bufferOutSize);
            CharBuffer charBuf = ByteBuffer.wrap(dataTmp)
                    .order(ByteOrder.BIG_ENDIAN)
                    .asCharBuffer();
            char[] dataToSend = new char[charBuf.remaining()];
            charBuf.get(dataToSend);
            return dataToSend;
        }
    }
     */

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

        ArrayList<String> executorWorkMode = config.GetValue(LZW_ExGrammar.LZW_ExConfFields.EXECUTOR_MODE.toString());
        if (executorWorkMode == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;
        else if (executorWorkMode.size() != 1)
            return RC_CONFIG_LZW_EXECUTOR_LOTS_ARGS;

        if (executorWorkMode.get(0).equals(Mode.ENCODE.toString()))
            executorMode = Mode.ENCODE;
        else if (executorWorkMode.get(0).equals(Mode.DECODE.toString()))
            executorMode = Mode.DECODE;
        else
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;

        ArrayList<String> maxBitsStr = config.GetValue(LZW_ExGrammar.LZW_ExConfFields.MAX_BITS.toString());
        if (maxBitsStr == null)
            return RC.RC_EXECUTOR_CONFIG_GRAMMAR_ERROR;
        else if (maxBitsStr.size() != 1)
            return RC_CONFIG_LZW_EXECUTOR_LOTS_ARGS;

        int maxBitsVocabulary;
        try {
            maxBitsVocabulary = Integer.parseInt(maxBitsStr.get(0));
            if (maxBitsVocabulary <= 8 || maxBitsVocabulary > 32)
                return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }
        catch (NumberFormatException ex) {
            return RC.RC_EXECUTOR_CONFIG_SEMANTIC_ERROR;
        }

        minConsumeredBufferSize = (int)Math.ceil((double) maxBitsVocabulary / 8);
        remainedData = new byte[0];

        exProcessor.InitExProcessor(maxBitsVocabulary);

        return RC.RC_SUCCESS;
    }

    @Override
    public RC setProvider(IProvider iProvider) {
        if (iProvider == null)
            return RC_NULL_PROVIDER;
        executorProvider = iProvider;

        currentType = null;
        TYPE[] provideTypes = executorProvider.getOutputTypes();
        for (int lzwTypes = 0; lzwTypes < supportedTypes.length && currentType == null; lzwTypes++)
            for (TYPE type : provideTypes)
                if (supportedTypes[lzwTypes] == type) {
                    currentType = supportedTypes[lzwTypes];
                    break;
                }

        if (currentType == null)
            return RC_EMPTY_SAME_TYPES_SET;

        mediator = executorProvider.getMediator(currentType);
        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        if (iConsumer == null)
            return RC_NULL_CONSUMER;
        executorComsumer = iConsumer;
        RC rc = executorComsumer.setProvider(this);
        if (!rc.isSuccess())
            return rc;
        return RC.RC_SUCCESS;
    }

    @Override
    public TYPE[] getOutputTypes() {
        return supportedTypes;
    }

    @Override
    public IMediator getMediator(TYPE type) {
        if (type == TYPE.BYTE_ARRAY)
            return new ByteBufferMediator();
        //else if (type == TYPE.CHAR_ARRAY)
        //    return new CharBufferMediator();
        else
            return null;
    }

    private RC ProcessBuffer(byte[] buffer) {
        // get buffer
        if (executorMode == Mode.ENCODE)
            bufferOut = exProcessor.LZWCompress(buffer);
        else {
            bufferOut = exProcessor.LZWDecompress(buffer);
            if (bufferOut == null && !exProcessor.decompressResult)
                return RC_BAD_WORD_INDEX_IN_FILE;
        }

        // get buffer length
        if (bufferOut != null)
            bufferOutSize = bufferOut.length;
        else
            bufferOutSize = -1;

        // consume
        RC consumePipeline = executorComsumer.consume();
        if (!consumePipeline.isSuccess())
            return consumePipeline;
        return RC.RC_SUCCESS;
    }

    private byte[] GetTransformedData() {
        if (currentType == TYPE.BYTE_ARRAY)
            return (byte[])mediator.getData();
        /*else if (currentType == TYPE.CHAR_ARRAY) {
            char[] charTmp = (char[])mediator.getData();
            if (charTmp == null)
                return null;

            ByteBuffer bytes = ByteBuffer.allocate(charTmp.length * 2);
            CharBuffer chars = bytes.asCharBuffer();
            chars.put(charTmp);
            return bytes.array();
        }*/
        else
            return null;
    }

    @Override
    public RC consume() {
        byte[] buff = GetTransformedData();

        if (buff == null) {
            RC rc = ProcessBuffer(remainedData);
            if (!rc.isSuccess())
                return rc;

            rc = ProcessBuffer(null);
            if (!rc.isSuccess())
                return rc;

            bufferOut = null;
            bufferOutSize = -1;
            RC stopPipeline = executorComsumer.consume();
            if (!stopPipeline.isSuccess())
                return stopPipeline;
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
        }
        return RC.RC_SUCCESS;
    }
}
