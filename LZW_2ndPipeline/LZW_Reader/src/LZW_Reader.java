import com.java_polytech.pipeline_interfaces.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LZW_Reader implements IReader {
    private static final RC RC_NULL_CONSUMER = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Reader taken 'null' IConsumer");
    private static final RC RC_NULL_ISTREAM = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Reader taken 'null' input stream");
    private static final RC RC_ISTREAM_ERR = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Input stream error");
    private static final RC RC_READER_CONFIG_LOTS_ARGUMENT = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Some field in 'reader' config, which must have one argument, has more than one argument");

    private final LZW_ConfGramAbstract grammar = new LZW_ReaderGrammar();

    private IConsumer readerConsumer;
    private InputStream readerInputStream;

    private int bufferSize, readedSize;
    private byte[] buffer;

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY, TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};

    private class ByteBufferMediator implements IMediator {
        @Override
        public Object getData() {
            if (readedSize <= 0)
                return null;

            byte[] dataToSend = new byte[readedSize];
            System.arraycopy(buffer, 0, dataToSend, 0, readedSize);
            return dataToSend;
        }
    }

    private class CharBufferMediator implements IMediator {
        @Override
        public Object getData() {
            if (readedSize <= 0)
                return null;

            byte[] dataTmp = new byte[readedSize];
            System.arraycopy(buffer, 0, dataTmp, 0, readedSize);
            CharBuffer charBuf = ByteBuffer.wrap(dataTmp)
                    .order(ByteOrder.BIG_ENDIAN)
                    .asCharBuffer();
            char[] dataToSend = new char[charBuf.remaining()];
            charBuf.get(dataToSend);
            return dataToSend;
        }
    }

    private class IntBufferMediator implements IMediator {
        @Override
        public Object getData() {
            if (readedSize <= 0)
                return null;

            byte[] dataTmp = new byte[readedSize];
            System.arraycopy(buffer, 0, dataTmp, 0, readedSize);
            IntBuffer intBuf = ByteBuffer.wrap(dataTmp)
                    .order(ByteOrder.BIG_ENDIAN)
                    .asIntBuffer();
            int[] dataToSend = new int[intBuf.remaining()];
            intBuf.get(dataToSend);
            return dataToSend;
        }
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
        buffer = new byte[bufferSize];

        do {
            try {
                readedSize = readerInputStream.read(buffer, 0, bufferSize);
                // if read 0 bytes or less -> exit and dont try to copy arrays
                if (readedSize <= 0)
                    break;
            } catch (IOException e) {
                return RC_ISTREAM_ERR;
            }

            RC consumerSend = readerConsumer.consume();
            if (!consumerSend.isSuccess())
                return consumerSend;
        } while (readedSize > 0);

        return readerConsumer.consume();
    }

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

        ArrayList<String> bufferSizeStr = config.GetValue(LZW_ReaderGrammar.LZW_ReaderConfFields.BUFFER_SIZE.toString());
        if (bufferSizeStr == null)
            return RC.RC_READER_CONFIG_GRAMMAR_ERROR;
        else if (bufferSizeStr.size() != 1)
            return RC_READER_CONFIG_LOTS_ARGUMENT;

        try {
            bufferSize = Integer.parseInt(bufferSizeStr.get(0));
            if (bufferSize <= 0)
                return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }
        catch (NumberFormatException ex) {
            return RC.RC_READER_CONFIG_SEMANTIC_ERROR;
        }

        return RC.RC_SUCCESS;
    }

    @Override
    public RC setConsumer(IConsumer iConsumer) {
        if (iConsumer == null)
            return RC_NULL_CONSUMER;
        readerConsumer = iConsumer;
        RC rc = readerConsumer.setProvider(this);
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
        else if (type == TYPE.CHAR_ARRAY)
            return new CharBufferMediator();
        else if (type == TYPE.INT_ARRAY)
            return new IntBufferMediator();
        else
            return null;
    }
}
