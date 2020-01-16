package com.company;

import java.net.*;
import java.io.*;

class FileClient {
    /**
     * Possible commands:
     * LIST / AFTP/1.0 - Shows all files
     * GET /Tekst.txt AFTP/1.0 - Get file from server
     * PUT /Tekst.txt AFTP/1.0 - Place file on server
     * DELETE AFTP/1.0 - Delete file from server
     *
     * socket   De connectie met de server over een specifieke poort
     * br       Buffer om de inputstream heen, haalt input uit de commandline
     * ps       De communicatie met de server
     * fileName Bestandsnaam van het bestand wat opgehaald/geplaatst wordt
    **/
    private static Socket socket;
    private static BufferedReader br;
    private static PrintStream ps;
    private static String fileName;
    private static String defaultPath = "C:\\Users\\aron1\\IdeaProjects\\AFTP_FileClient\\files\\";

    /**
     * Creates the connection
    **/
    FileClient() {
        try {
            socket = new Socket("192.168.43.101", 25444);
            getStatus();
        } catch (Exception e) {
            System.err.println(">Can't connect to the server, please try again later");
            System.exit(1);
        }
    }

    void startFileClient() throws IOException {
        while(true) {
            br = new BufferedReader(new InputStreamReader(System.in));
            ps = new PrintStream(socket.getOutputStream());

            // Een switch die de aan de hand van het eerste woord uit de commandline de input afhandelt
            String input = selectCommand();
            String[] inputArray = input.split(" ", 3);

            switch (inputArray[0]) {
                case "LIST":
                    ps.println(input);
                    getStatus();
                    continue;
                case "GET":
                    ps.println(input);
                    getFile();
                    getStatus();
                    continue;
                case "PUT":
                    ps.println(input);
                    putFile(inputArray[1]);
                    getStatus();
                    continue;
                case "DELETE":
                    if(inputArray[1] != null) {
                        ps.println(input);
                        getStatus();
                    }
                    continue;
                default:
                    ps.println("COMMAND NOT FOUND");
                    getStatus();
            }
        }
    }

    /**
     * This method asks the user for input from the commandline
     **/
    private static String selectCommand() throws IOException {
        System.out.print(">");

        return br.readLine();
    }

    /**
     * Requests the status from the server
     **/
    private static void getStatus() {
        try {
            InputStream in = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(in);
            String status = clientData.readUTF();
            System.out.println(">");
            System.out.println(status);
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }

    private static void getFile() {
        String filePath = null;
        OutputStream output = null;
        DataInputStream clientData = null;
        try {
            int bytesRead;
            clientData = new DataInputStream(socket.getInputStream());
            String currentFileName = clientData.readUTF();
            filePath = fileName;
            output = new FileOutputStream(filePath);
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }
            output.close();
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }

    private static void putFile(String putFileName) {
        try {
            File myFile = new File(putFileName);
            byte[] mybytearray = new byte[(int) myFile.length()];

            if(!myFile.exists()) {
                System.out.println(">");
                System.out.println("<AFTP/1.0 404 Not found");
            } else {
                FileInputStream fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                DataInputStream dis = new DataInputStream(bis);
                dis.readFully(mybytearray, 0, mybytearray.length);

                OutputStream os = socket.getOutputStream();

                //Sending file name and file size to the server
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeUTF(myFile.getName());
                dos.writeLong(mybytearray.length);
                dos.write(mybytearray, 0, mybytearray.length);
                dos.flush();
            }
        } catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }
}