package com.chat.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static Map<String, Set<ClientHandler>> groups = new HashMap<>();
    private static Set<ClientHandler> clientHandlers = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, clientHandlers, groups);
                clientHandlers.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Set<ClientHandler> clientHandlers;
    private Map<String, Set<ClientHandler>> groups;
    private String currentGroup;

    public ClientHandler(Socket socket, Set<ClientHandler> clientHandlers, Map<String, Set<ClientHandler>> groups) {
        this.socket = socket;
        this.clientHandlers = clientHandlers;
        this.groups = groups;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/join ")) {
                    joinGroup(message.substring(6));
                } else if (message.startsWith("/create ")) {
                    createGroup(message.substring(8));
                } else if (message.equals("/members")) {
                    listGroupMembers();
                } else {
                    broadcastMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clientHandlers.remove(this);
            if (currentGroup != null) {
                groups.get(currentGroup).remove(this);
            }
        }
    }

    private void joinGroup(String groupName) {
        groups.putIfAbsent(groupName, new HashSet<>());
        groups.get(groupName).add(this);
        currentGroup = groupName;
        out.println("Joined group " + groupName);
    }

    private void createGroup(String groupName) {
        groups.putIfAbsent(groupName, new HashSet<>());
        out.println("Group " + groupName + " created");
    }

    private void listGroupMembers() {
        if (currentGroup != null) {
            out.println("Members of " + currentGroup + ":");
            for (ClientHandler member : groups.get(currentGroup)) {
                out.println(member.socket.getInetAddress());
            }
        } else {
            out.println("You are not in a group");
        }
    }

    private void broadcastMessage(String message) {
        if (currentGroup != null) {
            for (ClientHandler clientHandler : groups.get(currentGroup)) {
                if (clientHandler != this) {
                    clientHandler.out.println(message);
                }
            }
        } else {
            out.println("You are not in a group");
        }
    }
}
