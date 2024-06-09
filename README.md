# data-model
A library designed to simplify data communication in Spring applications. Primarily featuring the DataWrapper and DataModel classes to streamline the process of data transfer and management.

## Introduction
The `data-model` library is designed to simplify data communication in Spring applications. It includes the `DataWrapper` and `DataModel` classes, which streamline the process of data transfer and management.

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
    <version>1.0.5</version>
</dependency>
```

## Usage
### Customizing Data Conversion
The data-model library allows you to customize data conversion by extending the `DataConverterDefault` class. Here is an example of how to create and register a custom data converter:

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

## Contributing
Contributions are welcome! If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request on GitHub. Make sure to follow the project's code style and add tests for any new features or changes.

## License
MIT License

## Authors
Hani Son
hison0319@gmail.com
