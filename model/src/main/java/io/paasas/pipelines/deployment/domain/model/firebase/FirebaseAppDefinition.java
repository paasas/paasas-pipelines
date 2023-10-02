package io.paasas.pipelines.deployment.domain.model.firebase;

import java.util.List;

import io.paasas.pipelines.deployment.domain.model.GitWatcher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class FirebaseAppDefinition {
	String config;
	GitWatcher git;
	String githubRepository;
	Npm npm;
	List<GitWatcher> tests;
}