package com.test.boot02.common.data;

public abstract class DataModelConverter{
    protected abstract Object convertEntityToDataModel(Object value);

    protected abstract Object convertDataModelToEntity(Object value, Class<?> targetType);

    protected abstract Object convertDataModelToEntityValueIsNull(Object value, Class<?> targetType);

    protected abstract Object convertDataModelToEntityValueIsNumber(Object value, Class<?> targetType);
    
    protected abstract Object convertDataModelToEntityValueIsBoolean(Object value, Class<?> targetType);
    
    protected abstract Object convertDataModelToEntityValueIsString(Object value, Class<?> targetType);
    
    protected abstract boolean isEntity(Object obj);
    
    protected abstract String getDateFormatEntityToDataModel();
    
    protected abstract String[] getDateFormatsDataModelToEntity();
}
