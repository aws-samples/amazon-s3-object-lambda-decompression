package example;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static example.ChecksumChecker.NO_EXPECTED_CRC;

class RequestHandler {

    public static final int CLIENT_ERROR_CODE = 400;
    public static final int LAMBDA_ERROR_CODE = 500;
    public static final int SUCCESS_ERROR_CODE = 200;


    private final Event parsedEvent;
    private final GetResponseWriter getResponseWriter;

    public RequestHandler(Event parsedEvent, GetResponseWriter getResponseWriter) {

        this.parsedEvent = parsedEvent;
        this.getResponseWriter = getResponseWriter;
    }

    public void handle() {
        try {
            var fileName = parsedEvent.getFileName();

            if (fileName.isEmpty()) {
                getResponseWriter.writeError(CLIENT_ERROR_CODE, "Error - missing 'file_name' query parameter");
                return;
            }

            var inCon = parsedEvent.getInputUrl().get();

            int inResponseCode = inCon.getResponseCode();
            if (inResponseCode != SUCCESS_ERROR_CODE) {
                InputStream errorStream = inCon.getErrorStream();

                var errorMessage = (errorStream != null)
                        ? IOUtils.toString(errorStream, StandardCharsets.UTF_8)
                        : "";
                getResponseWriter.writeError(inResponseCode, errorMessage);
                return;
            }

            var is = inCon.getInputStream();
            writeResponse(getResponseWriter, fileName.get(), inCon.getHeaderFields(), is);
        } catch (Exception e) {
            e.printStackTrace();
            getResponseWriter.writeError(LAMBDA_ERROR_CODE, "An internal error happened while processing");
        }
    }


    private void writeResponse(GetResponseWriter getResponseWriter, String fileName, Map<String, List<String>> headers, InputStream data) throws IOException {
        try (Zip zis = new Zip(data)) {
            var entry = zis.findEntry(fileName);

            if (entry == null) {
                //If the file is not a zip file, this gets executed too
                getResponseWriter.writeError(CLIENT_ERROR_CODE, "File name not found in archive");
                return;
            }

            getResponseWriter.writeGetResponse(entry, headers);
        }
    }
}
