//Adapted from http://cs.lmu.edu/~ray/notes/javanetexamples/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ChatServer {

	
	private static HashMap<String, String> accounts;  //username and password
	private static HashMap<String, PrintWriter> acc_sockets;  //username and sockets
	private static HashMap<String, List<String>> groups;
	private static HashMap<String, List<String>> undeliveredMessages;
	private static HashMap<String, String> accountStatus;

	// TODO: List of command, create enum for these
	private static final String CREATE = ":create"; 
	private static final String LISTACCOUNT = ":listaccount";
	private static final String LOGIN = ":login";
	private static final String LISTGROUP = ":listgroup";
	private static final String TOACCOUNT = ":send";
	private static final String TOGROUP = ":togroup";
	private static final String GROUP = ":group";
	private static final String SIGNOUT = ":signout";
	private static final String DELETE = ":delete"; 
	private static final String HELP = ":help"; 	   
	
    private static final String APPVERSION = "ChatApp_v0.1";
    /**
     * Application method to run the server runs in an infinite loop
     * listening on port 9898.  When a connection is requested, it
     * spawns a new thread to do the servicing and immediately returns
     * to listening.  The server keeps a unique client number for each
     * client that connects just to show interesting logging
     * messages.  It is certainly not necessary to do this.
     */
    public static void main(String[] args) throws Exception {
    	
        accounts = new HashMap<String, String>();
        acc_sockets = new HashMap<String, PrintWriter>();
        groups = new HashMap<String, List<String>>();
        undeliveredMessages = new HashMap<String, List<String>>();
        accountStatus = new HashMap<String, String>();
        
        System.out.println("The chat server is running.");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(9898);
        try {
            while (true) {
                new Chat(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }
   

    /**
     * A private thread to handle capitalization requests on a particular
     * socket.  The client terminates the dialogue by sending a single line
     * containing only a period.
     */
    private static class Chat extends Thread {
        private Socket socket;
        private int clientNumber;

        public Chat(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }

        /**
         * Services this thread's client by first sending the
         * client a welcome message then repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
            try {

                // Decorate the streams so we can send characters
                // and not just bytes.  Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
            	SendOverNetwork(out, "Hello, you are client #" + clientNumber + ".");
            	SendOverNetwork(out, "Please log in with \":login <username> <password>\" or create an account via "
                		+ "\":create <username> <password>");
            	SendOverNetwork(out, "Enter :help to get the list of commands");
            	SendOverNetwork(out, "Enter command: ");
                
                
                
                
                
                
                // Get messages from the client, line by line
                while (true) {
                    String input = in.readLine();
                    log("Receive from client# " + clientNumber + ": " + input);
                    if (input == null || input.equals(".")) {
                        break;
                    }
                    parseMessage(input, out);
                }
            } catch (IOException e) {
                log("Error handling client# " + clientNumber + ": " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client# " + clientNumber + " closed");
            }
        }

        //Parse the input string from the user to decide on the next course of action
        private void parseMessage(String input, PrintWriter out)
        {
        	//This output message will be returned to the client
        	String output = input;
        	
            input = checkHeaders(input);
            
            //check if the headers are correct
            if (input == null){
            	SendOverNetwork(out, "Chat Version incorrect.");
                
                return;
            }
            String[] msgs = input.trim().split(" ");

            boolean login_bool = false;
            
            //Deleting the account. remove the username from all hashtables
            if (DELETE.equals(msgs[0]) && msgs.length == 2) {
                accounts.remove(msgs[1]);
                accountStatus.remove(msgs[1]);
                undeliveredMessages.remove(msgs[1]);
                
            //return a list of the accounts on the server
            } else if (LISTACCOUNT.equals(msgs[0])){
            	output = accounts.keySet().toString();
            	
            //create a user account with <username> <password>
            }else if (CREATE.equals(msgs[0]) && msgs.length == 3){            	
                if(createAccount(msgs[1], msgs[2]))
                {
                	output = "Account Created. Logged in as:"+ msgs[1];

					accountStatus.put(msgs[1], "ONLINE");
					

					acc_sockets.put(msgs[1], out);
					
                } else {
                	output = "Username has been taken";
                }
          
            //login to an existing account with <username> <password>
            }else if (LOGIN.equals(msgs[0])&& msgs.length == 3){
            	login_bool = login(msgs[1], msgs[2]);
                if(login_bool == true)
                {
                	output = "Logged in as:" + msgs[1];
        			accountStatus.put(msgs[1], "ONLINE");
					acc_sockets.put(msgs[1], out);
					
                } else {
                	output = "Login Failed";
                }
          
            //sign out of an account
            } else if (SIGNOUT.equals(msgs[0]) && msgs.length == 2) {
            	output = "Logged off as: "+msgs[1];
            	signout(msgs[1]);
            	
            //list the groups that are present on the server
            } else if (LISTGROUP.equals(msgs[0])){
            	output = groups.keySet().toString();
            	
            //send a message to a recipient account
            } else if (TOACCOUNT.equals(msgs[0])&& msgs.length >= 4){
            	String receiver = msgs[1];
            	String sender = msgs[msgs.length-1];
            	String message = sender+":";
            	for (int x =2; x< msgs.length-1; x++){
            		message = message+" "+msgs[x];
            	}
            	output = sendChatMessage(receiver, message);

            } else if (TOGROUP.equals(msgs[0]) && msgs.length >= 4){
            	String sender = msgs[msgs.length-1];
            	String message = "Group <"+msgs[1]+">:";
            	for (int x =2; x< msgs.length-1; x++){
            		message = message+" "+msgs[x];
            	}
            	
         	   if (groups.containsKey(msgs[1])){
        		   List<String> group = groups.get(msgs[1]);
        		   for (String receiver : group){
        			   
        			   //Skip the sender. he doesn't need to get his own message back
        			   if (receiver.equals(sender)){
        				   continue;
        			   }
        			   
                   		output = sendChatMessage(receiver, message);        			   
        			   
        		   }
        		   output = "";
        	   }else {
        		   output = "Group doesn't exist "+msgs[1];
        	   }
            } else if (GROUP.equals(msgs[0]) && msgs.length >= 3){
          	   	List<String> groupMembers = new ArrayList<String>();
            	for (int x =2; x< msgs.length; x++){
            		groupMembers.add(msgs[x]); 

            	}
        	   groups.put(msgs[1], groupMembers);

            }else if (HELP.equals(msgs[0])){
            	SendOverNetwork(out, "Commands Available:");
            	SendOverNetwork(out, ":create <username> <password>");
            	SendOverNetwork(out, ":login <username> <password>");
            	SendOverNetwork(out, ":send <recipient> <message string>");
            	SendOverNetwork(out, ":signout");
            	SendOverNetwork(out, ":delete");
            	SendOverNetwork(out, ":listaccount");
            	SendOverNetwork(out, ":listgroup");
            	SendOverNetwork(out, ":group <groupname> <list of other group members>");
            	SendOverNetwork(out, ":togroup <groupname> <message string>");
            	SendOverNetwork(out, ":quit");
            	SendOverNetwork(out, ":help");

            	
            	output = "";
            } else {
            	output = "Command not found / Length of input is incorrect";
            } 
            //log("Returning output: "+ output +"\n");  
            SendOverNetwork(out, output);
            //out.println("Enter command: ");
            
            
            //Send user their offline messages when they first log in
            if (login_bool == true && undeliveredMessages.containsKey(msgs[1]))
            {
            	SendOverNetwork(out, "You have received messages while you were away:");
            	
            	List<String> messages = undeliveredMessages.get(msgs[1]); 
				for (int x = 0; x< messages.size(); x++)
				{
					SendOverNetwork(out, messages.get(x));
				}
				undeliveredMessages.remove(msgs[1]);
            }
        }
        
        
        private String checkHeaders(String msg){
        	
        	//Check that version number is correct
        	if (msg.startsWith(APPVERSION)){
            	String stripped = msg.substring(APPVERSION.length()+1);  
            	
            	//check that message length is correct
            	String[] splitted = stripped.trim().split(" ");
            	int length = Integer.parseInt(splitted[0]);
            	stripped = stripped.substring(splitted[0].length()+1);
            	if (stripped.length() == length){
            		return stripped;
            	}
            	else{
            		return null;
            	}
            		
        	} 
        	else {
        		return null;
        	}

        }
        
        
 
        //Add header information, such as version number and length of message
        //Send message to server
        private void SendOverNetwork(PrintWriter out, String message) {
    		out.println(APPVERSION + " " + message.length()+ " " + message);
        }
        
        
        //create an account with username and password
        //return false if the account has been taken
        private boolean createAccount(String name, String password){
            log("Creating User Account: "+ name +" "+password+"\n");
            boolean success = false; 

        	if (!accounts.containsKey(name)){
        		accounts.put(name, password);
     	   		accountStatus.put(name, "ONLINE"); 
     	   		success = true;
        		
        	}
        	return success;
        }
        
        private String sendChatMessage(String receiver, String message) {
        	String output = "";
        	//Check if this account exist
        	if (accountStatus.containsKey(receiver)){
        		
        		
        		//Check if the user is online
        		if (accountStatus.get(receiver).equals("ONLINE"))
        		{
        			//get the PrintWriter of the online recipient, and send him the message
        			PrintWriter recipient_writer = acc_sockets.get(receiver);
        			//recipient_writer.println(message);
        			SendOverNetwork(recipient_writer, message);
        		}
        		
        		//if the user is offline, store it in a hashmap
        		else
        		{
     			   if (undeliveredMessages.containsKey(receiver)){
    				   List<String> messages = undeliveredMessages.get(receiver);
    				   messages.add(message);
    				   undeliveredMessages.put(receiver, messages);
    			   }else {
    				   List<String> messages = new ArrayList<String>();
    				   messages.add(message);
    				   undeliveredMessages.put(receiver, messages);
    			   }
        		}
        		
        		//when the message is successfully sent to the receiver
        		//return an empty message to the original sender 
    			output = "Message Sent";
        	}
        	else
        	{
        		output = "Recipient does not exist.";
        	}
        	
        	return output;
        }
        
        //Check login credentials
        private boolean login(String name, String password){
            log("Logging in with: "+ name +" "+password+"\n");
        	
            //check if the account exist
        	if (accounts.containsKey(name))
        	{
        		//check if the password is correct
        		if (accounts.get(name).equals(password)){
        			return true;
 
        		} else {
        			return false;
        		}
        			
        	}else {
     	   		return false;
        	}
        	
        }        
        
        private void signout(String name){
            log("Signing out with: "+ name +"\n");

        	accountStatus.put(name, "OFFLINE");
        	
        }      
            
        
        /**
         * Logs a simple message.  In this case we just write the
         * message to the server applications standard output.
         */       
        private void log(String message) {
            System.out.println(message);
        }
    }
}