package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class LZW_Manager {
    private final static RC RC_CLOSE_STREAM_ERR = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't close stream");

    private final LZW_ConfGramAbstract grammar = new LZW_ManagerConfig();

    private String srcFilePath;
    private String dirFilePath;
    private String readerClassName;
    private String executorClassName;
    private String writerClassName;
    private String readerConfigPath;
    private String executorConfigPath;
    private String writerConfigPath;

    private RC ParseConfig(String configPath) {
        LZW_Config config = new LZW_Config(grammar);
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

        // READ FILE PATHS
        srcFilePath = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.SRC_FILE.toString());
        if (srcFilePath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        dirFilePath = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.DIR_FILE.toString());
        if (dirFilePath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        // READ CLASSES NAMES
        readerClassName = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.READER_CLASS.toString());
        if (readerClassName == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        executorClassName = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.EXECUTOR_CLASS.toString());
        if (executorClassName == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        writerClassName = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.WRITER_CLASS.toString());
        if (writerClassName == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        // READ PIPELINES' CONFIGS PATHS
        readerConfigPath = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.READER_CONFIG.toString());
        if (readerConfigPath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        executorConfigPath = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.EXECUTOR_CONFIG.toString());
        if (executorConfigPath == null) {
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        }

        writerConfigPath = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.WRITER_CONFIG.toString());
        if (writerConfigPath == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        return RC.RC_SUCCESS;
    }

    private IConfigurable CreatePipelineModule(String moduleClassName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clss = Class.forName(moduleClassName);
        Constructor<?> cnstrctr = clss.getConstructor();
        Object obj = cnstrctr.newInstance();
        return (IConfigurable)obj;
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

        IReader iReader;
        IExecutor iExecutor;
        IWriter iWriter;

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

        try {
            iReader = (IReader) CreatePipelineModule(readerClassName);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            return RC.RC_MANAGER_INVALID_READER_CLASS;
        }
        rc = iReader.setInputStream(iStream);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        try {
            iWriter = (IWriter) CreatePipelineModule(writerClassName);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return RC.RC_MANAGER_INVALID_WRITER_CLASS;
        }
        rc = iWriter.setOutputStream(oStream);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        try {
            iExecutor = (IExecutor) CreatePipelineModule(executorClassName);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
        }

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

        rc = iWriter.setConfig(writerConfigPath);
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
