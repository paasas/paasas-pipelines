package io.paasas.pipelines.deployment.module.adapter.gcp.cloudrun;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;

public final class Credentials {
	private Credentials() {
	}

	public static GoogleCredentials credentials(String credentialsJson) {
		try {
			return GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsJson.getBytes()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
