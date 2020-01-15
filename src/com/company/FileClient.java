package com.company;

import java.net.*;
import java.io.*;

public class FileClient {
    private static Socket socket;
    private static String fileName;
    private static BufferedReader br;
    private static PrintStream ps;

    public void startFileClient() throws IOException {
        while(true) {
            try {
                socket = new Socket("192.168.43.101", 25444);
                br = new BufferedReader(new InputStreamReader(System.in));
            } catch (Exception e) {
                System.err.println("Can't connect to the server, please try again later");
                System.exit(1);
            }

            ps = new PrintStream(socket.getOutputStream());

            try {
                switch (selectCommand()) {
                    case "LIST":
                        ps.println("LIST");
                        System.out.print("LIST: ");

                        fileName = br.readLine();
                        ps.println(fileName);
                        getFile(fileName);
                        continue;
                    case "GET":
                        ps.println("GET");
                        System.out.print("GET: ");

                        fileName = br.readLine();
                        ps.println(fileName);
                        getFile(fileName);
                        continue;
                    case "PUT":
                        ps.println("PUT");
                        System.out.println("PUT: ");

                        putFile();
                        continue;
                    case "DELETE":
                        ps.println("DELETE");
                        System.out.print("DELETE: ");

                        fileName = br.readLine();
                        ps.println(fileName);
                        getStatus();
                        continue;
                    default:
                        ps.println("COMMAND NOT FOUND");
                        continue;
                }
            } catch (Exception e) {
                System.err.println("Please enter a valid command");
            }
        }
    }

    public static String selectCommand() throws IOException {
        System.out.println("\nLIST - Shows all files");
        System.out.println("GET - Get file from server");
        System.out.println("PUT - Place file on server");
        System.out.println("DELETE - Delete file from server");
        System.out.println("--------------------------");
        System.out.print("Choose command: ");

        return br.readLine();
    }

    public static void getFile(String fileName) {
        try {
            int bytesRead;
            InputStream in = socket.getInputStream();

            DataInputStream clientData = new DataInputStream(in);

            fileName = clientData.readUTF();
            System.out.println(fileName);
            OutputStream output = new FileOutputStream(fileName);

            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            in.close();

            System.out.println("File "+fileName+" received from the server.");
        } catch (IOException ex) {
            System.out.println("Exception: "+ex);
            ex.printStackTrace();
        }
    }

    public static void putFile() {
        try {
            System.out.print("Enter file name: ");
            fileName = br.readLine();

            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            if(!myFile.exists()) {
                System.out.println("File doesn't exist");
                return;
            }

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
            System.out.println("File "+fileName+" sent to the server.");
        } catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }

    public static void getStatus() {
        try {
            InputStream in = socket.getInputStream();
            DataInputStream clientData = new DataInputStream(in);
            String status = clientData.readUTF();
            System.out.println(status);
        } catch (Exception e) {
            System.err.println("Exception: "+e);
        }
    }

}