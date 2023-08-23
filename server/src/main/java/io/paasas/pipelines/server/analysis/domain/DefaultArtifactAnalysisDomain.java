package io.paasas.pipelines.server.analysis.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.paasas.pipelines.deployment.domain.model.DeploymentManifest;
import io.paasas.pipelines.deployment.domain.model.TerraformWatcher;
import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.deployment.domain.model.firebase.FirebaseAppDefinition;
import io.paasas.pipelines.server.analysis.domain.model.AnalysisStatus;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.FindDeploymentRequest;
import io.paasas.pipelines.server.analysis.domain.model.FirebaseAppAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.TerraformAnalysis;
import io.paasas.pipelines.server.analysis.domain.port.api.ArtifactAnalysisDomain;
import io.paasas.pipelines.server.analysis.domain.port.backend.CloudRunDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.FirebaseAppDeploymentRepository;
import io.paasas.pipelines.server.analysis.domain.port.backend.TerraformDeploymentRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultArtifactAnalysisDomain implements ArtifactAnalysisDomain {
	CloudRunDeploymentRepository cloudRunDeploymentRepository;
	FirebaseAppDeploymentRepository firebaseAppDeploymentRepository;
	TerraformDeploymentRepository terraformDeploymentRepository;

	@Override
	public Optional<FirebaseAppAnalysis> firebaseAppAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getFirebaseApp())
				.map(this::firebaseAppAnalysis);
	}

	FirebaseAppAnalysis firebaseAppAnalysis(FirebaseAppDefinition firebaseAppDefinition) {
		if (firebaseAppDefinition.getGit().getTag() == null || firebaseAppDefinition.getGit().getTag().isBlank()) {
			return FirebaseAppAnalysis.builder()
					.deployments(List.of())
					.status(AnalysisStatus.TAG_REQUIRED)
					.build();
		}

		return FirebaseAppAnalysis.builder()
				.deployments(firebaseAppDeploymentRepository.find(new FindDeploymentRequest(
						firebaseAppDefinition.getGit().getUri(),
						firebaseAppDefinition.getGit().getPath(),
						firebaseAppDefinition.getGit().getTag())))
				.status(AnalysisStatus.REVISION_RESOLVED)
				.build();
	}

	@Override
	public List<TerraformAnalysis> terraformAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getTerraform())
				.orElse(List.of())
				.stream()
				.map(this::terraformAnalysis)
				.toList();
	}

	TerraformAnalysis terraformAnalysis(TerraformWatcher terraformWatcher) {
		if (terraformWatcher.getGit().getTag() == null || terraformWatcher.getGit().getTag().isBlank()) {
			return TerraformAnalysis.builder()
					.deployments(List.of())
					.packageName(terraformWatcher.getName())
					.status(AnalysisStatus.TAG_REQUIRED)
					.build();
		}

		return TerraformAnalysis.builder()
				.deployments(terraformDeploymentRepository.find(new FindDeploymentRequest(
						terraformWatcher.getGit().getUri(),
						terraformWatcher.getGit().getPath(),
						terraformWatcher.getGit().getTag())))
				.packageName(terraformWatcher.getName())
				.status(AnalysisStatus.REVISION_RESOLVED)
				.build();
	}

	CloudRunAnalysis appAnalysis(App app) {
		return CloudRunAnalysis.builder()
				.deployments(cloudRunDeploymentRepository.findByImageAndTag(app.getImage(), app.getTag()))
				.serviceName(app.getName())
				.build();
	}

	@Override
	public List<CloudRunAnalysis> cloudRunAnalysis(DeploymentManifest deploymentManifest) {
		return Optional.ofNullable(deploymentManifest.getApps())
				.orElse(List.of())
				.stream()
				.map(this::appAnalysis)
				.toList();
	}
}
