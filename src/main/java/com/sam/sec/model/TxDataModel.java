package com.sam.sec.model;

import com.sam.helper.TxStatusEnums;

public class TxDataModel extends StepData {
	private String tx;
	private String step;
	private TxStatusEnums.Root txStatus;

	public String getTx() {
		return tx;
	}

	public void setTx(String tx) {
		this.tx = tx;
	}

	public String getStep() {
		return step;
	}

	public void setStep(String step) {
		this.step = step;
	}

	public TxStatusEnums.Root getTxStatus() {
		return txStatus;
	}

	public void setTxStatus(TxStatusEnums.Root txStatus) {
		this.txStatus = txStatus;
	}

}
