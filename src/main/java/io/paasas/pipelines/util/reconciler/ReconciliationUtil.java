package io.paasas.pipelines.util.reconciler;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ReconciliationUtil {

	public static <T, U, V> ReconciliationResult<T, U> reconcile(
			List<U> existingValues,
			List<T> values,
			Reconciler<T, U, V> reconciler) {
		if (reconciler == null) {
			throw new IllegalArgumentException("reconciler is undefined");
		}

		var existingValuesByKey = Optional.ofNullable(existingValues).orElseGet(() -> List.of())
				.stream()
				.collect(Collectors.toMap(reconciler::getExistingValueKey, value -> value));

		var nonNullValues = Optional.ofNullable(values).orElseGet(() -> List.of());

		var keys = nonNullValues.stream().map(reconciler::getValueKey).toList();

		return ReconciliationResult.<T, U>builder()
				.toCreate(nonNullValues.stream()
						.filter(value -> !existingValuesByKey.keySet().contains(reconciler.getValueKey(value)))
						.toList())
				.toDelete(existingValuesByKey.entrySet().stream()
						.filter(entry -> !keys.contains(entry.getKey()))
						.map(Entry::getValue)
						.toList())
				.toUpdate(nonNullValues.stream()
						.flatMap(value -> Optional.ofNullable(existingValuesByKey.get(reconciler.getValueKey(value)))
								.filter(existingValue -> reconciler.matches(value, existingValue))
								.map(existingValue -> ToUpdateResult.<T, U>builder()
										.existingValue(existingValue)
										.value(value)
										.build())
								.stream())
						.toList())
				.build();
	}

}
