package io.paasas.pipelines.server.analysis.module.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.paasas.pipelines.server.analysis.domain.model.PullRequestAnalysis;
import io.paasas.pipelines.server.analysis.domain.model.RefreshPullRequestAnalysisRequest;
import io.paasas.pipelines.server.analysis.domain.port.api.PullRequestAnalysisDomain;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@RequestMapping("/api/ci/pull-request-analysis")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PullRequestAnalysisController {
	PullRequestAnalysisDomain pullRequestAnalysisDomain;

	@PostMapping
	public PullRequestAnalysis refresh(@RequestBody RefreshPullRequestAnalysisRequest request) {
		return pullRequestAnalysisDomain.refresh(request);
	}
}
