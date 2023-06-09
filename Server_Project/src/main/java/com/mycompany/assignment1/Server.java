/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.assignment1;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;


/**
 *
 * @author diamo
 */
public class Server extends JFrame implements ActionListener, Runnable {
    
    // If recall has been called
    static boolean recallStatus = false;
    
    // ArrayLists for Drone , Fire Objects and trucks
    public static ArrayList<DroneDetails> drones = new ArrayList<>();
    static ArrayList<FireDetails> fires = new ArrayList<>();
    static ArrayList<FiretruckDetails> trucks = new ArrayList<>();
    
    // GUI Setup, all elements of GUI declared
    private JLabel titleText = new JLabel("Drone Server");
    private static JTextArea outputText = new JTextArea(25, 25);
    private JLabel headingText = new JLabel("Server Output Log");
    private JLabel mapText = new JLabel("Drone and Fire Map");
    private JLabel buttonText = new JLabel("Admin Controls");
    private JButton deleteButton = new JButton("Delete Fire");
    private JButton recallButton = new JButton("Recall Drones");
    private JButton moveButton = new JButton("Move Drone");
    private JButton shutDownButton = new JButton("Shut Down");
    private JScrollPane scrollPane; // Scroll pane for the text area
    private MapPanel mapPanel;
    
    // Hash Maps to store positions of drones that need to be moved
    static HashMap<Integer, Integer> newXPositions = new HashMap<>();
    static HashMap<Integer, Integer> newYPositions = new HashMap<>();
    
    public class MapPanel extends JPanel {

        private ArrayList<DroneDetails> drones;
        private ArrayList<FireDetails> fires;
        private ArrayList<FiretruckDetails> trucks;

        public MapPanel(ArrayList<DroneDetails> drones, ArrayList<FireDetails> fires, ArrayList<FiretruckDetails> trucks) throws SQLException {
            this.drones = drones;
            this.trucks = trucks;
            this.fires = fires;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Set background color of map panel
            setBackground(Color.WHITE);
            
            // Draw drones as blue circles with drone id
            for (DroneDetails p : drones) {
                if (p.getActive()) {
                    // Converts coordinates for use on 400 by 400 grid
                    int x = (100 - p.getX_pos()) * 2;
                    int y = (100 - p.getY_pos()) * 2;
                    int size = 10;
                    g.setColor(Color.BLUE);
                    g.fillOval(x - size/2, y - size/2, size, size);
                    g.setColor(Color.BLACK);
                    g.drawString("Drone " + p.getId(), x - 30, y - 5);
                }
            }
            
            // Draw fires as red circles with fire id and severity
            for (FireDetails p : fires) {
                // Converts coordinates for use on 400 by 400 grid
                int x = (100 - p.getX_pos()) * 2;
                int y = (100 - p.getY_pos()) * 2;
                int severity = p.getSeverity();
                int size = 10;
                g.setColor(Color.RED);
                g.fillOval(x - size/2, y - size/2, size, size);
                g.setColor(Color.BLACK);
                g.drawString("Fire " + p.getId() + " (" + severity + ")", x - 30, y - 5);
            }
            
            for (int p = 0; p < trucks.size(); p++) {
            int x = (100 - fires.get(p).getX_pos()) * 2;
            int y = (100 - fires.get(p).getY_pos()) * 2 - 12;
            String name = trucks.get(p).getTruckName();
            int size = 5;
            g.setColor(Color.YELLOW);
            g.fillOval(x - size/2, y - size/2, size, size);
            g.setColor(Color.BLACK);
            g.drawString("Firetruck " + trucks.get(p).getId(), x - 30, y - 5);
}
        }
    }
    
        // Initialise connection for sql database
        private static Connection connection;
        
        // Creates string for connection
        private static String URL = "jdbc:mysql://localhost:3306/ibdms_server";
        private static final String USERNAME = "user";
        private static final String PASSWORD = "pass";
    
    Server() throws SQLException {
        // Sets settings for java swing GUI Frame
        super("Server GUI");
        
        // Sets font for title
        titleText.setFont(new Font("Arial", Font.PLAIN, 30));
        
        // Sets X button to do nothing, shut down should be used to exit
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Other GUI settings
        setSize(750, 600);
        this.setLayout(new FlowLayout());
        this.setResizable(false);
        
        // Heading Panel
        JPanel headingPanel = new JPanel();
        headingPanel.setPreferredSize(new Dimension(750, 40));
        headingPanel.add(titleText);
        
        // Set Text Area Wrapping and read-only
        outputText.setEditable(false);
        outputText.setLineWrap(true);
        outputText.setWrapStyleWord(true);
        
        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setPreferredSize(new Dimension(750, 40));
        buttonPanel.add(deleteButton);
        buttonPanel.add(recallButton);
        buttonPanel.add(moveButton);
        buttonPanel.add(shutDownButton);
        
        // Bottom panel
        JPanel bottomPanel = new JPanel();
        
        // Output Panel
        JPanel outputPanel = new JPanel();
        outputPanel.setPreferredSize(new Dimension(300, 500));
        outputPanel.add(headingText);
        outputPanel.add(outputText);
        
        // Text Area Vertical ScrollBar
        scrollPane = new JScrollPane(outputText);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outputPanel.add(scrollPane);
        
         // Map Panel
        mapPanel = new MapPanel(drones, fires, trucks);
        mapPanel.setPreferredSize(new Dimension(400, 400));
        
        // Outer Map Panel with text
        JPanel outerMapPanel = new JPanel();
        outerMapPanel.setPreferredSize(new Dimension(400, 500));
        
        // Add panels and text to GUI
        add(headingPanel);
        add(buttonText);
        add(buttonPanel);
        
        outerMapPanel.add(mapText);
        outerMapPanel.add(mapPanel);
        bottomPanel.add(outputPanel);
        bottomPanel.add(outerMapPanel);
        
        add(bottomPanel);
        
        // Makes the GUI visible
        this.setVisible(true);
        
        // Action Listeners for Buttons
        deleteButton.addActionListener(this);
        recallButton.addActionListener(this);
        moveButton.addActionListener(this);
        shutDownButton.addActionListener(this);
        
        // check to see if database is connectable
        // Tries to connect to  the database 
        try
        {
            // Uses URL, USERNAME and PASSWORRD  to connect to database
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
            // Confirmation message when able to connected to database
            System.out.println("Connection established");
            
            // closes connection
            connection.close();
        
        } catch (SQLException e) {
            // Confirmation message when unable to connect to database
            System.out.println("Could not connect to the database");
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // This runs when an object action is clicked
        // Gets the name of the object clicked and finds the case
        // Runs the corresponding method and breaks the switch
        String actionString=e.getActionCommand();
        switch(actionString) {
            case "Delete Fire":
            {
                try {
                    deleteFire();
                } catch (SQLException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
                break;

                
            case "Recall Drones":
                recallDrones();
                break;
                
            case "Move Drone":
                moveDrone();
                break;
                
            case "Shut Down":
                shutDown();
                break;
        }
    }
    
    public static void getAllFires() throws SQLException {
        // Sql statement
        String sql = "SELECT * FROM fire";
        
        // Uses URL, USERNAME and PASSWORRD  to connect to database
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        
        // Object statement
        PreparedStatement preStmt = connection.prepareStatement(sql);
        
        // 
        ResultSet rs = preStmt.executeQuery();
        
        // Will keep running until it goes through all records
        while(rs.next()) {
            int fireId = rs.getInt("id");
            boolean activity = rs.getBoolean("isActive");
            int intensity = rs.getInt("intensity");
            int xPos = rs.getInt("xpos");
            int yPos = rs.getInt("ypos");
            
            // Makes records into objects
            FireDetails fire = new FireDetails(fireId,activity,xPos,yPos,0,intensity);
            
            // Adds object into array list
            fires.add(fire);
            
            // Confirmation output
            System.out.println(fire);
        }
        // Returns array
        //return fires;
    }
    
    public static void getAllDrones() throws SQLException {
        // Sql statement
        String sql = "SELECT * FROM drone";
        
        // Uses URL, USERNAME and PASSWORRD  to connect to database
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        
        // Object statement
        PreparedStatement preStmt = connection.prepareStatement(sql);
        
        ResultSet rs = preStmt.executeQuery();
        
        // Will keep running until it goes through all records
        while(rs.next()) {
            int droneId = rs.getInt("id");
            String name = rs.getString("name");
            int xPos = rs.getInt("xpos");
            int yPos = rs.getInt("ypos");
            
            // Makes records into objects
            DroneDetails drone = new DroneDetails(droneId,name,xPos,yPos,true);
            
            // Adds object into array list
            drones.add(drone);
            
            // Confirmation output
            System.out.println(drone);
        }
        // Returns array
        //return drones;
    }

    public static void getAllTrucks() throws SQLException {
        // Sql statement
        String sql = "SELECT * FROM firetrucks";
        
        // Uses URL, USERNAME and PASSWORRD  to connect to database
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        
        // Object statement
        PreparedStatement preStmt = connection.prepareStatement(sql);
        
        ResultSet rs = preStmt.executeQuery();
        
        // Will keep running until it goes through all records
        while(rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");
            int fireId = rs.getInt("designatedFireId");
            
            // Makes records into objects
            FiretruckDetails truck = new FiretruckDetails(id,name,fireId);
            
            // Adds object into array list
            trucks.add(truck);
            
            // Confirmation output
            System.out.println(truck);
        }
        // Returns array
        //return trucks;
    }
    
    public static void main(String[] args) throws SQLException {
        
        // Adds database records into arraylists
        getAllDrones();
        getAllFires();
        getAllTrucks();
        
        // Starts thread to update map and GUI because that's how it works apparently
        Server obj = new Server();
        Thread thread = new Thread(obj);
        thread.start();
        
        // Sets up connection listener with port 8888
        try {
            int serverPort = 8888;
            ServerSocket listenSocket = new ServerSocket(serverPort);
            
            // Constantly on loop, checks for connections and sends connections to new thread
            while(true) {
                Socket clientSocket = listenSocket.accept();
                DroneConnection c = new DroneConnection(clientSocket) {};
            }
            
        }   catch(IOException e) {System.out.println("Listen Socket : " + e.getMessage());}
    }
    
    
    
    static boolean ifRecall() {
        // Returns if the recall status is true
        return recallStatus;
    }
    
    static void addDrone(DroneDetails tempDrone) {
    
            // Assumes drone is new until found otherwise
            boolean newDrone = true;
            
        try {
            // Uses URL, USERNAME and PASSWORRD  to connect to database
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
            for (DroneDetails p : drones) {
                if (p.getId() == tempDrone.getId()) {
                    String sql = "UPDATE drone SET xpos = ?, ypos = ? WHERE id = ?";
                    
                    PreparedStatement preStmt = connection.prepareStatement(sql);
                    
                    preStmt.setInt(1, tempDrone.getX_pos());
                    preStmt.setInt(2, tempDrone.getY_pos());
                    preStmt.setInt(3, tempDrone.getId());
                    
                    preStmt.executeUpdate();
                    
                    p.setX_pos(tempDrone.getX_pos());
                    p.setY_pos(tempDrone.getY_pos());
                    
                    outputLog("Drone " + p.getId() + " moved to coordinates: " + p.getX_pos() + ", " + p.getY_pos() + ".");
                    
                    connection.close();
                    
                    newDrone = false;
                    
                    
                    }
            }
            if (newDrone == true){
            // Tries to add new drone details to table    
            // Sql for inserting data into table
            String sql = "INSERT INTO drone (id,name,xpos,ypos) VALUES ( ?, ?, ?, ?);";
            
            // Statement object
            PreparedStatement preStmt = connection.prepareStatement(sql);
            
            // Set the values of the object
            preStmt.setInt(1, tempDrone.getId());
            preStmt.setString(2, tempDrone.getName());
            preStmt.setInt(3, tempDrone.getY_pos());
            preStmt.setInt(4, tempDrone.getX_pos());
            
            // Executes the sql
            preStmt.executeUpdate();
            
            // Confirmation statement
            System.out.println("Drone Added");
            
            // closes connection
            connection.close();   
            
            // Adds drone to arraylist
            DroneDetails drone = new DroneDetails(tempDrone.getId(), tempDrone.getName(), tempDrone.getX_pos(), tempDrone.getY_pos(), true);
            drones.add(drone);
            
            outputLog("Drone " + drone.getId() + " registered.");
            
            }
            } catch (SQLException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    static void addFire(FireDetails tempFire) {
            
            int max = 0;
            
            for (FireDetails p : fires) {
                if (p.getId() > max) {
                    max = p.getId();
                }
            }
            
            int fireId = max + 1;
            
            // Tries to add new fire details to table
            try {
                
            // Uses URL, USERNAME and PASSWORRD  to connect to database
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
            // Sql for inserting data into table
            String sql = "INSERT INTO fire (id,isActive,intensity,xpos,ypos) VALUES ( ?, ?, ?, ?, ?);";
            
            // Statement object
            PreparedStatement preStmt = connection.prepareStatement(sql);
            
            // Set the values of the object
            preStmt.setInt(1, fireId);
            preStmt.setBoolean(2, true);
            preStmt.setInt(3, tempFire.getSeverity());
            preStmt.setInt(4, tempFire.getX_pos());
            preStmt.setInt(5, tempFire.getY_pos());
            
            // Executes the sql
            int rows = preStmt.executeUpdate();
            
            // Confirmation statement
            System.out.println(rows + " row(s) inserted");
            
            // closes connection
            connection.close();
            
            // Adds drone to arraylist
            FireDetails fire = new FireDetails(fireId,true,tempFire.getX_pos(), tempFire.getY_pos(), tempFire.getDroneId(), tempFire.getSeverity());
            fires.add(fire);
            
            // Confirmation log
            outputLog("New Fire Spotted at " + fire.getX_pos() + ", " + fire.getY_pos() + " with severity " + fire.getSeverity() + ".");
            
            } catch (SQLException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    
    public void deleteFire() throws SQLException, ClassNotFoundException {
        // Triggered by Delete Fire Button
        // intId is the id that'll be entered
        int intId = -1;
        
        // Sql for inserting data into table
        String sql = "UPDATE fire SET isActive = false WHERE id = ?";
        
        // Uses URL, USERNAME and PASSWORRD  to connect to database
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            
        // Statement object
        PreparedStatement preStmt = connection.prepareStatement(sql);
        
        while (true) {
            // Prompts user to enter drone id
            String enteredId = JOptionPane.showInputDialog(null, "Enter a Fire ID");
            // Stops iff canceled
            if (enteredId == null) {
                return;
            }
            try {
                // converts string to int
                intId = Integer.parseInt(enteredId);
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "ID must be numerical.");
            }
        }
        // tries to set and execute sql
        try {
            
            // Set the values of the object
            preStmt.setInt(1, intId);
            
            // Executes sql
            int rows = preStmt.executeUpdate();
            
            // Confirmation statement
            System.out.println(rows + " fire deleted");
            
            // closes connection
            connection.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void recallDrones() {
        // Checks if a recall is initiated
        if (recallStatus) {
            recallStatus = false;
            outputLog("Recall uninitiated.");
        } else {
            recallStatus = true;
            outputLog("Recall initiated.");
        }
    }
    
    public void moveDrone() {
        // Triggered by move drone button
        // Initialisation of variables, -0 does not exist as a coordinate
        int intId = -1;
        int newX = -0;
        int newY = -0;
        boolean droneExists = false;
        
        /*
        Opens Option Pane prompting for a Drone ID
        If cancel is pressed, null will be returned causing the loop to break
        otherwise it'll attempt to parse the ID to int, if this fails the user will be reprompted after an error message
        */
        while (true) {
            String enteredId = JOptionPane.showInputDialog(null, "Enter ID of drone to be moved.");
            if (enteredId == null) {
                return;
            }
            try {
                intId = Integer.parseInt(enteredId);
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "ID must be numerical.");
            }
        }
        
        // Searches for ArrayList to check if a drone with the ID entered exists
        // If drone is active then it is good to be moved and droneExists is changed to true
        for (DroneDetails p : drones) {
            if (p.getId() == intId) {
                if (p.getActive()) {
                    droneExists = true;
                }
            }
        }
        
        // If no drone exists that is active then droneExists will be false and the user will be given an error message
        if (!droneExists) {
            JOptionPane.showMessageDialog(null, "Drone with ID " + intId + " does not exist or is not active.");
            return;
        }
        
        // Opens option pane prompting user to enter an X position for the drone to be moved to
        while (true) {
            String enteredX = JOptionPane.showInputDialog(null, "Enter new X position for drone " + intId + ".");
            if (enteredX == null) {
                return;
            }
            try {
                newX = Integer.parseInt(enteredX);
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "ID must be numerical.");
            }
        }
        
        // Opens option pane prompting user to enter an X position for the drone to be moved to
        while (true) {
            String enteredY = JOptionPane.showInputDialog(null, "Enter new Y position for drone " + intId + ".");
            if (enteredY == null) {
                return;
            }
            try {
                newY = Integer.parseInt(enteredY);
                break;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "ID must be numerical.");
            }
        }
        
        // Removes drone id from hash map in case it's there
        newXPositions.remove(intId);
        newYPositions.remove(intId);
        
        // When all information has been inputted, ids and new positions are added to hash maps
        newXPositions.put(intId, newX);
        newYPositions.put(intId, newY);
        
        // Outputs message to confirm move
        outputLog("Drone " + intId + " will be moved to " + newX + ", " + newY + ".");
    }
    
    public void shutDown() {

        outputLog("Shut Down Commencing.");
        
        System.exit(0);
    }
    
    public static void outputLog(String message) {
        // Outputs message given through the output text area along with a newline
        outputText.append(message + "\n");
        // Moves scrollbar straight to bottom to make textarea act as a log
        outputText.setCaretPosition(outputText.getDocument().getLength());
    }

    @Override
    public void run() {
        // Runs constantly
        while (true) {
            try {
                // Sleeps thread for 10 seconds to repaint the map
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            // Repaints mapPanel
            mapPanel.repaint();
        }
    }
}

class DroneConnection extends Thread {
    // Sets up input and output streams for socket
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket clientSocket;
    
    public DroneConnection (Socket aClientSocket) {
        
        // Assigns streams to the socket and starts the thread run()
        try {
            clientSocket = aClientSocket;
            in = new ObjectInputStream( clientSocket.getInputStream());
            out =new ObjectOutputStream( clientSocket.getOutputStream());
            this.start();
	} catch(IOException e) {System.out.println("Connection:"+e.getMessage());}
    }
     
    @Override
    public void run() {
        try {
            
            String message = "";
            String clientMessage = "";
            
            // Movement variables if drone is outside of boundaries
            // New positions will be set if required
            boolean outOfBounds = false;
            boolean movementRequired = false;
            
            // Gets drone object from client and adds it to tempDrone object
            DroneDetails tempDrone = (DroneDetails)in.readObject();
            
            // Confirm drone object
            message = "confirmed";
            out.writeObject(message);
            
            // Receives how many fires there are and confirms receival
            Integer numFires = (Integer)in.readObject();
            out.writeObject(message);
            
            // Loops for how many fires there are and receives the fire objects
            // Sends fire object to addFire(); for it to be added, sends confirmation message
            if (numFires > 0) {
                for (int i = 0; i < numFires; i++) {
                    FireDetails tempFire = (FireDetails)in.readObject();
                    Server.addFire(tempFire);
                    message = "confirmed";
                    out.writeObject(message);
                }
            }
            
            // Checks if drone is in hashmaps for movements
            // If so sets movementRequired to true, updates drone X and Y positions
            for (Integer i : Server.newXPositions.keySet()) {
                if (i == tempDrone.getId()) {
                    movementRequired = true;
                    tempDrone.setX_pos(Server.newXPositions.get(i));
                    Server.newXPositions.remove(i);
                }
            }
            
            for (Integer i : Server.newYPositions.keySet()) {
                if (i == tempDrone.getId()) {
                    movementRequired = true;
                    tempDrone.setY_pos(Server.newYPositions.get(i));
                    Server.newYPositions.remove(i);
                }
            }
            
            // Check x positions, set if out of bounds
            if (tempDrone.getX_pos() > 100) {
                outOfBounds = true;
                tempDrone.setX_pos(80);
            } else if (tempDrone.getX_pos() < -100) {
                outOfBounds = true;
                tempDrone.setX_pos(-80);
            }
            
            // Check y positions, set if out of bounds
            if (tempDrone.getY_pos() > 100) {
                outOfBounds = true;
                tempDrone.setY_pos(80);
            } else if (tempDrone.getY_pos() < -100) {
                outOfBounds = true;
                tempDrone.setY_pos(-80);
            }
            
            // If a Recall is active it will respond to the client saying so
            // Recall is done first since it matters the most, position doesn't matter if it's being recalled to 0,0 regardless
            if (Server.ifRecall()) {
                message = "recall";
                out.writeObject(message);
                clientMessage = (String)in.readObject();
                if (clientMessage.equals("Recall Confirmed")) {
                    // If drone confirms recall, set the drone active to false
                    tempDrone.setActive(false);
                    tempDrone.setX_pos(0);
                    tempDrone.setY_pos(0);
                }
            } else if (movementRequired || outOfBounds) {
                // Sends move message and receives confirmation between object writes
                message = "move";
                out.writeObject(message);
                clientMessage = (String)in.readObject();
                out.writeObject(tempDrone.getX_pos());
                clientMessage = (String)in.readObject();
                out.writeObject(tempDrone.getY_pos());
                clientMessage = (String)in.readObject();
                
                // Messages outputed based on if the drone was moved or out of bounds
                // Not an if else because both messages could be required
                if (movementRequired) {
                    Server.outputLog("Drone " + tempDrone.getId() + " successfully moved.");
                }
                
                if (outOfBounds) {
                    Server.outputLog("Drone " + tempDrone.getId() + " outside of boundaries. Moved back.");
                }
            } else {
                // Otherwise just confirms to the client it received the object
                message = "confirmed";
                out.writeObject(message);
            }
            
            // Sends tempDrone to the addDrone function to get it in the ArrayList
            Server.addDrone(tempDrone);
            
            System.out.println(tempDrone);
            
            System.out.println("There are " + numFires + " new fires.");
            System.out.println("There are " + Server.fires.size() + " fires.");
            
        }catch (EOFException e){System.out.println("EOF:"+e.getMessage());
        } catch(IOException e) {System.out.println("readline:"+e.getMessage());
	} catch(ClassNotFoundException ex){ ex.printStackTrace();
	} finally{ try {clientSocket.close();}catch (IOException e){/*close failed*/}}
    }

    PreparedStatement prepareStatement(String sql) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
