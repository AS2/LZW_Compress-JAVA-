import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BufReader {
    final private int charBitsCnt = 8;

    private File fileToRead;
    private FileInputStream fileToReadStream;

    private byte[] buffer;
    private int maxBufferSize, currentBufferSize, bufPos;
    private short bitePartLength;
    private byte bytePart;

    private boolean isAllReaden;

    private int ConvertToInt(byte Byte) {
        if (Byte < 0)
            return (int)(Byte) + 256;
        else
            return (Byte);
    }

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

    BufReader(String fileToReadPath, int maxBufSize) throws FileNotFoundException {
        fileToRead = new File(fileToReadPath);
        fileToReadStream = new FileInputStream(fileToRead);

        if (maxBufSize <= 0) {
            error.UpdateError(2, "LE: Incorrect buffer length");
            return;
        }

        buffer = new byte[maxBufSize];
        maxBufferSize = maxBufSize;
    }

    public void RereadBuffer() throws IOException {
        if (bufPos == currentBufferSize && !isAllReaden) {
            currentBufferSize = fileToReadStream.read(buffer, 0, maxBufferSize);
            isAllReaden = currentBufferSize < maxBufferSize ||  currentBufferSize == -1;
            bufPos = 0;
        }
    }

    public int ReadNBits(int bitsCount) throws IOException {
        int res = (short)bytePart;
        int bitsRemains = bitsCount - bitePartLength;

        while (bitsRemains >= charBitsCnt) {
            res = (res << charBitsCnt) | ConvertToInt(buffer[bufPos++]);
            bitsRemains -= charBitsCnt;

            RereadBuffer();
            // we read all file
            if (isFileReaden()) {
                res = res << bitsRemains;
                return res;
            }
        }

        if (bitsRemains != 0 && isFileReaden()) {
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

    public boolean isFileReaden() {
        return isAllReaden && (bufPos == currentBufferSize || currentBufferSize == -1);
    }

    public void CloseReader() throws IOException {
        fileToReadStream.close();
    }
}
