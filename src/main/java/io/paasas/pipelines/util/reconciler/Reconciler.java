package io.paasas.pipelines.util.reconciler;

public interface Reconciler<T, U, V> {
	V getValueKey(T value);
	
	V getExistingValueKey(U value);

	boolean matches(T value, U existingValue);
}
