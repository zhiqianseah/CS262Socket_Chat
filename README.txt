README file for Socket Chat application

Check out the source code and build the project, then follow the steps below to register and launch the chat application.

1. Start server
Open a new command window 
-cd to ~workspace/bin
-java com.cs262.sockets.server.ChatServer
-input the port number as user-input

3. Start Client
Open a new command window
-cd to ~workspace/bin
-java com.cs262.sockets.client.ChatClient
-input the server IP address and port number as user-input

4. Follow step 3 to start more clients

5. Use the application with following commands:
    5.1 Create a new account
	:create <username> <password>

    5.2 Login to an existing account
	:login <username> <password>

    5.3 Send a message string to a recipient. 
	:send <recipient> <message string>

    5.4 Sign out of a logged-in account
	:signout

    5.5 Delete an existing account
	:delete

    5.6 List all user accounts. Allow an optional wildcard entry
	:listaccount 
	:listaccount <wildcard>

    5.7 List all chat groups
	:listgroup
	:listgroup <wildcard>

    5.8 Create a new chat group with name <groupname> and members <list of members>. The creator is automatically added to the group
	:group <groupname> <list of other group members>

    5.9 Send a message to a group
	:togroup <groupname> <message string>

    5.10 Quit the chat app
	:quit

    5.11 Print the available commands
	:help