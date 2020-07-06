package example;

import com.amazonaws.internal.CRC32MismatchException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ChecksumCheckerTest {

    public static final int NO_EXPECTED_CRC = -1;

    @Test
    public void test0() throws IOException {
        test(0, NO_EXPECTED_CRC);
    }

    @Test
    public void test1() throws IOException {
        test(1, NO_EXPECTED_CRC);
    }

    @Test(expected = CRC32MismatchException.class)
    public void testInvalidCrc() throws IOException {
        test(1, 0);
    }


    @Test(expected = CRC32MismatchException.class)
    public void testDetectInMemoryMutation() throws IOException {
        ChecksumChecker sut = new ChecksumChecker(getTestStream(32), NO_EXPECTED_CRC);
        byte[] buff = new byte[8];
        sut.read(buff);
        //simulate unexpected mutation in memory
        buff[3]++;

        sut.read(buff);
    }

    @Test
    public void testClose() throws IOException {
        InputStream stream = mock(InputStream.class);
        ChecksumChecker sut = new ChecksumChecker(stream, NO_EXPECTED_CRC);

        sut.close();

        verify(stream, times(1)).close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void read0() {
        ChecksumChecker sut = new ChecksumChecker(getTestStream(0), NO_EXPECTED_CRC);
        sut.read();
    }


    @Test
    public void testCrc() throws IOException {
        test(1, 3523407757L);
    }

    @Test
    public void test0Crc() throws IOException {
        test(0, 0);
    }


    @Test
    public void test20MCrc() throws IOException {
        test(20 * 1024 * 1024, 3492374301L);
    }


    @Test
    public void test64MCrc() throws IOException {
        test(64 * 1024 * 1024 + 1, 2827989774L);
    }

    void test(int num, long crc) throws IOException {
        var sut = new ChecksumChecker(getTestStream(num), crc);
        Assert.assertArrayEquals(genArray(num), sut.readAllBytes());
    }

    static byte[] genArray(int size) {
        var buff = new byte[size];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = (byte) (i * 2);
        }

        return buff;
    }


    static InputStream getTestStream(int size) {

        return new ByteArrayInputStream(genArray(size));
    }
}