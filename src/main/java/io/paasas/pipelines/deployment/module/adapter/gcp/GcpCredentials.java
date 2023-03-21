package io.paasas.pipelines.deployment.module.adapter.gcp;

import java.io.IOException;
import java.util.List;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;

import io.paasas.pipelines.GcpConfiguration;
import io.paasas.pipelines.deployment.module.adapter.gcp.cloudrun.Credentials;

public class GcpCredentials {
	public static FixedCredentialsProvider credentialProviders(GcpConfiguration gcpConfiguration) {
		try {
			var cloudCredentials = gcpConfiguration.getCredentialsJson() == null
					|| gcpConfiguration.getCredentialsJson().isBlank()
							? GoogleCredentials.getApplicationDefault()
							: Credentials.credentials(gcpConfiguration.getCredentialsJson());

			return FixedCredentialsProvider.create(gcpConfiguration.getImpersonateServiceAccount() != null
					&& !gcpConfiguration.getImpersonateServiceAccount().isBlank()
					? ImpersonatedCredentials.create(
									cloudCredentials,
									gcpConfiguration.getImpersonateServiceAccount(),
									List.of(),
									List.of("https://www.googleapis.com/auth/cloud-platform"),
									360)
							: cloudCredentials);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
