package com.amazonaws.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.ListCollectionsRequest;
import com.amazonaws.services.rekognition.model.ListCollectionsResult;
import com.amazonaws.services.rekognition.model.ListFacesRequest;
import com.amazonaws.services.rekognition.model.ListFacesResult;
import com.amazonaws.services.rekognition.model.S3Object;
import com.google.gson.Gson;

public class FaceCollectionUtil {
	private static final Logger logger = LogManager.getLogger(FaceCollectionUtil.class);
	private static final Gson gson = new Gson();
	private AmazonRekognition rek = null;

	public FaceCollectionUtil(Regions region, String collectionId) {
		this.rek = AmazonRekognitionClientBuilder.standard().withRegion(region).build();
		this.createCollection(collectionId);
	}

	private void createCollection(String collectionId) {
		int maxResults = 10;
		ListCollectionsResult listCollectionsResult = null;
		String paginationToken = null;
		boolean exists = false;
		do {
			if (listCollectionsResult != null) {
				paginationToken = listCollectionsResult.getNextToken();
			}
			ListCollectionsRequest listCollectionsRequest = new ListCollectionsRequest().withMaxResults(maxResults)
					.withNextToken(paginationToken);
			listCollectionsResult = rek.listCollections(listCollectionsRequest);

			List<String> collectionIds = listCollectionsResult.getCollectionIds();
			for (String resultId : collectionIds) {
				if (collectionId.equals(resultId)) {
					exists = true;
					break;
				}
			}
			if (exists) {
				break;
			}
		} while (listCollectionsResult != null && listCollectionsResult.getNextToken() != null);
		if (!exists) {
			CreateCollectionRequest request = new CreateCollectionRequest().withCollectionId(collectionId);
			CreateCollectionResult createCollectionResult = this.rek.createCollection(request);
			String result = gson.toJson(createCollectionResult);
			logger.info(String.format("createCollectionResult: %s.", result));
		}
	}

	public List<FaceRecord> indexFaces(String bucket, String keyName, String collectionId) {
		S3Object s3Object = new S3Object().withBucket(bucket).withName(keyName);
		Image image = new Image().withS3Object(s3Object);
		String fileName = this.getFileName(keyName);
		IndexFacesRequest indexFacesRequest = new IndexFacesRequest().withImage(image).withCollectionId(collectionId)
				.withExternalImageId(fileName).withDetectionAttributes("ALL");
		IndexFacesResult indexFacesResult = rek.indexFaces(indexFacesRequest);
		List<FaceRecord> faceRecords = indexFacesResult.getFaceRecords();
		logger.info(String.format("faceRecords in indexFaces: %s.", gson.toJson(faceRecords)));
		return faceRecords;
	}

	public List<Face> listFaces(String collectionId) {
		int maxResults = 10;
		ListFacesResult listFacesResult = null;
		String paginationToken = null;
		List<Face> faceResults = new ArrayList<Face>();
		do {
			if (listFacesResult != null) {
				paginationToken = listFacesResult.getNextToken();
			}

			ListFacesRequest listFacesRequest = new ListFacesRequest().withCollectionId(collectionId)
					.withMaxResults(maxResults).withNextToken(paginationToken);
			listFacesResult = rek.listFaces(listFacesRequest);
			List<Face> faces = listFacesResult.getFaces();
			for (Face face : faces) {
				faceResults.add(face);
			}
		} while (listFacesResult != null && listFacesResult.getNextToken() != null);
		logger.info(String.format("faceResults in listFaces: %s.", gson.toJson(faceResults)));
		return faceResults;
	}

	private String getFileName(String fileKeyName) {
		String[] splits = fileKeyName.split("/");
		String fileName = splits[splits.length - 1];
		fileName = fileName.substring(0, fileName.length() - ".jpg".length());
		return fileName;
	}
}
