package com.amazonaws.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceDetection;
import com.amazonaws.services.rekognition.model.GetFaceDetectionResult;
import com.amazonaws.services.rekognition.model.VideoMetadata;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.gson.Gson;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Grab and crop image from video based on AWS Rekognition results.
 * 
 * @author xujun
 *
 */
public class JavaCVUtil {

	private static final Logger logger = LogManager.getLogger(JavaCVUtil.class);
	private static final Gson gson = new Gson();
	private static final AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
	// private FaceCollectionUtil faceCollectionUtil = null;
	// private Regions region = null;

	public JavaCVUtil(Regions region) {
		// this.region = region;
	}

	public void grabberVideoFrames(String bucketName, String keyName, List<GetFaceDetectionResult> results) {
		Frame frame = null;
		int index = 0;
		FFmpegFrameGrabber fFmpegFrameGrabber = null;
		try {
			S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, keyName));
			ObjectMetadata objectMetadata = s3Object.getObjectMetadata();
			String eTag = objectMetadata.getETag();
			// faceCollectionUtil = new FaceCollectionUtil(region, eTag);

			fFmpegFrameGrabber = new FFmpegFrameGrabber(this.getObject(bucketName, keyName));
			fFmpegFrameGrabber.start();
			int lengthInFrames = fFmpegFrameGrabber.getLengthInFrames();
			logger.info(String.format("results: %s: ", gson.toJson(results)));
			FaceDetection arrFaceDetection[] = new FaceDetection[lengthInFrames];
			for (int i = 0; i < arrFaceDetection.length; i++) {
				arrFaceDetection[i] = null;
			}
			long width = 0;
			long height = 0;
			for (GetFaceDetectionResult result : results) {
				List<FaceDetection> faceDetections = result.getFaces();
				VideoMetadata videoMetadata = result.getVideoMetadata();
				width = videoMetadata.getFrameWidth();
				height = videoMetadata.getFrameHeight();
				long durationMillis = videoMetadata.getDurationMillis();
				for (FaceDetection faceDetection : faceDetections) {
					long timeStamp = faceDetection.getTimestamp();
					int idx = (int) (lengthInFrames * timeStamp / durationMillis);
					arrFaceDetection[idx] = faceDetection;
				}
			}

			while (index <= lengthInFrames) {
				frame = fFmpegFrameGrabber.grabImage();
				if (frame != null && arrFaceDetection[index] != null) {
					FaceDetection faceDetection = arrFaceDetection[index];
					FaceDetail faceDetail = faceDetection.getFace();
					BoundingBox boundingBox = faceDetail.getBoundingBox();
					int x = (int) (width * boundingBox.getLeft());
					int y = (int) (height * boundingBox.getTop());
					int w = (int) (width * boundingBox.getWidth());
					int h = (int) (height * boundingBox.getHeight());
					x = x < 0 ? 0 : x;
					y = y < 0 ? 0 : y;
					x = x > width ? (int) width : x;
					y = y > height ? (int) height : y;
					BufferedImage bufferedImage = FrameToBufferedImage(frame);
					BufferedImage croppedImage = bufferedImage.getSubimage(x, y, w, h);

					String fileName = String.valueOf(faceDetection.getTimestamp());
					fileName = fileName.length() == 1 ? "00" + fileName : fileName;
					fileName = fileName.length() == 2 ? "0" + fileName : fileName;
					File file = File.createTempFile(fileName, ".jpg");
					File croppedFile = File.createTempFile(fileName + "_cropped", ".jpg");
					ImageIO.write(bufferedImage, "jpg", file);
					ImageIO.write(croppedImage, "jpg", croppedFile);
					String fileKeyName = "videos/" + eTag + "/" + fileName + ".jpg";
					String croppedFileKeyName = "videos/" + eTag + "/cropped/" + fileName + ".jpg";
					setObject(bucketName, fileKeyName, file);
					setObject(bucketName, croppedFileKeyName, croppedFile);

					// TODO:
					// Index faces for subsequent listing.
					// faceCollectionUtil.indexFaces(bucketName, croppedFileKeyName, eTag);
				}
				index++;
			}
			logger.info("After fFmpegFrameGrabber loop.");
			fFmpegFrameGrabber.stop();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			try {
				if (fFmpegFrameGrabber != null) {
					fFmpegFrameGrabber.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
	}

	private BufferedImage FrameToBufferedImage(Frame frame) {
		Java2DFrameConverter converter = new Java2DFrameConverter();
		BufferedImage bufferedImage = converter.getBufferedImage(frame);
		return bufferedImage;
	}

	private File getObject(String bucketName, String keyName) {
		try {
			S3Object o = s3.getObject(bucketName, keyName);
			S3ObjectInputStream s3is = o.getObjectContent();

			File file = File.createTempFile(keyName, ".mp4");
			logger.info(String.format("keyName.mp4: %s.mp4.", keyName));
			Files.copy(s3is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

			return file;
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return null;
	}

	private void setObject(String bucketName, String keyName, File file) {
		try {
			s3.putObject(
					new PutObjectRequest(bucketName, keyName, file)
			.withCannedAcl(CannedAccessControlList.PublicRead));
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}
