package com.sam.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import com.sam.helper.RepoException;
import com.sam.helper.SamException;
import com.sam.mdm.model.SAGAModel;
import com.sam.mdm.service.MDMService;

public class TokenUtil {
	private static String tokenName = "hmac";
	private static String authType = "Bearer";

	// https://www.jokecamp.com/blog/examples-of-creating-base64-hashes-using-hmac-sha256-in-different-languages/

	public static void validateHMAC(HttpServletRequest request, HttpServletResponse response, String path,
			String payload, MDMService sagaService)
			throws RepoException, SamException, IOException, InvalidKeyException, NoSuchAlgorithmException {
		Boolean isHMACPresent = isHMACPresent(request, response);
		if (isHMACPresent) {
			String hmacFromRequest = getBase64HMACFromRequest(request);
			String calculatedHmac = createEncodedDigest(request, path, payload, sagaService);
			if (!hmacFromRequest.equals(calculatedHmac)) {
				response.sendError(HttpStatus.UNAUTHORIZED.value(),
						"error=Invalid Authorization,error_description=Invalid hmac for saga.");
			}
		}
	}

	private static String getHMACKeyForSAGA(HttpServletRequest request, String path, MDMService sagaService)
			throws RepoException, SamException {
		String sagaName = request.getServletPath().substring(path.length() + 1);
		SAGAModel sagaModel = sagaService.getSagaDetails(sagaName);
		return sagaModel.getHmac();
	}

	private static String createEncodedDigest(HttpServletRequest httpServletRequest, String path, String payload,
			MDMService sagaService) throws RepoException, SamException, NoSuchAlgorithmException, InvalidKeyException {
		String sagaKey = getHMACKeyForSAGA(httpServletRequest, path, sagaService);
		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		SecretKeySpec secret_key = new SecretKeySpec(sagaKey.getBytes(), "HmacSHA256");
		sha256_HMAC.init(secret_key);
		return Base64.encodeBase64String(sha256_HMAC.doFinal(payload.getBytes()));
	}

	private static String getBase64HMACFromRequest(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		int startIndex = header.indexOf(tokenName) + tokenName.length() + 1;
		int endIndex = startIndex + 44;
		return header.substring(startIndex, endIndex);
	}

	public static String getAuthHeaderWithoutHMAC(HttpServletRequest request) {
		// assuming hmac is always present
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		int startIndex = header.indexOf(tokenName);
		int endIndex = startIndex + 44 + 1;
		String hmacData = header.substring(startIndex, endIndex);
		return StringUtils.replace(header, hmacData, "");
	}

	private static Boolean isHMACPresent(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		Boolean isPresent = Boolean.TRUE;
		if (!StringUtils.hasText(header)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value(),
					"error=No Authorization,error_description=No Authorization header");
			isPresent = Boolean.FALSE;
		} else if (!header.startsWith(authType)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value(),
					"error=No Authorization,error_description=No Bearer token");
			isPresent = Boolean.FALSE;
		} else if (!header.startsWith(authType + " " + tokenName)) {
			response.sendError(HttpStatus.UNAUTHORIZED.value(),
					"error=No Authorization,error_description=No hmac information");
			isPresent = Boolean.FALSE;
		} else if (header.length() < (44 + 6 + 1 + 4 + 1)) {
			// 6 for bearer,1 for space,4 for hmac,1 for space,44 for encoded
			// SHA 256
			response.sendError(HttpStatus.UNAUTHORIZED.value(),
					"error=Invalid Authorization,error_description=Invalid hmac information");
			isPresent = Boolean.FALSE;
		}
		return isPresent;
	}

	public static boolean shouldFilter(HttpServletRequest request, String path, MDMService sagaService)
			throws RepoException, SamException {
		String sagaName = request.getServletPath().substring(path.length() + 1);
		SAGAModel sagaModel = sagaService.getSagaDetails(sagaName);
		if (sagaModel.getHmacSecured() != null && sagaModel.getHmacSecured().equals("on")) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}