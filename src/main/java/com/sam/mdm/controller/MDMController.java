package com.sam.mdm.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sam.mdm.model.SAGAModel;
import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;
import com.sam.mdm.model.SagaResponse;
import com.sam.mdm.service.MDMService;
import com.sam.sec.model.StepModel;

@Controller
@RequestMapping("/saga")
public class MDMController {
	private Logger logger = LogManager.getLogger(MDMController.class);
	@Autowired
	private MDMService sagaService;

	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" }, path = "/names")
	@ResponseBody
	public List<String> getSagaNames(HttpServletResponse response) {
		try {
			return sagaService.getSagaNames();
		} catch (Exception e) {
			logger.error(e);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
		return new ArrayList<>();
	}

	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" }, path = "/{sagaName}")
	@ResponseBody
	public SAGAModel getSAGADetails(@PathVariable String sagaName, HttpServletResponse response) {
		try {
			return sagaService.getSagaDetails(sagaName);
		} catch (Exception e) {
			logger.error(e);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			return new SAGAModel();
		}
	}

	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	public String getSagas() {
		try {
			JSONObject responseDetailsJson = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			for (Saga saga : sagaService.getSagas()) {
				if (saga.getSteps() == null || saga.getSteps().size() == 0) {
					saga.getSteps().add(new Step());// this is done to reload
													// table when saga is added
				}
				for (Step step : saga.getSteps()) {
					JSONObject jsonArrayInner = new JSONObject();
					jsonArrayInner.put("sagaDetails", saga.getSagaModel().getSaga());
					jsonArrayInner.put("step", step.getName() == null ? "" : step.getName());
					// null checks here are for when only saga is added z
					jsonArrayInner.put("api", step.getApi() == null ? "" : step.getApi().getPath());
					jsonArrayInner.put("capi", step.getCapi() == null ? "" : step.getCapi().getPath());
					jsonArrayInner.put("apiMethod", step.getApi() == null ? "" : step.getApi().getMethod());
					jsonArrayInner.put("capiMethod", step.getCapi() == null ? "" : step.getCapi().getMethod());
					jsonArray.put(jsonArrayInner);
				}
			}
			responseDetailsJson.put("data", jsonArray);
			return responseDetailsJson.toString();
		} catch (Exception ex) {
			logger.error(ex);
			return "{\"data\":[]}";
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public SagaResponse addSaga(@RequestBody SAGAModel sagaModel, HttpServletResponse response) {
		SagaResponse sagaResponse = new SagaResponse();
		try {
			sagaService.createSAGA(sagaModel);
			sagaResponse.setSuccess(Boolean.TRUE);
			return sagaResponse;
		} catch (IllegalArgumentException | IllegalStateException exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError(exception.getMessage());
			return sagaResponse;
		} catch (Exception exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError("Something went wrong.");
			return sagaResponse;
		}
	}

	@RequestMapping(method = RequestMethod.POST, path = "/step")
	@ResponseBody
	public SagaResponse addStep(@RequestBody StepModel stepModel, HttpServletResponse response) {
		SagaResponse sagaResponse = new SagaResponse();
		try {
			sagaService.createStep(stepModel);
			sagaResponse.setSuccess(Boolean.TRUE);
			return sagaResponse;
		} catch (IllegalArgumentException | IllegalStateException exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError(exception.getMessage());
			return sagaResponse;
		} catch (Exception exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError("Something went wrong.");
			return sagaResponse;
		}
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	public SagaResponse deleteSaga(@RequestBody StepModel sagaModel, HttpServletResponse response) {
		SagaResponse sagaResponse = new SagaResponse();
		try {
			sagaService.delete(sagaModel);
			sagaResponse.setSuccess(Boolean.TRUE);
			return sagaResponse;
		} catch (IllegalArgumentException exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError(exception.getMessage());
			return sagaResponse;
		} catch (Exception exception) {
			logger.error(exception);
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			sagaResponse.setSuccess(Boolean.FALSE);
			sagaResponse.setError("Something went wrong.");
			return sagaResponse;
		}
	}
}