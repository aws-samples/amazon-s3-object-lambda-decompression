package example;


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class Zip implements AutoCloseable {
    private ZipInputStream stream;

    public Zip(InputStream stream) {
        this.stream = new ZipInputStream(stream);
    }

    public InputStream findEntry(String fileName) throws IOException {
        ZipEntry ze;
        while ((ze = stream.getNextEntry()) != null) {
            if (ze.getName().equals(fileName)) {
                return new ChecksumChecker(stream, ze.getCrc());
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
