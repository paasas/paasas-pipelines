package io.paasas.pipelines.server.github.module.adapter;

import org.springframework.web.client.RestTemplate;

import io.paasas.pipelines.server.github.domain.model.commit.CommitStatus;
import io.paasas.pipelines.server.github.domain.model.commit.CreateCommitStatus;
import io.paasas.pipelines.server.github.domain.port.backend.CommitStatusRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommitStatusWebClient implements CommitStatusRepository {
	RestTemplate restTemplate;

	@Override
	public CommitStatus createCommitStatus(String repository, String sha, CreateCommitStatus request) {
		return restTemplate.postForObject(
				"/repos/" + repository + "/statuses/{sha}",
				request,
				CommitStatus.class,
				sha);
	}
}
