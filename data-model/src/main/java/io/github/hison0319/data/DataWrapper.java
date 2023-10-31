package io.github.hison0319.data;

import java.util.HashMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The {@code DataWrapper} class provides a wrapper structure for structured data communication
 * between the client and the server. This class is designed to handle a combination of simple string
 * key-value pairs and data models (as instances of {@link DataModelBase}) for more complex structured data.
 * 
 * <p>Instances of this class can be serialized/deserialized with the help of Jackson annotations.</p>
 * 
 * <p><strong>Notes:</strong></p>
 * <ul>
 *     <li>For JSON communication, always wrap with {@code DataWrapper}.</li>
 *     <li>When deserialized with {@code DataWrapper}, the JavaScript object format follows the rule: 
 *     <pre>
 *     {
 *         key1 : "value", 
 *         key2 : [
 *             {key1 : "value"},
 *             {key1 : "value"}
 *         ]
 *     }
 *     </pre>
 *     This translates to key1 having the string value "value" and key2 having a {@code DataModelBase} value with {key1 : "value"}.
 *     </li>
 * </ul>
 * 
 * @author Hani son
 * @version 1.0.0
 */
@JsonDeserialize(using = DataWrapperDeserializer.class)
@JsonSerialize(using = DataWrapperSerializer.class)
public class DataWrapper{
    private HashMap<String, String> strings;
    private HashMap<String, DataModelBase> dataModels;

    private void validateType(Object value) {
        if (!(value instanceof String || value instanceof DataModelBase)) {
            throw new DataException("Value type must be a DataModel or String.");
        }
    }

    /**
     * Default constructor initializing the internal string and data model maps.
     */
    public DataWrapper() {
        this.strings = new HashMap<String, String>();
        this.dataModels = new HashMap<String, DataModelBase>();
    }
    
    /**
     * Constructs a {@code DataWrapper} with a single key-value pair.
     * 
     * @param key   the key for the provided value.
     * @param value the value associated with the provided key. Must be either a {@link String} or {@link DataModelBase}.
     * @throws DataException if the value is neither a {@link String} nor a {@link DataModelBase}.
     */
    public DataWrapper(String key, Object value) {
        this.strings = new HashMap<String, String>();
        this.dataModels = new HashMap<String, DataModelBase>();
        validateType(value);
        
        if(value instanceof String) {
            this.strings.put(key, (String) value);
        } else if (value instanceof DataModelBase) {
            this.dataModels.put(key, (DataModelBase) value);
        }
    }

    /**
     * Returns the map of string key-value pairs.
     * 
     * @return the map of strings.
     */
    public HashMap<String, String> getStrings() {
        return this.strings;
    }

    /**
     * Returns the map of data model key-value pairs.
     * 
     * @return the map of data models.
     */
    public HashMap<String, DataModelBase> getDataModels() {
        return this.dataModels;
    }
    
    /**
     * Associates the specified value with the specified key in the string map.
     * 
     * @param key   the key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     */
    public void putString(String key, String value) {
        strings.put(key, value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * 
     * @param key the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     */
    public String getString(String key) {
        if ((strings.containsKey(key)) && ((strings.get(key) instanceof String))) {
            return strings.get(key);
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in the data model map.
     * 
     * @param key   the key with which the specified value is to be associated.
     * @param value value to be associated with the specified key. It must be an instance of {@link DataModelBase}.
     */
    public void putDataModel(String key, DataModelBase value) {
        dataModels.put(key, value);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * 
     * @param key the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     */
    public DataModelBase getDataModel(String key) {
        if ((dataModels.containsKey(key)) && ((dataModels.get(key) instanceof DataModelBase))) {
            return dataModels.get(key);
        }
        return null;
    }

    /**
     * Inserts a key-value pair into the appropriate map, depending on the type of the value.
     * 
     * @param key   the key with which the specified value is to be associated.
     * @param value value to be associated with the specified key. It must be either a {@link String} or {@link DataModelBase}.
     * @return the previous value associated with key, or {@code null} if there was no mapping for key.
     * @throws DataException if the value is neither a {@link String} nor a {@link DataModelBase}.
     */
    public Object put(String key, Object value) {
        validateType(value);
        
        if(value instanceof String) {
            return this.strings.put(key, (String) value);
        } else if (value instanceof DataModelBase) {
            return this.dataModels.put(key, (DataModelBase) value);
        }
        return null;
    }

    @Override
    public String toString() {
        String r = "";
        for (String key : strings.keySet()) {
            r = r + key + " : " + strings.get(key) + "\n";
        }
        for (String key : dataModels.keySet()) {
            r = r + key + "\n" + dataModels.get(key).toString() + "\n";
        }
        return r;
    }
    
    /**
     * Returns a string representation of the contents of this data wrapper, using the provided separator for data models.
     * 
     * @param dataModelSeparator the separator to be used when rendering data models.
     * @return a string representation of this data wrapper.
     */
    public String toString(String dataModelSeparator) {
        String r = "";
        for (String key : strings.keySet()) {
            r = r + key + " : " + strings.get(key) + "\n";
        }
        for (String key : dataModels.keySet()) {
            r = r + key + "\n" + dataModels.get(key).toString(dataModelSeparator) + "\n";
        }
        return r;
    }
}
