package com.sam.helper;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import com.sam.mdm.model.Saga;
import com.sam.mdm.model.Saga.Step;

public class SagaValidator {

	public static void validateSAGA(Saga saga) throws SamException {
		if (!StringUtils.hasText(saga.getSagaModel().getSaga())) {
			throw new SamException("SAGA name is invalid.");
		}
		List<Step> allSteps = saga.getSteps();
		for (Step step : allSteps) {
			if (!StringUtils.hasText(step.getName()))
				throw new SamException("Step name is invalid.");
			if (!StringUtils.hasText(step.getApi().getPath()))
				throw new SamException("API path is invalid.");
			if (StringUtils.isEmpty(step.getCapi().getPath()))
				throw new SamException("C-API path is blank");
			if (!StringUtils.startsWithIgnoreCase(step.getApi().getPath(), "/"))
				throw new SamException("API path should start with / for step :: " + step.getName());
			if (!StringUtils.startsWithIgnoreCase(step.getCapi().getPath(), "/"))
				throw new SamException("C-API path should start with / for step :: " + step.getName());
			if (StringUtils.countOccurrencesOf(step.getApi().getPath(), "/") < 2)
				throw new SamException(
						"API path cannot be root /. Please provide proper path for step :: " + step.getName());
			if (StringUtils.countOccurrencesOf(step.getCapi().getPath(), "/") < 2)
				throw new SamException(
						"C-API path cannot be root /. Please provide proper path for step :: " + step.getName());
			if (step.getApi().getMethod() == null || (!HttpMethod.PUT.matches(step.getApi().getMethod())
					&& !HttpMethod.POST.matches(step.getApi().getMethod())
					&& !HttpMethod.DELETE.matches(step.getApi().getMethod())))
				throw new SamException("HTTP method [PUT/POST/DELETE] absent for api :: " + step.getApi().getPath());
			if (step.getCapi().getMethod() == null || (!HttpMethod.PUT.matches(step.getCapi().getMethod())
					&& !HttpMethod.POST.matches(step.getCapi().getMethod())
					&& !HttpMethod.DELETE.matches(step.getCapi().getMethod())))
				throw new SamException("HTTP method [PUT/POST/DELETE] absent for c-api :: " + step.getCapi().getPath());
		}
	}
}
