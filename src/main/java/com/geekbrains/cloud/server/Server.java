package com.geekbrains.cloud.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 8189;

    public static void main(String[] args) throws IOException {
        ServerSocket server  = new ServerSocket(PORT);
        System.out.println("Server started...");
        while (true){
            Socket socket = server.accept();
            System.out.println("Client accepted...");
            FileProcessorHandler handler = new FileProcessorHandler(socket);
            new Thread(handler).start();
        }
    }


}
