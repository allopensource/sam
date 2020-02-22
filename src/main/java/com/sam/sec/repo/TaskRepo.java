package com.sam.sec.repo;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.sam.helper.RepoException;
import com.sam.helper.TxStatusEnums;
import com.sam.helper.TxStatusEnums.Root;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.sec.model.StepData;

public interface TaskRepo {
	public void cleanUp(TxStatusEnums.Root... rootStatus) throws RepoException;

	public HashMap<Saga, Optional<List<Object>>> getSagaTransactionMapWithStatusInAndOlderThanTaskTime(
			Root[] rootStatus, Long waitTimeInMillis, Date taskStartTime, List<Saga> allSagas) throws RepoException;

	void updateTransactionStatus(Saga saga, Object tx, Root status) throws RepoException;

	public void clearTransaction(Saga saga, Object tx) throws RepoException;

	public String getTransactionPayload(String saga, Object tx) throws RepoException;

	public Optional<HashMap<Step, StepData>> getLastExecutedStep(Saga saga, Object tx) throws RepoException;
}