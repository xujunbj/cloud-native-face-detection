package com.amazonaws.services.s3.sample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MySign {
	static byte[] HmacSHA256(String data, byte[] key) throws Exception {
		String algorithm = "HmacSHA256";
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key, algorithm));
		return mac.doFinal(data.getBytes("UTF8"));
	}

	static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName)
			throws Exception {
		byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
		byte[] kDate = HmacSHA256(dateStamp, kSecret);
		byte[] kRegion = HmacSHA256(regionName, kDate);
		byte[] kService = HmacSHA256(serviceName, kRegion);
		byte[] kSigning = HmacSHA256("aws4_request", kService);
		return kSigning;
	}

	private static String readFileToStr(String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		String ls = System.getProperty("line.separator");
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(ls);
		}
		// delete the last new line separator
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		reader.close();

		String content = stringBuilder.toString();
		return content;
	}
	
	public static void main(String args[]) throws Exception {	
		String key = "";
		String dateStamp = "20200601";
		String regionName = "us-east-1";
		String serviceName = "s3";		
		byte[] byteSigning = getSignatureKey(key, dateStamp, regionName, serviceName);
		String plainPolicy = readFileToStr("/Users/xujunaws/workspace/myrekweb/doc/policy.json");
		byte[] bytePolicy = plainPolicy.getBytes(StandardCharsets.UTF_8);
		String base64Policy = Base64.getEncoder().encodeToString(bytePolicy);
		System.out.println(base64Policy);
		byte[] byteSignature = HmacSHA256(base64Policy, byteSigning);
		
		StringBuffer strBuf = new StringBuffer();
		for (byte byte_to_be_converted : byteSignature) {
			String my_hex_string = String.format("%02X", byte_to_be_converted); // this is a hex string now
			strBuf.append(my_hex_string.toLowerCase()); // this line will print the hex string
		}
		System.out.println(strBuf.toString());
	}
}
