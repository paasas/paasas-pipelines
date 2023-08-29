package io.paasas.pipelines.server.analysis.domain.model;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RegisterTerraformDeploymentTest {
	private static final String PAYLOAD = """
			{
			  "jobInfo": {
			    "build": "34",
			    "job": "terraform-apply-cloud-storage",
			    "pipeline": "advisor-client-facing-dev-ax360-deployment",
			    "projectId": "iapw-acf-d-ax360-dev1-1000",
			    "team": "ia-mgcp",
			    "url": "https://concourse.ci.ia-mgcp.ca"
			  },
			  "gitRevision": {
			    "commit": "ce05926e0e9b62be8099495de37af5d83f3578a3",
			    "commitAuthor": "ckrebeca@gmail.com",
			    "repository": "ia-mgcp/ia-iac-deployments",
			    "tag": "1.0.0"
			  },
			  "packageName": "cloud-storage",
			  "params": {"project_id":"iapw-acf-d-ax360-dev1-1000"}
			}""";

	@Test
	public void canDeserialize() throws JsonProcessingException {
		new ObjectMapper().findAndRegisterModules().readValue(PAYLOAD, RegisterTerraformDeployment.class);
	}
}
