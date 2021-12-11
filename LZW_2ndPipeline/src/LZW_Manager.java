import com.java_polytech.pipeline_interfaces.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LZW_Manager {
    private final static RC RC_MANAGER_BAD_ARGS_COUNT = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Count of started arguments must be 1!");
    private final static RC RC_CLOSE_STREAM_ERR = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't close stream");
    private final static RC RC_MANAGER_CONFIG_FIELD_LOTS_ARGS = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Some field in 'LZW_Manager' config, which must have 1 argument, has more than 1 argument");
    private final static RC RC_MANAGER_CONFIG_DIFFERENT_EXECUTORS_PARAMS_SIZES = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Different count of executors anr their configs : Some executors doesn't have config paths or there is too much config paths");
    private final static RC RC_LOGGER_DONT_INIT_PATH = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "No path to logger");
    private final static RC RC_LOGGER_INIT_FAILURE = new RC(RC.RCWho.MANAGER, RC.RCType.CODE_CUSTOM_ERROR, "Can't init logger");

    private final LZW_ConfGramAbstract grammar = new LZW_ManagerConfig();

    private String srcFilePath;
    private String dirFilePath;
    private String readerClassName;
    private String readerConfigPath;
    private ArrayList<String> executorClassesNames;
    private ArrayList<String> executorConfigsPaths;
    private String writerClassName;
    private String writerConfigPath;
    private String loggerFileName;

    private Logger logger;

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
        ArrayList<String> srcFilePathArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.SRC_FILE.toString());
        if (srcFilePathArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (srcFilePathArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        srcFilePath = srcFilePathArray.get(0);

        ArrayList<String> dirFilePathArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.DIR_FILE.toString());
        if (dirFilePathArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (dirFilePathArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        dirFilePath = dirFilePathArray.get(0);

        ArrayList<String> loggerPathArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.LOG_PATH.toString());
        if (loggerPathArray == null)
            return RC_LOGGER_DONT_INIT_PATH;
        else if (loggerPathArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        loggerFileName = loggerPathArray.get(0);

        // READ CLASSES NAMES
        // Reader
        ArrayList<String> readerClassNameArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.READER_CLASS.toString());
        if (readerClassNameArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (readerClassNameArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        readerClassName = readerClassNameArray.get(0);

        // Executors
        executorClassesNames = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.EXECUTOR_CLASSES.toString());
        if (executorClassesNames == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;

        // Writer
        ArrayList<String> writerClassNameArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.WRITER_CLASS.toString());
        if (writerClassNameArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (writerClassNameArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        writerClassName = writerClassNameArray.get(0);

        // READ PIPELINES' CONFIGS PATHS
        // Reader
        ArrayList<String> readerConfigPathArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.READER_CONFIG.toString());
        if (readerConfigPathArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (readerConfigPathArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        readerConfigPath = readerConfigPathArray.get(0);

        executorConfigsPaths = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.EXECUTOR_CONFIGS.toString());
        if (executorConfigsPaths == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (executorClassesNames.size() != executorConfigsPaths.size())
            return RC_MANAGER_CONFIG_DIFFERENT_EXECUTORS_PARAMS_SIZES;

        ArrayList<String> writerConfigPathArray = config.GetValue(LZW_ManagerConfig.LZW_ManagerConfFields.WRITER_CONFIG.toString());
        if (writerConfigPathArray == null)
            return RC.RC_MANAGER_CONFIG_GRAMMAR_ERROR;
        else if (writerConfigPathArray.size() != 1)
            return RC_MANAGER_CONFIG_FIELD_LOTS_ARGS;
        writerConfigPath = writerConfigPathArray.get(0);

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
        ArrayList<IExecutor> iExecutors = new ArrayList<>();
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

        // init executors
        IExecutor exTmp;
        for (String executorClassesName : executorClassesNames) {
            try {
                exTmp = (IExecutor) CreatePipelineModule(executorClassesName);
                iExecutors.add(exTmp);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                return RC.RC_MANAGER_INVALID_EXECUTOR_CLASS;
            }
        }

        // LINK PARTS
        rc = iReader.setConsumer(iExecutors.get(0));
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        rc = iExecutors.get(iExecutors.size() - 1).setConsumer(iWriter);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        for (int index = 0; index < iExecutors.size() - 1; index++) {
            rc = iExecutors.get(index).setConsumer(iExecutors.get(index + 1));
            if (!rc.isSuccess()) {
                CloseInputStream(iStream);
                CloseOutputStream(oStream);
                return rc;
            }
        }

        rc = iReader.setConfig(readerConfigPath);
        if (!rc.isSuccess()) {
            CloseInputStream(iStream);
            CloseOutputStream(oStream);
            return rc;
        }

        for (int index = 0; index < executorConfigsPaths.size(); index++) {
            rc = iExecutors.get(index).setConfig(executorConfigsPaths.get(index));
            if (!rc.isSuccess()) {
                CloseInputStream(iStream);
                CloseOutputStream(oStream);
                return rc;
            }
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

    private RC InitLogger() {
        logger = Logger.getLogger("Logger");
        FileHandler fh;
        try {
           fh = new FileHandler(loggerFileName);
        } catch (IOException e) {
            return RC_LOGGER_INIT_FAILURE;
        }

        SimpleFormatter sf = new SimpleFormatter();
        fh.setFormatter(sf);
        logger.addHandler(fh);
        logger.setUseParentHandlers(false);

        return RC.RC_SUCCESS;
    }

    public RC Run(String[] args) {
        System.out.println("LZW Manager start working");
        if (args.length != 1) {
            System.out.println("ERROR : " + RC_MANAGER_BAD_ARGS_COUNT.info);
            return RC_MANAGER_BAD_ARGS_COUNT;
        }

        RC configRC = ParseConfig(args[0]);
        if (!configRC.isSuccess()) {
            System.out.println("ERROR : " + configRC.info + " " + configRC.type);
            return configRC;
        }

        RC initLogger = InitLogger();
        if (!initLogger.isSuccess()) {
            System.out.println("Cant init logger");
            return initLogger;
        }

        RC pipelineRC = BuildPipeline();
        if (!pipelineRC.isSuccess()) {
            System.out.println("Something goes wrong. Check 'log' file.");
            logger.severe("ERROR : " + pipelineRC.info);
            return pipelineRC;
        }

        System.out.println("LZW Manager complete working process successful");
        logger.severe("LZW Manager complete working process successful");
        return RC.RC_SUCCESS;
    }
}
