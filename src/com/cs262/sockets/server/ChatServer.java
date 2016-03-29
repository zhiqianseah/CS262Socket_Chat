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

/**
 * Server side of the Socket Chat Application. Initialized by a command-line input of the port number
 * to listen to. The main thread will then listen to connections from chat clients. For each client
 * connection, the server will spawn a new thread of class 'chat' that will read the client inputs and respond accordingly.
 *
 */
public class ChatServer {

	/**Hash table to store user accounts. Used for login verification
	 * Key = username. Value = password
	 */
	private static HashMap<String, String> accounts; 
	
	/**Hash table to store client sockets. Used to send messages back to the client
	 * Key = username. Value = socket
	 */
	private static HashMap<String, PrintWriter> acc_sockets;  //username and sockets
	

	/**Hash table to store users in chat groups. 
	 *	Key = group name. Value = list of users in the group
	 */
	private static HashMap<String, List<String>> groups;
	

	/**Hash table to store messages received when a user is offline
	 *	key = username. Value = list of messages received when offline
	 */
	private static HashMap<String, List<String>> undeliveredMessages;
	

	/**Hash table to store the current status of the account. 
	 *	key = username. Value = one of [OFFLINE, ONLINE]
	 */
	private static HashMap<String, String> accountStatus;

	
    //List of accepted opcodes	
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
	
	private static final String ONLINE = "ONLINE"; 	   
	private static final String OFFLINE = "OFFLINE"; 	 	
	
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
   

    /**
     * When a new client is connected to the chat server, a new chat thread is spawned to service the client.
     * This chat class is initialized by the client socket and an assigned client number. A welcome message
     * is sent to the client to notify that the connection has been established, and to prompt the user
     * for commands to the chat server. The chat thread then listens for incoming client commands, and services
     * the commands accordingly. The thread is ended when the connection is broken, such as by the user closing
     * the chat app, or network disruption.
     */
    private static class Chat extends Thread {
        private Socket socket;
        private int clientNumber;

        /** Initialization of a new chat thread. Store the socket and client number internally, and log
         * the initialization.
         * @param socket Socket that the client is connecting from.
         * @param clientNumber An assigned client number to differentiate clients easily for logging/debugging purposes
         */
        public Chat(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }



        /** Run the chat thread. The chat thread will first store the server's input stream and client's output stream
         * to be used for server-client communication. It then sends a welcome message to the client to notify that
         * the connection is successful, and to prompt the user for chat commands.
         * The thread then goes into an infinite while loop to read inputs from the client, process it accordingly by calling
         * parseMessage. When the connection is broken, such as by the client closing the chat app or a network disruption,
         *  an exception is thrown and the thread is ended.
         * 
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
                    log("Couldn't close a socket");
                }
                log("Connection with client# " + clientNumber + " closed");
            }
        }

        //Parse the input string from the user to decide on the next course of action
        /** Process a command sent by the client.
         * The input message is first checked for valid headers, and split via whitespaces into a message array. 
         * The main content of the message is then processed based on the op_code of the main message. The opcode is always 
         * in the first element of the message array.
         * If a command requires user privileges (such as sending messages or deleting account), the message must contain
         * a cookie from the client to authenticate the user. For simplicity, the cookie is also the client username. The cookie 
         * is always stored at the last element of the message array.
         * 
         * @param input message sent by the client 
         * @param out Socket output stream for the client
         */
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

					accountStatus.put(msgs[1], ONLINE);
					

					acc_sockets.put(msgs[1], out);
	                SendOverNetworkCookie(out, output, msgs[1]);
                } else {
                	output = "Username has been taken";
                    SendOverNetwork(out, output);
                }
          
            //login to an existing account with <username> <password>
            }else if (LOGIN.equals(msgs[0])&& msgs.length == 3){
            	login_bool = check_credentials(msgs[1], msgs[2]);
                if(login_bool == true)
                {
                	output = "Logged in as:" + msgs[1];
        			accountStatus.put(msgs[1], ONLINE);
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
        
        
        /** Check if the headers of the client side message is valid. There are 2 levels of checks. 
         * First, the chat app version number has to be correct. 
         * Secondly, the message length has to be correct.
         * @param msg client-side message that will be checked.
         * @return the message with the headers removed if the message header is correct. Null if there
         * is a problem with the headers.
         */
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
        /**Pad the message with a header that does not modify the cookies and send it over the network.
         * The header contains the app version number, an opcode that says that this server reply does not
         * modify the cookie, 
         * and the length of the message (to be used as a checker on the client side).
         * This function is used for commands that does not involve changing on login status, such as sending 
         * messages or listing groups. 
         * @param out Client Socket that message will sent to
         * @param message message that will be sent to client
         */
        private void SendOverNetwork(PrintWriter out, String message) {
    		out.println(APPVERSION + " no_cookie " + message.length()+ " " + message);
        }
 

        /**Pad the message with a header to set cookies and send it over the network.
         * The header contains the app version number, an opcode to set the cookie followed by the cookie, 
         * and the length of the message (to be used as a checker on the client side).
         * This function is used when the client signs in to the chat application, either via
         * a successful ":login" or "create" command. This will set the cookie on the client side, so that the client 
         * can use this as an authentication device when performing commands that requires the user to be 
         * logged in.
         * @param out Client Socket that message will sent to
         * @param message message that will be sent to client
         * @param cookie cookie to be set at client side
         */
        private void SendOverNetworkCookie(PrintWriter out, String message, String cookie) {
    		out.println(APPVERSION + " cookie:" + cookie + " " + message.length()+ " " + message);
        }  
     

        /**Pad the message with a header to clear cookies and send it over the network.
         * The header contains the app version number, an opcode to clear the cookie on the client side, 
         * and the length of the message (to be used as a checker on the client side).
         * This function is only used when the client chooses to exit the chat application, either via
         * a successful ":signout" or ":delete" command. This will clear the cookie on the client side so 
         * that the client cannot send any operations that requires login.
         * @param out Client Socket that message will sent to
         * @param message message that will be sent to client
         */
        private void SendOverNetworkClearCookie(PrintWriter out, String message) {
    		out.println(APPVERSION + " clear_cookie " + message.length()+ " " + message);
        }     
        

        /** Create an account with (username, password)
         *  Check if the username has been taken. If it has not been taken, add the (username, password) 
         *  into the accounts table, and set the user as "ONLINE". This is because the chat server will automatically
         *  log the client in after he creates an account.  
         * @param name Username to be created
         * @param password password associated to the username.
         * @return false if the account name has already name. True otherwise
         */
        private boolean createAccount(String name, String password){
            log("Creating User Account: "+ name +" "+password+"\n");
            boolean success = false; 

        	if (!accounts.containsKey(name)){
        		accounts.put(name, password);
     	   		accountStatus.put(name, ONLINE); 
     	   		success = true;
        		
        	}
        	return success;
        }
        
        /** Send a message to another user (receiver). A few levels of checks has to be done.
         * Firstly, check if the receiver is a valid user by if the username exist in the accounts table.
         * Secondly, check if the receiver is online. If the user is online, the chat server can send the 
         * message to the receiver directly. If the receiver is offline, save the message in the 
         * undeliveredMessages hash table so that the receiver can receive when he logs back in. Returns a message
         * for the action performed back. This ultimately be sent back to the sender chat client.
         * @param receiver user who will receive the message
         * @param message message to be sent to be receiver
         * @return "Message Sent" if the message is successful sent (offline or online), 
         * "Recipient does not exist" if the receiver cannot be found. 
         */
        private String sendChatMessage(String receiver, String message) {
        	String output = "";
        	//Check if this account exist
        	if (accountStatus.containsKey(receiver)){
        		
        		
        		//Check if the user is online
        		if (accountStatus.get(receiver).equals(ONLINE))
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
        

        /** Check if the login credentials are valid. This is a 2-level check.
         *  Firstly, the username has to exist in the accounts.
         *  Secondly, the password given has to match the password in the accounts.
         * @param name username received by chat server
         * @param password password associated with the username
         * @return true if the username and password is correct, false otherwise
         */
        private boolean check_credentials(String name, String password){
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
        
        /** Called when a message with opcode SIGNOUT is received by the chat server. 
         * Sign the user out of the system by setting accountstatus to offline.
         * @param name username that will be signed out of the chat application.
         */
        private void signout(String name){
            log("Signing out with: "+ name +"\n");

        	accountStatus.put(name, OFFLINE);
        	
        }      
            


        /** output the logging to screen
         * @param message Log message that will be displayed on screen
         */
        private void log(String message) {
            System.out.println(message);
        }
    }
}