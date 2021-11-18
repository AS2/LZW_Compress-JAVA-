package com.java_polytech.lzw_manager;

import java.util.Arrays;

public class LZW_ManagerConfig extends LZW_ConfGramAbstract {
    enum LZW_ManagerConfFields {
        SRC_FILE,
        DIR_FILE,
        READER_CLASS,
        EXECUTOR_CLASS,
        WRITER_CLASS,
        EXECUTOR_CONFIG,
        READER_CONFIG,
        WRITER_CONFIG
    }

    protected LZW_ManagerConfig() {
        super(Arrays.stream(LZW_ManagerConfFields.values()).map(Enum::toString).toArray(String[]::new));
    }
}
