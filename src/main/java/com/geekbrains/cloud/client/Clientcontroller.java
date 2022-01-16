package com.geekbrains.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Clientcontroller implements Initializable {

    @FXML
    ListView<String> listView;
    @FXML
    TextField textField;
    @FXML
    ListView<String> listViewServer;
    @FXML
    TextField textFieldServer;
    private DataInputStream is; // Входящий поток
    private DataOutputStream os;// Исходящий поток
    private BufferedInputStream bis;//  Входящий поток буфера
    private File currentDir; // Текущая дирректория
    private byte[] buf; // Буфер
    private static final int SIZE = 256; //размер буфера
    private static final int PORT = 8189; //порт
    private static final String HOST = "localhost"; // адресс хоста
    private Socket socket;// сокет
    private ExecutorService executorService; // сервис потоков
    private static String command;
    String fileName;

    public void connect(){ //Подключение к серверу
        try {
            buf = new byte[SIZE];// устанавливаем размер буфера
            currentDir = new File("home");//определяем домашнюю дирректорию
            socket= new Socket(HOST, PORT);//создаем подключение
            is = new DataInputStream(socket.getInputStream()); // создаём входящий поток данных
            os = new DataOutputStream(socket.getOutputStream());//создаём исхлдящий поток данных
            System.out.println("Connected accept");
//            bis = new BufferedInputStream(socket.getInputStream());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void sendFile(ActionEvent actionEvent) throws IOException { //отправка файла на сервер
        fileName = textField.getText();//получаем имя файла из textField
        System.out.println(fileName);
        File currentFile = currentDir.toPath().resolve(fileName).toFile();// создаем объект
        System.out.println(currentFile);
        os.writeUTF("#SEND#FILE#");//отправляем команду серверу на прием файла
        os.writeUTF(fileName);//передаём имя файла
        os.writeLong(currentFile.length());//предаем размер файла
        try (FileInputStream is = new FileInputStream(currentFile)){//создаём поток чтения файла
            while (true){//создаем бесконечный цикл
                int read = is.read(buf); // создаём числовую переменную из потока чтения файла
                if (read == -1) { //если прочитали всё - прерываем цикл
                    break;
                }
                os.write(buf, 0, read);// пишем в исходящий поток
            }
        }
        os.flush();//очищаем исходящий поток
        System.out.println("Файл отправлен");
        textField.clear();//очищаем поле выбора
//        fillServerDirFile();
    }

    public void loadFile(ActionEvent actionEvent) throws IOException { //загрузка файла
        fileName = textFieldServer.getText();//получаем имя файла из textFieldServer
        os.writeUTF("#LOAD#FILE#");//отправляем команду серверу отправить файл
        System.out.println("#LOAD#FILE#");
        os.writeUTF(fileName);//отправляем серверу имя выбранного файла
        os.flush();//очищаем поток
        textFieldServer.clear();//очищаем поле выбора

    }

    public void listServer() throws IOException{//Запрос списка файлов
        os.writeUTF("#LIST#");
        os.flush();
    }

    public void read(){//Слушатель потока
        try {
            System.out.println("Thread read - accept");
            while (true){//цикл
                System.out.println("restart cicle");
                command = is.readUTF();//Слушаем команду из входящего потока
                System.out.println(command);
                if (command.equals("#LIST#")) {//сверяем команду, если список
                    Platform.runLater(() -> listViewServer.getItems().clear());//очищаем поле списка
                    int count = is.readInt();//читаем размер архива(Количество строчек)
                    for (int i = 0; i < count; i++) {//количество циклов чтения равно количеству строк
                        String fileName = is.readUTF();//читаем имя файла
                        Platform.runLater(() -> listViewServer.getItems().add(fileName));//пишем имя файла в листвью
                    }
                }
                if (command.equals("#SEND#FILE#")) {//команда на получение файла
                    fileName = is.readUTF();//получаем имя файла
                    buf = new byte[SIZE];//создаём буфер размера SIZE
                    long size = is.readLong();// получаем размер файла от сервера
                    System.out.println("Created file: " + fileName);
                    System.out.println("File size: " + size);
                    Path currentPath = currentDir.toPath().resolve(fileName);//Создаём целевой файл в текущей дирректории
                    try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {//создаём поток записи в файл
                        for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {//запускаем цикл от 0 до размер файла + размер буфера - 1 ии разделить на размер буфера
                            int read = is.read(buf);//читаем поток блоками равными буферу
                            fos.write(buf, 0, read);//пишем в файл
                        }
                    }
                    // client state updated
                    Platform.runLater(this::fillCurrentDirFiles);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            //reconnect to server
            connect();//в случае вылета делаем реконект
        }
    }

    private void fillCurrentDirFiles(){//
        listView.getItems().clear();//очищаем поле
        listView.getItems().add("..");//добавляем знак верхнего перехода
        listView.getItems().addAll(currentDir.list());//грузим список файлов из дирректории
        System.out.println("fillCurrentDir - accept");
    }

    private void fillServerDirFile() throws IOException {//получаем список файлов сервера
        listViewServer.getItems().clear();
        listViewServer.getItems().add("..");
        os.writeUTF("#LIST#");//запрос списка
        System.out.println("send command #LIST#");

        command = is.readUTF();//ловим ответ
        System.out.println(" Get command: " + command );
        if (command.equals("#LIST#")){//убеждаемся, что сервер нас понял
            int count = is.readInt();// размер списка
            System.out.println(" Count is: " + count);
            for (int i = 0; i < count; i++) {
                fileName = is.readUTF();
                System.out.println(fileName);
                listViewServer.getItems().add(fileName);
            }
        }


        os.flush();
    }

    private void initClickListener(){//ловим действия мыши
        listView.setOnMouseClicked(e -> {//создаем обработчик
            if (e.getClickCount() == 2) {//если двойной клик
                String fileName = listView.getSelectionModel().getSelectedItem();//вытаскиваем имя файла

                System.out.println("Выбран файл " + fileName);
                Path path = currentDir.toPath().resolve(fileName);

                if (Files.isDirectory(path)){//если выбрана директория
                    currentDir = path.toFile();//устанавливаем новую папку текущей
                    fillCurrentDirFiles();//перегружаем список
                    textField.clear();
                }else {
                    textField.setText(fileName);
                }
            }
        });

        listViewServer.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileNameServer = listViewServer.getSelectionModel().getSelectedItem();
                if (fileNameServer.equals("..")){
                    try {
                        os.writeUTF("#DIR#UP#");
                        System.out.println("#DIR#UP#");
                        os.flush();
//                        fillServerDirFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                System.out.println("Выбран файл " + fileNameServer);
                Path pathServer = currentDir.toPath().resolve(fileNameServer);//задел на дальше
                textFieldServer.setText(fileNameServer);

            }

        });
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connect();
            System.out.println("1 passed");
            fillCurrentDirFiles();
            System.out.println("2 passed");
            fillServerDirFile();
            System.out.println("3 passed");
            initClickListener();
            System.out.println("4 passed");
            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();

        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
