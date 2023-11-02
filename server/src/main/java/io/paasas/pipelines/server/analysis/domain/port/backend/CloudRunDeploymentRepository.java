package io.paasas.pipelines.server.analysis.domain.port.backend;

import java.util.List;

import org.springframework.data.domain.Sort;

import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;

public interface CloudRunDeploymentRepository {
	List<CloudRunDeployment> findByImageAndTag(String image, String tag, Sort sort);

	void registerDeployment(RegisterCloudRunDeployment registerCloudRunDeployment);
}
