package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.paasas.pipelines.deployment.domain.model.app.App;
import io.paasas.pipelines.server.analysis.domain.model.CloudRunDeployment;
import io.paasas.pipelines.server.analysis.domain.model.RegisterCloudRunDeployment;
import io.paasas.pipelines.server.analysis.module.adapter.database.DatabaseObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(name = "CloudRunDeployment")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CloudRunDeploymentEntity {
	@EmbeddedId
	DeploymentKey key;

	@Lob
	@Column(length = 16_777_216)
	String app;

	@Embedded
	DeploymentInfoEntity deploymentInfo;

	String image;
	String tag;

	public CloudRunDeployment to() {
		try {
			return CloudRunDeployment.builder()
					.app(DatabaseObjectMapper.OBJECT_MAPPER.readValue(app, App.class))
					.deploymentInfo(deploymentInfo.to(key))
					.image(image)
					.tag(tag)
					.build();
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static CloudRunDeploymentEntity from(RegisterCloudRunDeployment registerCloudRunDeployment) {
		return CloudRunDeploymentEntity.builder()
				.app(app(registerCloudRunDeployment.getApp()))
				.key(DeploymentKey.from(registerCloudRunDeployment.getDeploymentInfo()))
				.deploymentInfo(DeploymentInfoEntity.from(registerCloudRunDeployment.getDeploymentInfo()))
				.image(registerCloudRunDeployment.getImage())
				.tag(registerCloudRunDeployment.getTag())
				.build();
	}

	static String app(App app) {
		try {
			return DatabaseObjectMapper.OBJECT_MAPPER.writeValueAsString(app);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
