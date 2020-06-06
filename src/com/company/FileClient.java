package com.company;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

class FileClient {
    /**
     * Possible commands:
     * LIST - Shows all present files from the server
     * SYNCH - Synchronizes the Share folder with the Share folder on the server
     * GET Tekst.txt - Gets a file from the server
     * PUT Tekst.txt - Places a file on the server
     * DELETE Tekst.txt - Deletes a file from the server
     **/
    private static Socket socket;
    private static BufferedReader br;
    private static PrintStream ps;
    private static String shareName;
    private static String share;
    private ArrayList<String> listFiles;


    /**
     * Sets up the connection with the server.
     **/
    FileClient() {
        shareName = "Share";
        share = "Share\\";

        // Tries to connect to the server, if it fails an error is printed and the application is closed
        try {
//            socket = new Socket("192.168.43.124", 25444);
            socket = new Socket("localhost", 25444);
            getStatus();

        } catch (Exception e) {
            System.err.println(">Can't connect to the server, please try again later");
            System.exit(1);
        }
    }


    /**
     * This method keeps the program running using a while loop and decides what method to call, depending on the user input.
     **/
    void startFileClient() throws IOException, InterruptedException {
        while(true) {
            br = new BufferedReader(new InputStreamReader(System.in));
            ps = new PrintStream(socket.getOutputStream());

            // User input is split up into an array, and then used to decide which command to use and a filename is optionally stored
            String input = selectCommand();
            String[] inputArray = input.split(" ");

            switch (inputArray[0]) {
                case "LIST":
                    // The printstream sends the following command to the server: "LIST / AFTP/1.0"
                    ps.println(inputArray[0] + " / AFTP/1.0");
                    printList(getList());
                    continue;

                case "SYNCH":
                    ps.println(inputArray[0] + " / AFTP/1.0");
                    getMultipleFiles(getSynchFiles(getList()));
                    continue;

                case "GET":
                    ps.println(inputArray[0] + " " + inputArray[1] + " AFTP/1.0");
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

                // In case of an incorrect command
                default:
                    ps.println("COMMAND NOT FOUND");
                    getStatus();
            }
        }
    }


    /**
     * This method asks the user for input from the commandline.
     **/
    private static String selectCommand() throws IOException {
        System.out.print(">");

        return br.readLine();
    }


    /**
     * Compares the files from the server with the files from the client, and decides which files are going to be synched.
     *
     * @param fileNames All filenames from the server
     **/
    private ArrayList<String> getSynchFiles( String[] fileNames ) throws InterruptedException {
        listFiles = new ArrayList<>();
        ArrayList<String> filesToCheck = new ArrayList<>();
        ArrayList<String> serverFilesCheck = new ArrayList<>();
        ArrayList<String> returnValues = new ArrayList<>();

        folderWalker(share);

        int f = 0;
        for (String serverFile : fileNames ){
            if (!(f == 0)) {
                String[] CheckValue = serverFile.split(" ");
                String serverFileCheck = CheckValue[1].replace("\\", "\\\\");
                serverFilesCheck.add(serverFileCheck + " " + CheckValue[2]);
                File checkFile = new File(serverFileCheck);
                if (checkFile.exists()){
                    if (!(checkFile.isDirectory())) {
                        // file that is not an directory that already exist, checking later if it needs to be replaced.
                        String input = serverFileCheck + " " + checkFile.lastModified();
                        filesToCheck.add(input);
                    }
                } else {
                    if (!(serverFileCheck.contains("."))) {
                        checkFile.mkdir();
                    } else {
                        filesToCheck.add(serverFileCheck + " " + 0);
                    }
                }
            }
            if (f == 0) {
                f++;
            }
        }
        TimeUnit.MILLISECONDS.sleep(50);

        for (String item : filesToCheck) {
            String[] clientFile = item.split(" ");
            String clientName = clientFile[0];
            Long clientModification =  Long.parseLong(clientFile[1]);

            for (String serverItem : serverFilesCheck) {
                String[] serverFile = serverItem.split(" ");
                String serverName = serverFile[0];
                Long serverModification = Long.parseLong(serverFile[1]);

                if (serverName.equals(clientName)) {

                    if (clientModification < serverModification) {
                        returnValues.add(clientName.replace("Share\\\\",""));
                    }
                }
            }
        }
        return returnValues;
    }


    /**
     * Walks through all subfolders on the client, dependending on the given path.
     * This function is used in the SYNCH command.
     *
     * @param path The path which is being checked
     **/
    public void folderWalker( String path ) {
        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                //get all folders
                String[] editPathName = f.getAbsolutePath().split(shareName);
                String outValue = shareName + editPathName[1];
                listFiles.add(outValue);

                folderWalker( f.getAbsolutePath());
            }
            else {
                String[] editPathName = f.getAbsolutePath().split(shareName);
                String outValue = shareName + editPathName[1];
                listFiles.add(outValue);
            }
        }
    }


    /**
     * This function gets all the filenames from the server and returns it in an array.
     * This function is used in the LIST and SYNCH commands
     **/
    private String[] getList() {
        try {
            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            String[] returnValue = clientData.readUTF().replace("[","").replace("]","").split(",");

            return returnValue;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * The function associated with the LIST command, which gets all filenames from the server.
     *
     * @param list A list of the filenames who are going to be printed
     **/
    private void printList( String[] list ) {
        for (String item : list) {
            System.out.println(item);
        }
    }


    /**
     * The function associated with the GET command, which gets a file from the server.
     *
     * @param fileName The name of the file which is going to be received from the server
     **/
    private static void getFile( String fileName ) throws IOException {
        OutputStream output = null;
        String filePath = null;

        try {
            int bytesRead;
            DataInputStream clientData = new DataInputStream(socket.getInputStream());
            String serverResponse = clientData.readUTF();
            if ((serverResponse.equals("<AFTP/1.0 200 OK"))) {
                filePath = share + fileName;
                output = new FileOutputStream(filePath);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];

                // For every iteration, a part of 1024 bytes is received through the file output stream
                // The progress is checked through the long "size", this long is initially the size of the file and in every loop
                // the part that is received is substracted from the long, thus keeping track of the progress
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                // When this process is finished, the file output stream is closed
                output.close();

                System.out.println(serverResponse);
            } else {
                System.out.println(serverResponse);
            }
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
            File filePathCheck = new File(filePath);
            Boolean defaultPathCheck = filePathCheck.exists();

            // So if the file existed, it would be locked, if it didnt exist there's a server error.
            if (defaultPathCheck == true) {
                // Overwrite failed
                System.out.println("<AFTP/1.0 423 Locked");
            } else {
                // New file couldn't be made.
                System.out.println("<AFTP/1.0 500 Server Error");
            }
            output.close();
        }
    }


    /**
     * This function loops through an array of fileNames en gets each one of them using the getFile function.
     * This function is used in the SYNCH command.
     *
     * @param fileNames The list of files who are being synched
     **/
    private void getMultipleFiles( ArrayList<String> fileNames ) throws IOException {
        for (String item: fileNames) {
            System.out.println(">GET " + item +" AFTP/1.0");
            ps.println("GET " + item + " AFTP/1.0");
            getFile(item);
        }
    }


    /**
     * The function associated with the PUT command, which sends a file to the server.
     *
     * @param fileName The name of the file which is going to be sent to the server
     **/
    private static void putFile( String fileName ) {
        String fullPath = share + fileName;

        try {
            File myFile = new File(fullPath);
            byte[] byteArray = new byte[(int) myFile.length()];

            if(!(myFile.exists())) {
                // In case the file doesn't exist a 404 response is printed
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

}