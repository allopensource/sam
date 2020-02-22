package com.sam.mdm.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.sam.mdm.model.SAGAModel;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.sec.model.StepModel;
import com.sam.zookeeper.ZooKeeperConnector;

@Repository
public class MDMRepoImpl implements MDMRepo {

	Logger logger = LogManager.getLogger(MDMRepoImpl.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
	}

	@Autowired
	private ZooKeeperConnector zooKeeperConnector;

	public void createSAGAStep(Saga saga) throws RepoException {
		String sagaPath = SagaHelper.createPath(SagaConstants.sagaroot, saga.getSagaModel().getSaga());
		try {
			Stat sagaExists;
			sagaExists = zooKeeperConnector.getZookeeper().exists(sagaPath, Boolean.FALSE);
			if (sagaExists == null) {
				throw new RepoException(String.format("%s does not exist. Please create %s first.",
						saga.getSagaModel().getSaga(), saga.getSagaModel().getSaga()));
			}
			for (Step step : saga.getSteps()) {
				String stepPath = SagaHelper.createPath(sagaPath, step.getName());
				Stat stepExists = zooKeeperConnector.getZookeeper().exists(stepPath, Boolean.FALSE);
				if (stepExists != null)
					throw new RepoException(String.format("Step %s already exists.", step.getName()));
				byte[] data = mapper.writeValueAsBytes(step);
				zooKeeperConnector.getZookeeper().create(stepPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
		} catch (KeeperException | InterruptedException | JsonProcessingException e) {
			logger.error(e);
			throw new RepoException(e.getMessage());
		}
	}

	@Override
	public SAGAModel getSagaDetails(String saga) throws RepoException {
		String sagaPath = SagaHelper.createPath(SagaConstants.sagaroot, saga);
		Stat stat = new Stat();
		byte[] data;
		try {
			data = zooKeeperConnector.getZookeeper().getData(sagaPath, null, stat);
			return mapper.readValue(data, SAGAModel.class);
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	public List<String> getSagaNames() throws RepoException {
		try {
			return zooKeeperConnector.getZookeeper().getChildren(SagaConstants.sagaroot, Boolean.FALSE);
		} catch (KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void createSAGA(SAGAModel sagaModel) throws RepoException {
		try {
			Stat rootExists = zooKeeperConnector.getZookeeper().exists(SagaConstants.sagaroot, Boolean.FALSE);
			if (rootExists == null) {
				zooKeeperConnector.getZookeeper().create(SagaConstants.sagaroot, SagaConstants.sagaRootDesc.getBytes(),
						ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			String sagaPath = SagaHelper.createPath(SagaConstants.sagaroot, sagaModel.getSaga());
			Stat sagaExists = zooKeeperConnector.getZookeeper().exists(sagaPath, Boolean.FALSE);
			byte[] data = mapper.writeValueAsBytes(sagaModel);
			if (sagaExists == null) {
				zooKeeperConnector.getZookeeper().create(sagaPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			} else {
				throw new RepoException(String.format("%s already exists.", sagaModel.getSaga()));
			}
		} catch (KeeperException | InterruptedException | JsonProcessingException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public List<Saga> getSortedSagas() throws RepoException {
		try {
			List<String> sagaNames;
			sagaNames = zooKeeperConnector.getZookeeper().getChildren(SagaConstants.sagaroot, Boolean.FALSE);
			List<Saga> sagas = new ArrayList<>();
			if (sagaNames != null) {
				for (String sagaName : sagaNames) {
					Saga saga = new Saga();
					SAGAModel sagaModel = this.getSagaDetails(sagaName);
					saga.setSagaModel(sagaModel);
					String sagaPath = SagaHelper.createPath(SagaConstants.sagaroot, sagaName);
					List<String> stepNames = zooKeeperConnector.getZookeeper().getChildren(sagaPath, Boolean.FALSE);
					if (stepNames != null) {
						List<Step> steps = new ArrayList<>();
						Stat lastStepStat = null;
						for (String stepName : stepNames) {
							String stepPath = SagaHelper.createPath(sagaPath, stepName);
							Stat stat = new Stat();
							byte[] data = zooKeeperConnector.getZookeeper().getData(stepPath, null, stat);
							Step step = mapper.readValue(data, Step.class);
							if (lastStepStat == null || (lastStepStat.getCtime() <= stat.getCtime()))
								steps.add(step);
							else
								steps.add(steps.size() - 1, step);
							lastStepStat = stat;
						}
						saga.setSteps(steps);
					}
					sagas.add(saga);
				}
			}
			return sagas;
		} catch (KeeperException | InterruptedException | IOException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

	@Override
	public void delete(StepModel stepModel) throws RepoException {
		try {
			String sagaPath = SagaHelper.createPath(SagaConstants.sagaroot, stepModel.getSaga());
			List<String> stepNames = zooKeeperConnector.getZookeeper().getChildren(sagaPath, Boolean.FALSE);
			if (stepNames != null) {
				for (String stepName : stepNames) {
					String stepPath = SagaHelper.createPath(sagaPath, stepName);
					zooKeeperConnector.getZookeeper().delete(stepPath, 0);
				}
			}
			zooKeeperConnector.getZookeeper().delete(sagaPath, 0);
		} catch (KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}

	}
}
