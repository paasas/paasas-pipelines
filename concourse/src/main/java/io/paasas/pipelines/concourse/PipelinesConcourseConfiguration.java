package io.paasas.pipelines.concourse;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PipelinesConcourseConfiguration {
	@NotEmpty
	String ciSrcUri;

	@NotEmpty
	String githubRepository;

	@NotEmpty
	String platformSrcBranch;

	@NotEmpty
	String platformSrcUri;

	@NotEmpty
	String terraformSrcUri;
}
