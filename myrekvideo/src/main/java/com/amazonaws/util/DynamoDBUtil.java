package com.amazonaws.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

/**
 * Persist video metadata and AWS Rekognition jobid into DynamoDB for subsequent
 * query.
 * 
 * @author xujun
 *
 */
public class DynamoDBUtil {

	private static final Logger logger = LogManager.getLogger(DynamoDBUtil.class);
	private String tableName, primaryKeyName;
	private AmazonDynamoDB client;
	private DynamoDB dynamoDB;

	public DynamoDBUtil(Regions region, String tableName, String primaryKeyName) {
		this.tableName = tableName;
		this.primaryKeyName = primaryKeyName;
		client = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
		dynamoDB = new DynamoDB(client);
	}

	public void createItem(String primaryKey, String contentName, String content) {
		Table table = dynamoDB.getTable(tableName);
		try {
			Item item = new Item().withPrimaryKey(primaryKeyName, primaryKey).withString(contentName, content);
			table.putItem(item);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("Create item failed. %s.", e.getMessage()));
		}
	}

	public String retrieveItem(String primaryKey) {
		Table table = dynamoDB.getTable(tableName);
		try {
			Item item = table.getItem(primaryKeyName, primaryKey);
			if (item != null) {
				String strItem = item.toJSONPretty();
				logger.info(String.format("Printing item (%s) after retrieving it.", strItem));
				return strItem;
			}
			logger.info(String.format("No item (id: %s) has been found.", primaryKey));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("GetItem failed. The error is %s.", e.getMessage()));
		}
		return null;
	}
}
