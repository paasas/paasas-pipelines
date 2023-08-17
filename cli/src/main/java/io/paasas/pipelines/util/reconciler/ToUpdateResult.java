package io.paasas.pipelines.util.reconciler;

import lombok.Builder;

@Builder(toBuilder=true)
public record ToUpdateResult<T, U>(
		T value,
		U existingValue) {

}
