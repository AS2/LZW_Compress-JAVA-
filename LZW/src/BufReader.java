import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BufReader {
    final private int charBitsCnt = 8;      // length of 'char' type in bits

    private final FileInputStream fileToReadStream;     // stream of file to read

    private byte[] buffer;          // read buffer
    private int maxBufferSize,      // max buffer length
                currentBufferSize,  // current buffer size
                bufPos;             // position of first unread symbol in buffer
    private byte bytePart;          // remained byte part
    private short bitePartLength;   // length of remained byte part

    private boolean isAllReaden;    // flag - if file readen - 1, if not - 0

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

    // Buffer reader constructor
    // Args: - fileToReadPath - file path to read
    //       - maxBufSize - max read buffer size
    BufReader(String fileToReadPath, int maxBufSize) throws FileNotFoundException {
        fileToReadStream = new FileInputStream(fileToReadPath);

        if (maxBufSize <= 0) {
            error.UpdateError(2, "LE: Incorrect buffer length");
            return;
        }

        buffer = new byte[maxBufSize];
        maxBufferSize = maxBufSize;
    }

    // Update buffer method
    // Args: - fileToReadPath - file path to read
    //       - maxBufSize - max read buffer size
    public void RereadBuffer() throws IOException {
        if (bufPos == currentBufferSize && !isAllReaden) {
            currentBufferSize = fileToReadStream.read(buffer, 0, maxBufferSize);
            isAllReaden = currentBufferSize < maxBufferSize ||  currentBufferSize == -1;
            bufPos = 0;
        }
    }

    // Read N bites from file method
    // Args: - bitsCount - bits count to read
    public int ReadNBits(int bitsCount) throws IOException {
        int res = (short)bytePart;
        int bitsRemains = bitsCount - bitePartLength;

        while (bitsRemains >= charBitsCnt) {
            res = (res << charBitsCnt) | ConvertToInt(buffer[bufPos++]);
            bitsRemains -= charBitsCnt;

            RereadBuffer();
            // we read all file
            if (isFileRead()) {
                res = res << bitsRemains;
                return res;
            }
        }

        if (bitsRemains != 0 && isFileRead()) {
            res = res << bitsRemains;
            return res;
        }
        else if (bitsRemains != 0) {
            bytePart = buffer[bufPos++];

            res = (res << bitsRemains) | (ByteCorrectShiftLeft(bytePart, charBitsCnt - bitsRemains));
            bytePart = ByteCorrectShiftLeft((byte)(bytePart << bitsRemains), bitsRemains);
            bitePartLength = (short)(charBitsCnt - bitsRemains);

            RereadBuffer();
        }
        else {
            bytePart = 0;
            bitePartLength = 0;
        }

        return res;
    }

    // Checks is file read method
    // Returns: - boolean: True - file is already read, False - file hasn't been read yet
    public boolean isFileRead() {
        return isAllReaden && (bufPos == currentBufferSize || currentBufferSize == -1);
    }

    // Close read buffer method
    public void CloseReader() throws IOException {
        fileToReadStream.close();
    }
}