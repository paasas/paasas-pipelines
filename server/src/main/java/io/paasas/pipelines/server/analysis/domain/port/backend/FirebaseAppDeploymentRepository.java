package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import org.springframework.data.domain.Sort;

import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterFirebaseAppDeployment;

public interface FirebaseAppDeploymentRepository {
	List<FirebaseAppDeployment> find(FindDeploymentRequest findRequest, Sort sort);

	void registerDeployment(RegisterFirebaseAppDeployment registerFirebaseAppDeployment);
}
