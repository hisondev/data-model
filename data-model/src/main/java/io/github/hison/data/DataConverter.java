package io.github.hison.data;

public interface DataConverter{
    Object convertEntityToDataModel(Object value);

    Object convertDataModelToEntity(Object value, Class<?> targetType);

    Object convertDataModelToEntityValueIsNull(Object value, Class<?> targetType);

    Object convertDataModelToEntityValueIsNumber(Object value, Class<?> targetType);
    
    Object convertDataModelToEntityValueIsBoolean(Object value, Class<?> targetType);
    
    Object convertDataModelToEntityValueIsString(Object value, Class<?> targetType);
    
    boolean isEntity(Object obj);
    
    String getDateFormatEntityToDataModel();
    
    String[] getDateFormatsDataModelToEntity();
}
