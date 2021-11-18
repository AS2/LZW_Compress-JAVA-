package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.RC;

public class LZW_ExBufferReaderManager {
    final static private int charBitsCnt = 8;       // length of 'char' type in bits

    private static final RC RC_NULL_BYTES_ARRAY = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "LZW's bytes manager taken 'null' bytes array to manage");

    private static byte[] buffer;           // buffer to manage
    private static int currentBufferSize,   // current buffer size
            bufPos;                         // position of first unread symbol in buffer

    private static byte bytePart;                  // remained byte part
    private static short bitePartLength;           // length of remained byte part

    private static int resPart;
    private static int bitsRemains;

    public enum READNBITS_RESULT_TYPE {
        COMPLETED(0),
        NOT_COMPLETED(1);

        private final int type;
        READNBITS_RESULT_TYPE(int t) {
            type = t;
        }

        public int toInt() {
            return type;
        }
    }

    public static READNBITS_RESULT_TYPE READNBITS_RESULT = READNBITS_RESULT_TYPE.COMPLETED;

    // Convert "unsigned" char to int method
    // Args: - Byte - Byte to parse
    // Return: int - parsed Byte
    private static int ConvertToInt(byte Byte) {
        if (Byte < 0)
            return (int)(Byte) + 256;
        else
            return (Byte);
    }

    // Correct unsigned left shift method
    // Args: - num - number to shift left
    //       - shifts - shifts count
    // Return: byte - shifted number
    private static byte ByteCorrectShiftLeft(byte num, int shifts) {
        if (shifts >= 1) {
            if ((num & (byte)(-128)) == (byte)(-128)) {
                num += 128;
                num = (byte)(num >> 1);
                num += 64;
                num = (byte)(num >> (shifts - 1));
            }
            else
                num = (byte)(num >> shifts);

            return num;
        }
        return num;
    }

    public static RC InitBytesManager(byte[] newBuffer) {
        if (newBuffer == null)
            return RC_NULL_BYTES_ARRAY;
        buffer = newBuffer;
        currentBufferSize = buffer.length;
        bufPos = 0;
        return RC.RC_SUCCESS;
    }

    public static boolean isBufferEnded() {
        return bufPos == currentBufferSize;
    }

    public static byte ReadByte() {
        if (bufPos != currentBufferSize)
            return buffer[bufPos++];
        else
            return -1;
    }

    public static int ReadRemainedPart() {
        while (bitsRemains >= charBitsCnt) {
            resPart = (resPart << charBitsCnt) | ConvertToInt(buffer[bufPos++]);
            bitsRemains -= charBitsCnt;

            // TODO: IF WE END READING -> DONT GIVE RESULT: SAY THAT WE NEED MORE DATA. IF LAST DATA PACKAGE -> GIVE RESULT.
            // we read all buffer
            if (isBufferEnded() && bitsRemains == 0) {
                bytePart = 0;
                bitePartLength = 0;
                READNBITS_RESULT = READNBITS_RESULT_TYPE.COMPLETED;
                return resPart;
            }
            else if (isBufferEnded() && bitsRemains != 0) {
                READNBITS_RESULT = READNBITS_RESULT_TYPE.NOT_COMPLETED;
                return 0;
            }
        }

        // TODO: IF WE END READING -> DONT GIVE RESULT: SAY THAT WE NEED MORE DATA. IF LAST DATA PACKAGE -> GIVE RESULT.
        if (bitsRemains != 0 && isBufferEnded()) {
            READNBITS_RESULT = READNBITS_RESULT_TYPE.NOT_COMPLETED;
            return 0;
        }
        else if (bitsRemains != 0) {
            bytePart = buffer[bufPos++];

            resPart = (resPart << bitsRemains) | (ByteCorrectShiftLeft(bytePart, charBitsCnt - bitsRemains));
            bytePart = ByteCorrectShiftLeft((byte)(bytePart << bitsRemains), bitsRemains);
            bitePartLength = (short)(charBitsCnt - bitsRemains);
        }
        else {
            bytePart = 0;
            bitePartLength = 0;
        }

        READNBITS_RESULT = READNBITS_RESULT_TYPE.COMPLETED;
        return resPart;
    }

    // Read N bites from buffer method
    // Args: - bitsCount - bits count to read
    public static int ReadNBits(int bitsCount) {
        resPart = (short) bytePart;
        bitsRemains = bitsCount - bitePartLength;

        return ReadRemainedPart();
    }
}