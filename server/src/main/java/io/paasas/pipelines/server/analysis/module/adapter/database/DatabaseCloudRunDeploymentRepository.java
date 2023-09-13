package io.paasas.pipelines.server.analysis.module.adapter.database;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;

import io.paasas.pipelines.deployment.domain.model.deployment.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunDeploymentEntity;
import io.paasas.pipelines.server.analysis.module.adapter.database.entity.CloudRunTestReportEntity;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DatabaseCloudRunDeploymentRepository implements CloudRunDeploymentRepository {
	CloudRunDeploymentJpaRepository repository;
	CloudRunTestReportJpaRepository testReportRepository;

	@Override
	public List<CloudRunDeployment> findByImageAndTag(String image, String tag) {
		return repository.findByImageAndTag(image, tag)
				.stream()
				.map(this::to)
				.toList();
	}

	@Override
	@Transactional
	public void registerDeployment(RegisterCloudRunDeployment registerCloudRunDeployment) {
		var latestDeployment = repository
				.findByDeploymentInfoProjectIdAndApp(
						registerCloudRunDeployment.getJobInfo().getProjectId(),
						registerCloudRunDeployment.getApp().getName(),
						PageRequest.of(0, 1, Direction.DESC, "deploymentInfo.timestamp"))
				.stream()
				.findFirst();

		if (latestDeployment.isPresent()
				&& to(latestDeployment.get()).getApp().equals(registerCloudRunDeployment.getApp())) {
			return;
		}

		repository.save(CloudRunDeploymentEntity.from(registerCloudRunDeployment));
	}

	private CloudRunDeployment to(CloudRunDeploymentEntity deployment) {
		return deployment.to(
				testReportRepository.findByImageAndTag(deployment.getImage(), deployment.getTag())
						.stream()
						.map(CloudRunTestReportEntity::to)
						.toList());
	}

}
