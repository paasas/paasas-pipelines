package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.stereotype.Repository;

import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunDeploymentEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseCloudRunDeploymentRepository implements CloudRunDeploymentRepository {
	CloudRunDeploymentJpaRepository repository;

	@Override
	public List<CloudRunDeployment> findByImageAndTag(String image, String tag) {
		return repository.findByImageAndTag(image, tag)
				.stream()
				.map(CloudRunDeploymentEntity::to)
				.toList();
	}

	@Override
	public void registerDeployment(RegisterCloudRunDeployment registerCloudRunDeployment) {
		repository.save(CloudRunDeploymentEntity.from(registerCloudRunDeployment));
	}
}
