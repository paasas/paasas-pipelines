package io.paasas.pipelines.platform.module.adapter.concourse.model.step;

import java.util.List;

import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
public abstract class Step {
	List<AcrossVar> across;
	String timeout;
	int attemps;
	List<String> tags;
	Step onSuccess;
	Step onFailure;
	Step onAbort;
	Step onError;
	Step ensure;
}
