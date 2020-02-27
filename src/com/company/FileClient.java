package com.company;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
    private String ShareName;
    private static String Share;
    private ArrayList<String> ListFiles;

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

    void startFileClient() throws IOException, InterruptedException {

        while(true) {
            ShareName = "Share";
            Share = "Share\\";
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
                    ArrayList<String> needToSynch = synchFiles(getList());
                    getMultipleFiles(needToSynch);
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

    private ArrayList<String> synchFiles (String[] list) throws InterruptedException {
        ListFiles = new ArrayList<>();
        ArrayList<String> filesToCheck = new ArrayList<>();
        ArrayList<String> serverFilesCheck = new ArrayList<>();
        ArrayList<String> returnValues = new ArrayList<>();

        folderWalker(Share);

        int f = 0;
        for (String serverFile : list )
        {
            if (!(f == 0)) {
                String[] CheckValue = serverFile.split(" ");
                String serverFileCheck = CheckValue[1].replace("\\", "\\\\");
                serverFilesCheck.add(serverFileCheck + " " + CheckValue[2]);
                File checkFile = new File(serverFileCheck);
                if (checkFile.exists()){
                    if (!(checkFile.isDirectory())) {
                        // files thats not an directory that already exist, checking later if it needs to be replaced.
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

    public void folderWalker( String path ) {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                //get all folders
                String[] editPathName = f.getAbsolutePath().split(ShareName);
                String outValue = ShareName + editPathName[1];
                ListFiles.add(outValue);

                folderWalker( f.getAbsolutePath());

            }
            else {
                String[] editPathName = f.getAbsolutePath().split(ShareName);
                String outValue = ShareName + editPathName[1];
                ListFiles.add(outValue);
            }
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


    private void getMultipleFiles (ArrayList<String> getFiles) throws IOException {
        for (String item: getFiles) {
            System.out.println(">GET " + item +" AFTP/1.0");
            ps.println("GET " + item + " AFTP/1.0");
            getFile(item);
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
                filePath = Share +getFileName;
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
        String fullPath = Share + putFileName;

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