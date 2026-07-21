# data-model [![Maven Central](https://img.shields.io/maven-central/v/io.github.hisondev/data-model.svg?label=Maven%20Central)](https://mvnrepository.com/artifact/io.github.hisondev/data-model)

More detailed information can be found on the this.
[Homepage](https://hisondev.github.io/)

A library designed to simplify data communication in Spring applications. Primarily featuring the DataWrapper and DataModel classes to streamline the process of data transfer and management.

## Introduction
The `data-model` library is designed to simplify data communication in Spring applications. It includes the `DataWrapper` and `DataModel` classes, which streamline the process of data transfer and management.

For enhanced and convenient front-end and server communication, this library can be used in conjunction with `dataModel.min.js` from [hison-js](https://github.com/hisondev/hison-js).

## Getting Started
To start using the `data-model` library in your project, follow the installation and usage instructions below.

## Version Compatibility (Spring Boot)

> **Quick rule:**  
> - **Spring Boot 2.7.x** → use **data-model 1.x** (`javax.*`)  
> - **Spring Boot 3.x+** → use **data-model 2.x** (`jakarta.*`), **Java 17+** (recommended 21)

| data-model | Spring Boot | Java | Namespace |
|------------|-------------|------|-----------|
| **1.x**    | **2.7.x**   | 8+   | `javax.*` |
| **2.x**    | **3.x+**    | 17+  | `jakarta.*` |

If you are migrating to Spring Boot 3.x, switch your dependency to **data-model 2.x** and ensure your project uses **Java 17+**.

### Prerequisites
Before you can use the `data-model` library, you need to have the following software installed on your system:
- Java Development Kit (JDK) 8 or higher (v2.x requires JDK 21)
- Apache Maven (for building the project)

### Installation
You can add the `data-model` library to your project by including the following dependency in your Maven `pom.xml` file:

```xml
<dependency>
  <groupId>io.github.hisondev</groupId>
  <artifactId>data-model</artifactId>
  <version>1.0.8</version>
</dependency>
```

```xml
<dependency>
  <groupId>io.github.hisondev</groupId>
  <artifactId>data-model</artifactId>
  <version>2.0.2</version>
</dependency>
```

## Usage
### DataWrapper and DataModel Utilities
```java
import io.github.hison.data.wrapper.DataWrapper;
import io.github.hison.data.model.DataModel;

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
import io.github.hison.data.converter.DataConverterDefault;
import io.github.hison.data.converter.DataConverterFactory;

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

### Column Type Checking Policy (Flexible vs Strict) (since v2.0)
By default, `DataModel` uses a **flexible** type policy for column values — it does **not** enforce cross-row type consistency.
If you prefer to enforce that each column must keep the **same Java type across all rows**, enable **strict mode**:

```java
DataModel dm = new DataModel();
// default: flexible (no cross-row type enforcement)
dm.setStrictColumnType(true); // enable strict mode
```

## Memory Budget Guard (since v2.0)
To prevent accidental oversized payloads (e.g., overly broad date ranges) from causing Full GC or server instability,  
`DataModel` enforces a **memory budget** and throws a `DataException` when the estimated size exceeds the limit.

### Defaults
- Default budget: **32 MB** (tune per your policy)
- Heavy operations (e.g., `addRows(JsonNode)`, `addRows(ResultSet)`, `addRows(List<Object[]>, String[])`) check **immediately after completion**
- Small/frequent mutations (`addRow(...)`, `setValue(...)`, `removeRow(...)`, etc.) use **throttled checks** every _N_ mutations
- On exceed: throws `DataException("DataModel estimated size exceeds the configured budget...")`

### Quick Start
```java
DataModel dm = new DataModel();

// 1) Adjust budget (default 32 MB → 16 MB)
dm.setMaxEstimatedBytes(16L * 1024 * 1024);

// 2) (Optional) Tune throttled mutation check interval (default 128)
dm.setMutationCheckInterval(128);

// 3) (Optional) Swap estimator (default: JSON-serialization-based)
dm.setDataSizeEstimator(new DataModel.JsonSizeEstimator());

// 4) (Optional) Batch mode for large imports
dm.beginBulkUpdate();
try {
    // ... thousands of addRow / setValue calls ...
} finally {
    dm.endBulkUpdate(); // forces a final check
}
```

### DataModelDeserializer and DataModelSerializer
The `DataModelDeserializer` and `DataModelSerializer` classes are used to convert data between the JSON format (used for communication between the front end and back end) and the `DataModel` object format used within your application. This allows for seamless data exchange in a structured format.

Here is an example of how these classes can be utilized to serialize and deserialize data:

Serialization:
```java
import io.github.hison.data.model.DataModel;
import io.github.hison.data.model.DataModelSerializer;
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
import io.github.hison.data.model.DataModel;
import io.github.hison.data.model.DataModelDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

// JSON string received from the front end
String jsonString = "{\"columns\":[\"column1\",\"column2\"],\"rows\":[[\"value1\",\"value2\"]]}";

// Deserialize JSON to DataModel
ObjectMapper mapper = new ObjectMapper();
DataModel model = mapper.readValue(jsonString, DataModel.class);
System.out.println("Deserialized DataModel: " + model);
```
***By using `DataModelSerializer` and `DataModelDeserializer`, you can ensure that the data structure remains consistent and easily manageable across different layers of your application. This is particularly useful for handling complex data interactions in modern web applications.***

## Changelog

### 2.0.2
- **Fix (spec alignment)**: `setColumnSameValue` now **auto-creates the column when it does not exist** (previously a silent no-op), matching the documented hisonjs behavior ("If the column does not exist, it is created automatically"). Column creation counts as a structural change, so it is rejected when the model is structurally frozen (`setFreeze()`); value freezing (`setFreezeValues()`) is checked as before.
- **Fix (consistency)**: `setColumnSameValue` now routes the value through the configured `DataConverter` (same normalization as `setValue`/`addRow`) — previously raw values were stored, bypassing conversion.

### 2.0.1
- **Fix (freeze)**: `filterAndModify` now respects the frozen state (previously it could rewrite rows even after `setFreeze()`). Freeze checks are unified — structural changes (add/remove row·column, `clear`, `sort`, `filterAndModify`, `insert`) are governed by `setFreeze()`, while value changes (`setValue`, `setColumnSameValue/Format`, `searchAndModify`) are governed by `setFreezeValues()`. So `setFreezeValues()` now correctly locks values while still allowing structural changes.
- **Fix (encapsulation)**: `DataModel.getRows()` now returns deep copies of each row, so modifying the returned list's rows no longer corrupts the original DataModel (consistent with `getRow()`).
- **Fix (encapsulation)**: `DataWrapper.putDataModel()` now stores a clone of the DataModel (consistent with `put(...)`), so later external changes to the passed instance don't affect the stored value.
- **Add**: `equals()`/`hashCode()` for `DataModel` and `DataWrapper` (value-based) — useful when using them as DTO replacements.
- **Improvement**: value normalization now explicitly handles `BigDecimal`, `BigInteger`, `LocalDate`, and `LocalTime`.
- **Docs**: corrected `getString`/`getDataModel` Javadoc (they return `null`, they do not throw), `DataException` Javadoc, README import paths (`io.github.hison.data.*`) and version.

## Contributing
Contributions are welcome! If you have any ideas, suggestions, or bug reports, please open an issue or submit a pull request on GitHub. Make sure to follow the project's code style and add tests for any new features or changes.

## License
MIT License

## Authors
Hani Son
hison0319@gmail.com
