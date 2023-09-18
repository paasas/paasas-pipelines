package io.paasas.pipelines.server.analysis.module.adapter.database.entity;

import io.paasas.pipelines.server.analysis.domain.model.GitRevision;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestGitRevisionEntity {
	String testBranch;
	String testCommit;
	String testCommitAuthor;
	String testPath;
	String testRepository;
	String testTag;

	public static TestGitRevisionEntity from(GitRevision gitRevision) {
		return TestGitRevisionEntity.builder()
				.testBranch(gitRevision.getBranch())
				.testCommit(gitRevision.getCommit())
				.testCommitAuthor(gitRevision.getCommitAuthor())
				.testPath(gitRevision.getPath())
				.testRepository(gitRevision.getRepository())
				.testTag(gitRevision.getTag())
				.build();
	}
}
