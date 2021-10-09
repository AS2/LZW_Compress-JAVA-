import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BufWritter {
    final private int intBitsCnt = 32;   // bits in 'int' type
    final private int charBitsCnt = 8;   // bits in 'char' type

    private final FileOutputStream fileToWriteStream;   // file stream to write result

    private byte[] buffer;                  // write buffer massive
    private byte bytePart;                  // part of byte, which is not completed
    private short emptyBitLength;           // size of empty part of uncompleted byte 'bytePart'
    private int bufferLength,               // write buffer size
                bufferPos;                  // current position of first empty 'byte' in write buffer

    // Write buffer constructor
    // Args: - file name
    //       - buffer size
    BufWritter(String fileToWritePath, int bufferLen) throws FileNotFoundException {
        fileToWriteStream = new FileOutputStream(fileToWritePath);

        if (bufferLen <= 0) {
            error.UpdateError(2, "LE: Incorrect buffer length");
            return;
        }

        buffer = new byte[bufferLen];
        bufferLength = bufferLen;
        emptyBitLength = charBitsCnt;
    }

    // Write buffer with data to file method
    // Args: - length of buffer, which we need to write
    private void WriteBufToFile(int bufPartLength) throws IOException {
        if (bufPartLength > 0)
            fileToWriteStream.write(buffer, 0, bufPartLength);
        bufferPos = 0;
    }

    // Add number with some bits length to buffer method
    // Args: - num - number to add to buffer
    //       - bitLength - number length in bits
    public void AddToBuffer(int num, int bitLength) throws IOException {
        // add byte part to buffer
        byte toBuffer = (byte)(bytePart | (num >>> (bitLength - emptyBitLength)));
        buffer[bufferPos++] = toBuffer;
        if (bufferPos == bufferLength)
            WriteBufToFile(bufferLength);


        int bitRowLength = bitLength - emptyBitLength;
        num = (num << (intBitsCnt - bitRowLength)) >>> (intBitsCnt - bitRowLength);

        while (bitRowLength >= charBitsCnt) {
            toBuffer = (byte)(num >>> (bitRowLength - charBitsCnt));
            buffer[bufferPos++] = toBuffer;
            if (bufferPos == bufferLength)
                WriteBufToFile(bufferLength);

            bitRowLength -= charBitsCnt;
            num = (num << (intBitsCnt - bitRowLength)) >>> (intBitsCnt - bitRowLength);
        }
        bytePart = (byte)(num << (charBitsCnt - bitRowLength));
        emptyBitLength = (short)(charBitsCnt - bitRowLength);
    }

    // Write string in buffer method
    // Args: - str - string to write in buffer
    public void WriteString(String str) throws IOException {
        char[] strInChars = str.toCharArray();

        for (char ch : strInChars)
            AddToBuffer(ch, charBitsCnt);
    }

    // Close writter (write last buffer to file) method
    public void CloseWritter() throws IOException {
        if (emptyBitLength < charBitsCnt) {
            if (bufferPos == bufferLength) {
                fileToWriteStream.write(buffer, 0, bufferPos);
                bufferPos = 0;
            }
            buffer[bufferPos++] = bytePart;
        }

        if (bufferPos != 0)
            fileToWriteStream.write(buffer, 0, bufferPos);
        fileToWriteStream.close();
    }
}
