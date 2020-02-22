package com.sam.helper;

public class TxStatusEnums {

	public enum Root {
		STARTED, SUCCESS, FAILED, REPROCESSSTARTED, ROLLBACKSTARTED, ROLLBACKED
	};

	public enum Step {
		STARTED, FAILED, SUCCESS, COMPTXSTARTED, COMPTXSUCCESS, COMPTXFAILED
	}

}
