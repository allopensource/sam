package com.sam.ui.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sam.sec.model.TxDataModel;
import com.sam.ui.service.UIService;

@RestController
@RequestMapping(value = "/transactions")
public class UIController {
	private Logger logger = LogManager.getLogger(UIController.class);

	@Autowired
	private UIService uiService;

	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
	@ResponseBody
	public String getTransactions() {
		try {
			JSONObject responseDetailsJson = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			List<TxDataModel> sagaTransactions = uiService.getTransactions();
			for (TxDataModel sagaTxData : sagaTransactions) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tx", sagaTxData.getTx() == null ? "-" : sagaTxData.getTx());
				jsonObject.put("txStatus", sagaTxData.getTxStatus() == null ? "-" : sagaTxData.getTxStatus());
				jsonObject.put("step", sagaTxData.getStep() == null ? "-" : sagaTxData.getStep());
				jsonObject.put("apiPayload", sagaTxData.getApiPayload() == null ? "-" : sagaTxData.getApiPayload());
				jsonObject.put("apiResponse", sagaTxData.getApiResponse() == null ? "-" : sagaTxData.getApiResponse());
				jsonObject.put("apiResponseStatus",
						sagaTxData.getApiResponseStatus() == null ? "-" : sagaTxData.getApiResponseStatus().value());
				jsonObject.put("capiPayload", sagaTxData.getCapiPayload() == null ? "-" : sagaTxData.getCapiPayload());
				jsonObject.put("capiResponse",
						sagaTxData.getCapiResponse() == null ? "-" : sagaTxData.getCapiResponse());
				jsonObject.put("capiResponseStatus",
						sagaTxData.getCapiResponseStatus() == null ? "-" : sagaTxData.getCapiResponseStatus().value());
				jsonObject.put("status", sagaTxData.getStatus() == null ? "-" : sagaTxData.getStatus());
				jsonArray.put(jsonObject);
			}
			responseDetailsJson.put("data", jsonArray);
			return responseDetailsJson.toString();
		} catch (Exception ex) {
			logger.error("Exception in fetching tx data", ex);
			return "{\"data\":[]}";
		}
	}
}
