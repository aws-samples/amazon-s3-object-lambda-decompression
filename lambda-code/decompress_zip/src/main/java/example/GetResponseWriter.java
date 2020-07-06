package example;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.ServiceUtils;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.WriteGetObjectResponseRequest;
import com.amazonaws.util.DateUtils;
import com.amazonaws.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

class GetResponseWriter {
    public static final String X_AMZ_META = "x-amz-meta-";
    private static final Map<String, BiConsumer<String, WriteGetObjectResponseRequest>> HeadersMap = new HashMap<>() {{
        put("x-amz-fwd-error-message", (value, request) -> request.setErrorMessage(value));
        put("Content-Language", (value, request) -> request.setContentLanguage(value));
        put("Expires", (value, request) -> request.setExpires(DateUtils.parseRFC822Date(value)));
        put("x-amz-expiration", (value, request) -> request.setExpiration(value));
        put("Last-Modified", (value, request) -> request.setLastModified(ServiceUtils.parseRfc822Date(value)));
        put("x-amz-missing-meta", (value, request) -> request.setMissingMeta(Integer.parseInt(value)));
        put("x-amz-object-lock-mode", (value, request) -> request.setObjectLockMode(value));
        put("x-amz-object-lock-legal-hold", (value, request) -> request.setObjectLockLegalHoldStatus(value));
        put("x-amz-object-lock-retain-until-date", (value, request) -> request.setObjectLockRetainUntilDate(DateUtils.parseISO8601Date(value)));
        put("x-amz-mp-parts-count", (value, request) -> request.setPartsCount(Integer.parseInt(value)));
        put("x-fwd-header-x-amz-request-charged", (value, request) -> request.setRequestCharged(value));
        put("x-amz-restore", (value, request) -> request.setRestore(value));
        put("x-amz-server-side-encryption", (value, request) -> request.setServerSideEncryption(value));
        put("x-amz-server-side-encryption-customer-algorithm", (value, request) -> request.setSSECustomerAlgorithm(value));
        put("x-amz-server-side-encryption-aws-kms-key-id", (value, request) -> request.setSSEKMSKeyId(value));
        put("x-amz-server-side-encryption-customer-key-MD5", (value, request) -> request.setSSECustomerKeyMD5(value));
        put("x-amz-storage-class", (value, request) -> request.setStorageClass(value));
        put("x-amz-tagging-count", (value, request) -> request.setTagCount(Integer.parseInt(value)));
        put("x-amz-version-id", (value, request) -> request.setVersionId(value));
    }};
    private final Event event;
    private final AmazonS3 s3Client;

    public GetResponseWriter(Event event, AmazonS3 s3Client) {
        this.event = event;
        this.s3Client = s3Client;
    }

    public void writeError(int statusCode, String message) {
        var request =
                new WriteGetObjectResponseRequest()
                        .withRequestToken(event.getRequestToken())
                        .withRequestRoute(event.getRequestRoute())
                        .withStatusCode(statusCode);

        if (!StringUtils.isNullOrEmpty(message)) {
            System.out.println(message);
            request.withContentLength(0L).withInputStream(new ByteArrayInputStream(new byte[0])).setErrorMessage(message);
        }

        s3Client.writeGetObjectResponse(request);
    }

    public void writeGetResponse(InputStream body, Map<String, List<String>> headers) {
        var request = new WriteGetObjectResponseRequest();
        copyHeadersToRequest(headers, request);
        request
                .withRequestToken(event.getRequestToken())
                .withRequestRoute(event.getRequestRoute())
                .withInputStream(body);
        s3Client.writeGetObjectResponse(request);
    }

    void copyHeadersToRequest(Map<String, List<String>> headers, WriteGetObjectResponseRequest request) {

        headers.forEach((header, values) -> {
            if (StringUtils.isNullOrEmpty(header) || values == null) {
                //do nothing
            } else if (values.size() == 1) {
                BiConsumer<String, WriteGetObjectResponseRequest> applier;
                var value = values.get(0);
                if (header.startsWith(X_AMZ_META)) {
                    ObjectMetadata metadata = request.getMetadata();
                    if (metadata == null) {
                        metadata = new ObjectMetadata();
                        request.setMetadata(metadata);
                    }
                    metadata.addUserMetadata(header.substring(X_AMZ_META.length()), value);
                } else if ((applier = HeadersMap.get(header)) != null) {
                    applier.accept(value, request);
                }
            } else if (values.size() > 1) {
                throw new RuntimeException("Too many occurrences of the header " + header);
            }
        });
    }

}
