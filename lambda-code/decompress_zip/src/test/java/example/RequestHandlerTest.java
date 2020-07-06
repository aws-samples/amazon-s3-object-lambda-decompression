package example;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.ServiceUtils;
import com.amazonaws.services.s3.model.WriteGetObjectResponseRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestHandlerTest {

    @Mock
    private AmazonS3 s3Client;

    private static byte[] createDummyZipFile(String archiveFileName, byte[] content) throws IOException {

        ByteArrayOutputStream fos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry zipEntry = new ZipEntry(archiveFileName);
            zipEntry.setSize(content.length);
            CRC32 crc32 = new CRC32();
            crc32.update(content);
            zipEntry.setCrc(crc32.getValue());
            zipEntry.setMethod(ZipEntry.STORED);
            zos.putNextEntry(zipEntry);
            zos.write(content);
            zos.closeEntry();
        }
        return fos.toByteArray();
    }

    private static <T> T argThat(ArgThat<T> matcher) {
        return ArgumentMatchers.argThat(matcher);
    }

    private RequestHandler getRequestHandler(String dummyFile, int responseCode, byte[] content) {
        return getRequestHandler(dummyFile, responseCode, content, new HashMap<>());
    }

    private RequestHandler getRequestHandler(String dummyFile, int responseCode, byte[] content, Map<String, String> headers) {

        var urlConnection = mock(HttpURLConnection.class);
        var inputStream = content != null ? new ByteArrayInputStream(content) : null;
        try {
            when(urlConnection.getInputStream()).thenReturn(inputStream);
            when(urlConnection.getErrorStream()).thenReturn(inputStream);
            when(urlConnection.getResponseCode()).thenReturn(responseCode);
            if (headers.size() > 0) {
                Map<String, List<String>> headerFields = new HashMap<>();
                headers.forEach((key, value) -> headerFields.put(key, List.of(value)));
                when(urlConnection.getHeaderFields()).thenReturn(headerFields);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getRequestHandler(dummyFile, () -> urlConnection);
    }

    private RequestHandler getRequestHandler(String dummyFile, ThrowableSupplier<HttpURLConnection, IOException> httpConnection) {
        var fileName = dummyFile != null ? "?file_name=" + dummyFile : "";

        Map<Object, Object> map = new HashMap<>() {{
            put("getObjectContext", new HashMap<>() {{
                put("inputS3Url", "https://example.com");
                put("outputRoute", "route");
                put("outputToken", "token");
            }});
            put("userRequest", new HashMap<>() {{
                put("url", "/foo.zip" + fileName);
            }});
        }};

        var event = new MockEvent(httpConnection, map);

        var responseWriter = new GetResponseWriter(event, s3Client);

        return new RequestHandler(event, responseWriter);
    }

    @Test
    public void testHandlerWithSampleZip() throws Exception {
        var actualAllBytes = new AtomicReference<byte[]>();

        doAnswer(args -> {
            var request = (WriteGetObjectResponseRequest) args.getArgument(0);
            actualAllBytes.set(request.getInputStream().readAllBytes());
            return null;
        }).when(s3Client).writeGetObjectResponse(any());

        var data = "Hello World this is some dummy text".getBytes(StandardCharsets.UTF_8);
        var zipData = createDummyZipFile("dummyFile", data);
        var handler = getRequestHandler("dummyFile", 200, zipData);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
        {
            Assert.assertEquals("route", x.getRequestRoute());
            Assert.assertEquals("token", x.getRequestToken());
            Assert.assertArrayEquals(data, actualAllBytes.get());
        }));
    }

    @Test
    public void testHandlerWithMetadata() throws Exception {

        var data = "data".getBytes(StandardCharsets.UTF_8);
        var zipData = createDummyZipFile("dummyFile", data);
        var headers = new HashMap<String, String>() {{
            put("x-amz-meta-test", "value");
            put("x-amz-meta-test-2", "value-2");
        }};
        var handler = getRequestHandler("dummyFile", 200, zipData, headers);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
        {
            assertEquals("value", x.getMetadata().getUserMetadata().get("test"));
            assertEquals("value-2", x.getMetadata().getUserMetadata().get("test-2"));
        }));
    }

    @Test
    public void testHandlerWithHeaders() throws Exception {

        var data = "data".getBytes(StandardCharsets.UTF_8);
        var zipData = createDummyZipFile("dummyFile", data);
        var headers = new HashMap<String, String>() {{
            put("x-amz-fwd-error-message", "value1");
            put("Content-Language", "value2");
            put("Expires", ServiceUtils.formatRfc822Date(new Date(3000)));
            put("x-amz-expiration", "value4");
            put("Last-Modified", ServiceUtils.formatRfc822Date(new Date(5000)));
            put("x-amz-missing-meta", "6");
            put("x-amz-object-lock-mode", "value7");
            put("x-amz-object-lock-legal-hold", "value8");
            put("x-amz-object-lock-retain-until-date", ServiceUtils.formatIso8601Date(new Date(9000)));
            put("x-amz-mp-parts-count", "10");
            put("x-fwd-header-x-amz-request-charged", "value11");
            put("x-amz-restore", "value12");
            put("x-amz-server-side-encryption", "value13");
            put("x-amz-server-side-encryption-customer-algorithm", "value14");
            put("x-amz-server-side-encryption-aws-kms-key-id", "value15");
            put("x-amz-server-side-encryption-customer-key-MD5", "value16");
            put("x-amz-storage-class", "value17");
            put("x-amz-tagging-count", "18");
            put("x-amz-version-id", "value19");
        }};
        var handler = getRequestHandler("dummyFile", 200, zipData, headers);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
        {

            assertEquals("value1", x.getErrorMessage());
            assertEquals("value2", x.getContentLanguage());
            assertEquals(new Date(3000), x.getExpires());
            assertEquals("value4", x.getExpiration());
            assertEquals(new Date(5000), x.getLastModified());
            assertEquals(6, (int) x.getMissingMeta());
            assertEquals("value7", x.getObjectLockMode());
            assertEquals("value8", x.getObjectLockLegalHoldStatus());
            assertEquals(new Date(9000), x.getObjectLockRetainUntilDate());
            assertEquals(10, (int) x.getPartsCount());
            assertEquals("value11", x.getRequestCharged());
            assertEquals("value12", x.getRestore());
            assertEquals("value13", x.getServerSideEncryption());
            assertEquals("value14", x.getSSECustomerAlgorithm());
            assertEquals("value15", x.getSSEKMSKeyId());
            assertEquals("value16", x.getSSECustomerKeyMD5());
            assertEquals("value17", x.getStorageClass());
            assertEquals(18, (long) x.getTagCount());
            assertEquals("value19", x.getVersionId());
        }));
    }

    @Test
    public void testHandlerWithNoFileInZip() throws Exception {
        var data = "Hello World this is some dummy text".getBytes(StandardCharsets.UTF_8);
        var zipData = createDummyZipFile("someFile", data);
        var handler = getRequestHandler("dummyFile", 200, zipData);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
                assertEquals(400, (int) x.getStatusCode())));
    }

    @Test
    public void testHandlerWithNoFile() {
        var handler = getRequestHandler("dummyFile", 404, new byte[0]);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
                assertEquals(404, (int) x.getStatusCode())));
    }

    @Test
    public void testHandlerWithNoAccess() {
        var errorMessage = "No access";
        var handler = getRequestHandler("dummyFile", 403, errorMessage.getBytes(StandardCharsets.UTF_8));

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x -> {
            assertEquals(403, (int) x.getStatusCode());
            Assert.assertEquals("route", x.getRequestRoute());
            Assert.assertEquals("token", x.getRequestToken());
            assertEquals(errorMessage, x.getErrorMessage());
        }));
    }

    @Test
    public void testHandlerThrottledRequest() {
        var handler = getRequestHandler("dummyFile", 503, null);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x -> {
            assertEquals(503, (int) x.getStatusCode());
            assertNull(x.getErrorMessage());
        }));
    }

    @Test
    public void testInvalidFile() {
        byte[] someInvalidZipFile = "\0\1\2".getBytes(StandardCharsets.UTF_8);
        var handler = getRequestHandler("dummyFile", 200, someInvalidZipFile);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x ->
                assertEquals(400, (int) x.getStatusCode())));
    }

    @Test
    public void testNoFileName() throws Exception {
        var data = "Hello World this is some dummy text".getBytes(StandardCharsets.UTF_8);
        var zipData = createDummyZipFile("someFile", data);
        var handler = getRequestHandler(null, 200, zipData);

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x -> {
            assertEquals(400, (int) x.getStatusCode());
            assertEquals("Error - missing 'file_name' query parameter", x.getErrorMessage());
        }));
    }

    @Test
    public void testFailedInputConnection() {
        var handler = getRequestHandler("fileName", () -> {
            throw new RuntimeException("An exception");
        });

        handler.handle();

        verify(s3Client, times(1)).writeGetObjectResponse(argThat(x -> {
            assertEquals(500, (int) x.getStatusCode());
            assertEquals("An internal error happened while processing", x.getErrorMessage());
        }));
    }

    @Test(expected = RuntimeException.class)
    public void testMissingEventData() {
        var event = new MockEvent(() -> mock(HttpURLConnection.class), new HashMap<>());
        GetResponseWriter getResponseWriter = new GetResponseWriter(event, s3Client);

        new RequestHandler(event, getResponseWriter).handle();
    }

    interface ArgThat<T> extends ArgumentMatcher<T> {
        void apply(T x) throws Exception;

        @Override
        default boolean matches(T t) {
            try {
                apply(t);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class MockEvent extends Event {
        private final ThrowableSupplier<HttpURLConnection, IOException> mock;

        MockEvent(ThrowableSupplier<HttpURLConnection, IOException> mock, Map<Object, Object> map) {
            super(map);
            this.mock = mock;
        }

        @Override
        public ThrowableSupplier<HttpURLConnection, IOException> getInputUrl() throws MalformedURLException {
            super.getInputUrl();
            return mock;
        }
    }
}
