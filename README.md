ciao-docs-transformer
==================

*CIP to transform properties extracted from parsed documents*

Introduction
------------

The purpose of this CIP is to process an incoming [parsed document](https://github.com/nhs-ciao/ciao-docs-parser/blob/master/docs/parsed-document.md) by transforming the properties before publishing the enriched document for further processing by other CIPs.

`ciao-docs-transformer` is built on top of [Apache Camel](http://camel.apache.org/) and [Spring Framework](http://projects.spring.io/spring-framework/), and can be run as a stand-alone Java application, or via [Docker](https://www.docker.com/).

Each application can host multiple [routes](http://camel.apache.org/routes.html), where each route follows the following basic structure:

>   input queue (JMS) -\> [DocumentTransformer](./src/main/java/uk/nhs/ciao/docs/transformer/processor/DocumentTransformer.java) -\> output queue (JMS)

-	*The input and output queues both use the JSON-encoded representation of [ParsedDocument](https://github.com/nhs-ciao/ciao-docs-parser/blob/master/docs/parsed-document.md).*
-	DocumentTransformer uses a delegate [PropertiesTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/PropertiesTransformation.java) instance to transform the incoming `document.getProperties()` map.

The details of the JMS queues and document transformers are specified at runtime through a combination of [ciao-configuration](https://github.com/nhs-ciao/ciao-utils) properties and Spring XML files.

**Provided properties transformations:**

-   [PropertiesTransformer](./src/main/java/uk/nhs/ciao/docs/transformer/PropertiesTransformer.java) - Acts as a factory / DSL for specifying a list of specific transformations to perform
-   [NestedPropertiesTransformer](./src/main/java/uk/nhs/ciao/docs/transformer/NestedPropertiesTransformer.java) - A special-case `PropertiesTransformer` which performs transformations on a child property found from the incoming properties.
-   [RenamePropertyTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/RenamePropertyTransformation.java) - Moves / copies a property to a new name
-   [SplitPropertyTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/SplitPropertyTransformation.java) - Splits a property value into multiple values and assigns each to a specified target property
-   [SplitListPropertyTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/SplitListPropertyTransformation.java) - Splits a property value into a list of values (using the specified pattern) and assigns the resulting list to the target property
-   [CombinePropertiesTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/CombinePropertiesTransformation.java) - Combines a series of input properties into a combined output property. The default combined value is an HTML two-column table.
-   [FormatDatePropertyTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/FormatDatePropertyTransformation.java) - Parses and reformats a date property
-   [FindAndFormatDatePropertiesTransformation](./src/main/java/uk/nhs/ciao/docs/transformer/FindAndFormatDatePropertiesTransformation.java) - Finds all string properties matching the specified date pattern and reformats them to use the specified output pattern

For more advanced usages, custom transformations can be integrated by implementing the `PropertiesTransformation` Java interface and providing a suitable spring XML configuration on the classpath.

**Property Selectors:**

> The provided properties transformations use [PropertyName](https://github.com/nhs-ciao/ciao-docs-parser/blob/master/ciao-docs-parser-model/src/main/java/uk/nhs/ciao/docs/parser/PropertyName.java) to identify source and target properties.

Property names support addressing nested properties by key and index:
- nested keys: `root.child`
- nested arrays: `root[0]`

Names can be combined: `root.child[2].name.firstName`.

Special characters `[ ] . * \` must be delimited by a `\` prefix:
- `D\.O\.B`
- `first\\last`

Configuration
-------------

For further details of how ciao-configuration and Spring XML interact, please see [ciao-core](https://github.com/nhs-ciao/ciao-core).

### Spring XML

On application start-up, a series of Spring Framework XML files are used to construct the core application objects. The created objects include the main Camel context, input/output components, routes and any intermediate processors.

The configuration is split into multiple XML files, each covering a separate area of the application. These files are selectively included at runtime via CIAO properties, allowing alternative technologies and/or implementations to be chosen. Each imported XML file can support a different set of CIAO properties.

The Spring XML files are loaded from the classpath under the [META-INF/spring](./src/main/resources/META-INF/spring) package.

**Core:**

-   `beans.xml` - The main configuration responsible for initialising properties, importing additional resources and starting Camel.

**Processors:**

-   `processors/default.xml` - Creates the following `DocumentTransformer` instances: `kingsWordDischargeNotificationTransformer` and `kentEDNTransformer`

**Messaging:**

-   `messaging/activemq.xml` - Configures ActiveMQ as the JMS implementation for input/output queues.
-   `messaging/activemq-embedded.xml` - Configures an internal embedded ActiveMQ as the JMS implementation for input/output queues. *(For use during development/testing)*

### CIAO Properties

At runtime ciao-docs-transformer uses the available CIAO properties to determine which Spring XML files to load, which Camel routes to create, and how individual routes and components should be wired.

**Camel Logging:**

-	`camel.log.mdc` - Enables/disables [Mapped Diagnostic Context](http://camel.apache.org/mdc-logging.html) in Camel. If enabled, additional Camel context properties will be made available to Log4J and Logstash. 
-	`camel.log.trace` - Enables/disables the [Tracer](http://camel.apache.org/tracer.html) interceptor for Camel routes.
-	`camel.log.debugStreams` - Enables/disables [debug logging of streaming messages](http://camel.apache.org/how-do-i-enable-streams-when-debug-logging-messages-in-camel.html) in Camel.

**Spring Configuration:**

-   `processorConfig` - Selects which processor configuration to load:
    `processors/${processorConfig}.xml`

-   `messagingConfig` - Selects which messaging configuration to load:
    `messaging/${messagingConfig}.xml`

**Routes:**

-   `documentTransformerRoutes` - A comma separated list of route names to build

The list of route names serves two purposes. Firstly it determines how many routes to build, and secondly each name is used as a prefix to specify the individual properties of that route.

**Route Configuration:**

>   For 'specific' properties unique to a single route, use the prefix:
>   `documentTransformerRoutes.${routeName}.`
>
>   For 'generic' properties covering all routes, use the prefix:
>   `documentTransformerRoutes.`

-   `inputQueue` - Selects which queue to consume incoming documents from
-   `transformerId` - The Spring ID of the transformer to use when enriching documents
-   `outputQueue` - Selects which queue to publish enriched documents to

**In-progress Folder:**
> Details of the in-progress folder structure are available in the `ciao-docs-finalizer` [state machine](https://github.com/nhs-ciao/ciao-docs-finalizer/blob/master/docs/state-machine.md) documentation.

> `ciao-docs-parser` provides the [InProgressFolderManagerRoute](https://github.com/nhs-ciao/ciao-docs-parser/blob/master/ciao-docs-parser-model/src/main/java/uk/nhs/ciao/docs/parser/route/InProgressFolderManagerRoute.java) class to support storing control and event files in the in-progress directory.

- `inProgressFolder` - Defines the root folder that *document upload process* events are written to.

### Example
```INI
# Camel logging
camel.log.mdc=true
camel.log.trace=false
camel.log.debugStreams=false

# Select which processor config to use (via dynamic spring imports)
processorConfig=default

# Select which messaging config to use (via dynamic spring imports)
messagingConfig=activemq
# messagingConfig=activemq-embedded

# ActiveMQ settings (if messagingConfig=activemq)
activemq.brokerURL=tcp://localhost:61616
activemq.userName=smx
activemq.password=smx

# Setup route names (and how many routes to build)
documentTransformerRoutes=kings,kent

# Setup 'shared' properties across all-routes
documentTransformerRoutes.outputQueue=transformed-documents

# Setup per-route properties (can override the shared properties)
documentTransformerRoutes.kings.inputQueue=parsed-kings-documents
documentTransformerRoutes.kings.transformerId=kingsWordDischargeNotificationTransformer

documentTransformerRoutes.kent.inputQueue=parsed-kent-documents
documentTransformerRoutes.kent.transformerId=kentEDNTransformer

# Global properties
inProgressFolder=./in-progress

```

Building and Running
--------------------

To pull down the code, run:

	git clone https://github.com/nhs-ciao/ciao-docs-transformer.git
	
You can then compile the module via:

	mvn clean install -P bin-archive

This will compile a number of related modules - the main CIP module is `ciao-docs-transformer`, and the full binary archive (with dependencies) can be found at `target\ciao-docs-transformer-{version}-bin.zip`. To run the CIP, unpack this zip to a directory of your choosing and follow the instructions in the README.txt.

The CIP requires access to various file system directories and network ports (dependent on the selected configuration):

**etcd**:
 -  Connects to: `localhost:2379`

**ActiveMQ**:
 -  Connects to: `localhost:61616`

**Filesystem**:
 -  If etcd is not available, CIAO properties will be loaded from: `~/.ciao/`
 -  The default configuration will load JSON files for the filesystem if any `file://` URLs are specified in the `json.resourcePaths` or `json.resourcePath` properties. This can be altered by changing the CIAO properties configuration (via etcd, or the properties file in `~/.ciao/`)
 -  If an incoming document cannot be converted, the CIP will write an event to the folder specified by the `inProgressFolder` property.
