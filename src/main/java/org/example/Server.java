package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 9999;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final Executor executor;
    private final HashMap<String, Map<String, Handler>> handlers;

    public Server(int threadPoolSize) {
        executor = Executors.newFixedThreadPool(threadPoolSize);
        handlers = new HashMap<>();
    }

    public void start() {
        System.out.println("Server started");
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try {
                    executor.execute(() -> {
                        try {
                            proceedConnection(serverSocket.accept());
                        } catch (IOException | URISyntaxException e) {
                            System.out.println("Proceed exception: " + e);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void proceedConnection(Socket socket) throws IOException, URISyntaxException {

        final var out = new BufferedOutputStream(socket.getOutputStream());
        var request = Request.createRequest(new BufferedInputStream(socket.getInputStream()));
        System.out.println(request.getMethod() + "    " + request.getPath() + "     " + request.getHeaders());

        if (request == null) {
            response404(out);
        } else if (handlers.containsKey(request.getMethod()) && handlers.get(request.getMethod()).containsKey(request.getPath())) {
            handlers.get(request.getMethod()).get(request.getPath()).handle(request, out);
        } else {
            defaultHandler(request, out);
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) handlers.put(method, new HashMap<>());
        handlers.get(method).put(path, handler);
    }

    private void defaultHandler(Request request, BufferedOutputStream out) throws IOException {

        String path = request.getPath();

        if (!validPaths.contains(path)) response404(out);
        else {

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            final var length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();
        }
    }

    private void response404(BufferedOutputStream out) throws IOException {
        out.write((
                """
                        HTTP/1.1 404 Not Found\r
                        Content-Length: 0\r
                        Connection: close\r
                        \r
                        """
        ).getBytes());
        out.flush();
    }
}




