package com.sam;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@EnableZuulProxy
public class SagaServerApplication {

	@Value("${saga-server.connectTimeOut}")
	private Integer connectTimeOut;
	@Value("${saga-server.readTimeOut}")
	private Integer readTimeOut;

	public static void main(String[] args) {
		SpringApplication.run(SagaServerApplication.class, args);
	}

	@Bean
	@LoadBalanced
	/*@ConditionalOnProperty(name = "saga-server.enableServiceDiscovery", havingValue = "true")*/
	RestTemplate restTemplateLoadBalanced() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeOut == null ? 30000 : connectTimeOut);
		factory.setReadTimeout(readTimeOut == null ? 30000 : readTimeOut);
		return new RestTemplate(factory);
	}
/*
	@Bean
	@ConditionalOnProperty(name = "saga-server.enableServiceDiscovery", havingValue = "false")
	RestTemplate restTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeOut == null ? 30000 : connectTimeOut);
		factory.setReadTimeout(readTimeOut == null ? 30000 : readTimeOut);
		return new RestTemplate(factory);
	}*/
}