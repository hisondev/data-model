package io.github.hison.data.condition;

import java.util.HashMap;
import io.github.hison.data.model.DataModel;

/**
 * The {@code Condition} class provides a way to define search conditions for the {@link DataModel} methods.
 * This class extends {@link HashMap} and facilitates the encapsulation of search parameters in a key-value format.
 * 
 * <p>This class is primarily used to pass conditions to the search methods within the {@link DataModel}. 
 * It can be thought of as a specialized map tailored for search conditions.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 *     Condition condition = new Condition("username", "JohnDoe");
 *     dataModel.search(condition);
 * </pre>
 * 
 * @author Hani son
 * @version 1.0.4
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