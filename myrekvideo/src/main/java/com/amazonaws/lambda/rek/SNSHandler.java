package com.amazonaws.lambda.rek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.Consts;
import com.amazonaws.util.JavaCVUtil;
import com.amazonaws.util.VideoDetectUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * com.amazonaws.lambda.rek.SNSHandler::handleRequest
 * 
 * @author xujun
 *
 */
public class SNSHandler implements RequestHandler<SNSEvent, String> {

	private static final Logger logger = LogManager.getLogger(SNSHandler.class);
	private static final Gson gson = new Gson();
	private static final VideoDetectUtil videoDetect = new VideoDetectUtil(
			Consts.DEF_REGION, Consts.ARN_SNS_TPC_REK, Consts.ROLE_REK_S3_CW);
	private static final JavaCVUtil javaCVUtil = new JavaCVUtil(Consts.DEF_REGION);

	@Override
	public String handleRequest(SNSEvent event, Context context) {
		try {
			String strEvent = gson.toJson(event);
			logger.info(String.format("SNSEvent: %s.", strEvent));
			String message = event.getRecords().get(0).getSNS().getMessage();
			JsonObject obj = new JsonParser().parse(message).getAsJsonObject();
			String startJobId = obj.get("JobId").getAsString();
			JsonObject video = obj.get("Video").getAsJsonObject();
			String bucketName = video.get("S3Bucket").getAsString();
			String keyName = video.get("S3ObjectName").getAsString();

			logger.info(String.format("Received event: %s.", strEvent));
			logger.info(String.format("Jobid: %s.", startJobId));
			List<GetFaceDetectionResult> faceDetectionResults = videoDetect.GetResultsFaces(startJobId);
			String strFaceDetectionResults = gson.toJson(faceDetectionResults);
			javaCVUtil.grabberVideoFrames(bucketName, keyName, faceDetectionResults);
			return strFaceDetectionResults;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("Exception: %s.", e.getMessage()));
			return e.getMessage();
		}
	}
}
