package com.cleanroommc.flare.api.content;

import com.cleanroommc.flare.api.util.ThrowingConsumer;
import com.google.protobuf.AbstractMessageLite;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

public interface BytebinClient {

    String SPARK_HEAP_MEDIA_TYPE = "application/x-spark-heap";
    String SPARK_SAMPLER_MEDIA_TYPE = "application/x-spark-sampler";

    String FLARE_HEAP_MEDIA_TYPE = SPARK_HEAP_MEDIA_TYPE;
    String FLARE_SAMPLER_MEDIA_TYPE = SPARK_SAMPLER_MEDIA_TYPE;

    int DEFAULT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(10);

    String postContent(String contentType, String userAgentAddition, ThrowingConsumer<OutputStream> outputStreamConsumer) throws Throwable;

    String bytebinUrl();

    String userAgent();

    default String postContent(String contentType, ThrowingConsumer<OutputStream> outputStreamConsumer) throws Throwable {
        return postContent(contentType, null, outputStreamConsumer);
    }

    default String postContent(String contentType, String userAgentAddition, AbstractMessageLite<?, ?> proto) throws Throwable {
        return postContent(contentType, userAgentAddition, outputStream -> {
            try (OutputStream out = new GZIPOutputStream(outputStream)) {
                proto.writeTo(out);
            }
        });
    }

    default String postContent(String contentType, AbstractMessageLite<?, ?> proto) throws Throwable {
        return postContent(contentType, null, proto);
    }

}
