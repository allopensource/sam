package com.sam.mdm.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sam.helper.RepoException;
import com.sam.helper.SagaValidator;
import com.sam.helper.SamException;
import com.sam.mdm.model.SAGAModel;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.mdm.model.Saga.Step.API;
import com.sam.mdm.repo.MDMRepo;
import com.sam.sec.model.StepModel;

@Service
public class MDMService {

	@Autowired
	private MDMRepo mdmRepo;

	public void createStep(StepModel stepModel) throws RepoException, SamException {
		// add to zookeeper first
		List<Step> steps = new ArrayList<>();
		Step step = new Step();
		API api = new API();
		API capi = new API();
		//
		api.setPath(stepModel.getApiPath());
		api.setMethod(stepModel.getApiMethod().toUpperCase());
		//
		capi.setPath(stepModel.getCapiPath());
		capi.setMethod(stepModel.getCapiMethod().toUpperCase());
		step.setName(stepModel.getStep());
		step.setApi(api);
		step.setCapi(capi);
		//
		steps.add(step);
		Saga saga = new Saga();
		SAGAModel sagaModel = this.getSagaDetails(stepModel.getSaga());
		saga.setSteps(steps);
		saga.setSagaModel(sagaModel);
		SagaValidator.validateSAGA(saga);
		mdmRepo.createSAGAStep(saga);
	}

	public List<Saga> getSagas() throws RepoException {
		return mdmRepo.getSortedSagas();
	}

	public Optional<Saga> getSaga(String transaction) throws RepoException {
		List<Saga> sagas = this.getSagas();
		return sagas.stream().filter(saga -> saga.getSagaModel().getSaga().equals(transaction)).findAny();
	}

	public void delete(StepModel stepModel) throws RepoException {
		mdmRepo.delete(stepModel);
	}

	public void createSAGA(SAGAModel sagaModel) throws RepoException, SamException {
		if (!StringUtils.hasText(sagaModel.getSaga())) {
			throw new SamException("Please provide SAGA name.");
		}
		if (!StringUtils.hasText(sagaModel.getFailoverAction())) {
			throw new SamException("Please select failover action.");
		}
		if (sagaModel.getHmacSecured() != null && sagaModel.getHmacSecured().equals("on")) {
			SecureRandom random = new SecureRandom();
			byte bytes[] = new byte[32];
			random.nextBytes(bytes);
			sagaModel.setHmac(Base64.encodeBase64String(bytes));
		}
		mdmRepo.createSAGA(sagaModel);
	}

	public List<String> getSagaNames() throws RepoException {
		return mdmRepo.getSagaNames();

	}

	public SAGAModel getSagaDetails(String saga) throws RepoException, SamException {
		if (!StringUtils.hasText(saga)) {
			throw new SamException("SAGA name is absent.");
		}
		return mdmRepo.getSagaDetails(saga);
	}
}
