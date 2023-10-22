package io.github.hison0319.data;

import java.util.HashMap;

/**
 * The {@code Condition} class provides a way to define search conditions for the {@link DataModelBase} methods.
 * This class extends {@link HashMap} and facilitates the encapsulation of search parameters in a key-value format.
 * 
 * <p>This class is primarily used to pass conditions to the search methods within the {@link DataModelBase}. 
 * It can be thought of as a specialized map tailored for search conditions.</p>
 * 
 * <p>Example usage:
 * <pre>
 *     Condition condition = new Condition("username", "JohnDoe");
 *     DataModelBase.search(condition);
 * </pre>
 * </p>
 * 
 * @author Hani son
 * @version 1.0.0
 */
public class Condition extends HashMap<String, Object> {
    /**
     * Constructs a new {@code Condition} instance with the given key-value pair.
     * 
     * @param key the key representing the condition's parameter name
     * @param value the value associated with the condition's parameter
     */
    public Condition(String key, Object value) {
        this.put(key, value);
    }
}