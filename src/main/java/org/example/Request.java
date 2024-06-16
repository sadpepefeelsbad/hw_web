package org.example;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Request {

    private static final String GET = "GET";
    private static final String POST = "POST";

    private final String method;
    private final String path;
    private final List<String> headers;

    private Request(String method, String path, List<String> headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
    }

    public static Request createRequest(BufferedInputStream in) throws IOException {

        final List<String> allowedMethods = List.of(GET, POST);
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }

        final var path = requestLine[1];

        // ищем заголовки
        final var headerDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headerDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return new Request(method, path, null);
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        return new Request(method, path, headers);
    }

    // from Google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

}
