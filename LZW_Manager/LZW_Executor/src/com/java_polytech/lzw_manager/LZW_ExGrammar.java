package com.java_polytech.lzw_manager;
import java.util.Arrays;

public class LZW_ExGrammar extends LZW_ConfGramAbstract {
    enum LZW_ExConfFields {
        EXECUTOR_MODE,
        MAX_BITS
    }

    protected LZW_ExGrammar() {
        super(Arrays.stream(LZW_ExConfFields.values()).map(Enum::toString).toArray(String[]::new));
    }
}
