package com.sam.helper;

import java.util.Arrays;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.sam.sec.model.StepData;

public class SagaHelper {

	public static String createPath(String... steps) {
		if (steps == null)
			return null;
		return Arrays.asList(steps).toString().replace("[", "").replace("]", "").replaceAll(",", "/").replaceAll(" ",
				"");
	}

	@SuppressWarnings("unchecked")
	public static void createCAPIPayload(StepData stepData) throws JSONException {

		// in case the first step fails api response will be null
		String apiResponse = stepData.getApiResponse() == null ? "{}"
				: stepData.getApiResponse().startsWith("{apierror:") ? "{}" : stepData.getApiResponse();
		JSONObject[] jsonObjects = { new JSONObject(stepData.getApiPayload()), new JSONObject(apiResponse) };
		JSONObject jsonObject = new JSONObject();
		for (JSONObject temp : jsonObjects) {
			Iterator<String> keys = temp.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				jsonObject.put(key, temp.get(key));
			}

		}
		stepData.setCapiPayload(jsonObject.toString());
	}
}
