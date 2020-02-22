package com.sam.ui.repo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import com.sam.sec.model.TxData;
import com.sam.sec.model.TxDataModel;
import com.sam.zookeeper.ZooKeeperConnector;

@Repository
public class UIRepoImpl implements UIRepo {
	private static final Logger logger = LogManager.getLogger(UIRepoImpl.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
	}

	@Autowired
	private ZooKeeperConnector zooKeeperConnector;

	@Override
	public List<TxDataModel> getTransactions() throws RepoException {
		try {
			List<TxDataModel> stepDataList = new ArrayList<>();
			if (zooKeeperConnector.getZookeeper().exists(SagaConstants.txroot, null) == null) {
				return stepDataList;
			}
			List<String> txs = zooKeeperConnector.getZookeeper().getChildren(SagaConstants.txroot, null);
			if (txs != null) {
				for (String tx : txs) {
					String txPath = SagaHelper.createPath(SagaConstants.txroot, tx);
					byte[] txDataBytes = zooKeeperConnector.getZookeeper().getData(txPath, null, null);
					TxData txData = mapper.readValue(txDataBytes, TxData.class);
					List<String> txSteps = zooKeeperConnector.getZookeeper().getChildren(txPath, null);
					if (txSteps == null || txSteps.size() == 0) {
						TxDataModel txDataModel = new TxDataModel();
						txDataModel.setTx(tx);
						txDataModel.setTxStatus(txData.getStatus());
						stepDataList.add(txDataModel);
					} else if (txSteps != null) {
						Stat lastStepStat = null;
						for (String txStep : txSteps) {
							String txStepPath = SagaHelper.createPath(txPath, txStep);
							Stat stat = new Stat();
							byte[] data = zooKeeperConnector.getZookeeper().getData(txStepPath, null, stat);
							TxDataModel txDataModel = mapper.readValue(data, TxDataModel.class);
							txDataModel.setTx(tx);
							txDataModel.setStep(txStep);
							txDataModel.setTxStatus(txData.getStatus());
							if (lastStepStat == null || (lastStepStat.getCtime() <= stat.getCtime()))
								stepDataList.add(txDataModel);
							else
								stepDataList.add(stepDataList.size() - 1, txDataModel);
							lastStepStat = stat;
						}
					}
				}
			}
			return stepDataList;
		} catch (IOException | KeeperException | InterruptedException exception) {
			logger.error(exception);
			throw new RepoException(exception.getMessage());
		}
	}

}
