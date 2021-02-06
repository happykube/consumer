package com.springcloud;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import io.swagger.annotations.ApiOperation;

@RestController
@RefreshScope
public class Controller {
	private final Logger log = Logger.getLogger(getClass());

	@Autowired
	private RestTemplate webhookRestTemplate;

	@Autowired
	private LoadBalancerClient lbClient;

	@Value("${sleeptime:1000}")
	private long sleepTime;

	@GetMapping("/greeting/{message}")
	@ApiOperation(value = "Test Ribbon")
	public String greeting(@PathVariable String message) {
		log.info("### Received: /greeting/" + message);

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

		log.info("### Sent: " + "[" + baseUrl + "] " + response.getBody());
		return "[" + baseUrl + "] " + response.getBody();

	}

	private static HttpEntity<?> getHeaders() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}

	@GetMapping("/hystrix/{param}")
	@ApiOperation(value = "test hystrix")
	@HystrixCommand(fallbackMethod = "testHystrixFallback")
	public List<String> testHystrix(@PathVariable String param) {
		log.info("### Received: /hystrix/" + param);

		String baseUrl = "";
		try {
			final ServiceInstance instance = lbClient.choose("hystrix-consumer");
			baseUrl = String.format("http://%s:%s/%s", instance.getHost(), instance.getPort(), "delay/" + param);
			System.out.println("Url: " + baseUrl);
		} catch (Exception e) {
			System.out.println("*** NO hystrix consumer service!!!");
			return Collections.emptyList();
		}
		ResponseEntity<List<String>> response = null;

		try {
			response = webhookRestTemplate.exchange(baseUrl, HttpMethod.GET, null,
					new ParameterizedTypeReference<List<String>>() {
					});
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("### Sent: " + response.getBody());
		return response.getBody();
	}

	public List<String> testHystrixFallback(String param, Throwable t) {
		System.err.println("###### ERROR =>" + t.toString());
		return Collections.emptyList();
	}

	@GetMapping("/delay/{param}")
	@ApiOperation(value = "test hystrix2")

	public String testHystrix2(@PathVariable String param) {
		log.info("### Received: /delay/" + param);
		if (!"pass".equals(param)) {
			try {
				Thread.sleep(sleepTime);
			} catch (Exception e) {
			}
		}

		String msg = "I'm Working !";
		log.info("### Sent: " + msg);
		return msg;
	}
}
