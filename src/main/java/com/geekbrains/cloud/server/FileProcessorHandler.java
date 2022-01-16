package com.geekbrains.cloud.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class FileProcessorHandler implements Runnable{

    private File currentDir;
    private DataInputStream is;
    private DataOutputStream os;
    private BufferedOutputStream bos;
    private byte[] buf;
    private static final int SIZE = 256;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        bos = new BufferedOutputStream(socket.getOutputStream());;
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
                if (command.equals("#LIST#")){
//                    File[]  arrFiles = currentDir.listFiles();
//                    List<File> lst = Arrays.asList(arrFiles);
//                    os.writeLong(lst.size());
//                    os.writeUTF("#LIST#");
                    String[] files = currentDir.list();
                    if (files != null){
                        os.writeUTF("#LIST#");
                        os.writeInt(files.length);
                        System.out.println(files.length);
                        for (String file: files) {
                            System.out.println(file);
                            os.writeUTF(file);
                        }
                    }
                    System.out.println("Список отправлен " );

//                    os.writeUTF("List successfully uploaded");
                    os.flush();
                    System.out.println("поток очищен");
                }
                if (command.equals("#DIR#UP#")){
                    System.out.println("#DIR#UP#");

                }
                if (command.equals("#DIR#DOWN#")){
                    System.out.println("#DIR#DOWN#");
                }
                if (command.equals("#LOAD#FILE#")){
                    System.out.println("#LOAD#FILE#");
                    String fileName = is.readUTF();
                    System.out.println(fileName );
                    File currentFile = currentDir.toPath().resolve(fileName).toFile();
                    os.writeUTF("#SEND#FILE#");
                    os.writeUTF(fileName);
                    System.out.println(currentFile);
                    os.writeLong(currentFile.length());
                    try (FileInputStream is = new FileInputStream(currentFile)){
                        while (true){
                            int read = is.read(buf);
                            if (read == -1) {
                                break;
                            }
                            os.write(buf, 0, read);
                        }

                    }
                    System.out.println("File Send");
                    os.flush();
                    System.out.println("поток очищен");
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
