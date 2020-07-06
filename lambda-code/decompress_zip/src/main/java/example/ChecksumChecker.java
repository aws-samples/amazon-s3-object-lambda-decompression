package example;

import com.amazonaws.internal.CRC32MismatchException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;


class ChecksumChecker extends InputStream {

    public static final int NO_EXPECTED_CRC = -1;
    private final InputStream stream;
    private final long expectedCrc32;
    private final CRC32 crc = new CRC32();
    private final CRC32 newCrc = new CRC32();
    private ByteBuffer buffer;

    public ChecksumChecker(InputStream stream, long expectedCrc32) {
        this.stream = stream;
        this.expectedCrc32 = expectedCrc32;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {

        if (buffer != null) {
            newCrc.update(buffer);
        }
        if (newCrc.getValue() != crc.getValue()) {
            throw new CRC32MismatchException("CRC does not match expected value.");
        }

        var size = stream.read(b, off, len);

        if (size == -1) {
            if (expectedCrc32 != NO_EXPECTED_CRC && expectedCrc32 != crc.getValue()) {
                throw new CRC32MismatchException("CRC does not match expected value. " + crc.getValue());
            }
        } else {
            buffer = ByteBuffer.wrap(b, off, size);
            crc.update(buffer);
            buffer.position(off);
        }

        return size;
    }

    @Override
    public int read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
