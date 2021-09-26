import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BufWritter {
    final private int intBitsCnt = 32;
    final private int charBitsCnt = 8;

    private final FileOutputStream fileToWriteStream;

    private byte[] buffer;
    private byte bytePart;
    private short emptyBitLength;
    private int bufferLength, bufferPos;

    BufWritter(String fileToWritePath, int bufferLen) throws FileNotFoundException {
        File fileToWrite = new File(fileToWritePath);
        fileToWriteStream = new FileOutputStream(fileToWrite);

        if (bufferLen <= 0) {
            error.UpdateError(2, "LE: Incorrect buffer length");
            return;
        }

        buffer = new byte[bufferLen];
        bufferLength = bufferLen;
        emptyBitLength = charBitsCnt;
    }

    private void WriteBufToFile(int bufPartLength) throws IOException {
        if (bufPartLength > 0)
            fileToWriteStream.write(buffer, 0, bufPartLength);
        bufferPos = 0;
    }

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

    public void WriteString(String str) throws IOException {
        char[] strInChars = str.toCharArray();

        for (char ch : strInChars)
            AddToBuffer(ch, charBitsCnt);
    }

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
