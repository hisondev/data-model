package io.github.hison.data.wrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.hison.data.exception.DataException;
import io.github.hison.data.model.DataModel;

/**
 * The {@code DataWrapper} class provides a wrapper structure for structured data communication
 * between the client and the server. This class is designed to handle a combination of simple string
 * key-value pairs and data models (as instances of {@link DataModel}) for more complex structured data.
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
 *     This translates to key1 having the string value "value" and key2 having a {@code dataModel} value with {key1 : "value"}.
 *     </li>
 * </ul>
 * 
 * @author Hani son
 * @version 1.0.0
 */
@JsonDeserialize(using = DataWrapperDeserializer.class)
@JsonSerialize(using = DataWrapperSerializer.class)
public class DataWrapper implements Cloneable{
    private HashMap<String, Object> data;

    private void validateType(Object value) {
        if (value != null && !(value instanceof String || value instanceof DataModel)) {
            throw new DataException("Value type must be a DataModel or String.");
        }
    }

    /**
     * Default constructor initializing the internal string and data model maps.
     */
    public DataWrapper() {
        this.data = new HashMap<String, Object>();
    }
    
    public DataWrapper(String key, Object value) {
        this.data = new HashMap<String, Object>();
        validateType(value);
        
        this.data.put(key, value);
    }

    /**
     * Returns a copy of the data stored in this DataWrapper as a new HashMap.
     * The method performs a shallow copy for String values and a deep copy for dataModel instances.
     * This ensures any changes to the returned HashMap do not affect the original data.
     *
     * @return A new HashMap containing the copied data, with deep copies of dataModel instances.
     */
    public HashMap<String, Object> getDatas() {
        HashMap<String, Object> newData = new HashMap<String, Object>();
        Set<String> keys = this.data.keySet();
        for (String key : keys) {
            if(this.data.get(key) == null || this.data.get(key) instanceof String) {
                newData.put(key, this.data.get(key));
            } else if (this.data.get(key) instanceof DataModel) {
                newData.put(key, ((DataModel)this.data.get(key)).clone());
            }
        }
        return newData;
    }

    /**
     * Associates the specified String value with the specified key in this DataWrapper.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value the String value to be associated with the specified key.
     */
    public void putString(String key, String value) {
        data.put(key, value);
    }

    /**
     * Retrieves the String value associated with the specified key from this DataWrapper.
     * If the key exists but the value is null, it returns null. If the key exists and the value is a String, it returns the String value.
     * If the key does not exist or the value is not a String, a DataException is thrown.
     *
     * @param key the key whose associated String value is to be returned.
     * @return The String value associated with the specified key, or null if the key exists but the value is null.
     * @throws DataException if the key does not exist or if the value is not a String.
     */
    public String getString(String key) {
        if (data.containsKey(key) && data.get(key) == null) {
            return null;
        }
        if (data.containsKey(key) && data.get(key) instanceof String) {
            return (String) data.get(key);
        }
        throw new DataException("There is no inserted String in that key.");
    }

    /**
     * Associates the specified dataModel instance with the specified key in this DataWrapper.
     *
     * @param key   the key with which the specified dataModel instance is to be associated.
     * @param value the dataModel instance to be associated with the specified key.
     */
    public void putDataModel(String key, DataModel value) {
        data.put(key, value);
    }

    /**
     * Retrieves the dataModel instance associated with the specified key from this DataWrapper.
     * If the key exists but the value is null, it returns null. If the key exists and the value is a dataModel instance, it returns a clone of the dataModel instance.
     * If the key does not exist or the value is not a dataModel instance, a DataException is thrown.
     *
     * @param key the key whose associated dataModel instance is to be returned.
     * @return A clone of the dataModel instance associated with the specified key, or null if the key exists but the value is null.
     * @throws DataException if the key does not exist or if the value is not a dataModel instance.
     */
    public DataModel getDataModel(String key) {
        if (data.containsKey(key) && data.get(key) == null) {
            return null;
        }
        if (data.containsKey(key) && data.get(key) instanceof DataModel) {
            return ((DataModel)data.get(key)).clone();
        }
        throw new DataException("There is no inserted DataModel in that key.");
    }

    /**
     * Associates the specified key with the given value in this DataWrapper.
     * The value must be either a String, a dataModel instance, or null. 
     * The method validates the type of the value and performs the following:
     * - If the value is null, associates the key with a null value.
     * - If the value is a String or dataModel instance, associates the key with the value.
     * - If the value is a dataModel instance, a clone of the instance is stored.
     * If the value is of an invalid type, a DataException is thrown.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key, can be a String, dataModel instance, or null.
     * @throws DataException if the value is of an invalid type.
     */
    public void put(String key, Object value) {
        validateType(value);
        if (value == null) {
            this.data.put(key, null);
            return;
        } else if (value instanceof String) {
            this.data.put(key, value);
            return;
        } else if (value instanceof DataModel) {
            this.data.put(key, ((DataModel)value).clone());
            return;
        }
        throw new DataException("Please insert a valid type.");
    }

    /**
     * Retrieves the value associated with the specified key from this DataWrapper.
     * This method returns:
     * - null if the key exists but its associated value is null.
     * - the value itself if it is a String.
     * - a clone of the value if it is an instance of dataModel.
     * If the key does not exist or is associated with a value of an unrecognized type, null is returned.
     *
     * @param key the key whose associated value is to be returned.
     * @return The value associated with the specified key, which could be null, a String, or a cloned instance of dataModel.
     */
    public Object get(String key) {
        if (this.data.get(key) == null) {
            return null;
        } else if (this.data.get(key) instanceof String) {
            return this.data.get(key);
        } else if (this.data.get(key) instanceof DataModel) {
            return ((DataModel)this.data.get(key)).clone();
        }
        return null;
    }

    /**
     * Retrieves a copy of the set of keys from the data stored in this DataWrapper.
     * This method creates and returns a new HashSet containing all the keys present in the internal HashMap.
     * This ensures that modifications to the returned set do not affect the original data stored in the DataWrapper.
     *
     * @return A new HashSet containing the keys from the DataWrapper's internal HashMap.
     */
    public Set<String> getKeys() {
        return new HashSet<>(this.data.keySet());
    }

    /**
     * Creates and returns a deep copy of this DataWrapper.
     * 
     * This method performs a deep copy of both the 'strings' and 'dataModels' maps.
     * For each entry in these maps, a new copy is created and added to the new DataWrapper instance.
     * 
     * @return a new DataWrapper instance which is a deep copy of this DataWrapper.
     */
    public DataWrapper clone() {
        DataWrapper newDataWrapper = new DataWrapper();
        Set<String> keys = this.data.keySet();
        for (String key : keys) {
            if (this.data.get(key) == null) {
                newDataWrapper.put(key, null);
            } else if (this.data.get(key) instanceof String) {
                newDataWrapper.putString(key, (String)this.data.get(key));
            } else if (this.data.get(key) instanceof DataModel) {
                newDataWrapper.putDataModel(key, ((DataModel)this.data.get(key)).clone());
            }
        }
        return newDataWrapper;
    }

    /**
     * Clears all the data from this DataWrapper.
     * 
     * This method removes all entries from both the 'strings' and 'dataModels' maps in this DataWrapper.
     * After invoking this method, both maps will be empty.
     * 
     * @return this DataWrapper instance, cleared of all data, to allow for method chaining.
     */
    public DataWrapper clear() {
        this.data.clear();
        return this;
    }

    /**
     * Checks if the specified key is present in either of the maps (strings or dataModels) within this DataWrapper.
     * 
     * @param key the key whose presence in this DataWrapper is to be tested.
     * @return {@code true} if this DataWrapper contains a mapping for the specified key in either the strings map or the dataModels map; {@code false} otherwise.
     */
    public boolean containsKey(String key) {
        if(this.data.containsKey(key)) return true;
        return false;
    }

    @Override
    public String toString() {
        String r = "";
        Set<String> keys = this.data.keySet();
        for (String key : keys) {
            if (this.data.get(key) == null || this.data.get(key) instanceof String) {
                r = r + key + " : " + data.get(key) + "\n";
            } else if (this.data.get(key) instanceof DataModel) {
                r = r + key + "\n" + ((DataModel)data.get(key)).toString() + "\n";
            }
        }
        return r;
    }
    
    public String toString(String dataModelSeparator) {
        String r = "";
        Set<String> keys = this.data.keySet();
        for (String key : keys) {
            if (this.data.get(key) == null || this.data.get(key) instanceof String) {
                r = r + key + " : " + data.get(key) + "\n";
            } else if (this.data.get(key) instanceof DataModel) {
                r = r + key + "\n" + ((DataModel)data.get(key)).toString(dataModelSeparator) + "\n";
            }
        }
        return r;
    }
}
