package io.paasas.pipelines;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConcourseConfiguration {
	@NotEmpty
	String ciSrcUri;

	@NotEmpty
	String githubRepository;

	@NotNull
	String deploymentPathPrefix;

	@NotEmpty
	String deploymentSrcBranch;

	@NotEmpty
	String deploymentSrcUri;

	@NotNull
	String platformPathPrefix;

	@NotEmpty
	String platformSrcBranch;

	@NotEmpty
	String platformSrcUri;

	String slackChannel;

	String slackWebhookUrl;

	String teamsWebhookUrl;

	@NotEmpty
	String terraformBackendGcsBucket;

	@NotEmpty
	String terraformSrcBranch;

	@NotEmpty
	String terraformSrcUri;
}
