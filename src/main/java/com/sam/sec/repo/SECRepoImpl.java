package com.sam.sec.repo;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.helper.RepoException;
import com.sam.helper.SagaConstants;
import com.sam.helper.SagaHelper;
import com.sam.helper.TxStatusEnums;
import com.sam.mdm.model.Saga.Step;
import com.sam.sec.model.StepData;
import com.sam.sec.model.TxData;
import com.sam.zookeeper.ZooKeeperConnector;

@Repository
public class SECRepoImpl implements SECRepo {
	private Logger logger = LogManager.getLogger(SECRepoImpl.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
	}

	@Autowired
	private ZooKeeperConnector zooKeeperConnector;

	@Override
	public Optional<Object> preProcess(String saga, String payload) throws RepoException {
		try {
			this.checkAndCreateRootNode();
			String txRootPath = SagaHelper.createPath(SagaConstants.txroot, saga);
			TxData txData = new TxData();
			txData.setPayload(payload);
			txData.setStatus(TxStatusEnums.Root.STARTED);
			String txPath = zooKeeperConnector.getZookeeper().create(txRootPath, mapper.writeValueAsBytes(txData),
					ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
			return Optional.of(txPath);
		} catch (KeeperException | InterruptedException | JsonProcessingException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	private void checkAndCreateRootNode() throws KeeperException, InterruptedException {
		logger.debug("Checking root {}", SagaConstants.txroot);
		Stat sagaExists = zooKeeperConnector.getZookeeper().exists(SagaConstants.txroot, Boolean.FALSE);
		if (sagaExists == null) {
			zooKeeperConnector.getZookeeper().create(SagaConstants.txroot, SagaConstants.txRootDesc.getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
	}

	@Override
	public void preExecuteStep(Step step, StepData stepData, Optional<Object> preProcessData) throws RepoException {
		String transactionPath = (String) preProcessData.get();
		String stepPath = SagaHelper.createPath(transactionPath, step.getName());
		try {
			zooKeeperConnector.getZookeeper().create(stepPath, mapper.writeValueAsBytes(stepData),
					ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (JsonProcessingException | KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void setStepData(Step step, StepData stepData, Optional<Object> preStepData) throws RepoException {
		int versionNo = -1;
		String transactionPath = (String) preStepData.get();
		String stepPath = SagaHelper.createPath(transactionPath, step.getName());
		try {
			zooKeeperConnector.getZookeeper().setData(stepPath, mapper.writeValueAsBytes(stepData), versionNo);
		} catch (JsonProcessingException | KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void postProcess(String payload, Boolean executionSuccess, Optional<Object> preProcessData) {
		String txPath = (String) preProcessData.get();
		TxData txData = new TxData();
		txData.setPayload(payload);
		int version = -1;
		try {
			if (executionSuccess) {
				logger.debug("All steps ok.");
				txData.setStatus(TxStatusEnums.Root.SUCCESS);
				zooKeeperConnector.getZookeeper().setData(txPath, mapper.writeValueAsBytes(txData), version);
			} else {
				logger.debug("All steps not ok.");
				txData.setStatus(TxStatusEnums.Root.FAILED);
				zooKeeperConnector.getZookeeper().setData(txPath, mapper.writeValueAsBytes(txData), version);

			}
		} catch (JsonProcessingException | KeeperException | InterruptedException exception) {
			logger.error("Parent node {} has thrown exception. Was transaction success :: {}", txPath, executionSuccess,
					exception);
		}
	}

	@Override
	public StepData getStepData(Step step, Optional<Object> preStepData) throws RepoException {
		try {
			Stat stat = new Stat();
			String transactionPath = (String) preStepData.get();
			String stepPath = SagaHelper.createPath(transactionPath, step.getName());
			byte[] data;
			data = zooKeeperConnector.getZookeeper().getData(stepPath, null, stat);
			return mapper.readValue(data, StepData.class);
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}
}
