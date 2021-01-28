package com.springcloud;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.ApiOperation;

@RestController
//@RequiredArgsConstructor
public class Controller {
	@Autowired
	private RestTemplate webhookRestTemplate;

	@Autowired
	private LoadBalancerClient lbClient;

	@GetMapping("/greeting/{message}")
	@ApiOperation(value = "Test Ribbon")
	public String greeting(@PathVariable String message) {
		String baseUrl = "";
		try {
			final ServiceInstance instance = lbClient.choose("webhook");
			baseUrl = String.format("http://%s:%s/%s", instance.getHost(), instance.getPort(), "greeting/" + message);
			System.out.println("Url: " + baseUrl);
		} catch (Exception e) {
			System.out.println("*** NO webhook service!!!");
			return "NO DATA";
		}
		ResponseEntity<String> response = null;

		try {
			response = webhookRestTemplate.exchange(baseUrl, HttpMethod.GET, getHeaders(), String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "[" + baseUrl + "] " + response.getBody();

	}

	private static HttpEntity<?> getHeaders() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}
