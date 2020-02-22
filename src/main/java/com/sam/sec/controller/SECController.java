package com.sam.sec.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sam.helper.SagaValidator;
import com.sam.mdm.model.Saga;
import com.sam.mdm.service.MDMService;
import com.sam.sec.model.TxResponse;
import com.sam.sec.service.SECService;

@RestController
@RequestMapping(value = "/transaction")
public class SECController {
	private Logger logger = LogManager.getLogger(SECController.class);

	@Autowired
	private SECService secService;

	@Autowired
	private MDMService mdmService;

	@RequestMapping(method = RequestMethod.POST, path = "/{saga}", consumes = { "application/json" })
	@ResponseBody
	public TxResponse createTransaction(@PathVariable String saga, HttpServletRequest request,
			HttpServletResponse response) throws KeeperException, InterruptedException {
		TxResponse txResponse = new TxResponse();
		try {
			String payload = (String) request.getAttribute("payload");
			if (StringUtils.isEmpty(saga)) {
				throw new IllegalArgumentException("Transaction name is blank");
			}
			Optional<Saga> sagaObj = mdmService.getSaga(saga);
			if (!sagaObj.isPresent()) {
				throw new IllegalArgumentException(String.format("Transaction name %s is invalid", saga));
			}
			SagaValidator.validateSAGA(sagaObj.get());
			secService.execute(saga, payload);
			txResponse.setSuccess(Boolean.TRUE);
		} catch (IllegalStateException exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			txResponse.setSuccess(Boolean.FALSE);
			txResponse.setError(exception.getMessage());
		} catch (IllegalArgumentException exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			txResponse.setSuccess(Boolean.FALSE);
			txResponse.setError(exception.getMessage());
		} catch (Exception exception) {
			logger.error("Exception in creating tx", exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			txResponse.setSuccess(Boolean.FALSE);
			txResponse.setError("Unable to complete SAGA.");
		}
		return txResponse;
	}

}