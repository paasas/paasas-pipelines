package io.paasas.pipelines.server.analysis.module.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.paasas.pipelines.server.PaasasPipelinesServerApplication;

@SpringBootTest(classes = PaasasPipelinesServerApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.profiles.active=secrets,test")
public abstract class AnalysisWebTest {
	@Autowired
	protected WebTestClient client;
}
