package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import static com.company.FileClient.*;

public class Main {


    private static Socket sock;
    private static String fileName;
    private static BufferedReader stdin;
    private static PrintStream os;


    public static void main(String[] args) throws IOException {
        // write your code here

        FileClient fileClient = new FileClient();
        fileClient.startFileClient();

    }
}
