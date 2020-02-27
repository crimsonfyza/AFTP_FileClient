package com.company;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

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
    private static DataInputStream getClientData;

    /**
     * Creates the connection
    **/
    FileClient() {
        try {
            //socket = new Socket("192.168.43.101", 25444);
            socket = new Socket("localhost", 25444);
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
                    printList(getList());
                    continue;
                    case "SYNCH":
                    ps.println(inputArray[0] + " / AFTP/1.0");

                    continue;
                case "GET":
                    ps.println(inputArray[0] + " " + inputArray[1] + " AFTP/1.0");
                    getFile(inputArray[1]);
                    continue;
                case "PUT":
                    ps.println(inputArray[0] + " " + inputArray[1] + " AFTP/1.0");
                    putFile(inputArray[1]);
                    // putFile(inputArray[1]); Zo werkt het gewoon...
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

    private String[] getList() {

        try {
            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            String[] returnValue = clientData.readUTF().replace("[","").replace("]","").split(",");
            return returnValue;

        } catch (Exception e) {
            return null;
        }
    }

    private void printList (String[] list) {
        for (String item : list) {
            System.out.println(item);
        }
    }


    private static void getFile(String getFileName) throws IOException {
        OutputStream output = null;
        String filePath = null;

        try {
            int bytesRead;
            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            String currentFileName = clientData.readUTF();
            if ((currentFileName.equals("<AFTP/1.0 200 OK"))) {
                filePath = "Share\\" +getFileName;
                output = new FileOutputStream(filePath);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                output.close();

                System.out.println(currentFileName);
            } else {
                System.out.println(currentFileName);
            }
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
            File filePathCheck = new File(filePath);
            Boolean defaultPathCheck = filePathCheck.exists();
            //error file couldnt upload
            // so if the file existed, it would be locked, if it didnt exist theres a server error.
            if (defaultPathCheck == true) {
                //overwrite failed
                System.out.println("catch <AFTP/1.0 423 Locked");
            } else {
                //new file couldnt be made.
                System.out.println("catch <AFTP/1.0 500 Server Error");
            }
            output.close();
        }

    }




    private static void putFile(String putFileName) {
        String fullPath = "Share\\" + putFileName;

        try {
            File myFile = new File(fullPath);
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