package org.example;

public class Main {
    public static void main(String[] args) {

        int threadPoolSize = 64;
        Server server = new Server(threadPoolSize);
        server.start();

    }
}

