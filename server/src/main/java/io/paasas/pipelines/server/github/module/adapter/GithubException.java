package io.paasas.pipelines.server.github.module.adapter;

public class GithubException extends RuntimeException {
	public GithubException(GithubError githubError, Throwable cause) {
		super(githubError.getMessage(), cause);
	}
}
