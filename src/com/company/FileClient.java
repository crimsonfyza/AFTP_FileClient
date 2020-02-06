package com.company;

import java.net.*;
import java.io.*;

class FileClient {
    /**
     * Possible commands:
     * LIST - Shows all files
     * GET /Tekst.txt - Get file from server
     * PUT /Tekst.txt - Place file on server
     * DELETE /Tekst.txt - Delete file from server
     *
     * socket   The connection with the server over a specific port
     * br       The buffer surrounding the inputstream, gets its input from the commandline
     * ps       The communication with the server
     * fileName The filename of the received/sent file
    **/
    private static Socket socket;
    private static BufferedReader br;
    private static PrintStream ps;
    private static String fileName;

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

            String input = selectCommand();
            String[] inputArray = input.split(" ");

            switch (inputArray[0]) {
                case "LIST":
                    ps.println(inputArray[0] + " / AFTP/1.0");
                    getStatus();
                    continue;
                case "GET":
                    String test = inputArray[0] + " " + inputArray[1] + " AFTP/1.0";
                    ps.println(test);
                    getFile(inputArray[1]);
                    continue;
                case "PUT":
                    ps.println(inputArray[0] + " " + inputArray[1] + " AFTP/1.0");
                    putFile(inputArray[1]);
                    continue;
                case "DELETE":
                    if(inputArray[1] != null) {
                        ps.println(inputArray[0] + " " + inputArray[1] + " AFTP/1.0");
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

            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            String status = clientData.readUTF();
            System.out.println(">");
            System.out.println(status);

        } catch (Exception e) {

        }
    }

    private static void getFile(String getFileName) throws IOException {
        OutputStream output = null;
        DataInputStream clientData = null;
        try {
            int bytesRead;
            clientData = new DataInputStream(socket.getInputStream());
            String currentFileName = clientData.readUTF();
            if (!(currentFileName.equals("<AFTP/1.0 404 Not found"))) {
                output = new FileOutputStream(getFileName);
                int size = clientData.readInt();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }
                System.out.println(currentFileName);
            } else {
                System.out.println(currentFileName);
            }
        } catch (IOException ex) {
            //System.err.println("Client error. Connection closed.");
            System.out.println("<AFTP/1.0 404 Not found");
        }

    }

    private static void putFile(String putFileName) {
        try {
            File myFile = new File(putFileName);
            byte[] byteArray = new byte[(int) myFile.length()];

            if(!(myFile.exists())) {
                System.out.println(">");
                System.out.println("<AFTP/1.0 404 Not found");
            } else {
                FileInputStream fis = new FileInputStream(myFile);
                BufferedInputStream bis = new BufferedInputStream(fis);

                DataInputStream dis = new DataInputStream(bis);
                dis.readFully(byteArray, 0, byteArray.length);

                OutputStream os = socket.getOutputStream();

                DataOutputStream dos = new DataOutputStream(os);
                dos.writeUTF(myFile.getName());
                dos.writeLong(byteArray.length);
                dos.write(byteArray, 0, byteArray.length);
                dos.flush();

                getStatus();
            }
        } catch (Exception e) {
            System.err.println("Exception: " + e);
        }
    }
}