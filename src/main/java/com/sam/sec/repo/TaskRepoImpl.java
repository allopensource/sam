package com.sam.sec.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.helper.RepoException;
import com.sam.helper.SagaConstants;
import com.sam.helper.SagaHelper;
import com.sam.helper.TxStatusEnums;
import com.sam.helper.TxStatusEnums.Root;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.sec.model.StepData;
import com.sam.sec.model.TxData;
import com.sam.zookeeper.ZooKeeperConnector;

@Repository
public class TaskRepoImpl implements TaskRepo {
	private Logger logger = LogManager.getLogger(TaskRepoImpl.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
	}

	@Autowired
	private ZooKeeperConnector zooKeeperConnector;

	@Override
	public void cleanUp(TxStatusEnums.Root... rootStatus) throws RepoException {
		List<String> txPathsToDelete = new ArrayList<>();
		try {
			if (zooKeeperConnector.getZookeeper().exists(SagaConstants.txroot, null) != null) {
				List<String> txs;
				txs = zooKeeperConnector.getZookeeper().getChildren(SagaConstants.txroot, null);
				if (txs != null) {
					for (String tx : txs) {
						String txPath = SagaHelper.createPath(SagaConstants.txroot, tx);
						byte[] txDataByte = zooKeeperConnector.getZookeeper().getData(txPath, null, null);
						TxData txData = mapper.readValue(txDataByte, TxData.class);
						List<String> failOverStatus = Arrays.asList(rootStatus).stream().map(st -> st.name())
								.collect(Collectors.toList());
						if (failOverStatus.contains(txData.getStatus().name())) {
							txPathsToDelete.add(txPath);
						}
					}
				}
				for (String txPath : txPathsToDelete) {
					List<String> steps = zooKeeperConnector.getZookeeper().getChildren(txPath, null);
					for (String step : steps) {
						String stepPath = SagaHelper.createPath(txPath, step);
						zooKeeperConnector.getZookeeper().delete(stepPath, -1);
					}
					zooKeeperConnector.getZookeeper().delete(txPath, -1);
				}
			}
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public HashMap<Saga, Optional<List<Object>>> getSagaTransactionMapWithStatusInAndOlderThanTaskTime(
			Root[] rootStatus, Long waitTimeInMillis, Date taskStartTime, List<Saga> allSagas) throws RepoException {
		try {
			HashMap<Saga, Optional<List<Object>>> sagaMap = new HashMap<>();
			if (zooKeeperConnector.getZookeeper().exists(SagaConstants.txroot, null) == null) {
				return sagaMap;
			}
			List<String> txs = zooKeeperConnector.getZookeeper().getChildren(SagaConstants.txroot, null);
			if (txs != null) {
				for (String tx : txs) {
					String txPath = SagaHelper.createPath(SagaConstants.txroot, tx);
					Stat stat = new Stat();
					byte[] txDataByte = zooKeeperConnector.getZookeeper().getData(txPath, null, stat);
					TxData txData = mapper.readValue(txDataByte, TxData.class);
					List<String> failOverStatus = Arrays.asList(rootStatus).stream().map(st -> st.name())
							.collect(Collectors.toList());
					if (failOverStatus.contains(txData.getStatus().name())
							&& (stat.getCtime() + waitTimeInMillis <= taskStartTime.getTime())) {
						for (Saga saga : allSagas) {
							if (tx.startsWith(saga.getSagaModel().getSaga())) {
								if (sagaMap.get(saga) == null) {
									List<Object> txsTemp = new ArrayList<>();
									txsTemp.add(tx);
									sagaMap.put(saga, Optional.of(txsTemp));
								} else {
									Optional<List<Object>> txsTemp = sagaMap.get(saga);
									txsTemp.get().add(tx);
								}
							}
						}
					}
				}
			}
			return sagaMap;
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void updateTransactionStatus(Saga saga, Object tx, TxStatusEnums.Root status) throws RepoException {
		try {
			String txPath = SagaHelper.createPath(SagaConstants.txroot, (String) tx);
			byte[] txDataByte = zooKeeperConnector.getZookeeper().getData(txPath, null, null);
			TxData txData;
			txData = mapper.readValue(txDataByte, TxData.class);
			txData.setStatus(status);
			zooKeeperConnector.getZookeeper().setData(txPath, mapper.writeValueAsBytes(txData), -1);
		} catch (IOException | KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void clearTransaction(Saga saga, Object tx) throws RepoException {
		try {
			String txPath = SagaHelper.createPath(SagaConstants.txroot, (String) tx);
			List<String> steps = zooKeeperConnector.getZookeeper().getChildren(txPath, null);
			for (String step : steps) {
				String stepPath = SagaHelper.createPath(txPath, step);
				zooKeeperConnector.getZookeeper().delete(stepPath, -1);
			}
		} catch (KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public String getTransactionPayload(String saga, Object tx) throws RepoException {
		try {
			String txPath = SagaHelper.createPath(SagaConstants.txroot, (String) tx);
			byte[] data = zooKeeperConnector.getZookeeper().getData(txPath, null, null);
			return new String(data);
		} catch (KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public Optional<HashMap<Step, StepData>> getLastExecutedStep(Saga saga, Object tx) throws RepoException {
		try {
			HashMap<Step, StepData> stepMap = new HashMap<>();
			String txPath = SagaHelper.createPath(SagaConstants.txroot, (String) tx);
			List<String> steps;
			steps = zooKeeperConnector.getZookeeper().getChildren(txPath, null);
			String lastExecutedStepName = steps.get(steps.size() - 1);
			List<Step> allSteps = saga.getSteps();
			Iterator<Step> iter = allSteps.iterator();
			Step lastExecutedStep = null;
			while (iter.hasNext()) {
				Step step = iter.next();
				if (!step.getName().equals(lastExecutedStepName)) {
					iter.remove();
				} else {
					lastExecutedStep = step;
					break;
				}
			}
			if (lastExecutedStep != null) {
				String stepPath = SagaHelper.createPath(txPath, lastExecutedStep.getName());
				StepData stepData = mapper.readValue(zooKeeperConnector.getZookeeper().getData(stepPath, null, null),
						StepData.class);
				stepMap.put(lastExecutedStep, stepData);
				return Optional.of(stepMap);
			}
			return Optional.empty();
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

}
