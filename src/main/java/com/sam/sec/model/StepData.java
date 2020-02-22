package com.sam.sec.model;

import org.springframework.http.HttpStatus;

import com.sam.helper.TxStatusEnums;

public class StepData {
	private String apiPayload;
	private String apiResponse;
	private HttpStatus apiResponseStatus;

	private String capiPayload;
	private String capiResponse;
	private HttpStatus capiResponseStatus;
	private TxStatusEnums.Step status;

	public TxStatusEnums.Step getStatus() {
		return status;
	}

	public void setStatus(TxStatusEnums.Step status) {
		this.status = status;
	}

	public String getApiPayload() {
		return apiPayload;
	}

	public void setApiPayload(String apiPayload) {
		this.apiPayload = apiPayload;
	}

	public String getApiResponse() {
		return apiResponse;
	}

	public void setApiResponse(String apiResponse) {
		this.apiResponse = apiResponse;
	}

	public HttpStatus getApiResponseStatus() {
		return apiResponseStatus;
	}

	public void setApiResponseStatus(HttpStatus apiResponseStatus) {
		this.apiResponseStatus = apiResponseStatus;
	}

	public String getCapiPayload() {
		return capiPayload;
	}

	public void setCapiPayload(String capiPayload) {
		this.capiPayload = capiPayload;
	}

	public String getCapiResponse() {
		return capiResponse;
	}

	public void setCapiResponse(String capiResponse) {
		this.capiResponse = capiResponse;
	}

	public HttpStatus getCapiResponseStatus() {
		return capiResponseStatus;
	}

	public void setCapiResponseStatus(HttpStatus capiResponseStatus) {
		this.capiResponseStatus = capiResponseStatus;
	}

}
