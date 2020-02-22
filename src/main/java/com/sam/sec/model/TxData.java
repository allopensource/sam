package com.sam.sec.model;

import com.sam.helper.TxStatusEnums;

public class TxData {
	private String payload;
	private TxStatusEnums.Root status;

	public TxStatusEnums.Root getStatus() {
		return status;
	}

	public void setStatus(TxStatusEnums.Root status) {
		this.status = status;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

}
