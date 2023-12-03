package io.github.hison.data.converter;

/*
 * DataConverterProvider to inject DataConverter into DataModel
 */
public class DataConverterProvider {

    private static DataConverter getConverterBean() {
        return ApplicationContextProvider.getApplicationContext().getBean(DataConverter.class);
    }

    private final static DataConverter converter = getConverterBean();

    public static DataConverter getConverter() {
        return converter;
    }
}