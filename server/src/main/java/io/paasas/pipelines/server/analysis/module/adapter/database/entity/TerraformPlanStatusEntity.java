package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import org.springframework.data.domain.Persistable;

import io.paasas.pipelines.server.github.domain.model.commit.CommitState;
import jakarta.persistence.Cacheable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Cacheable(false)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity(name = "TerraformPlanStatus")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformPlanStatusEntity implements Persistable<TerraformExecutionKey> {
	@EmbeddedId
	TerraformExecutionKey key;

	@Enumerated(EnumType.STRING)
	CommitState commitState;

	@Override
	public boolean isNew() {
		return true;
	}

	@Override
	public TerraformExecutionKey getId() {
		return key;
	}
}
