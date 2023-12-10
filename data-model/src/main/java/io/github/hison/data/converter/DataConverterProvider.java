package io.github.hison.data.converter;

public class DataConverterProvider {

    private static DataConverter getConverterBean() {
        return ApplicationContextProvider.getApplicationContext().getBean(DataConverter.class);
    }

    private final static DataConverter converter = getConverterBean();

    public static DataConverter getConverter() {
        return converter;
    }
}