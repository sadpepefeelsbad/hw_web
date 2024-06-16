package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {

        int threadPoolSize = 64;
        Server server = new Server(threadPoolSize);

        // добавление хендлера
        server.addHandler("GET", "/classic.html", (request, responseStream) -> {

            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);

            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(content);
            responseStream.flush();
        });

        server.start();
    }
}

