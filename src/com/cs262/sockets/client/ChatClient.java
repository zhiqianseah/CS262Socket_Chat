//Help taken from http://cs.lmu.edu/~ray/notes/javanetexamples/
package com.cs262.sockets.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * Client side of the Chat Application. Each user starts a chat client and connect to the chat server
 * via a command-line input of (IP_address, Port Number). 
 * If connection fails, prompt the user for another (IP_address, Port number) until the connection succeeds.
 * Once connected, the chat client will read the user inputs, sends it to the chat server, and prints
 * the replies from the chat server.
 */

public class ChatClient {

    private static BufferedReader in;
    private static PrintWriter out;
    
    //List of accepted opcodes
    private static final String TOACCOUNT = ":send";
    private static final String TOGROUP = ":togroup";
    private static final String GROUP = ":group";    
	private static final String SIGNOUT = ":signout";    
	private static final String DELETE = ":delete";        
    //The cookie is used to maintain login status
    private static String cookie = null;
    private static final String APPVERSION = "ChatApp_v0.1";
	private static final String CREATE = ":create"; 
	private static final String LOGIN = ":login";

	
    /** Connect the chat client to the server via command-line inputs. It then create a separate thread to handle
     * the sending of messages (MessageSender class). This thread will continue to listen to messages from the server and 
     * print it to the screen. 
     * @throws IOException from sockets
     */
    private void connectToServer() throws IOException {

    	Socket socket = null;
    	
    	//repeat till user gives a valid IP and port number
    	while(socket == null){
	    	try {
	    		// Make connection and initialize streams
	        	System.out.print("Please key in the IP of the chat server:\n");
	        	Scanner scanner = new Scanner(System.in);	
	        	String serverAddress = scanner.nextLine();

	        	System.out.print("Please key in the port number:\n");
	        	String portnumber = scanner.nextLine();
	    		socket = new Socket(serverAddress, Integer.parseInt(portnumber));
	    	}
	    	catch(Exception e) {
	    		System.out.print("Error: " + e.getMessage() + "\n");    
	    		System.out.print("Please Try Again.\n");   
	    		
	    	}
    	}
    	
    	
    	//read the inputs from the socket
    	try{
            
	        in = new BufferedReader(
	                new InputStreamReader(socket.getInputStream()));
	        out = new PrintWriter(socket.getOutputStream(), true);
	        
	        //This creates a new thread that handles the sending of messages
    		MessageSender sender = new MessageSender();	        
	        // Consume the initial welcoming messages from the server
	        for (int i = 0; i < 4; i++) {
	        	String response = in.readLine();
	        	response = checkHeaders(response);
	        	System.out.print(response + "\n");
	        }
	        while(true) {
	        	String response = in.readLine();
	        	response = checkHeaders(response);
	        	
	            //check if the headers are correct
	            if (response == null){
	        		System.out.print("Chat Version incorrect.\n");
	                return;
	            }
        		System.out.print(response + "\n");

	        }
    	}catch(SocketException e){
    		System.out.print("Error: " + e.getMessage() + "\n");    
    		System.out.print("Please restart the chat application\n");    
    	
    	}finally {
    		socket.close();
    	}
    	
    	
    	
    	
    }
    
    /** Check that the message received from the server has valid headers. The message should have the correct
     * app number. If there is an op_code for setting or deleting the cookie, execute it accordingly on the client side.
     * Lastly, check that the message length is correct. Return the main content of the message
     * @param msg message received from the server
     * @return main content of the message (without the headers)
     */
    private String checkHeaders(String msg){
		//System.out.print(msg +"\n");
    	
    	//Check that version number is correct
    	if (msg.startsWith(APPVERSION)){
        	String stripped = msg.substring(APPVERSION.length()+1);  
        	
        	String[] splitted = stripped.trim().split(" ");
        	
        	//check if the message is fixing any cookies
        	if (splitted[0].startsWith("cookie:")) {
        		cookie = splitted[0].substring(7);

        		
        	}
        	else if (splitted[0].startsWith("clear_cookie")){
        		cookie = null;
        	}
        		
        			
            //check that message length is correct        			
        	int length = Integer.parseInt(splitted[1]);
        	stripped = stripped.substring(splitted[0].length() + 1 + splitted[1].length()+1);
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
    
    
    
    /** Separate thread to handle the sending of messages. This is necessary because a message may arrive while 
     * the current thread is waiting for a user-input. Runs in a infinite while loop until the user chooses to quit
     * the chat app. 
     *
     */
    private static class MessageSender implements Runnable {
        /**Creates a new thread and run it
         */
        public MessageSender() {
            (new Thread(this)).start();
        }

    	
        /** Run the message sender thread by listening to user command line inputs, sending it to the chat server. 
         * In an infinite while loop, listen to user input commands. check that the command correspond with the login status.
         * For example, sending a message requires the user to be logged in, while creating a new account requires the user to
         * be logged out. For commands that require logged in privileges, append the current cookie to the end of the message
         * Finally, send the message to the chat server.
         * Quit the chat client app if the command ":quit" is keyed. 
         */
        public void run() {

        	Scanner scanner = new Scanner(System.in);
    		//This will pause to get client input
        	
    		String command = scanner.nextLine();
    		
    		while(true)
    		{
	    		if (command.equals(":quit")){
	                System.exit(0);	        			
	    		} 
	    		
	    		//ensure that for operations that requires login, the user IS logged in
	    		//ensure that for operations that requires logout, the user IS logged out
	    		while(((command.startsWith(TOACCOUNT) || command.startsWith(TOGROUP)
	    				|| command.startsWith(GROUP) || command.startsWith(SIGNOUT)
	    				|| command.startsWith(DELETE))
	    				&& (cookie== null)  )
	    				|| ((command.startsWith(CREATE) || (command.startsWith(LOGIN)))
	    				&& (cookie != null))){
	    			
	    			if (cookie == null){
	    				System.out.print("Error: Not Logged in.\n");	
	    			} else {
	    				System.out.print("Error: Please Log out first!\n");	    				
	    			}
	        		command = scanner.nextLine();
	    		}
	    		
	    		//append the cookie to the end of the command
	    		if (command.startsWith(TOACCOUNT) || command.startsWith(TOGROUP)
	    				|| command.startsWith(GROUP) || command.startsWith(SIGNOUT)
	    				|| command.startsWith(DELETE)) {
	    			command = command + " " + cookie;
	    		}
	    		//send client input to server
	    		SendOverNetwork(command);
	    		
	    		command = scanner.nextLine();
    		}
        }
        

        /** Add header information, such as version number and length of message and 
         * send it to the server
         * @param message message to be sent over to the server
         */
        private void SendOverNetwork(String message) {
    		out.println(APPVERSION + " " +message.length()+ " " + message);
        }
    }
    

    /** main function to start a chat client. 
     * Create a new ChatClient instance, and connect to the server.
     * @throws Exception throws the exception from connectToServer
     */
    public static void main(String[] args) throws Exception {
    	ChatClient client = new ChatClient();
        client.connectToServer();
     
    }
}