# cloud-native-face-detection

Cloud native face detection based on rekognition

It explains how to build a demo system for recognizing faces in short videos based on AWS managed services.

How it works

1. User uploads a video from the web browser to Amazon S3.

2. The “object created” event on S3 triggers a Lambda function marked as “func1”. 

3. The Lambda function “func1” works as following. 

  3.1. Extract the metadata of the video stored in S3, save the ETag and metadata of the video into DynamoDB (table: “tbl_rich_metadata”, fields: “id” for ETag, “metadata”); 

  3.2. Invoke Rekognition service for face detection and get the JobId; 

  3.3. Save the ETag in 3.1 and JobId in 3.2 into DynamoDB (table: “tbl_rek_video”, fields: “id” for ETag, “jobid”). 

4. Recognition completed, send notification.

5. Two subscribers have subscribed the SNS message indicating the recognition is completed, one is an email user, and the other is a Lambda function marked as “func2”.

  5.1. Subscriber’s mail box receives the SNS message;

  5.2. Lambda function “func2” receives the SNSmessageindicating the recognition completed.

6. Lambda function “func2” works as following.

  6.1. Get JobId from the SNS message, retrieve face recognition result fromRekognition service;

  6.2. Based on the recognition result, grab frames from the videostored in S3,crop images, and save to S3.

7. The user queries the recognition result via web browser.

8. API Gateway forwards user’s request to Lambda function marked as “func3”.

9. Lambda function “func3” works as following.

  9.1. Query JobId by ETag (a parameter sent in the query) from DynamoDB;

  9.2. Retrieve face recognition result by JobId in 9.1 from Rekognition service;

10. Prepare relevant data including the image URLs of detected faces, return to API Gateway.

11. API Gateway returns the data to web browser for display.

12. CloudWatchmonitorsthe execution of Lambda functions, outputslogs for troubleshooting

