README file for Socket Chat application

Check out the source code and build the project, then follow the steps below to register and launch the chat application.

1. Start server
Open a new command window 
-cd to ~workspace/bin
-java com.cs262.sockets.server.ChatServer

3. Start Client
Open a new command window
-cd to ~workspace/bin
-java com.cs262.sockets.client.ChatClient

4. Follow step 3 to start more clients

5. Use the application with following commands:
:create <username> <password>
:login <username> <password>
:send <recipient> <message string>
:signout
:delete
:listaccount
:listgroup
:group <groupname> <list of other group members>
:togroup <groupname> <message string>
:quit
:help