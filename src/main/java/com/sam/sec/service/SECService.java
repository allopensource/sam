package com.sam.sec.service;

import java.util.ListIterator;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sam.helper.RepoException;
import com.sam.helper.SagaHelper;
import com.sam.helper.SamException;
import com.sam.helper.TxStatusEnums;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.mdm.service.MDMService;
import com.sam.sec.model.StepData;
import com.sam.sec.repo.SECRepo;

@Service
public class SECService {
	private Logger logger = LogManager.getLogger(SECService.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
	}

	@Autowired
	private MDMService sagaService;

	@Autowired
	private SECRepo secRepo;

	@Autowired
	private RestTemplate restTemplate;

	public void execute(String saga, String payload) throws RepoException, SamException {
		logger.debug("Executing :: {}", saga);
		Optional<Object> preStep = secRepo.preProcess(saga, payload);
		Boolean stepsSuccess = this.executeSteps(saga, preStep, payload);
		secRepo.postProcess(payload, stepsSuccess, preStep);
		if (!stepsSuccess)
			throw new SamException("Unable to complete SAGA.");
	}

	public Boolean executeSteps(String saga, Optional<Object> preStepData, String payload) throws RepoException {
		Optional<Saga> sagaObj = sagaService.getSaga(saga);
		Boolean executionSuccess = Boolean.FALSE;
		if (sagaObj.isPresent()) {
			ListIterator<Step> iterator = sagaObj.get().getSteps().listIterator();
			while (iterator.hasNext()) {
				Step step = iterator.next();
				logger.debug("Executing s.tep :: {} ", step.getName());
				executionSuccess = this.executeStep(step, preStepData, payload);
				logger.debug("Step success :: {}", executionSuccess);
				if (!executionSuccess) {
					logger.debug("All the previous steps will be attempted to be roll backed");
					while (iterator.hasPrevious()) {
						Step prevStep = iterator.previous();
						logger.debug("Executing previous step :: {} ", prevStep.getName());
						this.executeCompStep(prevStep, preStepData);
					}
					break;
				}
			}
		}
		return executionSuccess;
	}

	public Boolean executeStep(Step step, Optional<Object> preStepData, String payload) {
		Boolean isTxSuccess = false;
		StepData stepData = new StepData();
		stepData.setStatus(TxStatusEnums.Step.STARTED);
		stepData.setApiPayload(payload);
		try {
			secRepo.preExecuteStep(step, stepData, preStepData);
			try {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> httpEntity = new HttpEntity<String>(payload, headers);
				ResponseEntity<String> response = restTemplate.exchange("http:/" + step.getApi().getPath(),
						HttpMethod.resolve(step.getApi().getMethod()), httpEntity, String.class);
				stepData.setApiResponse(response.hasBody() ? response.getBody() : "");
				stepData.setApiResponseStatus(response.getStatusCode());
				stepData.setStatus(TxStatusEnums.Step.SUCCESS);
				secRepo.setStepData(step, stepData, preStepData);
				isTxSuccess = Boolean.TRUE;
			} catch (HttpClientErrorException clientErrorException) {
				stepData.setApiResponse("{apierror:" + clientErrorException.getResponseBodyAsString() + "}");
				stepData.setApiResponseStatus(clientErrorException.getStatusCode());
				stepData.setStatus(TxStatusEnums.Step.FAILED);
				secRepo.setStepData(step, stepData, preStepData);
			} catch (Exception th) {
				logger.error("Step {} has thrown exception", step.getName(), th);
				stepData.setStatus(TxStatusEnums.Step.FAILED);
				secRepo.setStepData(step, stepData, preStepData);
			}
		} catch (RepoException exception) {
			logger.error("Step {} has thrown exception", step.getName(), exception);
		}
		return isTxSuccess;
	}

	public void executeCompStep(Step step, Optional<Object> preStepData) {
		StepData stepData = null;
		try {
			try {
				stepData = secRepo.getStepData(step, preStepData);
				SagaHelper.createCAPIPayload(stepData);
				stepData.setStatus(TxStatusEnums.Step.COMPTXSTARTED);
				secRepo.setStepData(step, stepData, preStepData);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<String> httpEntity = new HttpEntity<String>(stepData.getCapiPayload(), headers);
				ResponseEntity<String> response = restTemplate.exchange("http:/" + step.getCapi().getPath(),
						HttpMethod.resolve(step.getCapi().getMethod()), httpEntity, String.class);
				stepData.setCapiResponse(response.hasBody() ? response.getBody() : "");
				stepData.setCapiResponseStatus(response.getStatusCode());
				stepData.setStatus(TxStatusEnums.Step.COMPTXSUCCESS);
				secRepo.setStepData(step, stepData, preStepData);
			} catch (HttpClientErrorException clientErrorException) {
				stepData.setCapiResponse("{capierror:" + clientErrorException.getResponseBodyAsString() + "}");
				stepData.setCapiResponseStatus(clientErrorException.getStatusCode());
				stepData.setStatus(TxStatusEnums.Step.COMPTXFAILED);
				secRepo.setStepData(step, stepData, preStepData);
			} catch (Exception ex) {
				if (stepData != null) {
					stepData.setStatus(TxStatusEnums.Step.COMPTXFAILED);
					secRepo.setStepData(step, stepData, preStepData);
				} else {
					logger.error(
							"Previous step {}  comp action has thrown exception and step data is not available.Cannot update the step data . Status of the transaction is unknown!!",
							step.getName(), ex);
				}
			}
		} catch (RepoException exception) {
			logger.error("Previous step {} comp action has thrown exception.Status of the transaction is unknown!!",
					step.getName(), exception);
		}
	}

}