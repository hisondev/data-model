package io.github.hison.data.converter;

/**
 * @author Hani son
 * @version 2.0.1
 */
public class DataConverterFactory {

    private static DataConverter customConverter;
    
    public static DataConverter getConverter() {
        if (customConverter != null) {
            return customConverter;
        }
        return new DataConverterDefault();
    }

    public static void setCustomConverter(DataConverter converter) {
        customConverter = converter;
    }
}