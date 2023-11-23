package io.github.hison.data;

/*
 * DataConverterProvider to inject DataConverter into DataModel
 */
public class DataConverterProvider {
    
    private static DataConverter getConverterBean() {
        return ApplicationContextProvider.getApplicationContext().getBean(DataConverter.class);
    }

    private final static DataConverter converter = getConverterBean();

    protected static DataConverter getConverter() {
        return converter;
    }
}
