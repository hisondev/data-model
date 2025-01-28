package io.github.hison.data.model;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.NullNode;

import io.github.hison.data.condition.Condition;
import io.github.hison.data.converter.DataConverter;
import io.github.hison.data.converter.DataConverterFactory;
import io.github.hison.data.exception.DataException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Represents a structured data model primarily used for data communication between client and server. 
 * It organizes data in a tabular structure, facilitating easy management and manipulation of data. 
 * This model is capable of handling complex data structures, including nested objects and arrays, 
 * represented as key-value pairs in a table-like format.
 *
 * <p>Features:</p>
 * <ul>
 *     <li>Facilitates the conversion and management of data between JSON format and DataModel, 
 *         as well as between Entity class data and DataModel.</li>
 *     <li>Utilizes {@link DataConverter} for customizable data conversion. 
 *         The default implementation can be overridden using Spring's dependency injection, 
 *         allowing for custom data handling strategies.</li>
 *     <li>Supports JSON data structure consisting of key-value pairs for easy server communication. 
 *         The structure is flexible to accommodate various data types including nested objects and arrays.</li>
 *     <li>Integrates seamlessly with 'hison' API-controller artifact and associated JavaScript libraries 
 *         (dataModel.js, dataLink.js) for streamlined data exchange without additional coding requirements.</li>
 *     <li>Relies on several key dependencies including Spring Context, Spring Boot Autoconfigure, 
 *         Jackson Databind, JPA API, and Servlet API to function effectively within a Spring-based application environment.</li>
 * </ul>
 *
 * <p>Example JSON structure for server communication:</p>
 * <pre>
 * {
 *     "key1": "value", 
 *     "key2": [
 *         {"key1": "value"},
 *         {"key1": "value"}
 *     ]
 * }
 * //insert a row
 * //or
 * [
 * {"column1":"value","column2":"value"...},
 * {"column1":"value","column2":"value"...},
 * {"column1":"value","column2":"value"...}...
 * ]
 * //insert rows
 * </pre>
 * 
 * <p>Customization:</p>
 * Developers using the DataModel.jar can customize the data conversion process by extending this class. 
 * Custom converters can be defined by extending the `DataConverterDefault` class and registering the custom 
 * converter using the `DataConverterFactory`. This allows for tailored data handling strategies that fit 
 * specific application requirements.
 *
 * <pre>
 * public class CustomDataConverter extends DataConverterDefault {
 *     public static void register() {
 *         DataConverterFactory.setCustomConverter(new CustomDataConverter());
 *     }
 *     // Custom logic...
 * }
 *
 * public class Application {
 *     public static void main(String[] args) {
 *         CustomDataConverter.register(); // Registering the custom converter
 *     }
 * }
 * </pre>
 * 
 * <p>Usage:</p>
 * <ul>
 *     <li>For custom conversion logic, define a new {@link DataConverter} and configure it as a Spring bean.</li>
 *     <li>When extending this class, override necessary methods to adapt the data model to specific application needs.</li>
 * </ul>
 *
 * @author Hani son
 * @version 1.0.5
 */
@JsonDeserialize(using = DataModelDeserializer.class)
@JsonSerialize(using = DataModelSerializer.class)
public final class DataModel implements Cloneable{
    private LinkedHashSet<String> cols;
    private ArrayList<HashMap<String, Object>> rows;
    private boolean freeze = false;
    private boolean freezeValues = false;
    
    private DataConverter getConverter() {
        return DataConverterFactory.getConverter();
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
                value = getConverter().getConvertJsonValueNodeToDataModelRowValue(valueNode);
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
                array.add(getConverter().getConvertJsonValueNodeToDataModelRowValue(elementNode));
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

    private void checkAddRowsRange(int rowIndex) {
        if (rowIndex < 0 || rowIndex > rows.size()) {
            throw new DataException("Provided Index: " + rowIndex + " is out of range. Valid range is 0 to " + rows.size() + ".");
        }
    }

    private List<Map<String, Object>> getConvertedEntitiesToMaps(List<Object> entities) {
        List<Map<String, Object>> maps = new ArrayList<>();
        ObjectMapper mapper = getConverter().getObjectMapperForConvertEntitiesToDataModel();
    
        for (Object entity : entities) {
            try {
                String json = mapper.writeValueAsString(entity);
                HashMap<String, Object> map = mapper.readValue(json, new TypeReference<HashMap<String, Object>>() {});
                maps.add(map);
            } catch (Exception e) {
                throw new DataException("Failed to convert entity to map", e);
            }
        }
        return maps;
    }

    /**
     * Default constructor for the dataModel class.
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
     * Constructor for the DataModel class with a single column name.
     *
     * <p>Initializes the columns (cols) with the provided single column name, ensuring order 
     * of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap}, where each HashMap represents a row with 
     * key-value pairs corresponding to column names and their respective values.</p>
     *
     * @param newColumn The name of the single column to be added when creating a new instance 
     *                  of DataModel.
     */
    public DataModel(String newColumn) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        this.cols.add(newColumn);
    }
    
    /**
     * Constructor for the dataModel class with variable column names.
     *
     * <p>Initializes the columns (cols) with the provided column names, ensuring order 
     * of insertion and uniqueness using a {@link LinkedHashSet}. The rows are initialized 
     * as an {@link ArrayList} of {@link HashMap} where each HashMap represents a row with 
     * key-value pairs corresponding to column names and their respective values.</p>
     *
     * @param newColumns A varargs parameter allowing the user to input any number of column 
     *                   names when creating a new instance of dataModel.
     */
    public DataModel(String... newColumns) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        for(String col : newColumns) {
            this.cols.add(col);
        }
    }

    /**
     * Constructor for the dataModel class with a set of column names.
     *
     * <p>Initializes the columns (cols) with the provided set of column names, maintaining the 
     * order of insertion and ensuring uniqueness using a {@link LinkedHashSet}. The rows are 
     * initialized as an {@link ArrayList} of {@link HashMap} where each HashMap represents a row 
     * with key-value pairs corresponding to column names and their respective values.</p>
     *
     * @param newColumns A set containing the column names to be initialized in the dataModel instance.
     */
    public DataModel(Set<String> newColumns) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        for(String col : newColumns) {
            this.cols.add(col);
        }
    }

    /**
     * Constructor for the dataModel class with an initial row represented as a map.
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
     * @param queryResult the Object array containing the data to initialize the DataModel.
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
     */
    public DataModel(ResultSet rs){
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        addRow(rs);
    }

    /**
     * Constructs a new DataModel instance initialized with data from a {@link JsonNode}. This constructor 
     * can handle both single JSON objects and arrays of JSON objects, converting them into one or more rows 
     * of the DataModel. It leverages the addRow(JsonNode) method for the conversion and addition of rows.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Initializes the columns (cols) as a {@link LinkedHashSet} and rows as an {@link ArrayList} of {@link HashMap}s.</li>
     *     <li>Uses the addRow(JsonNode) method to convert the provided JsonNode into DataModel rows.</li>
     *     <li>Handles both single JSON objects and JSON arrays, adding each as individual rows to the DataModel.</li>
     * </ul>
     *
     * <p>This constructor is particularly useful for initializing a new DataModel directly from JSON data, 
     * making it a versatile option for JSON data processing and manipulation.</p>
     *
     * @param node a {@link JsonNode} representing either a single row (as a JSON object) or multiple rows (as a JSON array)
     *             to initialize the DataModel
     */
    public DataModel (JsonNode node) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();
        
        addRows((JsonNode) node);
    }

    /**
     * Constructs a new DataModel instance initialized with a single row derived from an entity object.
     * This constructor facilitates the creation of a DataModel from an entity, converting the entity's fields
     * into columns and values of the DataModel's first row. It leverages {@link #addRow(Object)} to handle the
     * conversion and integration of the entity into the DataModel's structure.
     *
     * <p>Key Steps:</p>
     * <ul>
     *     <li>Checks if the provided entity is not null. Throws a {@link DataException} if null.</li>
     *     <li>Initializes the columns (cols) and rows of the DataModel as empty collections.</li>
     *     <li>Uses {@link #addRow(Object)} to convert the entity to a DataModel row and add it to the DataModel.</li>
     * </ul>
     *
     * <p>This constructor is particularly useful for initializing a new DataModel with data from an existing entity,
     * ensuring that the entity's data is accurately represented within the DataModel's tabular format.</p>
     *
     * @param entity An object representing the initial row to be added to the DataModel
     * @throws DataException if the provided entity is null
     */
    public DataModel(Object entity) {
        if(entity == null) {
            throw new DataException("You can not insert null.");
        }
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        addRow(entity);
    }

    /**
     * Constructs a new DataModel instance initialized with a list of rows. The type of the first item in the list
     * determines how the rows are added to the DataModel. This constructor supports initialization with various data
     * types including lists of strings (as column names), maps, tuples, or objects (entities).
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Initializes the columns (cols) as a {@link LinkedHashSet} and rows as an {@link ArrayList} of {@link HashMap}s.</li>
     *     <li>Checks the type of the first item in the newRows list and applies the appropriate method:</li>
     *     <li>If the items are Strings, sets them as column names using {@link #setColumns(List)}.</li>
     *     <li>If the items are Maps or Tuples, adds them as rows using {@link #addRows(List)}.</li>
     *     <li>If the items are Objects (entities), converts them to rows and adds them using {@link #addRows(List)}.</li>
     * </ul>
     *
     * <p>This constructor offers a versatile way to initialize a DataModel with different types of data, making it suitable 
     * for various use cases where the data format may vary.</p>
     *
     * @param <T> the type of the elements in the newRows list
     * @param newRows a list representing the initial data with which to populate the DataModel. The data type of the list 
     *                elements determines how they are processed and added to the DataModel.
     * @throws DataException if there's an error in setting columns or adding rows
     */
    @SuppressWarnings("unchecked")
    public <T> DataModel(List<T> newRows) {
        this.cols = new LinkedHashSet<String>();
        this.rows = new ArrayList<HashMap<String, Object>>();

        if (!newRows.isEmpty()) {
            T t = newRows.get(0);
            if (t instanceof String) {
                setColumns((List<String>) newRows);
            } else if (t instanceof Map) {
                addRows((List<Map<String, Object>>) newRows);
            } else if (t instanceof Object){
                addRows((List<Object>) newRows);
            }
        }
    }
        
    /**
     * Constructor for the dataModel class that initializes the DataModel with the results 
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
     * <code>dataModel dataModel = new dataModel(results, new String[]{"name", "age"});</code></p>
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
     * @return the <code>dataModel</code> instance with cleared columns and rows.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel clear() {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        cols.clear();
        rows.clear();

        return this;
    }

    /**
     * Creates a deep copy of the current dataModel instance.
     * 
     * <p>This method returns a new instance of <code>dataModel</code> with the same column definitions and data rows 
     * as the current instance. The returned instance is entirely independent of the original, and changes to 
     * the original won't affect the cloned instance and vice versa.</p>
     * 
     * <p><b>Deep Copy:</b> The columns and rows in the cloned instance are deep copies, ensuring that the original 
     * and the clone are completely isolated from each other.</p>
     *
     * <p><b>Example:</b><br>
     * dataModel original = new dataModel(...);
     * dataModel copy = original.clone();
     * // Modifications to 'original' won't affect 'copy' and vice versa.</p>
     * 
     * @return a new <code>dataModel</code> instance that's a deep copy of the current instance.
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
     * Inserts rows from the provided dataModel instance into the current instance.
     * 
     * <p>This method extracts rows from the given <code>dataModel</code> and appends them to the current instance.
     * Columns from the provided data model that don't exist in the current instance will be added. The order of rows
     * from the provided data model will be preserved when they are added to the current instance.</p>
     * 
     * <p><b>Example:</b><br>
     * dataModel dm1 = new dataModel(...); // Contains some rows
     * dataModel dm2 = new dataModel(...); // Contains some other rows
     * dm1.insert(dm2); // Now, 'dm1' contains rows from both 'dm1' and 'dm2'</p>
     * 
     * @param dataModel the <code>dataModel</code> instance from which rows will be extracted and added to the current instance.
     * @return the <code>dataModel</code> instance (i.e., the current instance) after inserting the new rows.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel insert(DataModel dataModel){
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        List<HashMap<String, Object>> newRows = dataModel.getRows();
        return addRows(newRows);
    }

    /**
     * Converts the current DataModel instance to a {@link JsonNode}, representing the DataModel's structure in JSON format.
     * This method leverages the {@link DataConverter#getConvertedJson(DataModel)} method, which can be customized by the user,
     * allowing for flexible and user-defined JSON conversion strategies.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Utilizes a customized {@link ObjectMapper} obtained from {@link DataConverter#getObjectMapperForConvertDataModelToJson()}
     *         to serialize the DataModel's columns, rows, and metadata into JSON.</li>
     *     <li>Includes the number of columns and rows as part of the JSON output.</li>
     *     <li>Handles potential exceptions during the conversion, encapsulating them in a {@link DataException}.</li>
     * </ul>
     *
     * <p>This method is essential for transforming the DataModel into a JSON representation, making it suitable for data exchange,
     * storage, or further processing. The flexibility in customization allows it to cater to various JSON structure requirements.</p>
     *
     * @return a {@link JsonNode} representing the DataModel in JSON format
     * @throws DataException if any issues occur during the conversion to JSON
     */
    public JsonNode getConvertedJson() {
        return getConverter().getConvertedJson(this);
    }

    /**
     * Serializes the current instance of DataModel into JSON format using a {@link JsonGenerator}.
     * This method delegates the serialization process to the {@link DataConverter#serialize(DataModel, JsonGenerator, SerializerProvider)} method,
     * allowing for a flexible and customizable serialization process.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Delegates the serialization task to the DataConverter's serialize method.</li>
     *     <li>Ensures that the DataModel's structure, including its columns and rows, is accurately represented in the JSON output.</li>
     *     <li>Handles the serialization process in a way that accommodates custom data types and formatting requirements as defined in the DataConverter.</li>
     * </ul>
     *
     * <p>This method is crucial for converting the DataModel into a JSON representation suitable for data exchange,
     * storage, or further processing. It encapsulates the complexity of the serialization process while providing
     * flexibility through the use of a customizable DataConverter.</p>
     *
     * @param dataModel The DataModel instance to be serialized.
     * @param gen The JsonGenerator used for writing JSON content.
     * @param serializers Provider that can be used to get serializers for serializing Objects value contains, if any.
     * @throws IOException if any issues occur during the conversion to JSON.
     */
    public void serialize(DataModel dataModel, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        getConverter().serialize(dataModel, gen, serializers);
    }

    /**
     * Converts the rows of the DataModel into a list of entities of the specified class. This method uses the
     * {@link DataConverter#getConvertedEntities(Class, DataModel)} method, which can be customized by the user,
     * allowing for flexible and user-defined entity conversion strategies.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Utilizes a customized {@link ObjectMapper} obtained from {@link DataConverter#getObjectMapperForConvertDataModelToEntities()}
     *         to deserialize the DataModel rows into entities of the specified class.</li>
     *     <li>Supports conversion of complex data structures represented in the DataModel rows into corresponding entity objects.</li>
     *     <li>Handles potential exceptions during the conversion, encapsulating them in a {@link DataException}.</li>
     * </ul>
     *
     * <p>This method is essential for transforming the structured data in the DataModel back into entity objects, making it 
     * suitable for business logic processing or data manipulation. The customization ability ensures that it can cater to 
     * various entity structure requirements.</p>
     *
     * @param <T> the class type of the entities to be created
     * @param entityClass the class of the entities to which the rows are to be converted
     * @return a list of entities of the specified class, each corresponding to a row in the DataModel
     * @throws DataException if any error occurs during the conversion process
     */
    public <T> List<T> getConvertedEntities(Class<T> entityClass) {
        return getConverter().getConvertedEntities(entityClass, this);
    }

    /**
     * Defines the columns for this dataModel instance using the specified column names.
     * 
     * <p>This method sets the column names for the current dataModel instance. If columns have 
     * already been defined for this instance, invoking this method will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns Varargs of column names to be set for this dataModel instance.
     * @return The current dataModel instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setColumns(String... columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Defines the columns for this dataModel instance using the specified set of column names.
     * 
     * <p>This method sets the column names for the current dataModel instance using a {@link Set} of 
     * column names. If columns have already been defined for this instance, invoking this method 
     * will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns A set of column names to be set for this dataModel instance.
     * @return The current dataModel instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setColumns(Set<String> columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Defines the columns for this dataModel instance using the specified list of column names.
     * 
     * <p>This method sets the column names for the current dataModel instance using a {@link List} of 
     * column names. If columns have already been defined for this instance, invoking this method 
     * will result in a {@link DataException}.</p>
     * 
     * <p><b>Note:</b> It's recommended to ensure that columns are not previously defined 
     * before invoking this method to prevent exceptions.</p>
     * 
     * @param columns A list of column names to be set for this dataModel instance.
     * @return The current dataModel instance with the columns set.
     * @throws DataException If columns have already been defined for this instance.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setColumns(List<String> columns) {
        if(isDefine()) {
            throw new DataException("The column has already been defined.");
        }
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        for(String column : columns) {
            cols.add(column);
        }

        return this;
    }

    /**
     * Sets the same value for a specified column across all rows of this dataModel instance.
     * 
     * <p>This method assigns a uniform value to a specific column for all rows in the current
     * dataModel instance. If the column does not exist within the dataModel, this method
     * does nothing and simply returns the current instance.</p>
     * 
     * <p><b>Note:</b> It's advisable to first check if the column exists using the {@code hasColumn}
     * method before using this method to ensure intended behavior.</p>
     * 
     * <p><b>Example:</b><br>
     * {@code
     * dataModel model = new dataModel();
     * model.setColumns("Name", "Age");
     * model.addRow(new HashMap<String, Object>(){{ put("Name", "John"); put("Age", 30); }});
     * model.addRow(new HashMap<String, Object>(){{ put("Name", "Jane"); put("Age", 25); }});
     * model.setColumnSameValue("Age", 28); // This will set the "Age" for both rows to 28.
     * }
     * </p>
     * 
     * @param column The name of the column for which the value is to be uniformly set.
     * @param value The value to be set across all rows for the specified column.
     * @return The current dataModel instance with updated values for the specified column.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setColumnSameValue(String column, Object value) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        if(!hasColumn(column)) return this;
        for (HashMap<String, Object> map : rows) {
            map.put(column, value);
        }
        return this;
    }

    /**
     * Formats values in a specified column using the provided formatter function. This method applies the formatter
     * to every value in the column, and if an error occurs during the formatting, it throws a {@link DataException}.
     *
     * <p>Key Aspects:</p>
     * <ul>
     *     <li>Checks if the specified column exists. If not, it throws a {@link DataException}.</li>
     *     <li>Applies the formatter function to each value in the specified column.</li>
     *     <li>If an error occurs while formatting a specific value, a {@link DataException} is thrown.</li>
     * </ul>
     *
     * <p><b>Note:</b> Ensure that the formatter function can handle all potential data types present in the column to avoid errors.</p>
     *
     * <p><b>Example:</b><br>
     * {@code
     * Function<Object, Object> formatter = value -> {
     *     if (value instanceof String) {
     *         return ((String) value).substring(0, Math.min(3, ((String) value).length()));
     *     } else {
     *         return value;
     *     }
     * };
     * dataModel.setColumnSameFormat("key1", formatter);
     * }
     * </p>
     *
     * @param column The name of the column whose values are to be formatted.
     * @param formatter The function to format values within the specified column.
     * @return The current DataModel instance with formatted values in the specified column.
     * @throws DataException If the column does not exist or an error occurs during formatting.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setColumnSameFormat(String column, Function<Object, Object> formatter) {
        if (!cols.contains(column)) {
            throw new DataException("Column does not exist: " + column);
        }
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }

        for (HashMap<String, Object> row : rows) {
            Object originalValue = row.get(column);
            try {
                Object formattedValue = formatter.apply(originalValue);
                row.put(column, formattedValue);
            } catch (Exception e) {
                throw new DataException("Error formatting value: " + originalValue + ". Leaving it as is.");
            }
        }
        return this;
    }

    /**
     * Adds an empty row to the end of the DataModel. This method utilizes the {@link #addRow(int)} method
     * by passing the current size of the rows as the index, effectively appending the new row to the end.
     *
     * The new row will have the same columns as the existing rows (if any), with all values set to null.
     * If there are no existing rows but columns are defined, an empty row with null values for those columns is added.
     * If both rows and columns are empty, a DataException is thrown, indicating that columns should be added first.
     *
     * <p>This method simplifies the process of adding a new row to the DataModel by automatically appending it to the end,
     * maintaining the structural integrity and allowing for dynamic data manipulation.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * // Assume dataModel is initialized with some columns
     * dataModel.addRow(); // Adds an empty row at the end of the DataModel
     * </pre>
     *
     * @return the current instance of DataModel, with the new row appended at the end
     * @throws DataException if both rows and columns are empty
     */
    public DataModel addRow() {
        return addRow(rows.size());
    }

    /**
     * Adds an empty row at the specified index in the DataModel. The new row will have the same columns
     * as the existing rows (if any), but all values will be set to null.
     * If there are no existing rows but columns are defined, an empty row with null values for those columns is added.
     * If both rows and columns are empty, a DataException is thrown, indicating that columns should be added first.
     *
     * <p>This method enables the insertion of an empty row at a specific position within the DataModel,
     * maintaining the structure consistency and allowing for dynamic data manipulation.</p>
     *
     * <p>Key Aspects:</p>
     * <ul>
     *     <li>Initializes a new row (HashMap) with null values for each column.</li>
     *     <li>Inserts the new row at the specified index in the rows list.</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * // Assume dataModel is initialized with some columns and rows
     * dataModel.addRow(2); // Adds an empty row at the third position
     * </pre>
     *
     * @param rowIndex the index at which the new row should be inserted
     * @return the current instance of DataModel, with the new row added at the specified index
     * @throws DataException if both rows and columns are empty, or if the rowIndex is out of the valid range
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel addRow(int rowIndex) {
        checkAddRowsRange(rowIndex);
        
        if (cols.isEmpty()) {
            throw new DataException("Please add columns first.");
        }
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }

        HashMap<String, Object> newRow = new HashMap<>();
        if (!rows.isEmpty()) {
            // Copy column keys from the last row in rows
            for (String key : rows.get(rows.size() - 1).keySet()) {
                newRow.put(key, null);
            }
        } else {
            // If rows is empty but cols is not, use cols keys
            for (String key : cols) {
                newRow.put(key, null);
            }
        }

        rows.add(rowIndex, newRow);
        return this;
    };

    /**
     * Adds a new row to the end of the DataModel, converting and integrating the provided data into the existing structure.
     * This method delegates to {@link #addRow(int, Map)} using the current size of the rows as the index,
     * effectively appending the new row to the end of the DataModel.
     * 
     * Each key in the newRow map corresponds to a column in the DataModel, and the values are processed
     * using {@link DataConverter#getConvertValueToDataModelRowValue(Object)} to ensure they are in the appropriate format.
     * 
     * <p>Key Aspects:</p>
     * <ul>
     *     <li>Initializes columns (cols) if they are not already set, using the keys from the newRow map.</li>
     *     <li>Converts each value in the newRow map to the appropriate format for the DataModel using the 
     *         configured DataConverter.</li>
     *     <li>Checks for type consistency in each column. If a type mismatch is detected, a {@link DataException} is thrown.</li>
     *     <li>Null values are added for any columns present in DataModel but missing in the newRow map.</li>
     *     <li>Adds the new row to the end of the existing rows.</li>
     * </ul>
     *
     * <p>This method simplifies the process of adding a new row to the DataModel by automatically appending it to the end,
     * maintaining the structural integrity and type consistency of the DataModel.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * Map&lt;String, Object&gt; newRow = new HashMap&lt;&gt;();
     * newRow.put("column1", "value1");
     * newRow.put("column2", 123);
     * dataModel.addRow(newRow);
     * // The DataModel now contains a new row at the end with "value1" in "column1" and 123 in "column2".
     * </pre>
     *
     * @param newRow a map representing the new row to be added, where keys are column names and values are the corresponding data
     * @return the current instance of DataModel, with the new row appended at the end
     */
    public DataModel addRow(Map<String, Object> newRow) {
        return addRow(rows.size(), newRow);
    }

    /**
     * Adds a new row to the DataModel at a specified index, converting and integrating the provided data into the existing structure.
     * Each key in the newRow map corresponds to a column in the DataModel, and the values are processed
     * using {@link DataConverter#getConvertValueToDataModelRowValue(Object)} to ensure they are in the appropriate format.
     *
     * <p>Key Aspects:</p>
     * <ul>
     *     <li>Initializes columns (cols) if they are not already set, using the keys from the newRow map.</li>
     *     <li>Converts each value in the newRow map to the appropriate format for the DataModel using the 
     *         configured DataConverter.</li>
     *     <li>Checks for type consistency in each column. If a type mismatch is detected, a {@link DataException} is thrown.</li>
     *     <li>Null values are added for any columns present in DataModel but missing in the newRow map.</li>
     *     <li>Inserts the new row at the specified index in the rows list.</li>
     * </ul>
     *
     * <p>This method enhances the dynamic construction of the DataModel, allowing rows of data 
     * to be inserted at a specific position while maintaining type integrity and structure consistency.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * Map&lt;String, Object&gt; newRow = new HashMap&lt;&gt;();
     * newRow.put("column1", "value1");
     * newRow.put("column2", 123);
     * dataModel.addRow(0, newRow);
     * // The DataModel now contains a row at the first position with "value1" in "column1" and 123 in "column2".
     * </pre>
     *
     * @param rowIndex the index at which the new row should be inserted
     * @param newRow a map representing the new row to be added, where keys are column names and values are the corresponding data
     * @return the current instance of DataModel, with the new row inserted at the specified index
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException if a column in newRow does not exist in DataModel, if there's a type mismatch in any column,
     *                       or if the rowIndex is out of the valid range
     */
    public DataModel addRow(int rowIndex, Map<String, Object> newRow) {
        checkAddRowsRange(rowIndex);
    
        if (cols.isEmpty()) {
            cols.addAll(newRow.keySet());
        }
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
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
            if (newRow.containsKey(key)) {
                Object value = getConverter().getConvertValueToDataModelRowValue(newRow.get(key));
                if (!rows.isEmpty()) {
                    if (rows.get(rows.size() - 1).get(key) != null && value != null) {
                        if (rows.get(rows.size() - 1).get(key).getClass() != value.getClass()) {
                            throw new DataException("Please enter the same type. Column: " + key);
                        }
                    }
                }
                hm.put(key, value);
            } else {
                hm.put(key, null);
            }
        }
    
        // Insert the new row at the specified index
        rows.add(rowIndex, hm);
    
        return this;
    }
    
    /**
     * Adds a new row of data to the end of this DataModel instance based on the provided array of objects and the array of column names.
     * 
     * <p>This method delegates the addition of a new row to {@link #addRow(int, Object[], String[])}, using the current size
     * of the rows as the index. It maps the elements from the {@code queryResult} array to the respective column names in 
     * {@code columnNames} and then adds them as a new row to the end of the DataModel instance.</p>
     * 
     * <p>If the sizes of the two provided arrays do not match, or if any of the arrays is null, the method will throw a DataException.</p>
     * 
     * @param queryResult An array of objects containing data to be added as a new row.
     * @param columnNames An array of strings where each string represents the column name for the respective data in {@code queryResult}.
     * @return The current DataModel instance with the added row at the end.
     * @throws DataException if there is a mismatch between the lengths of {@code queryResult} and {@code columnNames}, or if either is null.
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of elements in the <code>queryResult</code> array. If there's a mismatch, 
     * it may lead to a DataException indicating an issue with the input arrays.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Example result from a query
     * List&lt;Object[]&gt; results = memberRepository.findAllProjectedBy();
     * String[] columnNames = new String[]{"name", "age", "profession"};
     * 
     * // Create a new DataModel and add each array of results as a row at the end
     * DataModel dataModel = new DataModel();
     * for (Object[] result : results) {
     *     dataModel.addRow(result, columnNames);
     * }
     * </pre>
     */
    public DataModel addRow(Object[] queryResult, String[] columnNames) {
        return addRow(rows.size(), queryResult, columnNames);
    }

    /**
     * Adds a new row of data at the specified index in this DataModel instance based on the provided array of objects and the array of column names.
     * 
     * <p>This method maps the elements from the {@code queryResult} array to the respective column names in {@code columnNames} 
     * and then adds them as a new row at the specified index in the DataModel instance.</p>
     * 
     * <p>If the sizes of the two provided arrays do not match, or if any of the arrays is null, the method will throw a DataException.</p>
     * 
     * @param rowIndex The index at which the new row should be inserted.
     * @param queryResult An array of objects containing data to be added as a new row.
     * @param columnNames An array of strings where each string represents the column name for the respective data in {@code queryResult}.
     * @return The current DataModel instance with the added row at the specified index.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException if there is a mismatch between the lengths of {@code queryResult} and {@code columnNames}, or if either is null.
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of elements in the <code>queryResult</code> array. If there's a mismatch, 
     * it may lead to a DataException indicating an issue with the input arrays.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Example result from a query
     * List&lt;Object[]&gt; results = memberRepository.findAllProjectedBy();
     * String[] columnNames = new String[]{"name", "age", "profession"};
     * 
     * // Create a new DataModel and add the result as a row at a specified index
     * DataModel dataModel = new DataModel();
     * int index = 0;
     * for (Object[] result : results) {
     *     dataModel.addRow(index, result, columnNames);
     *     index++;
     * }
     * </pre>
     */
    public DataModel addRow(int rowIndex, Object[] queryResult, String[] columnNames) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        if (queryResult != null && columnNames != null && queryResult.length == columnNames.length) {
            HashMap<String, Object> row = new HashMap<>();
            for (int i = 0; i < columnNames.length; i++) {
                row.put(columnNames[i], queryResult[i]);
            }
            addRow(rowIndex, row);
        } else {
            throw new DataException("Mismatch between data and column names, or invalid input.");
        }
        return this;
    }

    /**
     * Adds a new row of data to the end of this DataModel instance based on the attributes found in the provided {@code HttpSession}.
     * 
     * <p>This method extracts each attribute's name and value from the {@code HttpSession} and maps them as key-value pairs in a new row,
     * which is then appended to the end of the DataModel. The method delegates the addition of the row to 
     * {@link #addRow(int, HttpSession)}, using the current size of the rows as the index.</p>
     * 
     * @param session An HttpSession instance containing attributes that should be added as a new row.
     * @return The current DataModel instance with the added row at the end.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * // Simulating setting attributes in an HttpSession
     * HttpSession session = request.getSession();
     * session.setAttribute("username", "JohnDoe");
     * session.setAttribute("lastLogin", new Date());
     * 
     * // Create a new DataModel and add the session's attributes as a row at the end
     * DataModel dataModel = new DataModel();
     * dataModel.addRow(session);
     * }
     * </pre>
     */
    public DataModel addRow(HttpSession session) {
        return addRow(rows.size(), session);
    }

    /**
     * Adds a new row of data at the specified index in this DataModel instance based on the attributes found in the provided {@code HttpSession}.
     * 
     * <p>This method extracts each attribute's name and value from the {@code HttpSession} and maps them as key-value pairs in a new row.
     * The row is then inserted at the specified index in the DataModel.</p>
     * 
     * @param rowIndex The index at which the new row should be inserted.
     * @param session An HttpSession instance containing attributes that should be added as a new row.
     * @return The current DataModel instance with the added row at the specified index.
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * // Simulating setting attributes in an HttpSession
     * HttpSession session = request.getSession();
     * session.setAttribute("username", "JohnDoe");
     * session.setAttribute("lastLogin", new Date());
     * 
     * // Create a new DataModel and add the session's attributes as a row at a specified index
     * DataModel dataModel = new DataModel();
     * int index = 0;
     * dataModel.addRow(index, session);
     * }
     * </pre>
     */
    public DataModel addRow(int rowIndex, HttpSession session) {
        Enumeration<String> attributeNames = session.getAttributeNames();
        HashMap<String, Object> row = new HashMap<String, Object>();

        while(attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);

            row.put(attributeName, attributeValue);
        }
        return addRow(rowIndex, row);
    }
        
    /**
     * Adds a new row to the DataModel at the end using an entity object. The entity is first converted to a Map representation,
     * with each field of the entity corresponding to a column in the DataModel. This conversion process utilizes
     * a customized {@link ObjectMapper} obtained from {@link DataConverter} getObjectMapperForConvertEntitiesToDataModel(),
     * allowing for user-defined data conversion strategies.
     *
     * <p>This method delegates the addition of the new row to {@link #addRow(int, Object)}, using the current size of the rows
     * as the index. It ensures that the entity's data is accurately represented within the DataModel's tabular structure at the end.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * {@code
     * DataModel dataModel = new DataModel();
     * MyEntity entity = new MyEntity();
     * entity.setField1("value1");
     * entity.setField2(123);
     * 
     * // Add the entity's data as a new row at the end of the DataModel
     * dataModel.addRow(entity);
     * // The DataModel now contains the entity's data as the last row, 
     * // with "value1" in the field1 column and 123 in the field2 column.
     * }
     * </pre>
     *
     * @param entity An object representing the data to be added as a new row in the DataModel
     * @return the current instance of DataModel, with the new row added at the end
     * @throws DataException if the provided object is null, not an entity, or if there's an error during the conversion process
     */
    public DataModel addRow(Object entity){
        return addRow(rows.size(), entity);
    }

    /**
     * Adds a new row to the DataModel at the specified index using an entity object. The entity is first converted to a Map representation,
     * with each field of the entity corresponding to a column in the DataModel. This conversion process utilizes
     * a customized {@link ObjectMapper} obtained from {@link DataConverter} getObjectMapperForConvertEntitiesToDataModel(),
     * allowing for user-defined data conversion strategies.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Validates that the provided entity is not null.</li>
     *     <li>Converts the entity to a Map using {@link #getConvertedEntitiesToMaps(List)}, which handles the conversion
     *         of each entity field to the corresponding DataModel column value.</li>
     *     <li>Adds the converted Map as a new row at the specified index in the DataModel.</li>
     * </ul>
     *
     * <p>This method is ideal for adding rows to the DataModel at a specific index when the data source is an entity object.
     * It ensures that the entity's data is accurately represented within the DataModel's tabular structure at the desired position.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * MyEntity entity = new MyEntity();
     * entity.setField1("value1");
     * entity.setField2(123);
     * int index = 1;
     * dataModel.addRow(index, entity);
     * // The DataModel now contains the entity's data as a row at the specified index, 
     * // with "value1" in the field1 column and 123 in the field2 column.
     * </pre>
     *
     * @param rowIndex The index at which the new row should be inserted.
     * @param entity An object representing the data to be added as a new row in the DataModel
     * @return the current instance of DataModel, with the new row added at the specified index
     * @throws DataException if the provided object is null, not an entity, or if there's an error during the conversion process
     */
    public DataModel addRow(int rowIndex, Object entity){
        if(entity == null) {
            throw new DataException("You can not insert null.");
        }
        List<Object> entities = new ArrayList<Object>();
        entities.add(entity);

        List<Map<String, Object>> map = getConvertedEntitiesToMaps(entities);
        return addRow(rowIndex, map.get(0));
    }

    /**
     * Adds multiple rows to the DataModel from a list of entities, maps, or tuples. This method intelligently 
     * determines the type of the items in the list and processes them accordingly. It supports direct addition 
     * of maps and tuples, as well as conversion of entity objects to DataModel rows.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Checks if the provided list is not null and not empty.</li>
     *     <li>Determines the type of the first item in the list and applies the appropriate conversion logic for each type:</li>
     *     <li>If the items are Maps, each map is added as a new row.</li>
     *     <li>If the items are Tuples, each tuple is converted to a Map and then added as a new row.</li>
     *     <li>If the items are entities, they are first converted to Maps using {@link #getConvertedEntitiesToMaps(List)}
     *         and then added as rows.</li>
     * </ul>
     *
     * <p>This method provides a flexible way to add multiple rows of various types to the DataModel, making it versatile for different data sources.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * List&lt;MyEntity&gt; entityList = Arrays.asList(new MyEntity("value1", 123), new MyEntity("value2", 456));
     * dataModel.addRows(entityList);
     * // The DataModel now contains rows corresponding to the entities in the list.
     * </pre>
     *
     * @param <T> the type of the elements in the newRows list
     * @param newRows a list of entities, maps, or tuples representing the rows to be added
     * @return the current instance of DataModel, with the new rows added
     * @throws DataException if any error occurs during the conversion or addition process
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    @SuppressWarnings("unchecked")
    public <T> DataModel addRows(List<T> newRows) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }

        if (newRows != null && !newRows.isEmpty() && newRows.get(0) != null) {
            Object first = newRows.get(0);
            
            if (first instanceof Map) {
                for (T hm : newRows) {
                    addRow((Map<String, Object>) hm);
                }
            }else if (first instanceof Object){
                List<Map<String, Object>> maps = getConvertedEntitiesToMaps((List<Object>) newRows);
                addRows(maps);
            }
        }
    
        return this;
    }

    /**
     * Adds a new row or multiple rows to the DataModel from a {@link JsonNode}. This method can handle both
     * single JSON objects and arrays of JSON objects. Each JSON object is converted into a DataModel row.
     * The conversion process utilizes {@link DataConverter#getConvertJsonValueNodeToDataModelRowValue(JsonNode)}
     * for appropriate value formatting, allowing for customization of the conversion logic.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>If the JsonNode is an object, it is directly converted to a DataModel row using {@link #parseJsonObjectToDataModel(JsonNode)}.</li>
     *     <li>If the JsonNode is an array, each element is converted to a separate DataModel row.</li>
     *     <li>Uses a customized {@link DataConverter} for converting JSON values to DataModel row values, enabling user-defined conversion strategies.</li>
     * </ul>
     *
     * <p>This method is essential for creating a DataModel from JSON data, particularly when the JSON structure is dynamic or complex.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * String json = "{ 'column1': 'value1', 'column2': 100 }";
     * JsonNode jsonNode = new ObjectMapper().readTree(json);
     * dataModel.addRow(jsonNode);
     * // The DataModel now contains a row with 'value1' in 'column1' and 100 in 'column2'.
     * </pre>
     *
     * @param node a {@link JsonNode} representing either a single row (as a JSON object) or multiple rows (as a JSON array)
     * @return the current instance of DataModel, with the new row(s) added
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel addRows(JsonNode node) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * Adds rows of data to this dataModel instance based on the data found in the provided {@code ResultSet}.
     * 
     * <p>This method fetches data from the ResultSet, which typically contains the result of a SQL query executed using JDBC, and transforms this data into rows in the dataModel format.</p>
     * 
     * @param rs A ResultSet instance containing data from a database query.
     * @return The current dataModel instance with the added rows.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
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
     *     // Create a new dataModel and add rows from the ResultSet
     *     dataModel dataModel = new dataModel();
     *     dataModel.addRow(rs);
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
    public DataModel addRows(ResultSet rs){
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * Adds multiple rows of data to this dataModel instance based on the provided list of query results and corresponding column names.
     * 
     * <p>This method is useful when fetching multiple records from a database using JPA, 
     * and you want to convert the results into a dataModel representation.</p>
     * 
     * <p><b>Notice:</b> The length of the <code>columnNames</code> array should match the number 
     * of columns in each object array within the <code>queryResults</code>. If there's a mismatch, 
     * it may lead to out of bounds exceptions or missing data.</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * 
     * MemberRepository extends JpaRepository&lt;Member, Long&gt;
     * &#64;Query("SELECT m.id, m.deptcode, m.email, m.membername, m.regdate FROM Member m")
     * List&lt;Object[]&gt; findAllMember();
     * 
     * List&lt;Object[]&gt; results = memberRepository.findAllMember();
     * String[] columnNames = {"id", "deptcode", "email", "membername", "regdate"};
     * dataModel dataModel = new dataModel();
     * dataModel.addRows(results, columnNames);
     * </pre>
     *
     * @param queryResults A list of object arrays, each representing a row of data fetched from the database.
     * @param columnNames An array of strings representing the names of the columns for the fetched data.
     * @return The current dataModel instance with the added rows.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel addRows(List<Object[]> queryResults, String[] columnNames) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        for(Object[] result : queryResults) {
            addRow(result, columnNames);
        }
        return this;
    }
    
    /**
     * Retrieves the count of columns currently in this dataModel instance.
     *
     * <p>This method provides a quick way to determine how many columns are present 
     * in the current data model, which can be useful in various data processing scenarios.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Add rows or columns to the data model]
     * int count = dataModel.getColumnCount();
     * System.out.println("Number of columns: " + count);
     * </pre>
     *
     * @return The number of columns currently present in this dataModel instance.
     */
    public int getColumnCount() {
        return cols.size();
    }

    /**
     * Retrieves the count of rows currently in this dataModel instance.
     *
     * <p>This method provides a convenient way to determine the number of rows present 
     * in the current data model. Such information can be helpful in various data processing 
     * and analysis scenarios.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Add rows to the data model]
     * int count = dataModel.getRowCount();
     * System.out.println("Number of rows: " + count);
     * </pre>
     *
     * @return The number of rows currently present in this dataModel instance.
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Retrieves the list of columns currently in this dataModel instance.
     *
     * <p>This method offers a convenient means to access the column names stored 
     * in the current data model. It returns a new list to ensure the original column 
     * data remains unmodified.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Define columns for the data model]
     * List&lt;String&gt; columns = dataModel.getColumns();
     * System.out.println("Columns: " + columns);
     * </pre>
     *
     * @return A new list containing the names of columns present in this dataModel instance.
     */
    public List<String> getColumns() {
        return (List<String>) new ArrayList<String>(cols);
    }

    /**
     * Retrieves the set of columns currently in this dataModel instance.
     *
     * <p>This method is particularly useful when users wish to ensure uniqueness in column names. 
     * It returns a new set to prevent any unintended modifications to the original column data.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Define columns for the data model]
     * Set&lt;String&gt; columnsSet = dataModel.getColumnsWithSet();
     * System.out.println("Unique Columns: " + columnsSet);
     * </pre>
     *
     * @return A new set containing the names of columns present in this dataModel instance.
     */
    public Set<String> getColumnsWithSet() {
        return new LinkedHashSet<>(cols);
    }

    /**
     * Retrieves all values associated with the specified column in this dataModel instance.
     *
     * <p>This method allows users to fetch all the values for a given column name. 
     * If the specified column doesn't exist in the data model, an empty list will be returned.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with rows of data]
     * List&lt;Object&gt; ageValues = dataModel.getColumnValues("age");
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
     * Checks if the specified column exists in this dataModel instance.
     *
     * <p>This method can be used to verify the presence of a specific column 
     * in the data model before performing operations on it.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with rows of data]
     * HashMap&lt;String, Object&gt; row = dataModel.getRow(0);
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
     * Retrieves a specific row as a {@code dataModel} based on the given row index.
     * 
     * <p><b>Note:</b> The row index starts from 0.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with rows of data]
     * dataModel singleRowModel = dataModel.getRowAsDataModel(0);
     * System.out.println(singleRowModel.getColumnValues("columnName"));
     * </pre>
     *
     * @param rowIndex The index of the row to be retrieved.
     * @return The row at the specified index wrapped in a new {@code dataModel} instance.
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with rows of data]
     * List&lt;HashMap&lt;String, Object&gt;&gt; allRows = dataModel.getRows();
     * for (HashMap&lt;String, Object&gt; row : allRows) {
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
     * dataModel dataModel = new dataModel();
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
     * Sets or updates the value of a specific cell in the DataModel. The cell is identified by its row index and column name.
     * The value is processed using {@link DataConverter#getConvertValueToDataModelRowValue(Object)} to ensure it is in the appropriate format.
     *
     * <p>Key Aspects:</p>
     * <ul>
     *     <li>Validates the rowIndex to ensure it falls within the current range of rows in the DataModel.</li>
     *     <li>Checks if the specified column exists. If not, throws a {@link DataException}.</li>
     *     <li>Converts the value to the appropriate format for the DataModel using the configured DataConverter.</li>
     *     <li>Ensures type consistency within the column. If the new value's type does not match the existing data types in the column, a {@link DataException} is thrown.</li>
     * </ul>
     *
     * <p>This method is crucial for modifying the contents of the DataModel on a cell-by-cell basis, allowing for precise control over the data contained within.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * DataModel dataModel = new DataModel();
     * // ... [Populate the data model with rows of data]
     * dataModel.setValue(2, "columnName", "New Value");  // Setting a new value in the third row and a column named "columnName"
     * Object updatedValue = dataModel.getValue(2, "columnName");  // Retrieving the updated value from the same cell
     * System.out.println("Updated value in rowIndex 2 and column 'columnName': " + updatedValue);
     * </pre>
     *
     * @param rowIndex the index of the row in which the value is to be set
     * @param column the name of the column in which the value is to be set
     * @param value the new value to set in the specified cell
     * @return the current instance of DataModel, with the updated value in the specified cell
     * @throws DataException if the column does not exist or if there's a type mismatch in the column
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setValue(int rowIndex, String column, Object value) {
        checkRowsRange(rowIndex);
        if (!hasColumn(column)) {
            throw new DataException("Column does not exist.");
        }
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }

        value = getConverter().getConvertValueToDataModelRowValue(value);

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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with rows of data]
     * HashMap&lt;String, Object&gt; removedRow = dataModel.removeRow(2); // Removes and returns the third row from the data model
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public HashMap<String, Object> removeRow(int rowIndex) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
        checkRowsRange(rowIndex);
        return rows.remove(rowIndex);
    }

    /**
     * Removes the specified column from the DataModel.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * dataModel dataModel = new dataModel();
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
     * @return The current dataModel instance with the column removed.
     * @throws DataException if the specified column does not exist in the DataModel.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel removeColumn(String column) {
        if (!hasColumn(column)) {
            throw new DataException("Column does not exist.");
        }
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
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
     * dataModel dataModel = new dataModel();
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
     * @return The current dataModel instance with the specified columns removed.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel removeColumns(String... columns) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @return The current dataModel instance with the specified columns removed.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel removeColumns(List<String> columns) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @return The current dataModel instance with the specified columns removed.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel removeColumns(Set<String> columns) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * dataModel dataModel = new dataModel();
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
     * @return The current dataModel instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel setValidColumns(String... columns) {
        Set<String> columnSet = new HashSet<>(Arrays.asList(columns));
        return setValidColumns(columnSet);
    }

    /**
     * Retains only the specified columns in the DataModel, removing all others.
     * 
     * @param columns The list of column names to be retained.
     * @return The current dataModel instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     */
    public DataModel setValidColumns(List<String> columns) {
        Set<String> columnSet = new HashSet<>(columns);
        return setValidColumns(columnSet);
    }

    /**
     * Retains only the specified columns in the DataModel, removing all others.
     * 
     * @param columns The set of column names to be retained.
     * @return The current dataModel instance retaining only the specified columns.
     * @throws DataException if any of the specified columns do not exist in the DataModel.
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel setValidColumns(Set<String> columns) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with columns and rows of data]
     * HashMap&lt;String, Object&gt; firstNullRow = dataModel.findFirstRowNullColumn("age");  
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with columns and rows of data]
     * HashMap&lt;String, Object&gt; firstDuplRow = dataModel.findFirstRowDuplColumn("name");  
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel matchedDataModel = dataModel.searchRowsAsDataModel(cond1, cond2);
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
     * dataModel dataModel = new dataModel();
     * // ... [Populate the data model with columns and rows of data]
     * dataModel matchedDataModel = dataModel.searchRowsAsDataModel(true, cond1, cond2);
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
     * dataModel dataModel = new dataModel();
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
     * dataModel dataModel = new dataModel();
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException if a column from the conditions does not exist in the DataModel.
     */
    public DataModel searchAndModify(Boolean bool, Condition... conditions) {
        if(freezeValues) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * Predicate&lt;HashMap&lt;String, Object&gt;&gt; ageFilter = row -&gt; {
     *     if (row.containsKey("age") &amp;&amp; row.get("age") instanceof Number) {
     *         return ((Number) row.get("age")).intValue() &gt;= 30;
     *     }
     *     return false;
     * };
     * List&lt;Integer&gt; matchedIndexes = originalDm.filterRowIndexes(ageFilter);
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
     * Predicate&lt;HashMap&lt;String, Object&gt;&gt; nameFilter = row -&gt; "John".equals(row.get("name"));
     * List&lt;HashMap&lt;String, Object&gt;&gt; matchedRows = originalDm.filterRows(nameFilter);
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
     * Predicate&lt;HashMap&lt;String, Object&gt;&gt; ageFilter = row -&gt; {
     *     if (row.containsKey("age") &amp;&amp; row.get("age") instanceof Number) {
     *         return ((Number) row.get("age")).intValue() &gt;= 30;
     *     }
     *     return false;
     * };
     * dataModel filteredDm = originalDm.filterRowsAsDataModel(ageFilter);
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
     * Predicate&lt;HashMap&lt;String, Object&gt;&gt; nameFilter = row -&gt; "John".equals(row.get("name"));
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel sortColumnAscending() {
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel sortColumnDescending() {
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     */
    public DataModel sortColumnReverse() {
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException If the specified column does not exist, or if mixed or unsupported types are encountered.
     */
    public DataModel sortRowAscending(String column, Boolean isIntegerOrder) {
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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
     * @throws DataException An error occurs if changes cannot be made through setFreeze.
     * @throws DataException If the specified column does not exist, or if mixed or unsupported types are encountered.
     */
    public DataModel sortRowDescending(String column, Boolean isIntegerOrder) {
        if(freeze) {
            throw new DataException("This DataModel is frozen and cannot be modified.");
        }
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

    /**
     * Checks if the DataModel is in a frozen state.
     * 
     * @return {@code true} if the DataModel is frozen and cannot be modified; {@code false} otherwise.
     */
    public boolean isFreeze() {
        return freeze;
    }

    /**
     * Checks if the values within the DataModel are frozen.
     * 
     * @return {@code true} if the values within the DataModel are frozen and cannot be modified; {@code false} otherwise.
     */
    public boolean isFreezeValues() {
        return freezeValues;
    }

    /**
     * Sets the DataModel to a frozen state, preventing any modifications to its structure and content.
     * Once set, the DataModel cannot be unfrozen and remains immutable.
     * 
     * @return The current instance of the DataModel for chaining methods.
     */
    public DataModel setFreeze() {
        freezeValues = true;
        freeze = true;
        return this;
    }

    /**
     * Sets the values within the DataModel to a frozen state, preventing any modifications to the content.
     * The structure (like the order and presence of columns and rows) can still be modified.
     * 
     * @return The current instance of the DataModel for chaining methods.
     */
    public DataModel setFreezeValues() {
        freezeValues = true;
        return this;
    }
}
