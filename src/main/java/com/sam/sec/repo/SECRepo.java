package com.sam.sec.repo;

import java.util.Optional;

import com.sam.helper.RepoException;
import com.sam.mdm.model.Saga.Step;
import com.sam.sec.model.StepData;

public interface SECRepo {
	public Optional<Object> preProcess(String saga, String payload) throws RepoException;

	public void preExecuteStep(Step step, StepData stepData, Optional<Object> preProcessData) throws RepoException;

	void setStepData(Step step, StepData stepData, Optional<Object> preStepData) throws RepoException;

	public StepData getStepData(Step step, Optional<Object> preStepData) throws RepoException;

	void postProcess(String payload, Boolean executionSuccess, Optional<Object> preProcessData);

}
