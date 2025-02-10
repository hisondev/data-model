# data-model [![Maven Central](https://img.shields.io/maven-central/v/io.github.hisondev/data-model.svg?label=Maven%20Central)](https://mvnrepository.com/artifact/io.github.hisondev/data-model)
A library designed to simplify data communication in Spring applications. Primarily featuring the DataWrapper and DataModel classes to streamline the process of data transfer and management.

## Introduction
The `data-model` library is designed to simplify data communication in Spring applications. It includes the `DataWrapper` and `DataModel` classes, which streamline the process of data transfer and management.

For enhanced and convenient front-end and server communication, this library can be used in conjunction with `dataModel.min.js` from [hison-js](https://github.com/hisondev/hison-js).

## Getting Started
To start using the `data-model` library in your project, follow the installation and usage instructions below.

### Prerequisites
Before you can use the `data-model` library, you need to have the following software installed on your system:
- Java Development Kit (JDK) 8 or higher
- Apache Maven (for building the project)

### Installation
You can add the `data-model` library to your project by including the following dependency in your Maven `pom.xml` file:

```xml
<dependency>
    <groupId>io.github.hisondev</groupId>
    <artifactId>data-model</artifactId>
    <version>1.0.7</version>
</dependency>
```

## Usage
### DataWrapper and DataModel Utilities
```java
import io.github.hisondev.datamodel.DataWrapper;
import io.github.hisondev.datamodel.DataModel;

// Example of using DataWrapper
DataWrapper wrapper = new DataWrapper();
wrapper.add("key1", "value1");

// Example of using DataModel
DataModel model = new DataModel();
model.setColumns(Arrays.asList("column1", "column2"));
model.addRow(Arrays.asList("value1", "value2"));
```

### Customizing Data Conversion
The `data-model` library allows you to customize data conversion by extending the `DataConverterDefault` class. Customizing data conversion is essential when the default conversion logic does not meet the specific needs of your application. For example, you may need to handle special data formats or apply specific business rules during the conversion process.

Here is an example of how to create and register a custom data converter:

1. **Create a custom data converter:**
Define a class that extends `DataConverterDefault` and override necessary methods for customization.

```java
import io.github.hisondev.datamodel.converter.DataConverterDefault;
import io.github.hisondev.datamodel.converter.DataConverterFactory;

public class CustomDataConverter extends DataConverterDefault {
    public static void register() {
        DataConverterFactory.setCustomConverter(new CustomDataConverter());
    }

    // Override methods for custom conversion logic
    @Override
    public Object convert(Object data) {
        // Custom conversion logic
        return super.convert(data);
    }
}
```

2. **Register the custom converter in your application:**
Ensure that the custom converter is registered when your Spring application starts.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        CustomDataConverter.register();
        SpringApplication.run(DemoApplication.class, args);
    }
}
```
***This setup allows you to customize how data is converted throughout your application by providing your own implementation of the DataConverterDefault class.***

### DataModelDeserializer and DataModelSerializer
The `DataModelDeserializer` and `DataModelSerializer` classes are used to convert data between the JSON format (used for communication between the front end and back end) and the `DataModel` object format used within your application. This allows for seamless data exchange in a structured format.

Here is an example of how these classes can be utilized to serialize and deserialize data:

Serialization:
```java
import io.github.hisondev.datamodel.DataModel;
import io.github.hisondev.datamodel.model.DataModelSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

// Create a DataModel instance
DataModel model = new DataModel();
model.setColumns(Arrays.asList("column1", "column2"));
model.addRow(Arrays.asList("value1", "value2"));

// Serialize DataModel to JSON
ObjectMapper mapper = new ObjectMapper();
String jsonString = mapper.writeValueAsString(model);
System.out.println("Serialized JSON: " + jsonString);
```

Deserialization:
```java
import io.github.hisondev.datamodel.DataModel;
import io.github.hisondev.datamodel.model.DataModelDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

// JSON string received from the front end
String jsonString = "{\"columns\":[\"column1\",\"column2\"],\"rows\":[[\"value1\",\"value2\"]]}";

// Deserialize JSON to DataModel
ObjectMapper mapper = new ObjectMapper();
DataModel model = mapper.readValue(jsonString, DataModel.class);
System.out.println("Deserialized DataModel: " + model);
```
***By using `DataModelSerializer` and `DataModelDeserializer`, you can ensure that the data structure remains consistent and easily manageable across different layers of your application. This is particularly useful for handling complex data interactions in modern web applications.***

## Contributing
Contributions are welcome! If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request on GitHub. Make sure to follow the project's code style and add tests for any new features or changes.

## License
MIT License

## Authors
Hani Son
hison0319@gmail.com
