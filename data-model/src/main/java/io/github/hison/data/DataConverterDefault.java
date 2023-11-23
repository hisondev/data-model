package io.github.hison.data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;

public class DataConverterDefault implements DataConverter{
    /**
     * Converts provided values to a consistent format that can be stored in the DataModelBase.
     * 
     * <p>This method is used by <code>addRow()</code> and <code>setValue()</code> to ensure that the values added to the 
     * rows in the <code>DataModelBase</code> are of a consistent data type. By default, if a value is of a primitive wrapper type, 
     * it gets converted to a string format.</p>
     *
     * <p>For instance, if a value is of type <code>LocalDateTime</code>, it's formatted into a string based on the specified 
     * date format. Similarly, if it's any of the primitive wrapper types, such as <code>Integer</code> or <code>Boolean</code>, 
     * it's converted to its string representation.</p>
     *
     * <p><b>Note:</b></p>
     * <ul>
     * <li>If developers want to store values of a different type, they can extend this class and override this method. 
     * However, overriding this behavior is not recommended, as it's crucial to maintain consistency in data type within the 
     * <code>DataModelBase</code>.</li>
     * </ul>
     *
     * @param value The value to be converted.
     * @return The converted value that's suitable for storage in the DataModelBase.
     */
    @Override
    public Object convertEntityToDataModel(Object value) {
        if (value == null) {
            return null;
        }
        if(value.getClass() == String.class) {
            return value;
        }
        else if (value.getClass() == LocalDateTime.class) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getDateFormatEntityToDataModel());
            value = ((LocalDateTime)value).format(formatter);
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
            return value;
        }
    }

    /**
     * Converts a value from the DataModel to a target type suitable for the entity.
     * 
     * <p>This method facilitates the conversion of values from the DataModel format to types that can be stored 
     * in the entity. Depending on the type of the value, appropriate helper methods are called to perform 
     * the conversion. This method is utilized by the {@code getConvertEntities} method for the conversion process.</p>
     * 
     * <p>Developers can extend this class and override this method or its helper methods to customize the conversion 
     * behavior as per their application's requirements.</p>
     *
     * @param value The value from the DataModel to be converted.
     * @param targetType The type to which the value needs to be converted for the entity.
     * @return The converted value suitable for the entity.
     * @throws DataException if no valid conversion can be performed.
     */
    @Override
    public Object convertDataModelToEntity(Object value, Class<?> targetType){
        if (value == null) {
            return convertDataModelToEntityValueIsNull(value, targetType);
        }
        
        // If the target type is an instance of the value, no conversion is needed
        if (targetType.isInstance(value)) {
            return value;
        }
    
        // Number conversions
        if (value instanceof Number) {
            return convertDataModelToEntityValueIsNumber(value, targetType);
        }
    
        // Boolean conversions
        if (value instanceof Boolean && (targetType == boolean.class || targetType == Boolean.class)) {
            return convertDataModelToEntityValueIsBoolean(value, targetType);
        }
    
        // String conversions
        if (value instanceof String) {
            return convertDataModelToEntityValueIsString(value, targetType);
        }
    
        // Other type conversions...
    
        // If the code reached here, no valid conversion was found
        throw new DataException("Unsupported conversion: Cannot convert " + value.getClass().getName() + " to " + targetType.getName());
    }

    /**
     * Helper method to handle conversion when the value from DataModel is null.
     * 
     * <p>This method can be overridden to provide custom behavior for null values during conversion.</p>
     *
     * @param value The null value from the DataModel.
     * @param targetType The type to which the value would have been converted if it was non-null.
     * @return The converted value (usually null).
     */
    @Override
    public Object convertDataModelToEntityValueIsNull(Object value, Class<?> targetType) {
        return null;
    }

    /**
     * Helper method to handle conversion when the value from DataModel is a number.
     * 
     * <p>This method handles conversion to various number types like Integer, Double, Long, etc. 
     * It can be overridden to modify or expand the number conversion logic.</p>
     *
     * @param value The number value from the DataModel.
     * @param targetType The type to which the value needs to be converted.
     * @return The converted value.
     * @throws DataException if the conversion is not supported.
     */
    @Override
    public Object convertDataModelToEntityValueIsNumber(Object value, Class<?> targetType) {
        Number numberValue = (Number) value;
        // Integer types
        if (targetType == int.class || targetType == Integer.class) {
            return numberValue.intValue();
        } 
        // Double types
        else if (targetType == double.class || targetType == Double.class) {
            return numberValue.doubleValue();
        } 
        // Long types
        else if (targetType == long.class || targetType == Long.class) {
            return numberValue.longValue();
        } 
        // Float types
        else if (targetType == float.class || targetType == Float.class) {
            return numberValue.floatValue();
        } 
        // Short types
        else if (targetType == short.class || targetType == Short.class) {
            return numberValue.shortValue();
        } 
        // Byte types
        else if (targetType == byte.class || targetType == Byte.class) {
            return numberValue.byteValue();
        } 
        // BigInteger types
        else if (targetType == BigInteger.class) {
            return BigInteger.valueOf(numberValue.longValue());
        } 
        // BigDecimal types
        else if (targetType == BigDecimal.class) {
            // Constructing a BigDecimal from a double should be avoided due to rounding issues, using string instead
            return new BigDecimal(numberValue.toString());
        } 
        else {
            throw new DataException("Unsupported number field type: " + targetType);
        }
    }

    /**
     * Helper method to handle conversion when the value from DataModel is a boolean.
     * 
     * <p>This method can be overridden to provide custom conversion logic for boolean values.</p>
     *
     * @param value The boolean value from the DataModel.
     * @param targetType The type to which the value needs to be converted.
     * @return The converted value.
     */
    @Override
    public Object convertDataModelToEntityValueIsBoolean(Object value, Class<?> targetType) {
        return value;
    }

    /**
     * Helper method to handle conversion when the value from DataModel is a string.
     * 
     * <p>This method manages conversions from string to various other types like LocalDateTime, Integer, Double, etc. 
     * It can be overridden to adjust or extend the string conversion logic.</p>
     *
     * @param value The string value from the DataModel.
     * @param targetType The type to which the value needs to be converted.
     * @return The converted value.
     * @throws DataException if the conversion is not supported.
     */
    @Override
    public Object convertDataModelToEntityValueIsString(Object value, Class<?> targetType) {
        String stringValue = (String) value;
        // From String to LocalDateTime
        if (targetType == LocalDateTime.class) {
            // Common date and time formats
            String[] datePatterns = getDateFormatsDataModelToEntity();

            LocalDateTime dateTime = null;
            for (String pattern : datePatterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    dateTime = LocalDateTime.parse(stringValue, formatter);
                    break; // If parsing successful, exit loop
                } catch (DateTimeParseException e) {
                    // If parsing fails, try next pattern
                }
            }

            // If none of the patterns were successful in parsing the string
            if (dateTime == null) {
                throw new DataException("Failed to parse date time: " + stringValue);
            }
            return dateTime;
        }

        // From String to integer
        else if (targetType == int.class || targetType == Integer.class) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse integer: " + stringValue, e);
            }
        }
        // From String to long
        else if (targetType == long.class || targetType == Long.class) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse long: " + stringValue, e);
            }
        }
        // From String to double
        else if (targetType == double.class || targetType == Double.class) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse double: " + stringValue, e);
            }
        }
        // From String to float
        else if (targetType == float.class || targetType == Float.class) {
            try {
                return Float.parseFloat(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse float: " + stringValue, e);
            }
        }
        // From String to short
        else if (targetType == short.class || targetType == Short.class) {
            try {
                return Short.parseShort(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse short: " + stringValue, e);
            }
        }
        // From String to byte
        else if (targetType == byte.class || targetType == Byte.class) {
            try {
                return Byte.parseByte(stringValue);
            } catch (NumberFormatException e) {
                throw new DataException("Failed to parse byte: " + stringValue, e);
            }
        }
        throw new DataException("Unsupported conversion: Cannot convert " + value.getClass().getName() + " to " + targetType.getName());
    }

    /**
     * Determines if the provided object is an instance of a class annotated with the {@code @Entity} annotation.
     * 
     * <p>This method checks if the object is not null and if its class is annotated with the {@code @Entity} annotation. 
     * Typically, this is used to identify objects that represent database entities in JPA (Java Persistence API) or similar frameworks.</p>
     * 
     * <p>Developers can extend this class and override this method to customize the entity detection mechanism as per their application's requirements.</p>
     *
     * @param obj The object to be checked.
     * @return {@code true} if the object is an instance of a class annotated with {@code @Entity}, {@code false} otherwise.
     */
    @Override
    public boolean isEntity(Object obj) {
        return obj != null && obj.getClass().isAnnotationPresent(Entity.class);
    }

    /**
     * Provides the date format pattern to be used when converting an entity's {@link LocalDateTime} value to a string representation in the data model.
     * 
     * <p>This method is invoked by {@code convertEntityToDataModel} when it encounters a value of type {@link LocalDateTime}. The returned format pattern dictates how the date-time value will be represented as a string in the data model.</p>
     *
     * <p>Developers can extend this class and override this method to customize the date format as per their application's requirements.</p>
     *
     * @return The date format pattern string for converting {@link LocalDateTime} to string representation in the data model. Default is "yyyy-MM-dd HH:mm:ss".
     */
    @Override
    public String getDateFormatEntityToDataModel() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    /**
     * Provides an array of date format patterns to be used when attempting to convert a string representation in the data model to an entity's {@link LocalDateTime} value.
     * 
     * <p>This method is used by {@code convertDataModelToEntity} (specifically the {@code convertDataModelToEntityValueIsString} method) when the target type for conversion is {@link LocalDateTime}. The method attempts to parse the string using each pattern in the returned array, in order, until a successful conversion is achieved.</p>
     *
     * <p>Developers can extend this class and override this method to customize the array of date formats as per their application's requirements.</p>
     *
     * @return An array of date format pattern strings for attempting conversion of string values to {@link LocalDateTime}. 
     */
    @Override
    public String[] getDateFormatsDataModelToEntity() {
        String[] dateFormats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyyMMdd HHmmss",
                    "yyyyMMdd",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy/MM/dd",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
                };
        return dateFormats;
    }
}
