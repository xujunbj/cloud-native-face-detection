package com.amazonaws.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.GetFaceDetectionRequest;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import com.amazonaws.services.rekognition.model.NotificationChannel;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.StartFaceDetectionRequest;
import com.amazonaws.services.rekognition.model.StartFaceDetectionResult;
import com.amazonaws.services.rekognition.model.Video;
import com.google.gson.Gson;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Analyzes videos using the Rekognition Video API.
 * 
 * @author xujun
 *
 */
public class VideoDetectUtil {

	private static final Logger logger = LogManager.getLogger(VideoDetectUtil.class);
	private static final Gson gson = new Gson();
	private AmazonRekognition rek = null;
	private NotificationChannel channel = null;

	public VideoDetectUtil(Regions region, String topicArn, String roleArn) {
		this.rek = AmazonRekognitionClientBuilder.standard().withRegion(region).build();
		this.channel = new NotificationChannel().withSNSTopicArn(topicArn).withRoleArn(roleArn);
	}

	public StartFaceDetectionResult StartFaces(String bucket, String video) throws Exception {
		S3Object s3Object = new S3Object().withBucket(bucket).withName(video);
		StartFaceDetectionRequest req = new StartFaceDetectionRequest().withVideo(new Video().withS3Object(s3Object))
				.withNotificationChannel(channel);
		StartFaceDetectionResult startfaceDetectionResult = rek.startFaceDetection(req);
		return startfaceDetectionResult;
	}

	public List<GetFaceDetectionResult> GetResultsFaces(String startJobId) throws Exception {
		int maxResults = 100;
		String paginationToken = null;
		List<GetFaceDetectionResult> faceDetectionResults = new ArrayList<GetFaceDetectionResult>();
		GetFaceDetectionResult faceDetectionResult = null;
		do {
			if (faceDetectionResult != null) {
				paginationToken = faceDetectionResult.getNextToken();
			}
			faceDetectionResult = rek.getFaceDetection(new GetFaceDetectionRequest().withJobId(startJobId)
					.withNextToken(paginationToken).withMaxResults(maxResults));
			if (faceDetectionResult != null) {
				faceDetectionResults.add(faceDetectionResult);
			}
		} while (faceDetectionResult != null && faceDetectionResult.getNextToken() != null);
		String strFaceDetectionResults = gson.toJson(faceDetectionResults);
		logger.info(String.format("Face detection results: %s.", strFaceDetectionResults));
		return faceDetectionResults;
	}
}