package io.github.hison.data.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import io.github.hison.data.exception.DataException;
import io.github.hison.data.model.DataModel;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * Default implementation of the {@link DataConverter} interface, providing standard methods for 
 * converting between JSON data, Entity class data, and the DataModel. This class serves as the 
 * backbone for data conversion within the DataModel framework, enabling seamless data transformation 
 * and formatting across different data representations.
 *
 * <p>Key functionalities:</p>
 * <ul>
 *     <li>Converts JSON nodes to DataModel row values, handling date-time conversions from ISO format 
 *         to 'yyyy-MM-dd HH:mm:ss' format.</li>
 *     <li>Determines if an object is a valid entity based on its annotation.</li>
 *     <li>Converts a DataModel instance to JSON representation, and vice versa.</li>
 *     <li>Facilitates conversion of DataModel rows to a list of entity instances of a specified class.</li>
 *     <li>Converts various data types to values suitable for inclusion in a DataModel row.</li>
 * </ul>
 *
 * <p>Customization:</p>
 * Developers using the DataModel.jar can customize the data conversion process by extending this class. 
 * Custom converters can be defined and injected as beans in the Spring context, allowing for tailored 
 * data handling strategies that fit specific application requirements.
 *
 * <pre>
 * &#64;Configuration
 * public class CustomDataConverterConfig {
 *     &#64;Bean
 *     &#64;Primary
 *     public DataConverter customDataConverter() {
 *         return new CustomDataConverter();
 *     }
 * }
 *
 * public class CustomDataConverter extends DataConverterDefault {
 *     // Custom logic...
 * }
 * </pre>
 *
 * This approach provides flexibility and extensibility in data conversion within the application, 
 * enhancing the adaptability of the DataModel to various data processing scenarios.
 */
public class DataConverterDefault implements DataConverter{
    /**
     * Determines whether the given object is an entity by checking for the presence of the {@link Entity} annotation.
     * This method is used in the context of the DataModel to verify if an object conforms to the entity structure
     * expected within the data conversion process.
     *
     * <p>Usage:</p>
     * This method is typically invoked before converting an object to a DataModel row, ensuring that the object
     * is a valid entity. It is an integral part of the validation process in methods like 
     * {@link DataModel} getConvertedEntitiesToMaps(List) to ascertain that only entities are processed for conversion.
     *
     * @param obj the object to be checked for entity status
     * @return true if the object is an entity (i.e., annotated with {@link Entity}), false otherwise
     */
    @Override
    public boolean isEntity(Object obj) {
        return obj != null && obj.getClass().isAnnotationPresent(Entity.class);
    }

    /**
     * Converts a JsonNode value to a string representation suitable for a DataModel row. 
     * This method specifically handles date-time values formatted in ISO-8601 format 
     * and converts them to a standard date-time format as specified by {@link #getDateFormat()}.
     * Non-date-time values are returned as their textual representation.
     * 
     * <p>For date-time strings, the method:</p>
     * <ul>
     *     <li>Checks if the value is in ISO-8601 format.</li>
     *     <li>Parses the ISO-8601 date-time string to a {@link ZonedDateTime} object. and get here {@link #getTimeZoneId()}</li>
     *     <li>Formats the date-time to the standard format specified by {@link #getDateFormat()}.</li>
     * </ul>
     *
     * <p>If the JsonNode is not a date-time string, it returns the text value of the node.</p>
     *
     * @param valueNode the JsonNode to be converted
     * @return a string representation of the JsonNode value, formatted for inclusion in a DataModel row.
     *         Date-time values are formatted according to {@link #getDateFormat()}, while other types are returned as-is.
     */
    @Override
    public String getConvertJsonValueNodeToDataModelRowValue(JsonNode valueNode) {
        if (valueNode.isTextual()) {
            String text = valueNode.asText();
            // Check for ISO date-time format
            if (text.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")) {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                String timeZoneId = getTimeZoneId();
                if (timeZoneId != null && !timeZoneId.isEmpty()) {
                    zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(timeZoneId));
                }
                return zonedDateTime.format(DateTimeFormatter.ofPattern(getDateFormat()));
            }
            return text;
        } else {
            return valueNode.asText();
        }
    }

    /**
     * Converts the rows of a DataModel object into a JSON array using a customized {@link ObjectMapper}. 
     * This method focuses on transforming the DataModel's row data into a JSON array representation, 
     * ideal for situations where only row data is required in JSON format.
     *
     * <p>Functionality:</p>
     * <ul>
     *     <li>Uses a custom ObjectMapper, configured through {@link #getObjectMapperForConvertDataModelToJson()},
     *         to serialize only the DataModel's rows into a JSON array.</li>
     *     <li>Ensures that special data types like {@link LocalDateTime} are correctly serialized according 
     *         to the format specified by {@link #getDateFormat()}.</li>
     *     <li>Handles potential exceptions during the conversion process, encapsulating them in a {@link DataException}.</li>
     * </ul>
     *
     * @param dm the DataModel instance whose rows are to be converted to a JSON array
     * @return a {@link JsonNode} representing the rows of the DataModel in JSON array format
     * @throws DataException if any issues occur during the conversion process
     */
    @Override
    public JsonNode getConvertedJson(DataModel dm) {
        ObjectMapper mapper = getObjectMapperForConvertDataModelToJson();
        try {
            return mapper.valueToTree(dm.getRows());
        } catch (Exception e) {
            throw new DataException("Failed to convert DataModel to Json in getConvertedJson", e);
        }
    }
    
    /**
     * Configures and returns a customized {@link ObjectMapper} for serializing DataModel objects to JSON.
     * This method specifically sets up the ObjectMapper with necessary modules and configurations to handle
     * custom data types such as {@link LocalDateTime}, ensuring accurate and efficient serialization of data.
     *
     * <p>Key Configurations:</p>
     * <ul>
     *     <li>Registers the {@link JavaTimeModule} to handle Java 8 date-time types, particularly customizing 
     *         the deserialization and serialization of {@link LocalDateTime} to the format specified by {@link #getDateFormat()}.</li>
     *     <li>Disables {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} to ignore unknown properties 
     *         during deserialization, providing flexibility in JSON data structure handling.</li>
     *     <li>Configures {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} to false to prevent issues with 
     *         self-referential objects.</li>
     * </ul>
     *
     * <p>This configured ObjectMapper is used throughout the DataModel's JSON conversion processes, ensuring 
     * consistent handling of date-time formats and unknown properties.</p>
     *
     * @return a configured {@link ObjectMapper} instance for DataModel to JSON serialization
     */
    @Override
    public ObjectMapper getObjectMapperForConvertDataModelToJson() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(getDateFormat())));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(getDateFormat())));
        mapper.registerModule(javaTimeModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        return mapper;
    }

    /**
     * Converts the rows of a DataModel into a list of entities of a specified class. This method utilizes 
     * a customized {@link ObjectMapper} configured by {@link #getObjectMapperForConvertDataModelToEntities()} 
     * for the conversion process, ensuring that the data types and formats in the DataModel are accurately 
     * mapped to the corresponding entity fields.
     *
     * <p>Process:</p>
     * <ul>
     *     <li>Uses a custom ObjectMapper to handle the deserialization of DataModel rows into entity objects.</li>
     *     <li>Supports conversion of complex data structures represented in the DataModel rows into corresponding 
     *         entity objects.</li>
     *     <li>Utilizes generic types to provide flexibility in converting to various entity classes.</li>
     * </ul>
     *
     * <p>This method is especially useful in scenarios where the DataModel's structured data needs to be 
     * transformed into entity instances for business logic processing or data manipulation.</p>
     *
     * @param <T> the class type of the entities to be created
     * @param entityClass the class of the entities to which the rows are to be converted
     * @param dm the DataModel containing the rows to be converted
     * @return a list of entities of the specified class, each corresponding to a row in the DataModel
     * @throws DataException if any error occurs during the conversion process
     */
    @Override
    public <T> List<T> getConvertedEntities(Class<T> entityClass, DataModel dm) {
        ObjectMapper mapper = getObjectMapperForConvertDataModelToEntities();
        try {
            return mapper.convertValue(dm.getRows(), mapper.getTypeFactory().constructCollectionType(List.class, entityClass));
        } catch (Exception e) {
            throw new DataException("Failed to convert rows to entities", e);
        }
    }

    /**
     * Provides a customized {@link ObjectMapper} for converting DataModel entities to and from JSON. 
     * This ObjectMapper is configured with specific modules and settings to accurately map JSON data 
     * to Java objects, particularly handling special data types like {@link LocalDateTime}.
     *
     * <p>Configurations:</p>
     * <ul>
     *     <li>Includes the {@link JavaTimeModule} to properly deserialize Java 8 date-time types.</li>
     *     <li>Configures the deserialization and serialization of {@link LocalDateTime} to a specific format from getDateFormat().</li>
     *     <li>Disables {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} to prevent errors 
     *         on encountering unknown JSON properties, providing robustness in JSON parsing.</li>
     *     <li>Sets {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} to false to avoid serialization 
     *         issues with self-referential objects.</li>
     * </ul>
     *
     * <p>This configured ObjectMapper is crucial for transforming JSON data contained in DataModel rows 
     * into corresponding Java entity objects, supporting the flexibility and complexity required in data conversion.</p>
     *
     * @return a configured {@link ObjectMapper} for DataModel entity conversion
     */
    @Override
    public ObjectMapper getObjectMapperForConvertDataModelToEntities() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(getDateFormat())));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(getDateFormat())));
        mapper.registerModule(javaTimeModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        return mapper;
    }

    /**
     * Configures and provides an {@link ObjectMapper} tailored for converting entity objects to DataModel format.
     * This ObjectMapper is specially set up to handle Java objects, especially custom data types like {@link LocalDateTime},
     * and map them accurately to a compatible format for DataModel rows.
     *
     * <p>Configurations:</p>
     * <ul>
     *     <li>Incorporates the {@link JavaTimeModule} to facilitate the serialization of Java 8 date-time types, 
     *         particularly customizing the deserializer and serialization of {@link LocalDateTime} to the format specified by 
     *         {@link #getDateFormat()}.</li>
     *     <li>Disables {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} to allow more flexibility in dealing 
     *         with varying JSON structures without causing errors during deserialization.</li>
     *     <li>Sets {@link SerializationFeature#FAIL_ON_SELF_REFERENCES} to false to prevent serialization issues 
     *         with self-referential objects.</li>
     * </ul>
     *
     * <p>This method is used in the conversion process where entity objects need to be transformed into a format 
     * suitable for inclusion in DataModel rows, ensuring that data is properly serialized and deserialized 
     * according to the defined configurations.</p>
     *
     * @return a configured {@link ObjectMapper} for converting entities to DataModel format
     */
    @Override
    public ObjectMapper getObjectMapperForConvertEntitiesToDataModel() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(getDateFormat())));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(getDateFormat())));
        mapper.registerModule(javaTimeModule);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        return mapper;
    }

    /**
     * Converts an object value to a format suitable for inclusion in a DataModel row. This method handles various data types,
     * including special handling for {@link LocalDateTime}, and ensures that each value is formatted appropriately for the DataModel.
     *
     * <p>Conversion Logic:</p>
     * <ul>
     *     <li>Null values are returned as null.</li>
     *     <li>String values are returned as is.</li>
     *     <li>{@link LocalDateTime} values are formatted to a string using the pattern defined by {@link #getDateFormat()}.</li>
     *     <li>Primitive wrapper types (Boolean, Character, Byte, Short, Integer, Long, Float, Double) are converted to their string representation.</li>
     *     <li>Other object types are returned as is, without conversion.</li>
     * </ul>
     *
     * <p>This method is primarily used within the DataModel class to ensure that values being added to a DataModel row are in a consistent format.
     * It plays a crucial role in the {@link DataModel#addRow(Map)} and {@link DataModel#setValue(int, String, Object)} methods to maintain data integrity and format consistency.</p>
     *
     * @param value the object value to be converted
     * @return the converted value, formatted for inclusion in a DataModel row
     */
    @Override
    public Object getConvertValueToDataModelRowValue(Object value) {
        if (value == null) {
            return null;
        }
        if(value.getClass() == String.class) {
            return value;
        }
        else if (value.getClass() == LocalDateTime.class) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getDateFormat());
            return ((LocalDateTime)value).format(formatter);
        }
        List<Class<?>> primitiveWrappers = Arrays.asList(
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class, 
            Integer.class,
            Long.class,
            Float.class,
            Double.class
        );
        if (primitiveWrappers.contains(value.getClass())) {
            return value.toString();
        } else {
            System.out.println(value.toString());
            throw new DataException("This is an invalid type. : " + value.getClass().getName());
        }
    }

    /**
     * Retrieves the configured time zone ID used for date-time conversions within the DataModel. 
     * The default return value is null, which indicates the use of the system's default time zone. 
     * However, this method can be customized to return a specific time zone ID, such as "Asia/Seoul".
     *
     * <p>Usage:</p>
     * This method is used to define the time zone context for date-time conversions, particularly when formatting 
     * date-time values to or from {@link LocalDateTime} objects within the DataModel.
     *
     * <p>Customization:</p>
     * By default, the method returns null. If a specific time zone is required (e.g., "Asia/Seoul"), 
     * the method can be overridden to return the desired time zone ID.
     *
     * @return the configured time zone ID as a string, or null if the system's default time zone is to be used
     */
    public String getTimeZoneId() {
        return null;
    }

    /**
     * Provides the date format pattern used for formatting date-time values in the DataModel. 
     * This format is applied when converting {@link LocalDateTime} objects to strings and vice versa.
     *
     * <p>Usage:</p>
     * The returned format pattern is utilized in various parts of the DataModel conversion process, ensuring 
     * consistent formatting of date-time values across the application.
     *
     * <p>Example Format:</p>
     * The default format pattern is "yyyy-MM-dd HH:mm:ss", which can be customized as needed.
     *
     * @return the date format pattern as a string
     */
    @Override
    public String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }
}