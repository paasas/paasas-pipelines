package io.paasas.pipelines.deployment.domain.model.app;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class Probe {
	int failureThreshold;
	int periodSeconds;
	TcpSocket tcpSocket;
	int timeoutSeconds;
}