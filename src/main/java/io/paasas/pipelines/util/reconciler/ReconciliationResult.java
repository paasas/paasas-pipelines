package io.paasas.pipelines.util.reconciler;

import java.util.List;

import lombok.Builder;

@Builder(toBuilder=true)
public record ReconciliationResult<T, U>(
		List<T> toCreate,
		List<U> toDelete,
		List<ToUpdateResult<T,U>> toUpdate) {

}
