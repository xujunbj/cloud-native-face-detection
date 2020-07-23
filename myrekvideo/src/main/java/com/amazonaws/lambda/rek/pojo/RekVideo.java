package com.amazonaws.lambda.rek.pojo;

/**
 * {
 *  "jobid": "f74e447149d430716bd7fa9161125cd1cd97a36e0cbcfbd9cd00739e04611940",
 *  "id": "f15e3fe0c26273ed68b9ef85b1ca8d4e"
 * }
 * @author xujun
 *
 */
public class RekVideo {
	private String id;
	private String jobid;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}
}
