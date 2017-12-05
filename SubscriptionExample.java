

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.eclipse.milo.examples.client.ClientExample;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ExecutionError;

public class SubscriptionExample implements ClientExample {
	private static OutputHandler outputHndler;

	Date now = null;

    public static void main(String[] args) throws Exception {

    	outputHndler = new OutputHandler();
        SubscriptionExample example = new SubscriptionExample();

        new ClientExampleRunner(example).run();
        
         
    }
    

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicLong clientHandles = new AtomicLong(1L);

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();
        
        ArrayList<String> subscriptionList = CsvReader.getSubscriptions(null);

        // create a subscription @ 1000ms -- TODO Appropriate required
        
        UaSubscription subscription = client.getSubscriptionManager().createSubscription(1000.0).get();

        subscriptionList.forEach((sitem)->{
        	
        
	        ReadValueId readValueId = new ReadValueId(
	            new NodeId(2,sitem),
	            AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
	
	        // important: client handle must be unique per item
	        UInteger clientHandle = uint(clientHandles.getAndIncrement());
	
	        MonitoringParameters parameters = new MonitoringParameters(
	            clientHandle,
	            1000.0,     // sampling interval
	            null,       // filter, null means use default
	            uint(10),   // queue size
	            false        // discard oldest
	        );
	
	        MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(
	            readValueId, MonitoringMode.Reporting, parameters);
	
	        // when creating items in MonitoringMode.Reporting this callback is where each item needs to have its
	        // value/event consumer hooked up. The alternative is to create the item in sampling mode, hook up the
	        // consumer after the creation call completes, and then change the mode for all items to reporting.
	 
	        BiConsumer<UaMonitoredItem, Integer> onItemCreated =
	            (item, id) -> { 
	            	item.setValueConsumer(this::onSubscriptionValue);
	            	};
	
	        List<UaMonitoredItem> items = null;
			try {
				items = subscription.createMonitoredItems(
				    TimestampsToReturn.Both,
				    newArrayList(request),
				    onItemCreated
				).get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        
	
	        for (UaMonitoredItem item : items) {
	            if (item.getStatusCode().isGood()) {
	                //logger.info("item created for nodeId={}", item.getReadValueId().getNodeId());
	            } else {
	                logger.warn(
	                    "failed to create item for nodeId={} (status={})",
	                    item.getReadValueId().getNodeId(), item.getStatusCode());
	            }
	        }
        });

     }
    
    

    private void onSubscriptionValue(UaMonitoredItem item, DataValue value) { 
          outputHndler.publishMessage(item, value);
          
    }

}
