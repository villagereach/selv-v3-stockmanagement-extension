# OpenLMIS Stock Management Extension Module
## Prerequisites
* Docker 1.11+

## Quick Start
1. Fork/clone this repository from GitHub.

 ```shell
 git clone https://github.com/villagereach/selv-v3-stockmanagement-extension.git
 ```
2. Fork/clone `openlmis-stockmanagement` repository from GitHub.

 ```shell
 git clone https://github.com/OpenLMIS/openlmis-stockmanagement.git
 ```
3. To assemble the outputs of project and create jar file run `docker-compose -f docker-compose.yml run builder`.
4. Edit configuration file `extensions.properties` from `selv-v3-extensions-config` repository to use your defined extension.
5. Run builder for `selv-v3-extensions-config` and build image.
6. Run `selv-v3-ref-distro` using `docker-compose.selv-v3-stockmanagement-extension.yml` and check if your changes has been applied.

## <a name="extensions"></a> Example of extensions usage

#### General information
OpenLMIS allows extending or overriding certain behavior of service using extension points and extension modules.
Every independent service can expose extension points that an extension module may utilize to extend its behavior.
Extension point is a Java interface placed in the service. Every extension point has its default implementation that
can be overridden. Extension modules can contain custom implementation of one or more extension points from main service.

Decision about which implementation should be used is made based on the configuration file `extensions.properties`.
This configuration file specifies which modules (implementations) should be used for the Service's extension points.
In selv-v3-extensions-configuration repository, there is an example of one such configuration file that specifies that
a `SequenceNumberGenerator` module should be used for the extension point `OrderNumberGenerator`.

```
#Example extensions configuration
OrderNumberGenerator=SequenceNumberGenerator
```

The extension point `OrderNumberGenerator` is an ID defined by the interface `OrderNumberGenerator.java`,
while the extension module `SequenceNumberGenerator` is an implementation of that interface whose name is a Spring Bean
defined in `SequenceNumberGenerator.java`

Configuration file lives in independent service repository. Every extension module should be deployed as JAR.
Example extension module and configuration file is published in the repository [selv-v3-fulfillment-extension](https://github.com/villagereach/selv-v3-fulfillment-extension).

Following classes are example of extension points usage:

- **OrderNumberGenerator.java** - sample extension point, that has Id defined in ExtensionPointId class.
- **Base36EncodedOrderNumberGenerator.java** - default implementation of that interface, it has `@Component` annotation that contains its Id.
- **SequenceNumberGenerator.java** -  class extending AdjustmentReasonValidator interface from openlmis-fulfillment repository. It has `@Component` annotation that contains its Id.
- **ExtensionManager.java** - class that has getExtensionByPointId method. It returns implementation of an extension class that is defined in
  configuration file for extension point with given Id.

#### Naming convention
The extension points' and extension modules' IDs should be **unique** and in **UpperCamelCase**.
A situation where two extension modules have the same ID leads to undeterministic behavior - it is not possible to predict which bean will be used.

* Extension points should be descriptive of the behavior that may be changed.  For example "RequisitionOrderQuantityCalculation" instead of "OrderQuantity".
* Extension Modules should describe the behaviour that is implemented, and the extension point that is being used.  For example "RequisitionOrderQuantityCalculationAMC" and "RequisitionOrderQuantityCalculationISA".