package com.sam.mdm.model;

public class SAGAModel {
	private String saga;
	private String hmacSecured;
	private String failoverAction;
	private String hmac;

	public String getSaga() {
		return saga;
	}

	public void setSaga(String saga) {
		this.saga = saga;
	}

	public String getHmacSecured() {
		return hmacSecured;
	}

	public void setHmacSecured(String hmacSecured) {
		this.hmacSecured = hmacSecured;
	}

	public String getFailoverAction() {
		return failoverAction;
	}

	public void setFailoverAction(String failoverAction) {
		this.failoverAction = failoverAction;
	}

	public String getHmac() {
		return hmac;
	}

	public void setHmac(String hmac) {
		this.hmac = hmac;
	}

}
