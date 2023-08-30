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
public class GitRevisionEntity {
	String branch;
	String commit;
	String commitAuthor;
	String path;
	String repository;
	String tag;

	public GitRevision to() {
		return GitRevision.builder()
				.branch(branch)
				.commit(commit)
				.commitAuthor(commitAuthor)
				.path(path)
				.repository(repository)
				.tag(tag)
				.build();
	}

	public static GitRevisionEntity from(GitRevision gitRevision) {
		return GitRevisionEntity.builder()
				.branch(gitRevision.getBranch())
				.commit(gitRevision.getCommit())
				.commitAuthor(gitRevision.getCommitAuthor())
				.path(gitRevision.getPath())
				.repository(gitRevision.getRepository())
				.tag(gitRevision.getTag())
				.build();
	}
}
