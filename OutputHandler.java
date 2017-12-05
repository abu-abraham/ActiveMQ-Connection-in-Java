

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OutputHandler extends Thread {
	private  MqttClient sampleClient = null;
	private  ArrayList<String> buffer = new ArrayList<String>();
	private  boolean sizeExceded = false;
	private  String bufferFileName = "dataFiles/output";
	private  long limit = 100000;	
	private int oldestFileIndex = 0;
	private int currentFileIndex = 0;
	private int noOfFiles;
	private final Logger logger = LoggerFactory.getLogger(OutputHandler.class);
	private String op = "";
	private long fileOpCOunter = 0;
	private Date ty = null;
	private String topic;

	public OutputHandler() {
		super();
		start();
		this.topic = PropertyFetcher.getValue("topic");
		this.noOfFiles = Integer.parseInt(PropertyFetcher.getValue("sizeInMB"))/9;
	}
	
	
	
	private void writeToFile() {
		System.out.println("Writting to file");
		if (ty!=null)
			System.out.println(new Date().getTime() - ty.getTime());
		ty = new Date();
		this.fileOpCOunter+=1;
		FileWriter fw;
		try {
			fw = new FileWriter(this.bufferFileName+this.currentFileIndex+".txt",false);
			this.buffer.forEach((item)->{
				try {
					fw.write(item);
					fw.write("\n");
				}catch(IOException ioe){
					logger.error("Failed to write into buffer file " + ioe.getMessage());
			}});		    
		    fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


		this.currentFileIndex= (this.currentFileIndex+1)%this.noOfFiles;
	}
	
	private ArrayList<String> getFromFile() {
		if (this.fileOpCOunter == 0) {
			this.sizeExceded = false;
		}
		this.fileOpCOunter-=1;
		File bufferFile= new File(this.bufferFileName+this.oldestFileIndex+".txt");
		BufferedReader reader;
		ArrayList<String> executionBuffer = new ArrayList<String>();
		String line;
		try {
			reader = new BufferedReader(new FileReader(bufferFile));
			while ((line = reader.readLine())!= null) {
				executionBuffer.add(line);
				
            }
			reader.close();
		} catch (Exception e) {
			logger.error("Failed to get from buffer file " + e.getMessage());
		}    
		this.oldestFileIndex = (this.oldestFileIndex+1)%this.noOfFiles;
                
		return executionBuffer;
		
	}
	
	
	public  void publishMessage(UaMonitoredItem item, DataValue value) {
		String content = new JsonResponse("" + item.getReadValueId().getNodeId().getIdentifier(),
				"" + value.getValue().getValue(), item.getStatusCode().isGood(), value.getServerTime().getJavaTime())
						.toJson();
		if (this.buffer.size() >= this.limit) {
			this.writeToFile();
			this.sizeExceded = true;
			this.buffer = new ArrayList<String>();
		}
		
		

		this.buffer.add(content);
	}
	
	public void run() {
		ArrayList<String> executionBuffer = new ArrayList<String>();
		while(true) {
		int qos = 2;  
		try {
			if (this.sampleClient == null || !this.sampleClient.isConnected()) {
				
				String broker = PropertyFetcher.getValue("sslUrl");
				String clientId = "/vroc/";
				this.sampleClient = new MqttClient(broker, clientId);
				MqttConnectOptions connOpts = new MqttConnectOptions();
				connOpts.setSocketFactory(SecureSocketUtil.getSocketFactory());
				sampleClient.connect(connOpts);
				logger.info("Connected to broker: " + broker);
				
			}
			if (this.sizeExceded)
				executionBuffer = this.getFromFile();
			else {
				executionBuffer = new ArrayList<String>(this.buffer);
				this.buffer = new ArrayList<String>();
			}
			
			if(executionBuffer.size()<=0)
				continue;
			op = String.join(", ", executionBuffer);
			MqttMessage message = new MqttMessage(op.getBytes());
			message.setQos(qos);
			message.setRetained(true);
			try {
				sampleClient.publish(this.topic, message);
			} catch (MqttPersistenceException e) {
				logger.warn("Failed to send message " +  e.getMessage());				
				e.printStackTrace();
			} catch (MqttException e) {
				logger.warn("Failed to send message " +  e.getMessage());				
				e.printStackTrace();
			}
			logger.info("Message block with size {} published",executionBuffer.size());

		} catch (MqttException me) {
			logger.warn("Failed to send message " +  me.getMessage());
			
		}
	}}

	private static void connectAndPublishInSSL() {
		ArrayList<String> executionBuffer = new ArrayList<String>();
		ActiveMQSslConnectionFactory connectionFactory = new ActiveMQSslConnectionFactory("ssl://localhost:8883");
		try {
			javax.jms.Connection connection = connectionFactory.createConnection();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			connection.start();
			Destination destination = session.createTopic(".vroc.q");
			//session.createQueue("/vroc/q"); - Incase we need queue
			MessageProducer producer = session.createProducer(destination);
			if (OutputHandler.sizeExceded) {
				executionBuffer = OutputHandler.getFromFile();
			} else
				executionBuffer = new ArrayList<String>(OutputHandler.buffer);
			executionBuffer.forEach((c) -> {
				try {
					producer.send(session.createTextMessage(c));
				} catch (JMSException e) {
					e.printStackTrace();
				}				
			});
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


	
		

}
