package com.sam.sec.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sam.helper.RepoException;
import com.sam.helper.TxStatusEnums;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.mdm.service.MDMService;
import com.sam.sec.model.StepData;
import com.sam.sec.repo.SECRepo;
import com.sam.sec.repo.TaskRepo;

@Service
public class TaskService {

	@Autowired
	private TaskRepo taskRepo;

	@Autowired
	private MDMService mdmService;

	@Autowired
	private SECService secService;

	@Autowired
	private SECRepo secRepo;

	private void rollBack(Saga saga, Optional<HashMap<Step, StepData>> lastStepMap, Object tx) {
		Set<Entry<Step, StepData>> entrySet = lastStepMap.get().entrySet();
		Step lastStep = entrySet.stream().findFirst().get().getKey();
		List<Step> stepsToRollBack = this.getLastExecutedStepsInclusiveCurrent(saga, tx, lastStep);
		for (Step step : stepsToRollBack) {
			secService.executeCompStep(step, Optional.of(tx));
		}
	}

	private Boolean continueExecution(Saga saga, Optional<HashMap<Step, StepData>> lastStepMap, Object tx)
			throws RepoException {
		Boolean isSuccess = Boolean.FALSE;
		String txData = taskRepo.getTransactionPayload(saga.getSagaModel().getSaga(), tx);
		// 1.In case there was no last step executed execute all the steps
		if (!lastStepMap.isPresent()) {
			for (Step pendingStep : saga.getSteps()) {
				isSuccess = isSuccess || secService.executeStep(pendingStep, Optional.of(tx), txData);
			}
			return isSuccess;
		}
		Set<Entry<Step, StepData>> entrySet = lastStepMap.get().entrySet();
		Step lastStep = entrySet.stream().findFirst().get().getKey();
		StepData stepData = entrySet.stream().findFirst().get().getValue();
		// ------------------Now comes the intermediate
		// states----------------------------//
		// 2. A intermediate state is when a step was started but was not
		// concluded
		if (stepData.getStatus().name().equals(TxStatusEnums.Step.COMPTXSTARTED.name())
				|| stepData.getStatus().name().equals(TxStatusEnums.Step.STARTED.name())) {
			secService.executeCompStep(lastStep, Optional.of(tx));
			stepData = secRepo.getStepData(lastStep, Optional.of(tx));
		}
		// 3. check if the step was compensated ,this means that when
		// saga failed it was executing comp txs so we will rollback
		// all the previous steps
		if (stepData.getStatus().name().equals(TxStatusEnums.Step.COMPTXSUCCESS.name())
				|| stepData.getStatus().name().equals(TxStatusEnums.Step.COMPTXFAILED.name())) {
			List<Step> stepsAlreadyExecuted = this.getLastExecutedSteps(saga, tx, lastStep);
			for (Step execStep : stepsAlreadyExecuted) {
				secService.executeCompStep(execStep, Optional.of(tx));
			}
		}

		// 3.if last step status is failed then it and all previous
		// steps should have been compensated i.e failed is also a type
		// of intermediate status
		if (stepData.getStatus().name().equals(TxStatusEnums.Step.FAILED.name())) {
			List<Step> stepsAlreadyExecuted = this.getLastExecutedStepsInclusiveCurrent(saga, tx, lastStep);
			for (Step execStep : stepsAlreadyExecuted) {
				secService.executeCompStep(execStep, Optional.of(tx));
			}
		}

		// 4. if last step was success then all the next steps should
		// now be executed
		if (stepData.getStatus().name().equals(TxStatusEnums.Step.SUCCESS.name())) {
			List<Step> nextStepsToExecute = this.getNextStepsToExecute(saga, tx, lastStep);
			for (Step nextStep : nextStepsToExecute) {
				isSuccess = isSuccess || secService.executeStep(nextStep, Optional.of(tx), txData);
			}
		}

		return isSuccess;
	}

	private List<Step> getNextStepsToExecute(Saga saga, Object tx, Step lastStep) {
		List<Step> allSteps = saga.getSteps();
		List<Step> stepsAlreadyExecuted = new ArrayList<>();
		Iterator<Step> iter = allSteps.iterator();
		while (iter.hasNext()) {
			Step step = iter.next();
			if (!step.getName().equals(lastStep)) {
				stepsAlreadyExecuted.add(step);
				iter.remove();
			} else {
				break;
			}
		}
		return allSteps;
	}

	private List<Step> getLastExecutedSteps(Saga saga, Object tx, Step lastStep) {
		List<Step> allSteps = saga.getSteps();
		List<Step> stepsAlreadyExecuted = new ArrayList<>();
		Iterator<Step> iter = allSteps.iterator();
		while (iter.hasNext()) {
			Step step = iter.next();
			if (!step.getName().equals(lastStep.getName())) {
				stepsAlreadyExecuted.add(step);
				iter.remove();
			} else {
				break;
			}
		}
		return stepsAlreadyExecuted;
	}

	private List<Step> getLastExecutedStepsInclusiveCurrent(Saga saga, Object tx, Step lastStep) {
		List<Step> stepsAlreadyExecuted = this.getLastExecutedSteps(saga, tx, lastStep);
		stepsAlreadyExecuted.add(lastStep);
		return stepsAlreadyExecuted;
	}

	public void cleanUp(TxStatusEnums.Root... rootStatus) throws RepoException {
		taskRepo.cleanUp(rootStatus);
	}

	public void recover(Long txAge, Date taskStartTime, TxStatusEnums.Root... rootStatus) throws RepoException {
		List<Saga> allSagas = mdmService.getSagas();
		HashMap<Saga, Optional<List<Object>>> sagaMappedTx = taskRepo
				.getSagaTransactionMapWithStatusInAndOlderThanTaskTime(rootStatus, txAge, taskStartTime, allSagas);
		this.executeFailOverAction(sagaMappedTx);
	}

	private void executeFailOverAction(HashMap<Saga, Optional<List<Object>>> sagaMappedTx) throws RepoException {
		for (Entry<Saga, Optional<List<Object>>> entry : sagaMappedTx.entrySet()) {
			Saga saga = entry.getKey();
			Optional<List<Object>> txs = entry.getValue();
			if (txs.isPresent())
				for (Object tx : txs.get()) {
					Optional<HashMap<Step, StepData>> lastStepMap = taskRepo.getLastExecutedStep(saga, tx);
					if (saga.getSagaModel().getFailoverAction().equals("None")) {
						taskRepo.updateTransactionStatus(saga, tx, TxStatusEnums.Root.SUCCESS);
					} else if (saga.getSagaModel().getFailoverAction().equals("Continue")) {
						Boolean isSuccess = this.continueExecution(saga, lastStepMap, tx);
						taskRepo.updateTransactionStatus(saga, tx,
								isSuccess ? TxStatusEnums.Root.SUCCESS : TxStatusEnums.Root.FAILED);
					} else if (saga.getSagaModel().getFailoverAction().equals("Rollback")) {
						taskRepo.updateTransactionStatus(saga, tx, TxStatusEnums.Root.ROLLBACKSTARTED);
						this.rollBack(saga, lastStepMap, tx);
						taskRepo.updateTransactionStatus(saga, tx, TxStatusEnums.Root.ROLLBACKED);
					} else if (saga.getSagaModel().getFailoverAction().equals("Reprocess")) {
						taskRepo.updateTransactionStatus(saga, tx, TxStatusEnums.Root.REPROCESSSTARTED);
						taskRepo.clearTransaction(saga, tx);
						String payload = taskRepo.getTransactionPayload(saga.getSagaModel().getSaga(), tx);
						secService.executeSteps(saga.getSagaModel().getSaga(), Optional.of(tx), payload);
					}
				}
		}
	}
}
