package com.cleanroommc.flare.common.content;

import com.cleanroommc.flare.api.content.BytebinClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class FlareBytebinClient implements BytebinClient {

    @Override
    public String postContent(String contentType, String userAgentAddition, Consumer<OutputStream> outputStreamConsumer) throws IOException {
        URL url = new URL(this.bytebinUrl() + "post");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String userAgent = userAgentAddition == null ? this.userAgent() : this.userAgent() + "/" + userAgentAddition;
        try {
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_TIMEOUT);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Content-Encoding", "gzip");
            connection.connect();
            try (OutputStream output = connection.getOutputStream()) {
                outputStreamConsumer.accept(output);
            }
            String key = connection.getHeaderField("Location");
            if (key == null) {
                throw new IllegalStateException("Key not returned");
            }
            return key;
        } finally {
            connection.getInputStream().close();
            connection.disconnect();
        }
    }

    @Override
    public String bytebinUrl() {
        return "https://bytebin.lucko.me/";
    }

    @Override
    public String userAgent() {
        return "spark-plugin";
    }

}
