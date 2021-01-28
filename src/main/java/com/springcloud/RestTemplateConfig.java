package com.springcloud;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	public RestTemplate getRestTemplate(int defaultMaxPerRoute, int maxTotal) {
		return new RestTemplate() {
			{
				setRequestFactory(new HttpComponentsClientHttpRequestFactory(
						HttpClientBuilder.create().setConnectionManager(new PoolingHttpClientConnectionManager() {
							{
								setDefaultMaxPerRoute(defaultMaxPerRoute);
								setMaxTotal(maxTotal);
							}
						}).build()) {
					{
						setConnectTimeout(2000);
						setReadTimeout(5000);
					}
				});
			}
		};

	/*
	 * 위 수행을 좀 더 쉽게 코딩하면 아래와 같습니다.  
	 * 	PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connManager.setMaxTotal(maxTotal);

		HttpClient client = HttpClientBuilder.create().setConnectionManager(connManager).build();

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(client);
		factory.setConnectTimeout(2000);
		factory.setReadTimeout(5000);

		return new RestTemplate(factory);

	 */
	}

	@Bean
	public RestTemplate webhookRestTemplate() {
		return getRestTemplate(20, 10);
	}
}
