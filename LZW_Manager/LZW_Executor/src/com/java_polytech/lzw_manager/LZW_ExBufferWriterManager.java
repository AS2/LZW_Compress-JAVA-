package com.java_polytech.lzw_manager;

import java.util.Arrays;

public class LZW_ExBufferWriterManager {
    final static private int charBitsCnt = 8;       // length of 'char' type in bits
    final static private int intBitsCnt = 32;       // bits in 'int' type

    private static byte[] buffer;          // buffer to manage
    private static int currentBufferSize,  // current buffer size
            bufPos;                 // position of first unread symbol in buffer

    private static byte bytePart;          // remained byte part
    private static short bitePartLength;   // length of remained byte part

    static {
        currentBufferSize = 256;
        buffer = new byte[currentBufferSize];
        bufPos = 0;
        bitePartLength = charBitsCnt;
    }

    private static void ResizeBuffer() {
        buffer = Arrays.copyOf(buffer,currentBufferSize * 2);
        currentBufferSize *= 2;
    }

    // Add number with some bits length to buffer method
    // Args: - num - number to add to buffer
    //       - bitLength - number length in bits
    public static void AddToBuffer(int num, int bitLength) {
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
    public static void WriteString(byte[] word, int wordLength) {
        for (int i = 0; i < wordLength; i++)
            AddToBuffer(word[i], charBitsCnt);
    }

    public static byte[] SendBufferToConsumer() {
        // save remained byte part and send complited buffer part
        byte[] bufferToSend = Arrays.copyOf(buffer, bufPos);
        bufPos = 0;
        return bufferToSend;
    }

    public static byte[] GetWritterBufferAtEnding() {
        if (bitePartLength < charBitsCnt) {
            if (bufPos == currentBufferSize)
                ResizeBuffer();
            buffer[bufPos++] = bytePart;
        }

        return Arrays.copyOf(buffer, bufPos);
    }

}
