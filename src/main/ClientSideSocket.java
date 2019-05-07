package main;

import controllers.OfflineMapEditorController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientSideSocket extends Thread{
    private int port;
    private String host;
    private OfflineMapEditorController controller;
    Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public ClientSideSocket(String host, int port) {
        this.port = port;
        this.host = host;
        try {
            System.out.println("Trying to connect at " + host + ":" + port);
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if(socket.isConnected())
                System.out.println("Connected!");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setDaemon(true);
        start();
    }

    public void run() {
        String input;
        String[] data;
        try {
            while((input = in.readLine()) != null) {
                data = input.split(":", 2);
                if(data[0].equalsIgnoreCase("LOGGED")) {
                    data = data[1].split(":", 2);
                    controller.setPID(Integer.parseInt(data[0]));
                    controller.setNickname(data[1]);
                }
                if(data[0].equalsIgnoreCase("MSG_BRD")) {
                    data = data[1].split(":", 2);
                    controller.receiveMessage(data[0], data[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setController(OfflineMapEditorController controller) {
        this.controller = controller;
    }

    public void requestNickname(String nickname) {
        out.println("REQ_NAM:" + nickname);
    }

    public void sendMessage(int PID, String msg) {
        out.println("MSG:" + PID + ":" + msg);
    }

    public void close() {
        try {
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
