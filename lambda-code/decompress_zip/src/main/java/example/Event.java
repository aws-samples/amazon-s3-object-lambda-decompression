package example;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Class parses Events provided to lambda.
 */
class Event {

    private final Map<Object, Object> map;

    public Event(Map<Object, Object> map) {
        this.map = map;
    }

    /**
     * Returns request token required to callback S3 Object Lambda.
     */
    public String getRequestToken() {
        return getString("getObjectContext", "outputToken");
    }

    /**
     * Returns request route required to callback S3 Object Lambda.
     */
    public String getRequestRoute() {
        return getString("getObjectContext", "outputRoute");
    }

    /**
     * Returns {@link HttpURLConnection} to S3 Object.
     */
    public ThrowableSupplier<HttpURLConnection, IOException> getInputUrl() throws MalformedURLException {
        URL inputUrl = getUrl("getObjectContext", "inputS3Url");
        return () -> (HttpURLConnection) inputUrl.openConnection();
    }

    /**
     * Returns requested file name if specified.
     */
    public Optional<String> getFileName() throws URISyntaxException {
        var query = getString("userRequest", "url");
        return URLEncodedUtils.parse(new URI(query), StandardCharsets.UTF_8)
                .stream()
                .filter(p -> p.getName().equals("file_name"))
                .findFirst()
                .map(NameValuePair::getValue);

    }

    private URL getUrl(String... path) throws MalformedURLException {
        return new URL(getString(path));
    }

    private String getString(String... path) {
        var map = this.map;

        for (int i = 0; i < path.length; i++) {
            var value = map.get(path[i]);
            if (value == null) {
                break;
            }

            if (i + 1 < path.length) {
                map = (Map<Object, Object>) value;
            } else {
                return (String) value;
            }
        }

        throw new RuntimeException("Path not found: " + String.join("/", path));
    }


}
