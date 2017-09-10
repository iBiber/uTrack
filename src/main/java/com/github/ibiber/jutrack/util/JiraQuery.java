package com.github.ibiber.jutrack.util;

import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.github.ibiber.jutrack.data.Credentials;

@Component
public class JiraQuery {
	private static final Logger LOGGER = LoggerFactory.getLogger(JiraQuery.class);

	private RestTemplate restTemplate;

	public JiraQuery(RestTemplateBuilder builder) {
		this.restTemplate = builder.build();
	}

	public <T> T query(Credentials credentials, Class<T> responseType, String path, String queryParameters)
	        throws Exception {
		String queryUrl = path + "?" + queryParameters;

		LOGGER.info("Query: " + queryUrl);

		HttpHeaders headers = new HttpHeaders();
		buildBasicAuthorizationHeader(headers, credentials);

		ResponseEntity<T> exchangeResult = restTemplate.exchange(queryUrl, HttpMethod.GET,
		        new HttpEntity<String>(headers), responseType);

		if (exchangeResult.getStatusCode().is2xxSuccessful()) {
			LOGGER.info("Query successful");
		} else {
			LOGGER.error("Query returns an error: " + exchangeResult.getStatusCode());
		}

		return exchangeResult.getBody();
	}

	void buildBasicAuthorizationHeader(HttpHeaders headers, Credentials credentials) {
		String encodeToString = Base64.getMimeEncoder()
		        .encodeToString((credentials.userName + ":" + credentials.password).getBytes());
		headers.add("Authorization", "Basic " + encodeToString);
	}
}
