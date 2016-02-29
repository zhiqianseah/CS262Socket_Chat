//Adapted from http://cs.lmu.edu/~ray/notes/javanetexamples/
package com.cs262.sockets.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ChatClient {

    private static BufferedReader in;
    private static PrintWriter out;
    
    private static final String TOACCOUNT = ":send";
    private static final String TOGROUP = ":togroup";
    private static final String GROUP = ":group";    
	private static final String SIGNOUT = ":signout";    
	private static final String DELETE = ":delete";        
    //This is essentially a cookie to maintain login status
    private static String cookie = null;
    private static final String APPVERSION = "ChatApp_v0.1";
	private static final String CREATE = ":create"; 
	private static final String LOGIN = ":login";

	
    public void connectToServer() throws IOException {
        // set as local address for now

    	System.out.print("Please key in the IP of the chat server:\n");
    	Scanner scanner = new Scanner(System.in);
		//This will pause to get client input
    	
		//String command = scanner.nextLine();
    	
    	String serverAddress = scanner.nextLine();

        // Make connection and initialize streams
        Socket socket = new Socket(serverAddress, 2001);
    	
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
        		/*
	        	if (response.contains("Logged in as:")) {
	            	
	            	//Save the logged in info as the cookie
	            	cookie = response.substring(response.indexOf("Logged in as:")+13);
	            	//System.out.print("cookie is:"+cookie +"\n");
	            }
	        	else if (response.contains("Logged off as:") || response.contains("Account Deleted:")) {
	        		cookie = null;
	            	//System.out.print("cookie is cleared.\n");
	        	}
	        	*/
	        }
    	}catch(SocketException e){
    		System.out.print("Error: " + e.getMessage() + "\n");    
    		System.out.print("Please restart the chat application\n");    
    	
    	}finally {
    		socket.close();
    	}
    	
    	
    	
    	
    }
    
    private String checkHeaders(String msg){
		System.out.print(msg +"\n");
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
    
    
    
    private static class MessageSender implements Runnable {
        public MessageSender() {
            (new Thread(this)).start();
        }

    	
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
        
        //Add header information, such as version number and length of message
        //Send message to server
        private void SendOverNetwork(String message) {
    		out.println(APPVERSION + " " +message.length()+ " " + message);
        }
    }
    
    public static void main(String[] args) throws Exception {
    	ChatClient client = new ChatClient();
        client.connectToServer();
     
    }
}