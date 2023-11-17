package com.test.boot02.common.data;

import java.util.List;
import java.util.Set;

import javax.persistence.Tuple;

public class DataModel extends DataModelBase{
    public DataModel() {
        super();
    }

    public DataModel(String... newColumns) {
        super(newColumns);
    }

    public DataModel(Set<String> newColumns) {
        super(newColumns);
    }

    public <T> DataModel(List<T> newRows) {
        super(newRows);
    }

    public DataModel(Tuple tuple) {
        super(tuple);
    }

    public DataModel(Object[] queryResult, String[] columnNames) {
        super(queryResult, columnNames);
    }

    public DataModel(List<Object[]> queryResults, String[] columnNames) {
        super(queryResults, columnNames);
    }

    @Override
    protected String getDateFormatEntityToDataModel() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected String[] getDateFormatsDataModelToEntity() {
        String[] dateFormats = {
            "yyyy-MM-dd HH:mm:ss"
        };
        return dateFormats;
    }
}
