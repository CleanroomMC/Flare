package com.cleanroommc.flare.api.content;

import com.google.protobuf.AbstractMessageLite;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public interface BytebinClient {

    String SPARK_HEAP_MEDIA_TYPE = "application/x-spark-heap";
    String SPARK_SAMPLER_MEDIA_TYPE = "application/x-spark-sampler";

    String FLARE_HEAP_MEDIA_TYPE = SPARK_HEAP_MEDIA_TYPE;
    String FLARE_SAMPLER_MEDIA_TYPE = SPARK_SAMPLER_MEDIA_TYPE;

    int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    String postContent(String contentType, String userAgentAddition, Consumer<OutputStream> outputStreamConsumer) throws IOException;

    String bytebinUrl();

    String userAgent();

    default String postContent(String contentType, Consumer<OutputStream> outputStreamConsumer) throws IOException {
        return postContent(contentType, null, outputStreamConsumer);
    }

    default String postContent(String contentType, String userAgentAddition, AbstractMessageLite<?, ?> proto) throws IOException {
        return postContent(contentType, userAgentAddition, outputStream -> {
            try (OutputStream out = new GZIPOutputStream(outputStream)) {
                proto.writeTo(out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    default String postContent(String contentType, AbstractMessageLite<?, ?> proto) throws IOException {
        return postContent(contentType, null, proto);
    }

}
