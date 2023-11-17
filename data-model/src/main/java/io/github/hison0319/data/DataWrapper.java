package com.test.boot02.common.data;

import java.util.HashMap;
import java.util.Set;

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
public class DataWrapper implements Cloneable{
    private HashMap<String, Object> data;

    private void validateType(Object value) {
        if (value != null && !(value instanceof String || value instanceof DataModelBase)) {
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

    public HashMap<String, Object> getDatas() {
        HashMap<String, Object> newData = new HashMap<String, Object>();
        Set<String> keys = this.data.keySet();
        for (String key : keys) {
            if(this.data.get(key) == null || this.data.get(key) instanceof String) {
                newData.put(key, this.data.get(key));
            } else if (this.data.get(key) instanceof DataModelBase) {
                newData.put(key, ((DataModelBase)this.data.get(key)).clone());
            }
        }
        return newData;
    }
    
    /**
     * Associates the specified value with the specified key in the string map.
     * 
     * @param key   the key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     */
    public void putString(String key, String value) {
        data.put(key, value);
    }

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
     * Associates the specified value with the specified key in the data model map.
     * 
     * @param key   the key with which the specified value is to be associated.
     * @param value value to be associated with the specified key. It must be an instance of {@link DataModelBase}.
     */
    public void putDataModel(String key, DataModelBase value) {
        data.put(key, value);
    }

    public DataModelBase getDataModel(String key) {
        if (data.containsKey(key) && data.get(key) == null) {
            return null;
        }
        if (data.containsKey(key) && data.get(key) instanceof DataModelBase) {
            return ((DataModelBase)data.get(key)).clone();
        }
        throw new DataException("There is no inserted DataModel in that key.");
    }

    public void put(String key, Object value) {
        validateType(value);
        
        if (value == null) {
            this.data.put(key, null);
            return;
        } else if (value instanceof String) {
            this.data.put(key, value);
            return;
        } else if (value instanceof DataModelBase) {
            this.data.put(key, ((DataModelBase)value).clone());
            return;
        }
        throw new DataException("Please insert a valid type.");
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
            } else if (this.data.get(key) instanceof DataModelBase) {
                newDataWrapper.putDataModel(key, ((DataModelBase)this.data.get(key)).clone());
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
            } else if (this.data.get(key) instanceof DataModelBase) {
                r = r + key + "\n" + ((DataModelBase)data.get(key)).toString() + "\n";
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
            } else if (this.data.get(key) instanceof DataModelBase) {
                r = r + key + "\n" + ((DataModelBase)data.get(key)).toString(dataModelSeparator) + "\n";
            }
        }
        return r;
    }
}
