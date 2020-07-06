package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3Client;

import java.util.Map;


public class Handler {
    public void handleRequest(Map<Object, Object> event, Context context) {

        var parsedEvent = new Event(event);
        var s3 = AmazonS3Client.builder().build();
        var getResponseWriter = new GetResponseWriter(parsedEvent, s3);
        var handler = new RequestHandler(parsedEvent, getResponseWriter);
        handler.handle();
    }
}

