package reseau;


import game.Game;
import model.Carte;
import model.Flotte;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // to check if server is running
    private boolean keepGoing;
    // notification
    private String notif = " *** ";
    private Game mainGame;

    //constructor that receive the port to listen to for connection as parameter

    public Server(int port) {
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // an ArrayList to keep the list of the Client
        al = new ArrayList<ClientThread>();
        mainGame = new Game();
    }

    public void start() {
        keepGoing = true;
        //create socket server and wait for connection requests
        try
        {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections ( till server is active )
            while(keepGoing)
            {
                if(al.size()<2) {
                    display("Server waiting for Clients on port " + port + ".");
                    // accept connection if requested from client
                    Socket socket = serverSocket.accept();
                    // break if server stoped
                    if (!keepGoing)
                        break;
                    // if client is connected, create its thread
                    ClientThread t = new ClientThread(socket);
                    //add this client to arraylist
                    al.add(t);
                    System.out.println(al.size());
                    t.start();
                }
                if(al.size()==2){
                    broadcast("Nous sommes au complet, le jeu va pouvoir commencer");
                    broadcast("Le jeu commencera dans 3...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast("2...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast("1...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    broadcast("--- START ---");

                    mainGame.partie();
                    mainGame.infoGame();
                    Socket socket = serverSocket.accept();
                    if(!keepGoing)
                        break;
                    // if client is connected, create its thread
                    ClientThread t = new ClientThread(socket);
                    t.start();
                }

            }
            // try to stop the server
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    // to stop the server
    public  void stop() {
        keepGoing = false;
        try {
            new Socket("127.0.0.1", port);
        }
        catch(Exception e) {
        }
    }

    // Display an event to the console
    public void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    // to broadcast a message to all Clients
    public synchronized boolean broadcast(String message) {
        // add timestamp to the message
        String time = sdf.format(new Date());

        // to check if message is private i.e. client to client message
        String[] w = message.split(" ",3);

        String messageLf = time + " " + message + "\n";
        // display message
        System.out.print(messageLf);

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
        return true;


    }

    // if client sent LOGOUT message to exit
    public synchronized void remove(int id) {

        String disconnectedClient = "";
        // scan the array list until we found the Id
        for(int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // if found remove it
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat room." + notif);
    }

    /*
     *  To run as a console application
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1500;
        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    // One instance of this thread will run for each client
    public class ClientThread extends Thread {
        // the socket to get messages from client
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        //PrintWriter pw = new PrintWriter(sOutput,true);
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // message object to recieve message and its type
        ChatMessage cm;
        // timestamp
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            //Creating both Data Stream
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room." + notif);
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        // infinite loop to read and forward message
        public void run() {
            // to loop until LOGOUT
            /*if(al.size()==2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                broadcast("Nous sommes au complet, nous pouvons commencer le jeu");
            }*/
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // get the message from the ChatMessage object received
                String message = cm.getMessage();

                // different actions based on type message
                switch(cm.getType()) {

                    case ChatMessage.MESSAGE:
                    boolean confirmation =  broadcast(username + ": " + message);
                    if(confirmation==false){
                    String msg = notif + "Sorry. No such user exists." + notif;
                    writeMsg(msg);
                    }
                    break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for(int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                    case ChatMessage.JOUER:
                        broadcast(username+" veut jouer");
                        writeMsg("Voulez vous commencer a jouer ?");
                        try {
                            cm = (ChatMessage) sInput.readObject();
                        }
                        catch (IOException e) {
                            display(username + " Exception reading Streams: " + e);
                            break;
                        }
                        catch(ClassNotFoundException e2) {
                            break;
                        }

                        switch (cm.getType()){
                            case ChatMessage.YES:

                                mainGame.partie();

                                /*try {
                                    cm = (ChatMessage) sInput.readObject();
                                }
                                catch (IOException e) {
                                    display(username + " Exception reading Streams: " + e);
                                    break;
                                }
                                catch(ClassNotFoundException e2) {
                                    break;
                                }

                                switch (cm.getType()){
                                    case ChatMessage.CARTE:
                                }*/

                                break;
                        }
                        break;
                }

            }
            // if out of the loop then disconnected and remove from client list
            remove(id);
            close();
        }

        // close everything
        public void close() {
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        // write a String to the Client output stream
        public boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display(notif + "Error sending message to " + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}
