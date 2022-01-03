package com.geekbrains.cloud.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

public class FileProcessorHandler implements Runnable{

    private File currentDir;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;
    private static final int SIZE = 256;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        currentDir = new File("serverDir");
    }

    @Override
    public void run() {
        try {
            while (true){
                String command = is.readUTF();
                System.out.println("Received: " + command);
                if (command.equals("#SEND#FILE#")){
                    String fileName = is.readUTF();
                    long size = is.readLong();
                    System.out.println("Created file: " + fileName);
                    System.out.println("File size: " + size);

                    Path currentPath = currentDir.toPath().resolve(fileName);
                    try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())){
                        for (int i = 0; i < (size + SIZE - 1) / SIZE; i++){
                            int read = is.read(buf);
                            fos.write(buf, 0, read);
                        }
                    }
                    os.writeUTF("File successfully uploaded");
                    os.flush();

                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
