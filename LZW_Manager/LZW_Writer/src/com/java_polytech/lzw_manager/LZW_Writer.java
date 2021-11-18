package com.java_polytech.lzw_manager;

import com.java_polytech.lzw_manager.LZW_Config;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.*;

public class LZW_Writer implements IWriter {
    private final static RC RC_NULL_OSTEAM = new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Writter taken 'null' OutputStream");
    private final static RC RC_OSTREAM_BUFFERED_ERR = new RC(RC.RCWho.WRITER, RC.RCType.CODE_CUSTOM_ERROR, "Error with buffered output stream working");

    private final LZW_ConfGramAbstract grammar = new LZW_WriterGrammar();

    private int bufferSize;
    private OutputStream writterOutputStream;
    private BufferedOutputStream bufWritterOutputStream;

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

        String bufferSizeStr = config.GetValue(LZW_WriterGrammar.LZW_WriterConfFields.BUFFER_SIZE.toString());
        if (bufferSizeStr == null)
            return RC.RC_WRITER_CONFIG_GRAMMAR_ERROR;

        try {
            bufferSize = Integer.parseInt(bufferSizeStr);
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
    public RC consume(byte[] buff) {
        if (buff == null) {
            try {
                bufWritterOutputStream.flush();
                writterOutputStream.close();
            } catch (IOException e) {
                return RC_OSTREAM_BUFFERED_ERR;
            }
            return RC.RC_SUCCESS;
        }
        else {
            try {
                bufWritterOutputStream.write(buff);
            } catch (IOException e) {
                return RC_OSTREAM_BUFFERED_ERR;
            }
            return RC.RC_SUCCESS;
        }
    }

    @Override
    public RC setOutputStream(OutputStream output) {
        if (output == null)
            return RC_NULL_OSTEAM;
        writterOutputStream = output;

        if (bufWritterOutputStream == null && bufferSize != 0)
            bufWritterOutputStream = new BufferedOutputStream(writterOutputStream, bufferSize);

        return RC.RC_SUCCESS;
    }
}
