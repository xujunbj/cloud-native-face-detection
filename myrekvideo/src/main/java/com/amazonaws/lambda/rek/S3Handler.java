package com.amazonaws.lambda.rek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.rekognition.model.StartFaceDetectionResult;

import java.net.URLDecoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.Consts;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.DynamoDBUtil;
import com.amazonaws.util.VideoDetectUtil;
import com.google.gson.Gson;

/**
 * com.amazonaws.lambda.rek.S3Handler::handleRequest
 * 
 * @author xujun
 *
 */
public class S3Handler implements RequestHandler<S3Event, String> {

	private static final Logger logger = LogManager.getLogger(S3Handler.class);
	private static final String tbl_fld_metadata = "metadata";
	private static final String tbl_fld_jobid = "jobid";
	private static final Gson gson = new Gson();
	private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
	private static final DynamoDBUtil ddbMetadata = new DynamoDBUtil(Consts.DEF_REGION, "tbl_rich_metadata", "id");
	private static final DynamoDBUtil ddbRekVideo = new DynamoDBUtil(Consts.DEF_REGION, "tbl_rek_video", "id");
	private static final VideoDetectUtil videoDetect = new VideoDetectUtil(
			Consts.DEF_REGION, Consts.ARN_SNS_TPC_REK, Consts.ROLE_REK_S3_CW);

	@Override
	public String handleRequest(S3Event event, Context context) {
		// LambdaLogger logger = context.getLogger();
		String bucketName = "";
		String objectKey = "";
		StartFaceDetectionResult startfaceDetectionResult = null;
		String strStartfaceDetectionResult = null;
		try {
			logger.info(String.format("Received event: %s.", gson.toJson(event)));
			bucketName = event.getRecords().get(0).getS3().getBucket().getName();
			objectKey = event.getRecords().get(0).getS3().getObject().getKey();
			objectKey = URLDecoder.decode(objectKey, "UTF-8");
			logger.info(String.format("bucketName: %s, objectKey: %s.", bucketName, objectKey));
			S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, objectKey));
			ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
			String eTag = objectMetadata.getETag();
			String strItem = ddbMetadata.retrieveItem(eTag);
			if (strItem == null || strItem.equals("")) {
				// Insert metadata of the file into DynamoDB.
				String strObjectMetadata = gson.toJson(objectMetadata);
				ddbMetadata.createItem(eTag, tbl_fld_metadata, strObjectMetadata);
				logger.info(String.format("eTag: %s, metadata: %s inserted into DynamoDB.", eTag, strObjectMetadata));

				// Start to recognize the file. Here is the MP4 file.
				logger.info(String.format("'%s' in '%s', face detection begin.", objectKey, bucketName));
				startfaceDetectionResult = videoDetect.StartFaces(bucketName, objectKey);
				String startJobId = startfaceDetectionResult.getJobId();
				strStartfaceDetectionResult = gson.toJson(startfaceDetectionResult);
				// Insert jobid into DynamoDB.
				ddbRekVideo.createItem(eTag, tbl_fld_jobid, startJobId);
				logger.info(String.format(
						"'eTag: '%s', startJobId: '%s', startfaceDetectionResult: '%s'" + " inserted into DynamoDB.",
						eTag, startJobId, strStartfaceDetectionResult));

				// It's an asynchronous operation, you should call it in an SNS handler.
				// videoDetect.GetResultsFaces(startJobId);
			} else {
				logger.info(String.format("File '%s' (eTag: %s) already exists.", objectKey, eTag));
			}
			return strStartfaceDetectionResult;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("Exception: %s.", e.getMessage()));
			return e.getMessage();
		}
	}
}
