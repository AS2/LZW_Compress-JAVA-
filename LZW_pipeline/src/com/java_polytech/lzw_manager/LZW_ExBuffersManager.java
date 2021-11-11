package com.java_polytech.lzw_manager;

import com.java_polytech.pipeline_interfaces.RC;

import java.util.Arrays;

public class LZW_ExBuffersManager {
    public static class BytesReaderManager {
        final static private int charBitsCnt = 8;       // length of 'char' type in bits

        private static final RC RC_NULL_BYTES_ARRAY = new RC(RC.RCWho.EXECUTOR, RC.RCType.CODE_CUSTOM_ERROR, "LZW's bytes manager taken 'null' bytes array to manage");

        private byte[] buffer;          // buffer to manage
        private int currentBufferSize,  // current buffer size
                bufPos;                 // position of first unread symbol in buffer

        private byte bytePart;          // remained byte part
        private short bitePartLength;   // length of remained byte part

        private int resPart;
        private int bitsRemains;

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

       public READNBITS_RESULT_TYPE READNBITS_RESULT = READNBITS_RESULT_TYPE.COMPLETED;

        // Convert "unsigned" char to int method
        // Args: - Byte - Byte to parse
        // Return: int - parsed Byte
        private int ConvertToInt(byte Byte) {
            if (Byte < 0)
                return (int)(Byte) + 256;
            else
                return (Byte);
        }

        // Correct unsigned left shift method
        // Args: - num - number to shift left
        //       - shifts - shifts count
        // Return: byte - shifted number
        private byte ByteCorrectShiftLeft(byte num, int shifts) {
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

        BytesReaderManager() {
        }

        RC InitBytesManager(byte[] newBuffer) {
            if (newBuffer == null)
                return RC_NULL_BYTES_ARRAY;
            buffer = newBuffer;
            currentBufferSize = buffer.length;
            bufPos = 0;
            return RC.RC_SUCCESS;
        }

        public boolean isBufferEnded() {
            return bufPos == currentBufferSize;
        }

        public byte ReadByte() {
            if (bufPos != currentBufferSize)
                return buffer[bufPos++];
            else
                return -1;
        }

        public int ReadRemainedPart() {
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
        public int ReadNBits(int bitsCount) {
            resPart = (short) bytePart;
            bitsRemains = bitsCount - bitePartLength;

            return ReadRemainedPart();
        }
    }


    public static class BytesWriterManager {
        final static private int charBitsCnt = 8;       // length of 'char' type in bits
        final static private int intBitsCnt = 32;       // bits in 'int' type

        private byte[] buffer;          // buffer to manage
        private int currentBufferSize,  // current buffer size
                bufPos;                 // position of first unread symbol in buffer

        private byte bytePart;          // remained byte part
        private short bitePartLength;   // length of remained byte part

        BytesWriterManager() {
            currentBufferSize = 256;
            buffer = new byte[currentBufferSize];
            bufPos = 0;
            bitePartLength = charBitsCnt;
        }

        private void ResizeBuffer() {
            buffer = Arrays.copyOf(buffer,currentBufferSize * 2);
            currentBufferSize *= 2;
        }

        // Add number with some bits length to buffer method
        // Args: - num - number to add to buffer
        //       - bitLength - number length in bits
        public void AddToBuffer(int num, int bitLength) {
            // add byte part to buffer
            byte toBuffer = (byte)(bytePart | (num >>> (bitLength - bitePartLength)));
            buffer[bufPos++] = toBuffer;

            if (bufPos == currentBufferSize) {
                ResizeBuffer();
            }

            int bitRowLength = bitLength - bitePartLength;
            num = (num << (intBitsCnt - bitRowLength)) >>> (intBitsCnt - bitRowLength);

            while (bitRowLength >= charBitsCnt) {
                toBuffer = (byte)(num >>> (bitRowLength - charBitsCnt));
                buffer[bufPos++] = toBuffer;

                if (bufPos == currentBufferSize) {
                    ResizeBuffer();
                }

                bitRowLength -= charBitsCnt;
                num = (num << (intBitsCnt - bitRowLength)) >>> (intBitsCnt - bitRowLength);
            }
            bytePart = (byte)(num << (charBitsCnt - bitRowLength));
            bitePartLength = (short)(charBitsCnt - bitRowLength);
        }

        // Write string in buffer method
        // Args: - str - string to write in buffer
        public void WriteString(byte[] word, int wordLength) {
            for (int i = 0; i < wordLength; i++)
                AddToBuffer(word[i], charBitsCnt);
        }

        public byte[] SendBufferToConsumer() {
            // save remained byte part and send complited buffer part
            byte[] bufferToSend = Arrays.copyOf(buffer, bufPos);
            bufPos = 0;
            return bufferToSend;
        }

        public byte[] GetWritterBufferAtEnding() {
            if (bitePartLength < charBitsCnt) {
                if (bufPos == currentBufferSize)
                    ResizeBuffer();
                buffer[bufPos++] = bytePart;
            }

            return Arrays.copyOf(buffer, bufPos);
        }
    }
}
