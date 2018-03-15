package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;

import java.util.HashMap;
import java.util.Map;

public class DynamoDBHandler {

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
            .withRegion("ap-southeast-2").build();
    private static DynamoDB dynamoDB = new DynamoDB(client);
    private static String workerConfigTableName = getTableName();

    public static  Map<String, Object> getSubscription(String id) {
        Map<String, Object> config = new HashMap<>();

        Table table = dynamoDB.getTable(workerConfigTableName);
        Item item = table.getItem("workerID", id);
        if (item != null){
            config = item.asMap();
        }
        return config;

    }


    public static boolean updateLastID(String id, Integer lastID, Long timeStamp ) {
        Map<String,AttributeValue> key = new HashMap<>();
        key.put(Constants.WORKER_ID,new AttributeValue().withS(id));

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(workerConfigTableName)
                .withKey(key)
                .addAttributeUpdatesEntry(Constants.LAST_PMID_LABEL,
                        new AttributeValueUpdate().withValue(new AttributeValue().withN(String.valueOf(lastID))))
                .addAttributeUpdatesEntry(Constants.LAST_UPDATE_TIME,
                        new AttributeValueUpdate().withValue(new AttributeValue().withN(String.valueOf(timeStamp))));
        UpdateItemResult updateItemResult = client.updateItem(updateItemRequest);
        return true;
    }

    private static String getTableName() {
        System.out.println("Getting DynamoDB TableName");
        String env = ConfigLoader.getENV();
        String tableName;
        if (env !=null && env.equals("AWS")) {
             tableName= ConfigLoader.getWorkerConfigTableName();
        }
        else
            tableName="test-worker-config";
        System.out.println(String.format("Worker config table name:%s", tableName ));
        return tableName;

    }





}
