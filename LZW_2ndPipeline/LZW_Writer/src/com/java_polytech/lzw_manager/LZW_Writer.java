package com.java_polytech.lzw_manager;

import com.java_polytech.lzw_manager.LZW_Config;
import com.java_polytech.pipeline_interfaces.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LZW_Writer implements IWriter {
    private static final RC RC_NULL_PROVIDER = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "Executor taken 'null' IProvider");
    private static final RC RC_EMPTY_SAME_TYPES_SET = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "IProvider and LZW_Executor don't have same supported times");
    private final static RC RC_NULL_OSTEAM = new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Writter taken 'null' OutputStream");
    private final static RC RC_OSTREAM_BUFFERED_ERR = new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error with buffered output stream working");
    private static final RC RC_WRITER_CONFIG_LOTS_ARGUMENT = new RC(RC.RCWho.READER, RC.RCType.CODE_CUSTOM_ERROR, "Some field in 'writer' config, which must have one argument, has more than one argument");


    private final LZW_ConfGramAbstract grammar = new LZW_WriterGrammar();

    private final TYPE[] supportedTypes = {TYPE.BYTE_ARRAY,TYPE.CHAR_ARRAY, TYPE.INT_ARRAY};
    private TYPE currentType = null;

    private IProvider writerProvider;
    private IMediator mediator;

    private int bufferSize;
    private OutputStream writterOutputStream;
    private BufferedOutputStream bufWritterOutputStream;

    @Override
    public RC setOutputStream(OutputStream outputStream) {
        if (outputStream == null)
            return RC_NULL_OSTEAM;
        writterOutputStream = outputStream;

        if (bufWritterOutputStream == null && bufferSize != 0)
            bufWritterOutputStream = new BufferedOutputStream(writterOutputStream, bufferSize);

        return RC.RC_SUCCESS;
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
            return RC.RC_WRITER_CONFIG_FILE_ERROR;
        }

        ArrayList<String> bufferSizeStr = config.GetValue(LZW_WriterGrammar.LZW_WriterConfFields.BUFFER_SIZE.toString());
        if (bufferSizeStr == null)
            return RC.RC_WRITER_CONFIG_GRAMMAR_ERROR;
        else if (bufferSizeStr.size() != 1)
            return RC_WRITER_CONFIG_LOTS_ARGUMENT;
        try {
            bufferSize = Integer.parseInt(bufferSizeStr.get(0));
            if (bufferSize <= 0)
                return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        }
        catch (NumberFormatException ex) {
            return RC.RC_WRITER_CONFIG_SEMANTIC_ERROR;
        }

        if (bufWritterOutputStream == null && writterOutputStream != null)
            bufWritterOutputStream = new BufferedOutputStream(writterOutputStream, bufferSize);

        return RC.RC_SUCCESS;
    }

    @Override
    public RC setProvider(IProvider iProvider) {
        if (iProvider == null)
            return RC_NULL_PROVIDER;
        writerProvider = iProvider;

        currentType = null;
        TYPE[] provideTypes = writerProvider.getOutputTypes();
        for (int lzwTypes = 0; lzwTypes < supportedTypes.length && currentType == null; lzwTypes++)
            for (TYPE type : provideTypes)
                if (supportedTypes[lzwTypes] == type) {
                    currentType = supportedTypes[lzwTypes];
                    break;
                }

        if (currentType == null)
            return RC_EMPTY_SAME_TYPES_SET;

        mediator = writerProvider.getMediator(currentType);
        return RC.RC_SUCCESS;
    }


    private byte[] GetTransformedData() {
        if (currentType == TYPE.BYTE_ARRAY)
            return (byte[])mediator.getData();
        else if (currentType == TYPE.CHAR_ARRAY) {
            char[] charTmp = (char[])mediator.getData();
            if (charTmp == null)
                return null;

            ByteBuffer bytes = ByteBuffer.allocate(charTmp.length * 4);
            CharBuffer chars = bytes.asCharBuffer();
            chars.put(charTmp);
            return bytes.array();
        }
        else if (currentType == TYPE.INT_ARRAY) {
            int[] intTmp = (int[])mediator.getData();
            if (intTmp == null)
                return null;
            ByteBuffer bytes = ByteBuffer.allocate(intTmp.length * 4);
            IntBuffer ints = bytes.asIntBuffer();
            ints.put(intTmp);
            return bytes.array();
        }
        else
            return null;
    }

    @Override
    public RC consume() {
        byte[] buff = GetTransformedData();

        if (buff == null) {
            try {
                bufWritterOutputStream.flush();
                writterOutputStream.close();
            } catch (IOException e) {
                return RC_OSTREAM_BUFFERED_ERR;
            }
        }
        else {
            try {
                bufWritterOutputStream.write(buff);
            } catch (IOException e) {
                return RC_OSTREAM_BUFFERED_ERR;
            }
        }
        return RC.RC_SUCCESS;
    }
}
