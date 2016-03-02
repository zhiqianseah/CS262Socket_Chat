//Help taken from http://cs.lmu.edu/~ray/notes/javanetexamples/
package com.cs262.sockets.server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {

	
	private static HashMap<String, String> accounts;  //username and password
	private static HashMap<String, PrintWriter> acc_sockets;  //username and sockets
	private static HashMap<String, List<String>> groups;
	private static HashMap<String, List<String>> undeliveredMessages;
	private static HashMap<String, String> accountStatus;

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

    public static void main(String[] args) throws Exception {
    	
        accounts = new HashMap<String, String>();
        acc_sockets = new HashMap<String, PrintWriter>();
        groups = new HashMap<String, List<String>>();
        undeliveredMessages = new HashMap<String, List<String>>();
        accountStatus = new HashMap<String, String>();
 
        
        //Get a user-input port number for the chat server
    	System.out.print("Please key in the port number:\n");
    	Scanner scanner = new Scanner(System.in);
    	String portnumber = scanner.nextLine();
    	
        
        int clientNumber = 0;
        
        //create the server socket using the port number
        ServerSocket listener = new ServerSocket(Integer.parseInt(portnumber));
        System.out.println("The chat server is running.");
        try {
            while (true) {
                new Chat(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }
   

    private static class Chat extends Thread {
        private Socket socket;
        private int clientNumber;

        public Chat(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }


        public void run() {
            try {

                // Decorate the streams so we can send characters
                // and not just bytes.  Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send a welcome message to the client.
            	SendOverNetwork(out, "Welcome to the Chat App");
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
                output = "Account Deleted:"+msgs[1];
                SendOverNetworkClearCookie(out, output);
                
            //return a list of the accounts on the server
            } else if (LISTACCOUNT.equals(msgs[0]) && msgs.length <= 2){
            	
            	if (msgs.length == 1) {
            		output = accounts.keySet().toString();
            	}
            	//wildcard search. eg return "[user1, user2]" for ":listaccount us*"
            	else {
            		List<String> searchResult = new ArrayList<String>();
            		for (String accountName : accounts.keySet()){
            			if (accountName.matches(msgs[1].replace("*", ".*?"))){
            				searchResult.add(accountName);
            			}
            		}
            		
            		output = searchResult.toString();
            	}
                SendOverNetwork(out, output);
            	
            //create a user account with <username> <password>
            }else if (CREATE.equals(msgs[0]) && msgs.length == 3){            	
                if(createAccount(msgs[1], msgs[2]))
                {
                	output = "Account Created. Logged in as:"+ msgs[1];

					accountStatus.put(msgs[1], "ONLINE");
					

					acc_sockets.put(msgs[1], out);
	                SendOverNetworkCookie(out, output, msgs[1]);
                } else {
                	output = "Username has been taken";
                    SendOverNetwork(out, output);
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
                SendOverNetworkCookie(out, output, msgs[1]);
            //sign out of an account
            } else if (SIGNOUT.equals(msgs[0]) && msgs.length == 2) {
            	output = "Logged off as: "+msgs[1];
            	signout(msgs[1]);
            	SendOverNetworkClearCookie(out, output);
            	
            //list the groups that are present on the server
            } else if (LISTGROUP.equals(msgs[0])&& msgs.length <= 2){
            	
            	if (msgs.length == 1) {
            		output = groups.keySet().toString();
            	}
            	
            	//wildcard search. eg return "[user1, user2]" for ":listgroup us*"
            	else {
            		List<String> searchResult = new ArrayList<String>();
            		for (String accountName : groups.keySet()){
            			if (accountName.matches(msgs[1].replace("*", ".*?"))){
            				searchResult.add(accountName);
            			}
            		}
            		output = searchResult.toString();
            		
            	}
                SendOverNetwork(out, output);
            	
            //send a message to a recipient account
            } else if (TOACCOUNT.equals(msgs[0])&& msgs.length >= 4){
            	String receiver = msgs[1];
            	String sender = msgs[msgs.length-1];
            	String message = sender+":";
            	for (int x =2; x< msgs.length-1; x++){
            		message = message+" "+msgs[x];
            	}
            	output = sendChatMessage(receiver, message);
                SendOverNetwork(out, output);

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
                   SendOverNetwork(out, output);
        	   }
            } else if (GROUP.equals(msgs[0]) && msgs.length >= 3){
          	   	List<String> groupMembers = new ArrayList<String>();
            	for (int x =2; x< msgs.length; x++){
            		groupMembers.add(msgs[x]); 

            	}
        	   groups.put(msgs[1], groupMembers);
        	   output = "Group Created";
               SendOverNetwork(out, output);

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
                SendOverNetwork(out, output);
            } 
            //log("Returning output: "+ output +"\n");  

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
    		out.println(APPVERSION + " no_cookie " + message.length()+ " " + message);
        }
 
        //Add header information, such as version number and length of message
        //Send message to server
        private void SendOverNetworkCookie(PrintWriter out, String message, String cookie) {
    		out.println(APPVERSION + " cookie:" + cookie + " " + message.length()+ " " + message);
        }  
     
        //Add header information, such as version number and length of message
        //Send message to server
        private void SendOverNetworkClearCookie(PrintWriter out, String message) {
    		out.println(APPVERSION + " clear_cookie " + message.length()+ " " + message);
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
            

        //output logging to screen
        private void log(String message) {
            System.out.println(message);
        }
    }
}