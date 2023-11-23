package io.github.hison.data.model;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.servlet.http.HttpSession;

import com.example.demo.common.data.condition.Condition;
import com.example.demo.common.data.converter.DataConverter;
import com.example.demo.common.data.converter.DataConverterProvider;
import com.example.demo.common.data.exception.DataException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents the base structure for data models designed for structured data communication
 * between the client and the server. This class provides a mechanism to handle data as key-value pairs
 * where the values are maintained as strings. It facilitates data conversion and formatting, especially 
 * when dealing with entities and date-time formats.
 * 
 * <p><strong>Notes:</strong></p>
 * <ul>
 *     <li>For JSON communication, always wrap with {@code DataWrapper}.</li>
 *     <li>The values inside {@code DataModelBase} are managed as strings.</li>
 *     <li>It's recommended to create a {@code DataModel.java} and extend {@code DataModelBase} for usage. Protected members can be overridden through inheritance.</li>
 *     <li>By default, date formatting for entities is defined only for {@code LocalDateTime}.</li>
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
@JsonDeserialize(using = DataModelDeserializer.class)
@JsonSerialize(using = DataModelSerializer.class)
public class DataModel implements Cloneable{
    private LinkedHashSet<String> cols;
    private ArrayList<HashMap<String, Object>> rows;
    
    private DataConverter getConverter() {
        return DataConverterProvider.getConverter();
    }

    private HashMap<String, Object> parseJsonObjectToDataModel(JsonNode node) {
        HashMap<String, Object> row = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            HashMap.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();

            Object value;
            if (valueNode.isNull() || valueNode instanceof NullNode) {
                value = null;
            } else if (valueNode.isArray()) {
                value = parseJsonArrayToDataModel(valueNode);
            } else if (valueNode.isObject()) {
                value = parseJsonObjectToDataModel(valueNode);
            } else {
                value = valueNode.asText();
            }

            row.put(key, value);
        }
        return row;
    }

    private ArrayList<Object> parseJsonArrayToDataModel(JsonNode arrayNode) {
        ArrayList<Object> array = new ArrayList<>();
        for (JsonNode elementNode : arrayNode) {
            if (elementNode.isNull() || elementNode instanceof NullNode) {
                array.add(null);
            } else if (elementNode.isArray()) {
                array.add(parseJsonArrayToDataModel(elementNode));
            } else if (elementNode.isObject()) {
                array.add(parseJsonObjectToDataModel(elementNode));
            } else {
                array.add(elementNode.asText());
            }
        }
        return array;
    }
    
    private void checkRowsRange(int rowIndex) {
        if (rows.isEmpty()) {
            throw new DataException("The rows are empty.");
        }
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new DataException("Provided Index: " + rowIndex + " is out of range. Valid range is 0 to " + (rows.size() - 1) + ".");
        }
    }

    private HashMap<String, Object> entityTransferHashMap(Object obj) {
        if (!isEntity(obj)) {
            throw new DataException("The provided object is not a entity.");
        }
        try {
            HashMap<String, Object> row = new HashMap<String, Object>();
            PropertyDescriptor[] props = Introspector.getBeanInfo(obj.getClass(), Object.class).getPropertyDescriptors();
            for (PropertyDescriptor pd : props) {
                String propName = pd.getName();
                Method getterMethod = pd.getReadMethod();
                // Object value = getterMethod.invoke(obj).toString();
                row.put(propName, convertEntityToDataModel(getterMethod.invoke(obj)));
            }
            return row;
        } catch (Exception e) {
            throw new DataException("Failed to convert object to map", e);
        }
    }

    private Object convertEntityToDataModel(Object value) {
        return getConverter().convertEntityToDataModel(value);
    }

    private Object convertDataModelToEntity(Object value, Class<?> targetType){
        return getConverter().convertDataModelToEntity(value, targetType);
    }

    private boolean isEntity(Object obj) {
        return getConverter().isEntity(obj);
    }

    /**
     * Default constructor for the DataModelBase class.
     *
     * <p>Initializes the columns (cols) as a {@link LinkedHashSet} to maintain the order 
     * of insertion and ensure uniqueness of columns. The rows are initialized as an 
     * {@link ArrayList} of {@link HashMap} where each HashMap represents a row with 
     * key-value pairs corresponding to column names and their respective values.</p>
     */
    public DataModel() {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();
    }

    /**
     * Constructor for the DataModelBase class with variable column names.
     *
     * <p>Initializes the columns (cols) with the provided column names, ensuring order 
     * of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap} where each HashMap represents a row with 
     * key-value pairs corresponding to column names and their respective values.</p>
     *
     * @param newColumns A varargs parameter allowing the user to input any number of column 
     *                   names when creating a new instance of DataModelBase.
     */
    public DataModel(String... newColumns) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        for(String col : newColumns) {
            this.cols.add(col);
        }
    }

    /**
     * Constructor for the DataModelBase class with a set of column names.
     *
     * <p>Initializes the columns (cols) with the provided set of column names, maintaining the 
     * order of insertion and ensuring uniqueness using a {@link LinkedHashSet}. The rows are 
     * initialized as an {@link ArrayList} of {@link HashMap} where each HashMap represents a row 
     * with key-value pairs corresponding to column names and their respective values.</p>
     *
     * @param newColumns A set containing the column names to be initialized in the DataModelBase instance.
     */
    public DataModel(Set<String> newColumns) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        for(String col : newColumns) {
            this.cols.add(col);
        }
    }

    /**
     * Constructor for the DataModelBase class with an initial row represented as a map.
     *
     * <p>Initializes the columns (cols) based on the keys from the provided map and ensures 
     * order of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap}. The provided row (as a map) is then added 
     * to the rows.</p>
     *
     * @param newRow A map representing the initial row with key-value pairs corresponding to 
     *               column names and their respective values.
     */
    public DataModel(Map<String, Object> newRow) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        addRow(newRow);
    }

    /**
     * Initializes a new DataModel using a single {@link Tuple}.
     * 
     * <p>The provided {@link Tuple} is traversed, and each of its elements is added
     * as a key-value pair in a new row {@link HashMap}.</p>
     * 
     * <p>This constructor facilitates direct conversion of a {@link Tuple} resulting
     * from a query into a structured {@link DataModel} without necessitating 
     * manual conversion or entity instantiation.</p>
     * 
     * @param tuple the {@link Tuple} containing the data to initialize the DataModel.
     */
    public DataModel(Tuple tuple) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();
        
        addRow(tuple);
    }

    /**
     * Initializes a new DataModel using a single Object array.
     * 
     * <p>The provided Object array is converted into a new row {@link HashMap},
     * with each object becoming a value associated with a respective column name
     * from the provided column names array.</p>
     * 
     * <p>This constructor offers a streamlined method to convert query results,
     * often returned as Object arrays, into a structured {@link DataModel}, negating 
     * the need for manual conversion or entity creation.</p>
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of columns in each object array within the <code>queryResults</code>. If there's a mismatch, 
     * it may lead to out of bounds exceptions or missing data.</p>
     * 
     * @param data the Object array containing the data to initialize the DataModel.
     * @param columnNames the array of column names corresponding to the data elements.
     */
    public DataModel(Object[] queryResult, String[] columnNames) {
        this.cols = new LinkedHashSet<>(Arrays.asList(columnNames));
        this.rows = new ArrayList<HashMap<String, Object>>();
        
        addRow(queryResult, columnNames);
    }

    /**
     * Constructs a new instance of {@link DataModel} using the provided HttpSession.
     * 
     * <p>The constructor extracts attribute names and values from the HttpSession and populates
     * the DataModel accordingly. Attribute names are used as column identifiers, while attribute
     * values are used as data in the corresponding rows. One row will be created for each
     * attribute present in the HttpSession.</p>
     * 
     * <p><b>Note:</b> The attribute values are directly inserted into the DataModel without
     * any transformation, maintaining their original types. Ensure that subsequent processing
     * of this DataModel can accommodate the potential variety of data types.</p>
     * 
     * @param session the HttpSession from which to extract attribute names and values.
     */
    public DataModel(HttpSession session) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        addRow(session);
    }

    /**
     * Constructs a new instance of {@link DataModel} using the provided {@link ResultSet}.
     *
     * <p>This constructor fetches the column names from the ResultSet's metadata
     * and iterates through the ResultSet to fetch the rows and populate the DataModel.</p>
     * 
     * <p><b>Note:</b> It is imperative that the provided ResultSet is positioned before the first row
     * (this is the default positioning when a ResultSet is initially created).</p>
     *
     * @param rs the {@link ResultSet} containing the data to populate the DataModel.
     * @throws SQLException if a database access error occurs or this method is 
     *                      called on a closed result set.
     */
    public DataModel(ResultSet rs){
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        addRow(rs);
    }

    /**
     * Constructor for the DataModelBase class with an initial row represented as a {@link JsonNode}.
     *
     * <p>Initializes the columns (cols) based on the keys from the provided {@link JsonNode} and ensures 
     * order of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap}. The provided row (as a {@link JsonNode}) is then converted 
     * and added to the rows.</p>
     *
     * @param node A {@link JsonNode} representing the initial row with key-value pairs corresponding to 
     *             column names and their respective values.
     */
    public DataModel (JsonNode node) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();
        
        addRow((JsonNode) node);
    }

    /**
     * Constructor for the DataModelBase class with an initial row represented as an entity object.
     *
     * <p>Initializes the columns (cols) based on the properties of the provided entity object and ensures 
     * order of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap}. The provided entity is then converted 
     * into a {@link HashMap} using the {@code entityTransferHashMap} method, and the resulting map 
     * is added as a row.</p>
     * 
     * <p><b>Note:</b> This constructor supports providing entity objects as parameters. However, 
     * it employs {@link java.lang.reflect} operations, such as introspection, to transform them into 
     * {@link HashMap}s. As a result, the performance might be suboptimal in certain contexts due 
     * to these reflective operations. For optimal performance, it's recommended to directly provide 
     * data in {@link HashMap} format whenever feasible.</p>
     *
     * @param entity An entity object that is converted into a row in the DataModelBase.
     * @throws DataException If the provided object is not recognized as an entity or if there's an error during conversion.
     */
    public DataModel(Object entity) {
        if(entity == null) {
            throw new DataException("You can not insert null.");
        }
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        HashMap<String, Object> hm = entityTransferHashMap(entity);
        addRow(hm);
    }

    /**
     * Constructor for the DataModelBase class that accepts a list of generic type <code>T</code>.
     *
     * <p>Based on the type of the first element in the provided list, the constructor performs 
     * the following operations:</p>
     * <ul>
     *     <li>If {@link String}: Initializes the columns (cols) with the provided strings.</li>
     *     <li>If {@link Map}: Calls the <code>addRows</code> method to add the provided rows.</li>
     *     <li>If {@link Tuple}: Converts each tuple into a {@link HashMap} using its elements and aliases, and then adds them as rows.</li>
     *     <li>If {@link Object}: Converts each object into a {@link HashMap} using the <code>entityTransferHashMap</code> method, and then adds it as a row.</li>
     * </ul>
     * 
     * <p><b>Note:</b> This constructor supports providing entity objects within the list as parameters. However, 
     * when converting these entities into {@link HashMap}s, it employs {@link java.lang.reflect} operations, 
     * such as introspection. The performance might be suboptimal in certain contexts due to these reflective operations. 
     * For optimal performance, direct provision of data in {@link HashMap} format is recommended whenever feasible.</p>
     *
     * @param <T>   the generic type of the elements in the provided list, which can be either 
     *              {@link String}, {@link Map}, {@link Tuple}, or other {@link Object} instances.
     * @param newRows a list of <code>T</code> type instances for the initialization.
     */
    @SuppressWarnings("unchecked")
    public <T> DataModel(List<T> newRows) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        if (!newRows.isEmpty()) {
            T t = newRows.get(0);
            if (t instanceof String) {
                List<String> newColumns = (List<String>) newRows;
                for(String col : newColumns) {
                    this.cols.add(col);
                }
            } else if (t instanceof Map) {
                List<Map<String, Object>> list = (List<Map<String, Object>>) newRows;
                addRows(list);
            } else if (t instanceof Tuple) {
                List<Tuple> tuples = (List<Tuple>) newRows;
                for(Tuple tuple : tuples) {
                    addRow(tuple);
                }
                
            } else if (t instanceof Object){
                List<Object> entities = (List<Object>) newRows;
                for (Object entity : entities) {
                    HashMap<String, Object> hm = entityTransferHashMap(entity);
                    addRow(hm);
                }
            }
        }
    }
        
    /**
     * Constructor for the DataModelBase class that initializes the DataModel with the results 
     * of a database query and a corresponding array of column names.
     *
     * <p>This constructor is particularly useful when you want to convert the results of a 
     * native or JPQL query, where the results are fetched as a list of object arrays, 
     * into the DataModel format. Each object array in the <code>queryResults</code> represents 
     * a row, and the <code>columnNames</code> array provides the keys for these rows.</p>
     * 
     * <p>For example, when executing a native query like:<br>
     * <code>Query query = entityManager.createNativeQuery("SELECT name, age FROM Person");</code><br>
     * <code>List&lt;Object[]&gt; results = query.getResultList();</code><br>
     * <code>DataModelBase dataModelBase = new DataModelBase(results, new String[]{"name", "age"});</code></p>
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of columns in each object array within the <code>queryResults</code>. If there's a mismatch, 
     * it may lead to out of bounds exceptions or missing data.</p>
     *
     * @param queryResults   a list of object arrays, each representing a row from a query result.
     * @param columnNames    an array of strings, representing the column names/keys for the rows.
     */
    public DataModel(List<Object[]> queryResults, String[] columnNames) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();
    
        addRows(queryResults, columnNames);
    }

    /**
     * Converts the DataModel to a string representation using tabs (<code>\t</code>) as separators.
     * 
     * <p>This method iterates through the columns and rows of the DataModel and represents them 
     * as a string, where each column is separated by a tab and each row by a new line.</p>
     *
     * <p><b>Example:</b><br>
     * If the DataModel contains columns "name" and "age", and two rows with values "John, 25" and "Jane, 30", 
     * the output will be:<br>
     * <code>name\tage\nJohn\t25\nJane\t30</code></p>
     * 
     * @return a string representation of the DataModel with tabs as separators.
     */
    @Override
    public String toString() {
        String r = "";
        for (String key : cols) {
            r =  r + key + "\t";
        }
        for (HashMap<String, Object> map : rows) {
            r =  r + "\n";
            for (String key : cols) {
                r =  r + map.get(key) + "\t";
            }
        }
        return r;
    }

    /**
     * Converts the DataModel to a string representation using the provided separator.
     * 
     * <p>This method is an overloaded version of the <code>toString()</code> method that allows 
     * the user to specify a custom separator between columns. Each row will still be separated by a new line.</p>
     *
     * <p><b>Example:</b><br>
     * Given the separator as <code>"|"</code>, if the DataModel contains columns "name" and "age", 
     * and two rows with values "John, 25" and "Jane, 30", the output will be:<br>
     * <code>name|age\nJohn|25\nJane|30</code></p>
     * 
     * @param separator   the string to use as a separator between columns in the output.
     * @return a string representation of the DataModel with the specified separator.
     */
    public String toString(String separator) {
        String r = "";
        for (String key : cols) {
            r =  r + key + separator;
        }
        for (HashMap<String, Object> map : rows) {
            r =  r + "\n";
            for (String key : cols) {
                r =  r + map.get(key) + separator;
            }
        }
        return r;
    }

    /**
     * Checks if the DataModel has defined columns.
     * 
     * <p>This method evaluates whether the columns of the DataModel are defined or not. 
     * If there are any columns present, it returns <code>true</code>, otherwise <code>false</code>.</p>
     * 
     * <p><b>Example:</b><br>
     * If the DataModel has columns "name" and "age", this method will return <code>true</code>. 
     * If no columns are defined, it will return <code>false</code>.</p>
     *
     * @return <code>true</code> if the DataModel has defined columns; <code>false</code> otherwise.
     */
    public boolean isDefine() {
        return !cols.isEmpty();
    }

    /**
     * Clears all columns and rows from the DataModel.
     * 
     * <p>This method removes all the defined columns and their associated data rows from the DataModel. 
     * After invoking this method, the DataModel will be empty.</p>
     *
     * @return the <code>DataModelBase</code> instance with cleared columns and rows.
     */
    public DataModel clear() {
        cols.clear();
        rows.clear();

        return this;
    }

    /**
     * Creates a deep copy of the current DataModelBase instance.
     * 
     * <p>This method returns a new instance of <code>DataModelBase</code> with the same column definitions and data rows 
     * as the current instance. The returned instance is entirely independent of the original, and changes to 
     * the original won't affect the cloned instance and vice versa.</p>
     * 
     * <p><b>Deep Copy:</b> The columns and rows in the cloned instance are deep copies, ensuring that the original 
     * and the clone are completely isolated from each other.</p>
     *
     * <p><b>Example:</b><br>
     * DataModelBase original = new DataModelBase(...);
     * DataModelBase copy = original.clone();
     * // Modifications to 'original' won't affect 'copy' and vice versa.</p>
     * 
     * @return a new <code>DataModelBase</code> instance that's a deep copy of the current instance.
     */
    public DataModel clone() {
        DataModel newModel = new DataModel(this.cols);
    
        LinkedHashSet<String> newCol = new LinkedHashSet<>(this.cols);
        newModel.cols = newCol;
    
        ArrayList<HashMap<String, Object>> newRow = new ArrayList<>();
        for (HashMap<String, Object> oldMap : this.rows) {
            HashMap<String, Object> newMap = new HashMap<>(oldMap);
            newRow.add(newMap);
        }
        newModel.rows = newRow;
    
        return newModel;
    }

    /**
     * Inserts rows from the provided DataModelBase instance into the current instance.
     * 
     * <p>This method extracts rows from the given <code>DataModelBase</code> and appends them to the current instance.
     * Columns from the provided data model that don't exist in the current instance will be added. The order of rows
     * from the provided data model will be preserved when they are added to the current instance.</p>
     * 
     * <p><b>Example:</b><br>
     * DataModelBase dm1 = new DataModelBase(...); // Contains some rows
     * DataModelBase dm2 = new DataModelBase(...); // Contains some other rows
     * dm1.insert(dm2); // Now, 'dm1' contains rows from both 'dm1' and 'dm2'</p>
     * 
     * @param dataModelBase the <code>DataModelBase</code> instance from which rows will be extracted and added to the current instance.
     * @return the <code>DataModelBase</code> instance (i.e., the current instance) after inserting the new rows.
     */
    public DataModel insert(DataModel dataModelBase){
        List<HashMap<String, Object>> newRows = dataModelBase.getRows();
        return addRows(newRows);
    }

    /**
     * Converts the current DataModelBase instance to a JSON node.
     * 
     * <p>This method produces a JSON representation of the current instance, converting both columns and rows 
     * into appropriate JSON structures. It also adds metadata, specifically the column and row counts.</p>
     * 
     * <p><b>Note:</b> The method configures the {@link com.fasterxml.jackson.databind.ObjectMapper} to prevent self-referential cycles (recursion) 
     * using the <code>SerializationFeature.FAIL_ON_SELF_REFERENCES</code> configuration. This is important to prevent potential 
     * stack overflow errors when dealing with objects that reference themselves.</p>
     * 
     * <p><b>Caution:</b> If the rows contain data types not directly supported by {@link com.fasterxml.jackson.databind.ObjectMapper}, 
     * such as <code>LocalDateTime</code>, the conversion will fail and raise an error. Ensure that the data within the rows is 
     * compatible with ObjectMapper's default serialization capabilities, or consider providing custom serializers.</p>
     * 
     * @return The {@link com.fasterxml.jackson.databind.JsonNode} representation of the current instance.
     * @throws DataException If any error occurs during the conversion process.
     */
    public JsonNode getConvertJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
            
            ObjectNode rootNode = mapper.createObjectNode();
            
            rootNode.set("columns", mapper.valueToTree(cols));
            rootNode.set("rows", mapper.valueToTree(rows));
            rootNode.put("columnCount", getColumnCount());
            rootNode.put("rowCount", getRowCount());
            
            return rootNode;
        } catch (Exception e) {
            throw new DataException("Failed to convert DataModel to Json in getConvertJson", e);
        }
    }

    /**
     * Converts the rows of the current DataModelBase instance into a list of entities of the specified class.
     * 
     * <p>This method iterates over each row of data in the current instance and constructs an entity of the 
     * given class by using reflection to set the fields of the entity based on the keys in the row.</p>
     * 
     * <p><b>Note:</b> This method leverages Java reflection to dynamically instantiate entities and set their fields. 
     * Using reflection can introduce performance overheads, especially if invoked repeatedly or with large data sets.
     * Due to the performance considerations associated with reflection, when converting DataModel instances to entities, 
     * it might be more efficient to first extract the information into {@link HashMap}s and then convert these to entities
     *  using domain-specific logic rather than relying on reflective operations of this method</p>
     * 
     * @param entityClass The class type of the entities to be created.
     * @return A list of entities constructed from the rows of data in the current instance.
     * @throws DataException If any error occurs during the conversion process.
     */
    public List<Object> getConvertEntities(Class<?> entityClass) {
        // DataMode => Entity
        List<Object> entities = new ArrayList<>();
        Field[] fields = entityClass.getDeclaredFields();
    
        for (HashMap<String, Object> row : rows) {
            try {
                Object entity = entityClass.getConstructor().newInstance();
    
                for (Field field : fields) {
                    String fieldName = field.getName();
                    Method setterMethod;
                    try {
                        // Assuming setter methods are named in a standard way: "set[FieldName]"
                        String methodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        setterMethod = entityClass.getMethod(methodName, field.getType());
                    } catch (NoSuchMethodException e) {
                        continue; // No setter method, skip this field
                    }
    
                    if (row.containsKey(fieldName)) {
                        Object value = row.get(fieldName);
                        if (value != null && !(value instanceof NullNode)) {
                            // Convert value type if necessary
                            value = convertDataModelToEntity(value, field.getType());
                            // Set the value using the setter method
                            setterMethod.invoke(entity, value);
                        }
                    }
                }
                entities.add(entity);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new DataException("Failed to convert object to hashMap", e);
            }
        }
        return entities;
    }

    /**
     * Defines the columns for this DataModelBase instance using the specified column names.
     * 
     * <p>This method sets the column names for the current DataModelBase instance. If columns have 
     * already been defined for this instance, invoking this method will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns Varargs of column names to be set for this DataModelBase instance.
     * @return The current DataModelBase instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     */
    public DataModel setColumns(String... columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Defines the columns for this DataModelBase instance using the specified set of column names.
     * 
     * <p>This method sets the column names for the current DataModelBase instance using a {@link Set} of 
     * column names. If columns have already been defined for this instance, invoking this method 
     * will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns A set of column names to be set for this DataModelBase instance.
     * @return The current DataModelBase instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     */
    public DataModel setColumns(Set<String> columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Defines the columns for this DataModelBase instance using the specified list of column names.
     * 
     * <p>This method sets the column names for the current DataModelBase instance using a {@link List} of 
     * column names. If columns have already been defined for this instance, invoking this method 
     * will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns A list of column names to be set for this DataModelBase instance.
     * @return The current DataModelBase instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     */
    public DataModel setColumns(List<String> columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Sets the same value for a specified column across all rows of this DataModelBase instance.
     * 
     * <p>This method assigns a uniform value to a specific column for all rows in the current
     * DataModelBase instance. If the column does not exist within the DataModelBase, this method
     * does nothing and simply returns the current instance.</p>
     * 
     * <p><b>Note:</b> It's advisable to first check if the column exists using the {@code hasColumn}
     * method before using this method to ensure intended behavior.</p>
     * 
     * <p><b>Example:</b><br>
     * {@code
     * DataModelBase model = new DataModelBase();
     * model.setColumns("Name", "Age");
     * model.addRow(new HashMap<String, Object>(){{ put("Name", "John"); put("Age", 30); }});
     * model.addRow(new HashMap<String, Object>(){{ put("Name", "Jane"); put("Age", 25); }});
     * model.setColumnSameValue("Age", 28); // This will set the "Age" for both rows to 28.
     * }
     * </p>
     * 
     * @param column The name of the column for which the value is to be uniformly set.
     * @param value The value to be set across all rows for the specified column.
     * @return The current DataModelBase instance with updated values for the specified column.
     */
    public DataModel setColumnSameValue(String column, Object value) {
        if(!hasColumn(column)) return this;

        for (HashMap<String, Object> map : rows) {
            map.put(column, value);
        }
        return this;
    }

    /**
     * Formats values in a specified column using the provided formatter function.
     * 
     * <p>This method applies the given formatter function to every value in the specified column.
     * If an error occurs while formatting a value, the original value remains unchanged for that particular
     * row, and an error message is printed.</p>
     * 
     * <p>Before applying the formatter, this method checks if the specified column exists within 
     * the DataModelBase instance. If the column doesn't exist, a message indicating the non-existence 
     * of the column is printed and the method returns without making any modifications.</p>
     * 
     * <p><b>Note:</b> Ensure that the formatter function can handle all potential data types 
     * present in the specified column to avoid unexpected behavior or errors.</p>
     * 
     * <p><b>Example:</b><br>
     * {@code
     * // Define a formatter function that truncates strings to the first three characters.
     * Function<Object, Object> formatter = value -> {
     *     if (value instanceof String) {
     *         return ((String) value).substring(0, Math.min(3, ((String) value).length()));
     *     } else {
     *         return value;
     *     }
     * };
     * 
     * // Example method call.
     * dataModelBase.setColumnSameFormat("key1", formatter);
     * }
     * </p>
     * 
     * @param column The name of the column whose values are to be formatted.
     * @param formatter The function to format values within the specified column.
     * @return The current DataModelBase instance with formatted values for the specified column.
     */
    public DataModel setColumnSameFormat(String column, Function<Object, Object> formatter) {
        if (!cols.contains(column)) {
            System.out.println("Column does not exist: " + column);
            return this;
        }

        for (HashMap<String, Object> row : rows) {
            Object originalValue = row.get(column);
            try {
                Object formattedValue = formatter.apply(originalValue);
                row.put(column, formattedValue);
            } catch (Exception e) {
                System.out.println("Error formatting value: " + originalValue + ". Leaving it as is.");
            }
        }
        return this;
    }

    /**
     * Adds a new row of data to this DataModelBase instance.
     * 
     * <p>If the {@code DataModelBase} has no columns defined yet, the keys from the provided 
     * {@code newRow} map will define the columns. Otherwise, only the columns already defined 
     * will be taken into account when adding the new row.</p>
     * 
     * <p>This method also performs type validation. It ensures that new data being added to a 
     * column matches the data type of existing entries in that column.</p>
     * 
     * <p><b>Note:</b> This method uses {@code convertEntityToDataModel} to convert and possibly normalize 
     * the data being added. By default, {@code convertEntityToDataModel} will convert certain Java primitive 
     * types to their corresponding String representations. Users can override this method to customize 
     * its behavior.</p>
     * 
     * @param newRow A map containing the new row of data to be added.
     * @return The current DataModelBase instance with the added row.
     * @throws DataException If there's a type mismatch between the new data and existing data in a column.
     */
    public DataModel addRow(Map<String, Object> newRow){
        if (cols.isEmpty()) {
            cols.addAll(newRow.keySet());
        }
        
        /*
        // An error occurs if a column that does not exist exists.
        for (String key : newRow.keySet()) {
            if (!cols.contains(key)) {
                throw new DataException("Columns that do not exist in the initialized DataModel cannot be inserted.");
            }
        }
        */

        HashMap<String, Object> hm = new HashMap<String, Object>();
        for (String key : cols) {
            if(newRow.containsKey(key)) {
                Object value = convertEntityToDataModel(newRow.get(key));
                if(!rows.isEmpty()) {
                    if(rows.get(rows.size() - 1).get(key) != null && value != null) {
                        if(rows.get(rows.size() - 1).get(key).getClass() != value.getClass()) {
                            throw new DataException(" Please enter the same type. Column: " + key);
                        }
                    }
                }
                hm.put(key, value);
            } else {
                hm.put(key, null);
            }
        }

        rows.add(hm);

        return this;
    }

    /**
     * Adds a new row of data to this DataModelBase instance based on the provided Tuple.
     * 
     * <p>The method will extract elements from the {@code Tuple} and map them into a row using the
     * aliases of the Tuple elements as column names.</p>
     * 
     * <p>If the provided {@code Tuple} is null, the method will simply return without adding any row.</p>
     * 
     * @param tuple A Tuple containing data to be added as a new row. 
     *              Each TupleElement's alias will be used as the column name for that data.
     * @return The current DataModelBase instance with the added row.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * // Using JPA's EntityManager
     * EntityManager em = ... // Obtain an EntityManager instance.
     *
     * // Example JPQL query
     * Query query = em.createQuery("SELECT name AS nameAlias, age AS ageAlias FROM Person", Tuple.class);
     * List<Tuple> results = query.getResultList();
     * 
     * // Create a new DataModelBase and add each tuple as a row
     * DataModelBase dataModelBase = new DataModelBase();
     * for (Tuple tuple : results) {
     *     dataModelBase.addRow(tuple);
     * }
     * }
     * </pre>
     */
    public DataModel addRow(Tuple tuple) {
        if (tuple != null) {
            HashMap<String, Object> row = new HashMap<>();
            for (TupleElement<?> element : tuple.getElements()) {
                if(element.getAlias() == null) {
                    throw new DataException("Please specify an alias in your query.");
                }
                row.put(element.getAlias(), tuple.get(element.getAlias()));
            }
            addRow(row);
        }
        return this;
    }
    
    /**
     * Adds a new row of data to this DataModelBase instance based on the provided array of objects and the array of column names.
     * 
     * <p>The method maps the elements from the {@code queryResult} to the respective column names in {@code columnNames} 
     * and then adds them as a new row to the DataModelBase instance.</p>
     * 
     * <p>If the sizes of the two provided arrays do not match, or if any of the arrays is null, the method will throw a DataException.</p>
     * 
     * @param queryResult An array of objects containing data to be added as a new row.
     * @param columnNames An array of strings where each string represents the column name for the respective data in {@code queryResult}.
     * @return The current DataModelBase instance with the added row.
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of columns in each object array within the <code>queryResults</code>. If there's a mismatch, 
     * it may lead to out of bounds exceptions or missing data.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * // Example result from a query
     * List<Object[]> results = memberRepository.findAllProjectedBy();
     * String[] columnNames = new String[]{"name", "age", "profession"};
     * 
     * // Create a new DataModelBase and add the result as a row
     * DataModelBase dataModelBase = new DataModelBase();
     * for (Object[] result : results) {
     *     dataModel.addRow(result, columnNames);
     * }
     * </pre>
     */
    public DataModel addRow(Object[] queryResult, String[] columnNames) {
        if (queryResult != null && columnNames != null && queryResult.length == columnNames.length) {
            HashMap<String, Object> row = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                row.put(columnNames[i], queryResult[i]);
            }
            addRow(row);
        } else {
            throw new DataException("Mismatch between data and column names, or invalid input.");
        }
        return this;
    }

    /**
     * Adds a new row of data to this DataModelBase instance based on the attributes found in the provided {@code HttpSession}.
     * 
     * <p>The method extracts each attribute's name and value from the {@code HttpSession} and maps them as key-value pairs in a new row.</p>
     * 
     * @param session An HttpSession instance containing attributes that should be added as a new row.
     * @return The current DataModelBase instance with the added row.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * // Simulating setting attributes in an HttpSession
     * HttpSession session = request.getSession();
     * session.setAttribute("username", "JohnDoe");
     * session.setAttribute("lastLogin", new Date());
     * 
     * // Create a new DataModelBase and add the session's attributes as a row
     * DataModelBase dataModelBase = new DataModelBase();
     * dataModelBase.addRow(session);
     * }
     * </pre>
     */
    public DataModel addRow(HttpSession session) {
        Enumeration<String> attributeNames = session.getAttributeNames();
        HashMap<String, Object> row = new HashMap<String, Object>();

        while(attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);

            row.put(attributeName, attributeValue);
        }
        addRow(row);
        return this;
    }

    /**
     * Adds rows of data to this DataModelBase instance based on the data found in the provided {@code ResultSet}.
     * 
     * <p>This method fetches data from the ResultSet, which typically contains the result of a SQL query executed using JDBC, and transforms this data into rows in the DataModelBase format.</p>
     * 
     * @param rs A ResultSet instance containing data from a database query.
     * @return The current DataModelBase instance with the added rows.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * try {
     *     Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
     *     Statement stmt = conn.createStatement();
     *     String sql = "SELECT id, name, age FROM users";
     *     ResultSet rs = stmt.executeQuery(sql);
     *     
     *     // Create a new DataModelBase and add rows from the ResultSet
     *     DataModelBase dataModelBase = new DataModelBase();
     *     dataModelBase.addRow(rs);
     *     
     *     // Close resources
     *     rs.close();
     *     stmt.close();
     *     conn.close();
     * } catch (SQLException se) {
     *     se.printStackTrace();
     * }
     * }
     * </pre>
     */
    public DataModel addRow(ResultSet rs){
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
    
            while (rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                addRow(row);
            }
        } catch (Exception e) {
            throw new DataException(e.toString());
        }
        return this;
    }

    /**
     * Adds rows of data to this DataModelBase instance based on the provided {@code JsonNode}.
     * 
     * <p>This method processes the JsonNode, which can represent either a JSON object or an array. If the JsonNode represents a JSON object,
     * the data is added as a single row to the DataModelBase. If the JsonNode represents a JSON array, each element of the array is treated 
     * as an individual JSON object and added as separate rows to the DataModelBase.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Given a JsonNode representing a JSON object
     * String jsonStr = "{ \"name\": \"John\", \"age\": 30 }";
     * JsonNode node = new ObjectMapper().readTree(jsonStr);
     * dataModelBase.addRow(node);
     * 
     * // Given a JsonNode representing a JSON array
     * String jsonArrayStr = "[ { \"name\": \"John\", \"age\": 30 }, { \"name\": \"Doe\", \"age\": 25 } ]";
     * JsonNode arrayNode = new ObjectMapper().readTree(jsonArrayStr);
     * dataModelBase.addRow(arrayNode);
     * </pre>
     *
     * @param node A JsonNode instance, which can be a JSON object or an array.
     * @return The current DataModelBase instance with the added rows.
     */
    public DataModel addRow(JsonNode node) {
        if (node.isObject()) {
            addRow(parseJsonObjectToDataModel(node));
        } 
        else if (node.isArray()) {
            for (JsonNode elementNode : node) {
                addRow(parseJsonObjectToDataModel(elementNode));
            }
        }
        return this;
    }
        
    /**
     * Adds a row of data to this DataModelBase instance based on the provided entity object.
     * 
     * <p>This method uses the {@code entityTransferHashMap} function to convert the given entity object into a HashMap. 
     * As a result, there might be potential performance implications due to the use of reflection in the conversion process.</p>
     *
     * <p><b>Note:</b> This constructor supports providing entity objects as parameters. However, 
     * it employs {@link java.lang.reflect} operations, such as introspection, to transform them into 
     * {@link HashMap}s. As a result, the performance might be suboptimal in certain contexts due 
     * to these reflective operations. For optimal performance, it's recommended to directly provide 
     * data in {@link HashMap} format whenever feasible.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * MyEntity entity = new MyEntity();
     * // set properties for the entity
     * dataModelBase.addRow(entity);
     * </pre>
     *
     * @param entity An object representing an entity.
     * @return The current DataModelBase instance with the added row.
     */
    public DataModel addRow(Object entity){
        if(entity == null) {
            throw new DataException("You can not insert null.");
        }
        HashMap<String, Object> hm = entityTransferHashMap(entity);
        return addRow(hm);
    }

    /**
     * Adds multiple rows to the DataModel.
     *
     * <p>This method accepts a list of generic type <code>T</code> which can be either 
     * {@link HashMap}&lt;String, Object&gt;, {@link Tuple}, or any other <code>Object</code> instances.</p>
     * <ul>
     *     <li>If {@link HashMap}: Adds the passed <code>HashMap</code> directly to the <code>DataModel</code>.</li>
     *     <li>If {@link Tuple}: Converts each <code>Tuple</code> to a <code>HashMap</code> where keys are the aliases 
     *     of the tuple elements, and then adds them to the <code>DataModel</code>.</li>
     *     <li>If {@link Object}: Converts the <code>Object</code> to a <code>HashMap</code> using <code>entityTransferHashMap</code>
     *     method and then adds it to the <code>DataModel</code>.</li>
     * </ul>
     * 
     * <p><b>Note:</b> The type checking is performed during runtime, based on the first item in the list. 
     * Therefore, it is assumed that all objects in the list are of the same type as the first item. 
     * Inconsistencies in type within the list may lead to runtime errors.</p>
     *
     * <p><b>Note:</b> This constructor supports providing entity objects as parameters. However, 
     * it employs {@link java.lang.reflect} operations, such as introspection, to transform them into 
     * {@link HashMap}s. As a result, the performance might be suboptimal in certain contexts due 
     * to these reflective operations. For optimal performance, it's recommended to directly provide 
     * data in {@link HashMap} format whenever feasible.</p>
     * 
     * @param <T>   the generic type of the elements to be added which can be either 
     *              {@link HashMap}&lt;String, Object&gt;, {@link Tuple}, or other {@link Object} instances.
     * @param newRows a list of <code>T</code> type instances to be added to the <code>DataModel</code>.
     * @return the <code>DataModel</code> instance with the added rows.
     */
    @SuppressWarnings("unchecked")
    public <T> DataModel addRows(List<T> newRows) {
        if (newRows != null && !newRows.isEmpty() && newRows.get(0) != null) {
            Object first = newRows.get(0);
            
            if (first instanceof Map) {
                for (T hm : newRows) {
                    addRow((Map<String, Object>) hm);
                }
            } else if (first instanceof Tuple) {
                for (T tupleItem : newRows) {
                    Tuple tuple = (Tuple) tupleItem;
                    addRow(tuple);
                }
            } else if (first instanceof Object){
                for (T entity : newRows) {
                    HashMap<String, Object> hm = entityTransferHashMap(entity);
                    addRow(hm);
                }
            }
        }
    
        return this;
    }

    /**
     * Adds multiple rows of data to this DataModelBase instance based on the provided list of query results and corresponding column names.
     * 
     * <p>This method is useful when fetching multiple records from a database using JPA, 
     * and you want to convert the results into a DataModelBase representation.</p>
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of columns in each object array within the <code>queryResults</code>. If there's a mismatch, 
     * it may lead to out of bounds exceptions or missing data.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * 
     * MemberRepository extends JpaRepository&lt;Member, Long&gt;
     * @Query("SELECT m.id, m.deptcode, m.email, m.membername, m.regdate FROM Member m")
     * List&lt;Object[]&gt; findAllMember();
     * 
     * List&lt;Object[]&gt; results = memberRepository.findAllMember();
     * String[] columnNames = {"id", "deptcode", "email", "membername", "regdate"};
     * DataModelBase dataModelBase = new DataModelBase();
     * dataModelBase.addRows(results, columnNames);
     * </pre>
     *
     * @param queryResults A list of object arrays, each representing a row of data fetched from the database.
     * @param columnNames An array of strings representing the names of the columns for the fetched data.
     * @return The current DataModelBase instance with the added rows.
     */
    public DataModel addRows(List<Object[]> queryResults, String[] columnNames) {
        for(Object[] result : queryResults) {
            addRow(result, columnNames);
        }
        return this;
    }
    
    /**
     * Retrieves the count of columns currently in this DataModelBase instance.
     *
     * <p>This method provides a quick way to determine how many columns are present 
     * in the current data model, which can be useful in various data processing scenarios.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Add rows or columns to the data model]
     * int count = dataModel.getColumnCount();
     * System.out.println("Number of columns: " + count);
     * </pre>
     *
     * @return The number of columns currently present in this DataModelBase instance.
     */
    public int getColumnCount() {
        return cols.size();
    }

    /**
     * Retrieves the count of rows currently in this DataModelBase instance.
     *
     * <p>This method provides a convenient way to determine the number of rows present 
     * in the current data model. Such information can be helpful in various data processing 
     * and analysis scenarios.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Add rows to the data model]
     * int count = dataModel.getRowCount();
     * System.out.println("Number of rows: " + count);
     * </pre>
     *
     * @return The number of rows currently present in this DataModelBase instance.
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Retrieves the list of columns currently in this DataModelBase instance.
     *
     * <p>This method offers a convenient means to access the column names stored 
     * in the current data model. It returns a new list to ensure the original column 
     * data remains unmodified.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Define columns for the data model]
     * List<String> columns = dataModel.getColumns();
     * System.out.println("Columns: " + columns);
     * </pre>
     *
     * @return A new list containing the names of columns present in this DataModelBase instance.
     */
    public List<String> getColumns() {
        return (List<String>) new ArrayList<String>(cols);
    }

    /**
     * Retrieves the set of columns currently in this DataModelBase instance.
     *
     * <p>This method is particularly useful when users wish to ensure uniqueness in column names. 
     * It returns a new set to prevent any unintended modifications to the original column data.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Define columns for the data model]
     * Set<String> columnsSet = dataModel.getColumnsWithSet();
     * System.out.println("Unique Columns: " + columnsSet);
     * </pre>
     *
     * @return A new set containing the names of columns present in this DataModelBase instance.
     */
    public Set<String> getColumnsWithSet() {
        return new LinkedHashSet<>(cols);
    }

    /**
     * Retrieves all values associated with the specified column in this DataModelBase instance.
     *
     * <p>This method allows users to fetch all the values for a given column name. 
     * If the specified column doesn't exist in the data model, an empty list will be returned.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * List<Object> ageValues = dataModel.getColumnValues("age");
     * System.out.println("All age values: " + ageValues);
     * </pre>
     *
     * @param column The name of the column whose values are to be fetched.
     * @return A list containing all the values associated with the specified column.
     */
    public List<Object> getColumnValues(String column) {
        List<Object> colValues = new ArrayList<Object>();

        for(HashMap<String, Object> map : rows) {
            if(map.containsKey(column)) {
                colValues.add(map.get(column));
            }
        }

        return (List<Object>) colValues;
    }

    /**
     * Checks if the specified column exists in this DataModelBase instance.
     *
     * <p>This method can be used to verify the presence of a specific column 
     * in the data model before performing operations on it.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * if (dataModel.hasColumn("age")) {
     *     System.out.println("Age column exists in the data model.");
     * } else {
     *     System.out.println("Age column doesn't exist in the data model.");
     * }
     * </pre>
     *
     * @param column The name of the column to check for existence.
     * @return {@code true} if the column exists in the data model; {@code false} otherwise.
     */
    public boolean hasColumn(String column) {
        return cols.contains(column);
    }

    /**
     * Retrieves a specific row as a {@code HashMap} based on the given row index.
     * 
     * <p><b>Note:</b> The row index starts from 0.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * HashMap<String, Object> row = dataModel.getRow(0);
     * System.out.println(row.get("columnName"));
     * </pre>
     *
     * @param rowIndex The index of the row to be retrieved.
     * @return The row at the specified index as a {@code HashMap}.
     * @throws IndexOutOfBoundsException If the rowIndex is out of bounds of the rows.
     */
    public HashMap<String, Object> getRow(int rowIndex) {
        checkRowsRange(rowIndex);
        HashMap<String, Object> hm = new HashMap<String, Object>(rows.get(rowIndex));
        return hm;
    }

    /**
     * Retrieves a specific row as a {@code DataModelBase} based on the given row index.
     * 
     * <p><b>Note:</b> The row index starts from 0.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * DataModelBase singleRowModel = dataModel.getRowAsDataModel(0);
     * System.out.println(singleRowModel.getColumnValues("columnName"));
     * </pre>
     *
     * @param rowIndex The index of the row to be retrieved.
     * @return The row at the specified index wrapped in a new {@code DataModelBase} instance.
     * @throws IndexOutOfBoundsException If the rowIndex is out of bounds of the rows.
     */
    public DataModel getRowAsDataModel(int rowIndex) {
        checkRowsRange(rowIndex);
        DataModel dm = new DataModel(this.cols);
        dm.addRow(getRow(rowIndex));
        return dm;
    }
    
    /**
     * Retrieves all rows in the DataModel as a {@code List} of {@code HashMap} representations.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * List<HashMap<String, Object>> allRows = dataModel.getRows();
     * for (HashMap<String, Object> row : allRows) {
     *     System.out.println(row.get("columnName"));
     * }
     * </pre>
     *
     * @return A {@code List} containing all rows in the DataModel, where each row is represented as a {@code HashMap}.
     */
    public List<HashMap<String, Object>> getRows() {
        return (List<HashMap<String, Object>>) new ArrayList<HashMap<String, Object>>(rows);
    }

    /**
     * Retrieves the value from the specified column and row index in the DataModel.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * Object value = dataModel.getValue(2, "columnName");  // Assume you want the value from the third row and a column named "columnName"
     * System.out.println("Value from rowIndex 2 and column 'columnName': " + value);
     * </pre>
     *
     * <p><b>Note:</b> Row index starts from 0. Make sure to provide a valid row index. If the rows are empty or the provided row index 
     * is out of range (i.e., less than 0 or greater than the number of rows minus one), a DataException will be thrown.</p>
     *
     * @param rowIndex The index of the row (0-based) from which the value should be retrieved.
     * @param column The name of the column from which the value should be retrieved.
     * @return The value from the specified column and row index.
     * @throws DataException if the column does not exist, the rows are empty, or the row index is out of range.
     */
    public Object getValue(int rowIndex, String column) {
        checkRowsRange(rowIndex);
        if (!hasColumn(column)) {
            throw new DataException("Column does not exist.");
        }
        return rows.get(rowIndex).get(column);
    }

    /**
     * Sets the value in the specified column and row index in the DataModel.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * dataModel.setValue(2, "columnName", "newValue");  // Set a new value in the third row and a column named "columnName"
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>Row index starts from 0. Make sure to provide a valid row index. If the rows are empty or the provided row index 
     * is out of range (i.e., less than 0 or greater than the number of rows minus one), a DataException will be thrown.</li>
     * <li>If you haven't customized the <code>DataModelBase</code>, the default type for incoming Object values is <code>String</code>. It's advisable to maintain the String data type.</li>
     * <li>It's important to ensure that the value being set has the same data type as other values in the same column. Mismatched data types will result in a <code>DataException</code>.</li>
     * <li>Values are internally passed through the <code>convertEntityToDataModel(value)</code> function to ensure proper conversion and consistency within the DataModel.</li>
     * </ul>
     *
     * @param rowIndex The index of the row (0-based) where the value should be set.
     * @param column The name of the column where the value should be set.
     * @param value The new value to be set in the specified column and row.
     * @return The current DataModelBase instance with the updated value.
     * @throws DataException if the column does not exist, the rows are empty, the row index is out of range, or if there's a type mismatch with the provided value.
     */
    public DataModel setValue(int rowIndex, String column, Object value) {
        checkRowsRange(rowIndex);
        if (!hasColumn(column)) {
            throw new DataException("Column does not exist.");
        }

        value = convertEntityToDataModel(value);

        for(int i = 0; i < rows.size(); i++) {
            if(rowIndex == i) continue;
            if(rows.get(i).get(column) != null && value != null) {
                if(rows.get(i).get(column).getClass() == value.getClass()) {
                    break;
                }
                else {
                    throw new DataException(" Please enter the same type. Column: " + column);    
                }
            }
        }

        // Set the value in the specified row and column
        HashMap<String, Object> row = rows.get(rowIndex);
        row.put(column, value);
    
        return this;
    }

    /**
     * Removes the row at the specified row index from the DataModel and returns it.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with rows of data]
     * HashMap<String, Object> removedRow = dataModel.removeRow(2); // Removes and returns the third row from the data model
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>Row index starts from 0. The provided row index must be valid. If the rows are empty or the provided row index 
     * is out of range, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param rowIndex The index of the row (0-based) to be removed.
     * @return The HashMap representing the removed row.
     * @throws DataException if the rowIndex is out of range or rows are empty.
     */
    public HashMap<String, Object> removeRow(int rowIndex) {
        checkRowsRange(rowIndex);
        return rows.remove(rowIndex);
    }

    /**
     * Removes the specified column from the DataModel.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel.removeColumn("age");  // Removes the "age" column from the data model
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to be removed.
     * @return The current DataModelBase instance with the column removed.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public DataModel removeColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column does not exist.");
        }
        cols.remove(column);
        for (HashMap<String, Object> row : rows) {
            row.remove(column);
        }
        return this;
    }

    /**
     * Removes the specified columns from the DataModel.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel.removeColumns("age", "name");  // Removes the "age" and "name" columns from the data model
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If any of the specified columns do not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param columns The names of the columns to be removed.
     * @return The current DataModelBase instance with the specified columns removed.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel removeColumns(String... columns) {
        for (String column : columns) {
            if (!hasColumn(column)) {
                throw new DataException("Column does not exist.");
            }
            cols.remove(column);
        }
        for (HashMap<String, Object> row : rows) {
            for (String column : columns) {
                row.remove(column);
            }
        }
        return this;
    }

    /**
     * Removes the specified columns from the DataModel.
     *
     * <p>Functionally identical to {@link #removeColumns(String...)}, but accepts a List of column names.</p>
     *
     * @param columns The list of column names to be removed.
     * @return The current DataModelBase instance with the specified columns removed.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel removeColumns(List<String> columns) {
        for (String column : columns) {
            if (!hasColumn(column)) {
                throw new DataException("Column does not exist.");
            }
            cols.remove(column);
        }
        for (HashMap<String, Object> row : rows) {
            for (String column : columns) {
                row.remove(column);
            }
        }
        return this;
    }

    /**
     * Removes the specified columns from the DataModel.
     *
     * <p>Functionally identical to {@link #removeColumns(String...)}, but accepts a Set of column names.</p>
     *
     * @param columns The set of column names to be removed.
     * @return The current DataModelBase instance with the specified columns removed.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel removeColumns(Set<String> columns) {
        for (String column : columns) {
            if (!hasColumn(column)) {
                throw new DataException("Column does not exist.");
            }
            cols.remove(column);
        }
        for (HashMap<String, Object> row : rows) {
            for (String column : columns) {
                row.remove(column);
            }
        }
        return this;
    }

    /**
     * Retains only the specified columns in the DataModel, removing all others.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel.validColumns("age", "name");  // Keeps only the "age" and "name" columns in the data model
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If any of the specified columns do not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param columns The names of the columns to be retained.
     * @return The current DataModelBase instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel setValidColumns(String... columns) {
        Set<String> columnSet = new HashSet<>(Arrays.asList(columns));
        return setValidColumns(columnSet);
    }

    /**
     * Retains only the specified columns in the DataModel, removing all others.
     *
     * <p>Functionally identical to {@link #validColumns(String...)}, but accepts a List of column names.</p>
     * 
     * @param columns The list of column names to be retained.
     * @return The current DataModelBase instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel setValidColumns(List<String> columns) {
        Set<String> columnSet = new HashSet<>(columns);
        return setValidColumns(columnSet);
    }

    /**
     * Retains only the specified columns in the DataModel, removing all others.
     *
     * <p>Functionally identical to {@link #validColumns(String...)}, but accepts a Set of column names.</p>
     * 
     * @param columns The set of column names to be retained.
     * @return The current DataModelBase instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel setValidColumns(Set<String> columns) {
        for (String column : columns) {
            if (!hasColumn(column)) {
                throw new DataException("Column " + column + " does not exist.");
            }
        }
        cols.retainAll(columns);
        for (HashMap<String, Object> row : rows) {
            row.keySet().retainAll(columns);
        }
        return this;
    }

    /**
     * Checks if all rows have non-null values for the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * boolean allNonNull = dataModel.isNotNullColumn("age");  // Returns true if all rows have non-null "age" values
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to check.
     * @return True if all rows have non-null values for the specified column, otherwise false.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public boolean isNotNullColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        for (int i = 0; i < rows.size(); i++) {
            HashMap<String, Object> row = rows.get(i);
            if (row.get(column) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds and returns the first row with a null value for the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * HashMap<String, Object> firstNullRow = dataModel.findFirstRowNullColumn("age");  
     * // Returns the first row with a null "age" value, or null if no such row exists
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to check.
     * @return The first row with a null value for the specified column, or null if no such row exists.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public HashMap<String, Object> findFirstRowNullColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        for (int i = 0; i < rows.size(); i++) {
            HashMap<String, Object> row = rows.get(i);
            if (row.get(column) == null) {
                return getRow(i);
            }
        }
        return null;
    }

    /**
     * Checks if all rows have unique (non-duplicated) values for the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * boolean allUnique = dataModel.isNotDuplColumn("name");  // Returns true if all rows have unique "name" values
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to check.
     * @return True if all rows have unique values for the specified column, otherwise false.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public Boolean isNotDuplColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        Set<Object> seenValues = new HashSet<>();
        for (HashMap<String, Object> row : rows) {
            Object value = row.get(column);
            if (value != null) {
                if (seenValues.contains(value)) {
                    return false;
                } else {
                    seenValues.add(value);
                }
            }
        }
        return true;
    }

    /**
     * Finds and returns the first row with a duplicated value for the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * HashMap<String, Object> firstDuplRow = dataModel.findFirstRowDuplColumn("name");  
     * // Returns the first row with a duplicated "name" value, or null if no such row exists
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to check.
     * @return The first row with a duplicated value for the specified column, or null if no such row exists.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public HashMap<String, Object> findFirstRowDuplColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        Set<Object> seenValues = new HashSet<>();
        for (HashMap<String, Object> row : rows) {
            Object value = row.get(column);
            if (value != null) {
                if (seenValues.contains(value)) {
                    return new HashMap<>(row);
                } else {
                    seenValues.add(value);
                }
            }
        }
        return null;
    }

    /**
     * Validates all non-null values of a specified column using a provided validator.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate&lt;Object&gt; isEmail = value -&gt; {
     *     if (!(value instanceof String)) return false;
     *     String stringValue = (String) value;
     *     String emailPattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Za-z]{2,4}$";
     *     return stringValue.matches(emailPattern);
     * };
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * if (dataModel.isValidValue("emailColumn", isEmail)) {
     *     System.out.println("All emails are valid.");
     * } else {
     *     System.out.println("There is an invalid email.");
     * }
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to validate.
     * @param validator A predicate that defines the validation criteria.
     * @return True if all non-null values of the specified column are valid, otherwise false.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public boolean isValidValue(String column, Predicate<Object> validator) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        for (HashMap<String, Object> row : rows) {
            Object value = row.get(column);
            if (value != null && !validator.test(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds and returns the first row with an invalid value for the specified column using a provided validator.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate&lt;Object&gt; isEmail = value -&gt; {
     *     if (!(value instanceof String)) return false;
     *     String stringValue = (String) value;
     *     String emailPattern = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Za-z]{2,4}$";
     *     return stringValue.matches(emailPattern);
     * };
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * HashMap&lt;String, Object&gt; firstInvalidRow = dataModel.findFirstRowInvalidValue("emailColumn", isEmail);
     * if (firstInvalidRow != null) {
     *     System.out.println("Row containing the first invalid email: " + firstInvalidRow);
     * } else {
     *     System.out.println("All emails are valid.");
     * }
     * </pre>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If the specified column does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param column The name of the column to validate.
     * @param validator A predicate that defines the validation criteria.
     * @return The first row with an invalid value for the specified column, or null if all values are valid.
     * @throws DataException if the specified column does not exist in the DataModel.
     */
    public HashMap<String, Object> findFirstRowInvalidValue(String column, Predicate<Object> validator) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        for (int i = 0; i < rows.size(); i++) {
            HashMap<String, Object> row = rows.get(i);
            Object value = row.get(column);
            if (value != null && !validator.test(value)) {
                return getRow(i);
            }
        }
        return null;
    }

    /**
     * Searches for rows that match all the given conditions (treated as AND conditions) and returns 
     * the indexes of the matched rows as a list.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * List&lt;Integer&gt; matchedIndexes = dataModel.searchRowIndexes(cond1, cond2);
     * System.out.println("Matching row indexes: " + matchedIndexes);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A list of indexes for the rows that match all conditions.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public List<Integer> searchRowIndexes(Condition... conditions) {
        return searchRowIndexes(true, conditions);
    }

    /**
     * Searches for rows that match (or do not match, based on the {@code bool} parameter) all the given 
     * conditions (treated as AND conditions) and returns the indexes of those rows as a list.
     * 
     * <p>If {@code bool} is set to {@code true}, the method will return the indexes of rows that 
     * match all conditions. If set to {@code false}, the method will return the indexes of rows 
     * that do not match any of the conditions.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * List&lt;Integer&gt; matchedIndexes = dataModel.searchRowIndexes(true, cond1, cond2);
     * System.out.println("Matching row indexes: " + matchedIndexes);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param bool If {@code true}, return indexes of rows that match all conditions. If {@code false}, return indexes of rows that do not match any of the conditions.
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A list of indexes for the rows based on the conditions and the value of {@code bool}.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public List<Integer> searchRowIndexes(boolean bool, Condition... conditions) {
        List<Integer> matchedIndexes = new ArrayList<>();
        HashMap<String, Object> row;
        boolean matchesAll;

        for (int i = 0; i < rows.size(); i++) {
            row = rows.get(i);
            matchesAll = true;

            for (Condition condition : conditions) {
                for (Map.Entry<String, Object> entry : condition.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (!row.containsKey(key)) {
                        throw new DataException("Column " + key + " does not exist.");
                    }
                    if(row.get(key) == null) {
                        if(value != null) {
                            matchesAll = false;
                            break;
                        }
                    }
                    else {
                        if (!row.get(key).equals(value)) {
                            matchesAll = false;
                            break;
                        }
                    }
                }
                if (!matchesAll) break;
            }

            if ((bool && matchesAll) || (!bool && !matchesAll)) {
                matchedIndexes.add(i);
            }
        }
        return (List<Integer>) matchedIndexes;
    }

    /**
     * Searches for rows that match all the given conditions (treated as AND conditions) and returns 
     * the matched rows as a list.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * List&lt;HashMap&lt;String, Object&gt;&gt; matchedRows = dataModel.searchRows(cond1, cond2);
     * System.out.println("Matching rows: " + matchedRows);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A list of rows that match all conditions.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public List<HashMap<String, Object>> searchRows(Condition... conditions) {
        return searchRows(true, conditions);
    }

    /**
     * Searches for rows that match (or do not match, based on the {@code bool} parameter) all the given 
     * conditions (treated as AND conditions) and returns the matched rows as a list.
     * 
     * <p>If {@code bool} is set to {@code true}, the method will return the rows that 
     * match all conditions. If set to {@code false}, the method will return the rows 
     * that do not match any of the conditions.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * List&lt;HashMap&lt;String, Object&gt;&gt; matchedRows = dataModel.searchRows(true, cond1, cond2);
     * System.out.println("Matching rows: " + matchedRows);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param bool If {@code true}, return rows that match all conditions. If {@code false}, return rows that do not match any of the conditions.
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A list of rows based on the conditions and the value of {@code bool}.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public List<HashMap<String, Object>> searchRows(Boolean bool, Condition... conditions) {
        List<HashMap<String, Object>> matchedRows = new ArrayList<>();
        HashMap<String, Object> row;
        boolean matchesAll;

        for (int i = 0; i < rows.size(); i++) {
            row = rows.get(i);
            matchesAll = true;

            for (Condition condition : conditions) {
                for (Map.Entry<String, Object> entry : condition.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (!row.containsKey(key)) {
                        throw new DataException("Column " + key + " does not exist.");
                    }
                    if(row.get(key) == null) {
                        if(value != null) {
                            matchesAll = false;
                            break;
                        }
                    }
                    else {
                        if (!row.get(key).equals(value)) {
                            matchesAll = false;
                            break;
                        }
                    }
                }
                if (!matchesAll) break;
            }

            if ((bool && matchesAll) || (!bool && !matchesAll)) {
                matchedRows.add(new HashMap<String, Object>(row));
            }
        }
        return (List<HashMap<String, Object>>) matchedRows;
    }

    /**
     * Searches for rows that match all the provided conditions (treated as AND conditions) 
     * and returns the matched rows encapsulated in a new {@link DataModel}.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * DataModelBase matchedDataModel = dataModel.searchRowsAsDataModel(cond1, cond2);
     * System.out.println("Matched DataModel: " + matchedDataModel);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A new {@link DataModel} containing rows that match all provided conditions.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public DataModel searchRowsAsDataModel(Condition... conditions) {
        return searchRowsAsDataModel(true, conditions);
    }

    /**
     * Searches for rows that match (or do not match, based on the {@code bool} parameter) 
     * all the provided conditions (treated as AND conditions) and returns the matched rows 
     * encapsulated in a new {@link DataModel}.
     * 
     * <p>If {@code bool} is set to {@code true}, the method will return the rows that 
     * match all conditions. If set to {@code false}, the method will return the rows 
     * that do not match any of the conditions.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * DataModelBase matchedDataModel = dataModel.searchRowsAsDataModel(true, cond1, cond2);
     * System.out.println("Matched DataModel: " + matchedDataModel);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * </ul>
     *
     * @param bool If {@code true}, return rows that match all conditions. If {@code false}, return rows that do not match any of the conditions.
     * @param conditions Varargs of conditions to be used for matching rows.
     * @return A new {@link DataModel} containing rows based on the conditions and the value of {@code bool}.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public DataModel searchRowsAsDataModel(Boolean bool, Condition... conditions) {
        DataModel matchedDm = new DataModel(this.cols);
        HashMap<String, Object> row;
        boolean matchesAll;

        for (int i = 0; i < rows.size(); i++) {
            row = rows.get(i);
            matchesAll = true;

            for (Condition condition : conditions) {
                for (Map.Entry<String, Object> entry : condition.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (!row.containsKey(key)) {
                        throw new DataException("Column " + key + " does not exist.");
                    }
                    if(row.get(key) == null) {
                        if(value != null) {
                            matchesAll = false;
                            break;
                        }
                    }
                    else {
                        if (!row.get(key).equals(value)) {
                            matchesAll = false;
                            break;
                        }
                    }
                }
                if (!matchesAll) break;
            }

            if ((bool && matchesAll) || (!bool && !matchesAll)) {
                matchedDm.addRow(new HashMap<String, Object>(row));
            }
        }
        return matchedDm;
    }

    /**
     * Filters the current {@link DataModel} based on the provided conditions (treated as AND conditions), 
     * retaining only the rows that match all conditions. The DataModel is modified in place.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel.searchAndModify(cond1, cond2);
     * System.out.println("Filtered DataModel: " + dataModel);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * <li>This operation modifies the original DataModel.</li>
     * </ul>
     *
     * @param conditions Varargs of conditions to be used for filtering rows.
     * @return The modified {@link DataModel} containing rows that match all provided conditions.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public DataModel searchAndModify(Condition... conditions) {
        return searchAndModify(true, conditions);
    }

    /**
     * Filters the current {@link DataModel} based on the provided conditions (treated as AND conditions) and the value of {@code bool}, 
     * retaining only the rows that match (or do not match, based on the {@code bool} parameter) all conditions. 
     * The DataModel is modified in place.
     * 
     * <p>If {@code bool} is set to {@code true}, the method will retain the rows that 
     * match all conditions. If set to {@code false}, the method will retain the rows 
     * that do not match any of the conditions.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Condition cond1 = new Condition("column1", "value1");
     * Condition cond2 = new Condition("column2", "value2");
     * 
     * DataModelBase dataModel = new DataModelBase();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel.searchAndModify(true, cond1, cond2);
     * System.out.println("Filtered DataModel: " + dataModel);
     * </pre>
     * 
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If a specified column from the conditions does not exist in the DataModel, a {@link DataException} will be thrown.</li>
     * <li>This operation modifies the original DataModel.</li>
     * </ul>
     *
     * @param bool If {@code true}, retain rows that match all conditions. If {@code false}, retain rows that do not match any of the conditions.
     * @param conditions Varargs of conditions to be used for filtering rows.
     * @return The modified {@link DataModel} containing rows based on the conditions and the value of {@code bool}.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public DataModel searchAndModify(Boolean bool, Condition... conditions) {
        List<HashMap<String, Object>> matchedRows = new ArrayList<>();
        HashMap<String, Object> row;
        boolean matchesAll;

        for (int i = 0; i < rows.size(); i++) {
            row = rows.get(i);
            matchesAll = true;

            for (Condition condition : conditions) {
                for (Map.Entry<String, Object> entry : condition.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (!row.containsKey(key)) {
                        throw new DataException("Column " + key + " does not exist.");
                    }
                    if(row.get(key) == null) {
                        if(value != null) {
                            matchesAll = false;
                            break;
                        }
                    }
                    else {
                        if (!row.get(key).equals(value)) {
                            matchesAll = false;
                            break;
                        }
                    }
                }
                if (!matchesAll) break;
            }

            if ((bool && matchesAll) || (!bool && !matchesAll)) {
                matchedRows.add(row);
            }
        }
        this.rows.clear();
        this.addRows(matchedRows);
        return this;
    }

    /**
     * Returns the indices of rows that satisfy the provided filter predicate.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate<HashMap<String, Object>> ageFilter = row -> {
     *     if (row.containsKey("age") && row.get("age") instanceof Number) {
     *         return ((Number) row.get("age")).intValue() >= 30;
     *     }
     *     return false;
     * };
     * List<Integer> matchedIndexes = originalDm.filterRowIndexes(ageFilter);
     * </pre>
     *
     * @param filter The filter predicate to test each row.
     * @return List of integers representing the indices of matched rows.
     */
    public List<Integer> filterRowIndexes(Predicate<HashMap<String, Object>> filter) {
        List<Integer> matchedIndexes = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (filter.test(rows.get(i))) {
                matchedIndexes.add(i);
            }
        }
        return matchedIndexes;
    }

    /**
     * Returns a list of rows that satisfy the provided filter predicate.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate<HashMap<String, Object>> nameFilter = row -> "John".equals(row.get("name"));
     * List<HashMap<String, Object>> matchedRows = originalDm.filterRows(nameFilter);
     * </pre>
     *
     * @param filter The filter predicate to test each row.
     * @return List of rows that match the filter condition.
     */
    public List<HashMap<String, Object>> filterRows(Predicate<HashMap<String, Object>> filter) {
        List<HashMap<String, Object>> matchedRows = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            if (filter.test(row)) {
                matchedRows.add(new HashMap<String, Object>(row));
            }
        }
        return matchedRows;
    }

    /**
     * Filters the current {@link DataModel} based on the provided filter predicate and 
     * returns a new {@link DataModel} containing only the rows that satisfy the filter.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate<HashMap<String, Object>> ageFilter = row -> {
     *     if (row.containsKey("age") && row.get("age") instanceof Number) {
     *         return ((Number) row.get("age")).intValue() >= 30;
     *     }
     *     return false;
     * };
     * DataModelBase filteredDm = originalDm.filterRowsAsDataModel(ageFilter);
     * </pre>
     *
     * @param filter The filter predicate to test each row.
     * @return A new {@link DataModel} containing rows that match the filter condition.
     */
    public DataModel filterRowsAsDataModel(Predicate<HashMap<String, Object>> filter) {
        DataModel matchedDm = new DataModel(this.cols);
        for (HashMap<String, Object> row : rows) {
            if (filter.test(row)) {
                matchedDm.addRow(new HashMap<String, Object>(row));
            }
        }
        return matchedDm;
    }

    /**
     * Filters the current {@link DataModel} based on the provided filter predicate. 
     * Only rows that satisfy the filter condition are retained. The DataModel is modified in place.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * Predicate<HashMap<String, Object>> nameFilter = row -> "John".equals(row.get("name"));
     * originalDm.filterAndModify(nameFilter);
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel.</p>
     *
     * @param filter The filter predicate to test each row.
     * @return The modified {@link DataModel} containing rows that match the filter condition.
     */
    public DataModel filterAndModify(Predicate<HashMap<String, Object>> filter) {
        ArrayList<HashMap<String, Object>> matchedRows = new ArrayList<>();
        for (HashMap<String, Object> row : rows) {
            if (filter.test(row)) {
                matchedRows.add(row);
            }
        }
        this.rows.clear();
        this.addRows(matchedRows);
        return this;
    }

    /**
     * Sorts the columns in ascending order. The order of columns in the {@link DataModel} 
     * is updated to reflect the sorted order.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortColumnAscending();
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's column order.</p>
     *
     * @return The modified {@link DataModel} with columns sorted in ascending order.
     */
    public DataModel sortColumnAscending() {
        List<String> list = new ArrayList<>(cols);
        Collections.sort(list);
        cols.clear();
        cols.addAll(list);
        return this;
    }

    /**
     * Sorts the columns in descending order. The order of columns in the {@link DataModel} 
     * is updated to reflect the sorted order.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortColumnDescending();
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's column order.</p>
     *
     * @return The modified {@link DataModel} with columns sorted in descending order.
     */
    public DataModel sortColumnDescending() {
        List<String> list = new ArrayList<>(cols);
        Collections.sort(list, Collections.reverseOrder());
        cols.clear();
        cols.addAll(list);
        return this;
    }

    /**
     * Reverses the order of columns in the {@link DataModel}. If the original column order 
     * was [A, B, C], after this operation it will be [C, B, A].
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortColumnReverse();
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's column order.</p>
     *
     * @return The modified {@link DataModel} with columns in reversed order.
     */
    public DataModel sortColumnReverse() {
        List<String> list = new ArrayList<>(cols);
        Collections.reverse(list);
        cols.clear();
        cols.addAll(list);
        return this;
    }

    /**
     * Sorts the rows in ascending order based on the values of the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortRowAscending("columnName");
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's row order.</p>
     *
     * @param column The column name based on which the rows will be sorted.
     * @return The modified {@link DataModel} with rows sorted in ascending order based on the specified column.
     * @throws DataException If the specified column does not exist.
     */
    public DataModel sortRowAscending(String column) {
        return sortRowAscending(column, false);
    }

    /**
     * Sorts the rows in ascending order based on the values of the specified column, 
     * with an option to treat the string values as integers.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortRowAscending("columnName", true);
     * </pre>
     *
     * <p>If {@code isIntegerOrder} is set to true, the method attempts to parse string values
     * as numbers to perform the comparison. This is useful for cases where string values are
     * numerical but are stored as strings (e.g., "100", "200").</p>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's row order.</p>
     *
     * @param column The column name based on which the rows will be sorted.
     * @param isIntegerOrder If true, attempts to treat string values as numbers for sorting.
     * @return The modified {@link DataModel} with rows sorted in ascending order based on the specified column.
     * @throws DataException If the specified column does not exist, or if mixed or unsupported types are encountered.
     */
    public DataModel sortRowAscending(String column, Boolean isIntegerOrder) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        rows.sort((m1, m2) -> {
            Object v1 = m1.get(column);
            Object v2 = m2.get(column);

            if (v1 == null) return 1;
            if (v2 == null) return -1;

            if (v1 instanceof String || v1 instanceof Character) {
                if (!(v2 instanceof String || v2 instanceof Character)) {
                    throw new DataException("Mixed types are not allowed");
                }
                if (isIntegerOrder) {
                    try {
                        return Double.compare(Double.parseDouble((String) v1), Double.parseDouble((String) v2));
                    } catch (NumberFormatException e) {
                        throw new DataException("Cannot convert string to number");
                    }
                } else {
                    return ((String) v1).compareTo((String) v2);
                }
            }

            if (v1 instanceof Boolean) {
                if (!(v2 instanceof Boolean)) {
                    throw new DataException("Mixed types are not allowed");
                }
                return Boolean.compare((Boolean) v1, (Boolean) v2);
            }

            if (v1 instanceof Number) {
                if (!(v2 instanceof Number)) {
                    throw new DataException("Mixed types are not allowed");
                }
                return Double.compare(((Number) v1).doubleValue(), ((Number) v2).doubleValue());
            }

            throw new DataException("Unsupported type or mixed types are not allowed");
        });
        return this;
    }

    /**
     * Sorts the rows in descending order based on the values of the specified column.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortRowDescending("columnName");
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's row order.</p>
     *
     * @param column The column name based on which the rows will be sorted.
     * @return The modified {@link DataModel} with rows sorted in descending order based on the specified column.
     * @throws DataException If the specified column does not exist.
     */
    public DataModel sortRowDescending(String column) {
        return sortRowDescending(column, false);
    }

    /**
     * Sorts the rows in descending order based on the values of the specified column, 
     * with an option to treat the string values as integers.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortRowDescending("columnName", true);
     * </pre>
     *
     * <p>If {@code isIntegerOrder} is set to true, the method attempts to parse string values
     * as numbers to perform the comparison. This is useful for cases where string values are
     * numerical but are stored as strings (e.g., "100", "200").</p>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's row order.</p>
     *
     * @param column The column name based on which the rows will be sorted.
     * @param isIntegerOrder If true, attempts to treat string values as numbers for sorting.
     * @return The modified {@link DataModel} with rows sorted in descending order based on the specified column.
     * @throws DataException If the specified column does not exist, or if mixed or unsupported types are encountered.
     */
    public DataModel sortRowDescending(String column, Boolean isIntegerOrder) {
        if (!hasColumn(column)) {
            throw new DataException("Column " + column + " does not exist.");
        }
        rows.sort((m1, m2) -> {
            Object v1 = m1.get(column);
            Object v2 = m2.get(column);

            if (v1 == null) return -1;
            if (v2 == null) return 1;

            if (v1 instanceof String || v1 instanceof Character) {
                if (!(v2 instanceof String || v2 instanceof Character)) {
                    throw new DataException("Mixed types are not allowed");
                }
                if (isIntegerOrder) {
                    try {
                        return Double.compare(Double.parseDouble((String) v2), Double.parseDouble((String) v1));
                    } catch (NumberFormatException e) {
                        throw new DataException("Cannot convert string to number");
                    }
                } else {
                    return ((String) v2).compareTo((String) v1);
                }
            }

            if (v1 instanceof Boolean) {
                if (!(v2 instanceof Boolean)) {
                    throw new DataException("Mixed types are not allowed");
                }
                return Boolean.compare((Boolean) v2, (Boolean) v1);
            }

            if (v1 instanceof Number) {
                if (!(v2 instanceof Number)) {
                    throw new DataException("Mixed types are not allowed");
                }
                return Double.compare(((Number) v2).doubleValue(), ((Number) v1).doubleValue());
            }

            throw new DataException("Unsupported type or mixed types are not allowed");
        });
        return this;
    }

    /**
     * Reverses the order of rows in the DataModel.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * originalDm.sortRowReverse();
     * </pre>
     *
     * <p><b>Note:</b> This operation modifies the original DataModel's row order.</p>
     *
     * @return The modified {@link DataModel} with its rows in reverse order.
     */
    public DataModel sortRowReverse() {
        Collections.reverse(rows);
        return this;
    }
}
