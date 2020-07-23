package com.amazonaws.lambda.rek;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.Consts;
import com.amazonaws.lambda.rek.pojo.RekVideo;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.DynamoDBUtil;
import com.amazonaws.util.VideoDetectUtil;
import com.google.gson.Gson;

/**
 * com.amazonaws.lambda.rek.LambdaHandler::handleRequest
 * 
 * @author xujun
 *
 */
public class LambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final Logger logger = LogManager.getLogger(LambdaHandler.class);
	private static final String method_key = "method";
	private static final String method_videos = "videos";
	private static final String method_video_faces = "faces";
	private static final String method_video_faces_distinct = "faces_distinct";
	private static final String param_key_video_etag = "etag";
	private static final Gson gson = new Gson();
	private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
	private static final DynamoDBUtil ddbRekVideo = new DynamoDBUtil(Consts.DEF_REGION, "tbl_rek_video", "id");
	private static final VideoDetectUtil videoDetect = new VideoDetectUtil(
			Consts.DEF_REGION, Consts.ARN_SNS_TPC_REK, Consts.ROLE_REK_S3_CW);
	// private FaceCollectionUtil faceCollectionUtil = null;

	// Lambda Function
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {

		APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
		try {
			// fetching the value send in the request body
			String strBody = requestEvent.getBody();
			String reqEvt = gson.toJson(requestEvent);
			logger.info(String.format("body: %s.", strBody));
			String ctx = gson.toJson(context);
			logger.info(String.format("requestEvent: %s, context: %s.", reqEvt, ctx));
			String response = String.format("Welcome to LambdaHandler. %d", System.currentTimeMillis());
			if (requestEvent != null) {
				Map<String, String> queryStringParameters = requestEvent.getQueryStringParameters();
				String method = null;
				String eTag = null;
				if (queryStringParameters != null) {
					for (Map.Entry<String, String> entry : queryStringParameters.entrySet()) {
						logger.info("Key = " + entry.getKey() + ", Value = " + entry.getValue());
						if (entry.getKey().equalsIgnoreCase(method_key)) {
							method = entry.getValue();
						} else if (entry.getKey().equalsIgnoreCase(param_key_video_etag)) {
							eTag = entry.getValue();
						}
					}

					// Retrieve videos in S3.
					if (method != null && method.equalsIgnoreCase(method_videos)) {
						// ObjectListing listing = s3.listObjects("uev-bkt-rek-www", "videos/");
						ObjectListing listing = s3.listObjects(new ListObjectsRequest().
							    withBucketName("uev-bkt-rek-www").
							    withPrefix("videos/").withDelimiter("/"));
						List<S3ObjectSummary> summaries = listing.getObjectSummaries();

						while (listing.isTruncated()) {
							listing = s3.listNextBatchOfObjects(listing);
							summaries.addAll(listing.getObjectSummaries());
						}
						response = gson.toJson(summaries);
					}

					// Retrieve faces detected by Rekognition.
					if (method != null && method.equalsIgnoreCase(method_video_faces)) {
						String strRekVideo = ddbRekVideo.retrieveItem(eTag);
						RekVideo rekVideo = gson.fromJson(strRekVideo, RekVideo.class);
						String startJobId = rekVideo.getJobid();
						logger.info(String.format("eTag: %s, startJobId: %s", eTag, startJobId));
						List<GetFaceDetectionResult> results = videoDetect.GetResultsFaces(startJobId);
						String strResults = gson.toJson(results);
						logger.info(String.format("results: %s", strResults));
						response = strResults;
					}
					
					// TODO: 
					// Retrieve distinct faces detected by Rekognition.
					if (method != null && method.equalsIgnoreCase(method_video_faces_distinct)) {
						// logger.info(String.format("eTag: %s", eTag));
						// Retrieve the faces ever indexed in JavaCVUtil.
						// faceCollectionUtil = new FaceCollectionUtil(Consts.DEF_REGION, eTag);
						// List<Face> results = faceCollectionUtil.listFaces(eTag);
						// String strResults = gson.toJson(results);
						// logger.info(String.format("results: %s", strResults));
						// response = strResults;
					}
				}
			}
			// setting up the response message
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("Access-Control-Allow-Origin", "*");
			responseEvent.setHeaders(headers);
			responseEvent.setBody(response);
			responseEvent.setStatusCode(HttpStatus.SC_OK);
			logger.info(String.format("requestEvent: %s, response: %s, context: %s.", reqEvt, response, ctx));
			return responseEvent;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("Exception. %s", e.getMessage()));
			responseEvent.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			return responseEvent;
		}
	}
}