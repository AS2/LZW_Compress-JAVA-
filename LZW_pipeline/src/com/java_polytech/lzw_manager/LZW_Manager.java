package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.IExecutor;
import com.java_polytech.pipeline_interfaces.IReader;
import com.java_polytech.pipeline_interfaces.IWriter;
import com.java_polytech.pipeline_interfaces.RC;

import java.io.*;
import java.net.URLClassLoader;
import java.util.stream.Stream;

public class LZW_Manager {
    private final static String SRC_FILE = "SRC_FILE";
    private final static String DIR_FILE = "DIR_FILE";
    private final static String EXECUTOR_CONFIG  = "EXECUTOR_CONFIG";
    private final static String READER_CONFIG = "READER_CONFIG";
    private final static String WRITER_CONFIG  = "WRITER_CONFIG";

    private final static RC RC_CLOSE_STREAM_ERR = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't close stream");

    private String srcFilePath;
    private String dirFilePath;
    private String readerConfigPath;
    private String executorConfigPath;
    private String writterConfigPath;

    private RC ParseConfig(String configPath) {
        LZW_Config config = new LZW_Config();
        FileReader configFile;

        try {
            configFile = new FileReader(configPath);
            RC configParserRC = config.Parse(configFile);

            if (!configParserRC.isSuccess()) {
                return configParserRC;
            }
        } catch (FileNotFoundException ex) {
            return RC.RC_MANAGER_CONFIG_FILE_ERROR;
        }

        srcFilePath = config.GetValue(SRC_FILE);
        if (srcFilePath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        dirFilePath = config.GetValue(DIR_FILE);
        if (dirFilePath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        readerConfigPath = config.GetValue(READER_CONFIG);
        if (readerConfigPath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        executorConfigPath = config.GetValue(EXECUTOR_CONFIG);
        if (executorConfigPath == null) {
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        }

        writterConfigPath = config.GetValue(WRITER_CONFIG);
        if (writterConfigPath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        return RC.RC_SUCCESS;
    }

    private RC CloseInputStream(InputStream iStream) {
        try {
            iStream.close();
        } catch (IOException e) {
            return RC_CLOSE_STREAM_ERR;
        }
        return RC.RC_SUCCESS;
    }

    private RC CloseOutputStream(OutputStream oStream) {
        try {
            oStream.close();
        } catch (IOException e) {
            return RC_CLOSE_STREAM_ERR;
        }
        return RC.RC_SUCCESS;
    }

    private RC BuildPipeline() {
        InputStream iStream;
        OutputStream oStream;
        RC rc;

        try {
            iStream = new FileInputStream(srcFilePath);
        } catch (FileNotFoundException e) {
            return RC.RC_MANAGER_INVALID_INPUT_FILE;
        }

        try {
            oStream = new FileOutputStream(dirFilePath);
        } catch (FileNotFoundException e) {
            CloseInputStream(iStream);
            return RC.RC_MANAGER_INVALID_OUTPUT_FILE;
        }

        IReader iReader = new LZW_Reader();
        rc = iReader.setInputStream(iStream);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        IWriter iWriter = new LZW_Writter();
        rc = iWriter.setOutputStream(oStream);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        IExecutor iExecutor = new LZW_Executor();

        rc = iReader.setConsumer(iExecutor);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iExecutor.setConsumer(iWriter);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iReader.setConfig(readerConfigPath);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iExecutor.setConfig(executorConfigPath);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iWriter.setConfig(writterConfigPath);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iReader.run();
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = CloseInputStream(iStream);
        if (!rc.isSuccess()) {
            CloseOutputStream(oStream);
            return rc;
        }

        rc = CloseOutputStream(oStream);
        if (!rc.isSuccess())
            return rc;

        return RC.RC_SUCCESS;
    }

    public RC Run(String configPath) {
        RC configRC = ParseConfig(configPath);
        if (!configRC.isSuccess())
            return configRC;

        RC pipelineRC = BuildPipeline();
        if (!pipelineRC.isSuccess())
            return pipelineRC;

        return RC.RC_SUCCESS;
    }
}
