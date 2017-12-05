# ActiveMQ-Connection-using-JAVA

My usual coding mainly involves searching in StackOverflow for quick solutions and easy understanding of things and then, building my solutions based on that or in some cases copying the same. When I started with this project on connecting KEPServer to Java and then to ActiveMQ (After making the necessary modifications), like all the other times, I searched online and I found very limited resources explaining each step I wanted. 

In this repository, there are all the required files which we need to receive data from KEPServer or any other OPC-UA protocol and then publish it to ActiveMQ using MQTT and JMS protocols. 

Subscription from OPC-UA can be implemented mostly by using the same examples provided in the [eclipse-milo project](https://github.com/eclipse/milo). Following it, we can quickly recive the subscribed values in the subscription-callback function. Now, to connect to ActiveMQ I referred to [eclipse-paho](https://www.eclipse.org/paho/). 


ActiveMQ provides a variety of different messaging patterns. While queues and topics are the most famous ones, virtual topics can combine the best of both worlds: multiple consumers with their own dedicated queue. Can see this in OutputHandler.java

Server, client certificates and keystores can be generated using the following commands.  
 
1-Create a keystore for the broker SERVER 
$ keytool -genkey -alias amq-server -keyalg RSA -keysize 2048 -validity 90 -keystore amq-server.ks 
 
2-Export the broker SERVER certificate from the keystore 
$ keytool -export -alias amq-server -keystore amq-server.ks -file amq-server_cert 
 
3-Create the CLIENT keystore 
$ keytool -genkey -alias amq-client -keyalg RSA -keysize 2048 -validity 90 -keystore amq-client.ks 
 
4-Import the previous exported broker's certificate into a CLIENT truststore 
$ keytool -import -alias amq-server -keystore amq-client.ts -file amq-server_cert 
 
5-If you want to make trusted also the client, you must export the client's certificate from the keystore 
$ keytool -export -alias amq-client -keystore amq-client.ks -file amq-client_cert 
 
6-Import the client's exported certificate into a broker SERVER truststore 
$ keytool -import -alias amq-client -keystore amq-server.ts -file amq-client_cert 
 
To see whether the correct keystore is attached, ie clients key in servers store and vice versa, use the command.  
 
 keytool -list -keystore amq-client.ks 
 keytool -list -keystore amq-server.ts 
 
Should be same and similarly, 
 
 keytool -list -keystore amq-client.ts 
 keytool -list -keystore amq-server.ks 

Once this is done, we can connect to the port 8883 (default, can change in activeMQ.xml file). I have provided a function using MQTT protocol, which is widely used for lightweight message transport and one using the Java Message Service Protocol. 


**To access the ActiveMQ webclient, make sure that useJmx="false" is removed from the apache.xml.

