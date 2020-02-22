package com.sam.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sam.helper.RepoException;
import com.sam.helper.SamException;
import com.sam.mdm.service.MDMService;

/**
 * @author Z003SSDS
 *
 */
@Component
public final class TokenFilter extends OncePerRequestFilter {
	private static final Logger logger = LogManager.getLogger(TokenFilter.class);

	@Autowired
	private MDMService mdmService;
	private String path;

	public TokenFilter() {
		this("/transaction");
	}

	public TokenFilter(String path) {
		Assert.hasLength(path, "URL cannot be blank");
		this.path = path;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// payload needs to be extracted here and set as request attribute
		// because it cannot be read twice i.e. once again by spring controller
		String payload = request.getContentLength() <= 0 ? ""
				: request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		request.setAttribute("payload", payload);
		try {
			Boolean shouldFilter = TokenUtil.shouldFilter(request, path, mdmService);
			if (!shouldFilter) {
				filterChain.doFilter(request, response);
				return;
			}
		} catch (IllegalArgumentException e) {
			logger.error("Invalid data ", e);
			response.sendError(HttpStatus.BAD_REQUEST.value(),
					"error=Invalid data,error_description=" + e.getMessage());
			return;
		} catch (Exception e) {
			logger.error("Exception occurred in should filter", e);
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"error=Unexpected result,error_description=" + e.getMessage());
			return;
		}
		try {
			// validate the hmac
			TokenUtil.validateHMAC(request, response, path, payload, mdmService);
			// if hmac is ok skip spring security and forward the request to
			// transaction controller
			// using servlet path to forward this request will make sure the
			// spring controller gets correct path variable as transaction name
			if (!response.isCommitted() && response.getStatus() != HttpStatus.UNAUTHORIZED.value()) {
				logger.info("TokenFilter request: OK, Forwarding " + request.getServletPath());
				request.getRequestDispatcher(request.getServletPath()).forward(request, response);
			} else {
				logger.info("TokenFilter request: Not OK, Blocking " + request.getServletPath());
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | RepoException | SamException e) {
			if (!response.isCommitted()) {
				response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"error=Unexpected result,error_description=" + e.getMessage());
			}
		}
	}

	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
		logger.debug("TokenFilter path: {}", request.getServletPath());
		return !request.getServletPath().startsWith(path) || !request.getMethod().equals("POST");
	}

}
