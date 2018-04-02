# Some notes of Developing an openHAB binding
<author>Markus Michels (markus7017)</author>

Please make sure to read the following documents before starting your development
* <a href="https://www.eclipse.org/smarthome/documentation/index.html">Eclipse Smart Home (ESH) - Documentation Overview</a><br/>
describes the basic concepts etc.
* <a href="https://docs.openhab.org/developers/development/guidelines.html">Coding Guidelines</a><br/>
You need to follow those guidelines to pass the static code analysis if you intent to publish your binding as part of the official distribution.
* <a href="https://docs.openhab.org/developers/development/bindings.html">Developing a Binding for openHAB 2</a><br/>
gives a basic introduction. This document provides additional hints to get you up-to-speed without struggeling with some common problems.
* <a href="https://docs.openhab.org/developers/development/logging.html">Logging</a><br/>
provides you information on the logging concept and configuration. You will need a lot of those messages ;-)

##Installing the development environment
* Follow the steps provided here: <a href="https://docs.openhab.org/developers/development/ide.html">Setting up an IDE for openHAB</a>
- Make sure the <a href="https://docs.openhab.org/developers/development/ide.html#prerequisites">pre-requisites</a> are given. Download and install <a href="https://git-scm.com/downloads">GiteHub tools</a>, <a href="https://maven.apache.org/download.cgi">Maven build tools</a> (this is also required to create the binding jar, see below).
- Make sure to use the <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html">Oracle Java 1.8 JDK</a> (not only JRE).
- Follow the <a href="https://docs.openhab.org/developers/development/ide.html#installation">instructions</a> to ownload and run the <a href="https://wiki.eclipse.org/Eclipse_Installer">Eclipse Installaer</a>
- <a href="https://docs.openhab.org/developers/development/ide.html#building-running-and-debugging">Build the openHAB</a>project for the first time.

### openHAB 3rd party bundles

You should add another software site to the target platform, this supplies several Maven bundles not part of the standard setup, but used by several projects (e.g. JScience / javax.measure.*).
- open project infrastructure->launch
- double click openhab.target
- click on Locations->Add
- click add->software site
- select Software Site->Next
- click Add
- Enter any name
- site: http://eclipse.github.io/smarthome/third-party/target/repository
- ok
- open Maven osgi bundles
- scroll down the list
- checkmark all tec.unon.* and Units of Measurement*
- Finish
- Click Reload Target Platform in the upper right corner
- Save
- Rebuild starts
- Open project Runtime->openhab.core.karaf
- double click on the pom.xml
- click on the red message “Plugin execution not covered…”
- click “Mark goal highest…”

Maybe some errors are remaining, but those are related to specific bundles and could be ignored / delete the associated projects.


##Some more preperations
- fixing pom.xml
- removing demo.*
The distribution comes with a buch conf config files for demonstration purpose. This is helpful to get different examples like item definitions, rules, scripts etc. and play with time. The setup includes some kind of simulation and also a ClassUI sitemap - all good.<p/>
However, when developing this creates a lot of console output, which is not related to your binding. For this you should remove the demo.* files and cleanup your debug log.<p/>
* Open Eclipse
* Go to the end of the project list and open infrastructure->distro-resources->src->main->resources
* remove the demo.* files from all sub-folders
* open resources->persistence->rrd4j.persist, remove everything from the Items section, make it look like
 
```
Items {
}
```
<p/>

##First run, verify everything is properly installed and running.
* Open the workspace in Eclipse
* macOS<p/>
When developing on macOS you might encounter the problem that your openHAB environment starts in Eclipse debugging mode, but once you try to open the UI the browser seems to hang.<p/>
Thanks Vicent (doume), he found the solution on that, check<br/>
<a href="https://community.openhab.org/t/getting-started-with-oh-dev/39007/17">openHAB Community - Getting started with OH Dev</a> and<br>
<a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=469745">native+java launcher hangs loading swing+swt application on macosx"</a><p/>
To fix this:<br/>
* Go to Run->Debug Configurations
* De-collapse "Eclipse Applicasions", select "openHAB Runtime"
* Select tab "Arguments"
* add -XstartOnFirstThread to the list of arguments, e.g.

```
-os ${target.os} -ws ${target.ws} -arch ${target.arch} -nl ${target.nl} -consoleLog -console -XstartOnFirstThread 
```

<p/>

* You should disable the simple mode to get full control on thing/item creation.<br/>
Open <a href="https://127.0.0.1:8443/paperui/index.html#/configuration/system">PaperUI</a> and go to Configuration->System. Disable "Simple Mode" under "Item Linking" and click [Save]. Now a new thing will be shown in the PaperUI's Inbox and you could add it manually + creating an item with a given name.

## Develop your binding
- openHAB community<p/>
It's always a good idea to open a discussion thread in the <a href="https://community.openhab.org">openHAB Community</a> to discuss functional requirements, design etc. Before starting binding development you should check the community if someone is already working on a similar binding or an existing binding supports your use case in general and you could contribute to extending the functionality.<p/>
Example thread: <a href="https://community.openhab.org/t/rachio-smart-sprinkler-controller/7078/9?u=markus7017">Rachio Sprinkler Binding</a><p/>
The community is driven by all openHAB users willable to contribute with trouble shooting, configuration samples, integration of devices not supported by openHAB, but also discussing and providing development related information. The community is a place to be for a binding developer. If you are not signed in, you could create a free account and start contributing.

### Creating a binding from skeleton
- First read <a href="https://docs.openhab.org/developers/development/bindings.html#creating-a-skeleton">Creating a Skeleton</a>
- <a href="https://www.eclipse.org/smarthome/documentation/concepts/index.html">Things, Channels, Bindings, Items and Links</a> gives you an overview on the badsic concepts of bridges, things, items and links. <a href="https://www.eclipse.org/smarthome/documentation/concepts/categories.html">Thing Categories</a> explains the differen thing categories.
- run skeleton script
- Importing project into Eclipse
Refer to <a href="https://www.eclipse.org/smarthome/documentation/development/bindings/how-to.html#structure-of-a-binding">Structure of a Binding Project</a> to get an overview on the project structure.
- change binding name
- fix build properties
using “mvn install” in the binding directory I was able to build a jar after fixing build.properties. The scripts, which generates the skeleton creates the following file:

```
source..=src/main/java/
output..=target/classes
bin.includes=META-INF/,\
             .,\
             OSGI-INF/,\
             ESH-INF/,\
             NOTICE
```
However, there is no file NOTICE. Instead you need to include about.html:

```
source..=src/main/java/
output..=target/classes
bin.includes=META-INF/,\
             .,\
             OSGI-INF/,\
             ESH-INF/,\
             about.html
```
- enable binding in debug config
<p/>

### Debugging
- Adding your Binding to the Debug Eclipse Configuration
- Enable debugging for your binding
* Open project infrastructurte->launch->home
* Edit file logback_debug.xml and insert a line at the end (before tag /configuration)

```
<configuration scan="true">
...
    <logger name="org.openhab.binding.rachio" level="DEBUG" />
</configuration>
```

- Running the binding
- home directory
- Debug output (using the logger class)
- Definiting your own log file

###Developing the logic
- Defining SUPPORTED_THING_TYPES
You'll find a file <Binding>BindingConstants.java in package org.openhab.binding.<binding>. This file you be used to put all constants here, which are global for the binding like:<br/>

```
    public static final String BINDING_ID = "rachio";
    public static final String BINDING_VENDOR = "Rachio";

    // List of non-standard Properties
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_MAC_ADDRESS = "macAddress";
    public static final String PROPERTY_SN = "serialNumber";

    // List of all Device Channel ids
    public static final String CHANNEL_DEVICE_NAME = "name";
    public static final String CHANNEL_DEVICE_ACTIVE = "active";
    public static final String CHANNEL_DEVICE_ONLINE = "online";
    public static final String CHANNEL_DEVICE_PAUSED = "paused";
 ```
 
Avoid in any case hard coded strings for CHANNEL names, PROPERTY etc. They need to match the XML-based definitions etc.

- The HandlerFactory
One a thing should be created (e.g. when adding a thing to the .things file or being auto discovered) the ThingHandlerFactory will be called. Your implementation extends the BaseThingHandlerFactory. Usually this class will be implemented in org.openhab.binding.<binding>.internal:

```
@Component(service = { ThingHandlerFactory.class,
        RachioHandlerFactory.class }, immediate = true, configurationPid = "binding.rachio")
public class RachioHandlerFactory extends BaseThingHandlerFactory {
```


If your binding supports multiple things you need to take care that the matching Handler is created. Use thing.getThingTypeUID() to query the thing type, which can then be compared with the definitions from RachioBindingConsdtants.SUPPPORTED_xxx_THING_TYPE.<p/>

```
protected @Nullable ThingHandler createHandler(Thing thing) {
    try {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.trace("RachioHandlerFactory: Create thing handler for type {}", thingTypeUID.toString());
        if (RachioBindingConstants.SUPPORTED_BRIDGE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return createBridge((Bridge) thing);
        } else if (RachioBindingConstants.SUPPORTED_ZONE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new RachioZoneHandler(thing);
        } else if (RachioBindingConstants.SUPPORTED_DEVICE_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new RachioDeviceHandler(thing);
        }
    } catch (Exception e) {
        logger.error("RachioHandlerFactory:Exception while creating Rachio RThing handler: {}", e);
    }

    logger.debug("RachioHandlerFactory:: Unable to create thing handler!");
    return null;
    }
```

- Thing initialization<p/>
- Filling Thing Properties<p/>
- Defining channels<p/>
- Filling thing properties<p/>
- Processing channel commands<p/>
- Updating channel data<p/>
- Adding additional libraries to your project
All libraries (.jar) you want to edit need to be added to META-INF/MANIFEST.MF. Add a line for each of them to the Import-Package section.
```
...
Import-Package: 
 com.google.gson,
 org.apache.commons.net,
 ...
```


### Implementing a bridge device
- Bridge thing<p/>

###Dynamic Device Discovery - A must have for the user
- Discovery Service<p/>
<a href="https://www.eclipse.org/smarthome/documentation/concepts/discovery.html">Thing Discovery</a> explains the basic concept of the Inbox and Device Discovery in openHAB.

```
    /**
     * Register the given cloud handler to participate in discovery of new beds.
     * @param cloudHandler the cloud handler to register (must not be <code>null</code>)
     */
    private synchronized void registerDiscoveryService(final RachioBridgeHandler cloudHandler) {
        logger.debug("RachioHandlerFactory: Registering Rachio discovery service");
        RachioDiscoveryService discoveryService = new RachioDiscoveryService();
        discoveryService.setCloudHandler(cloudHandler);
        discoveryServiceReg.put(cloudHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    /**
     * Unregister the given cloud handler from participating in discovery of new beds.
     *
     * @param cloudHandler the cloud handler to unregister (must not be <code>null</code>)
     */
    private synchronized void unregisterDiscoveryService(final RachioBridgeHandler cloudHandler) {
        ThingUID thingUID = cloudHandler.getThing().getUID();
        ServiceRegistration<?> serviceReg = discoveryServiceReg.get(thingUID);
        if (serviceReg == null) {
            return;
        }

        logger.debug("RachioHandlerFactory: Unregistering Rachio discovery service");
        serviceReg.unregister();
        discoveryServiceReg.remove(thingUID);
    }
```

### Creating the Bundle jar
- <a href="https://docs.openhab.org/developers/development/bindings.html#include-the-binding-in-the-build-and-the-distro">Include the Binding in the Build and the Dist</a> gives some basic information on how to export the jar for your binding.<p/>
- Adding external libs/jars to your bundle
Before you could build your binding .jar file you need to add all external to your build.properties file.<br/>
In this case we are adding lib/gson-2.7.jar (which is located in the lib sub folder) to the bundle.

```
source..=src/main/java/
output..=target/classes
bin.includes=META-INF/,\
             .,\
             OSGI-INF/,\
             ESH-INF/,\
             lib/gson-2.7.jar,\
             about.html,\
             README.MD
```
- mvn build
<?xml version="1.0" encoding="UTF-8" standalone="no"?><project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

openhab2-addons/addons/binding/pom.xml

```
...
  <modules>
    <module>org.openhab.binding.rachio</module>
...
  </modules>
</project>
```

openhab2-addons/features/openhab-addons/src/main/feature/feature.xml

```
<features name="${project.artifactId}-${project.version}" xmlns="http://karaf.apache.org/xmlns/features/v1.4.0">
...
    <feature name="openhab-binding-rachio" description="Rachio Binding" version="${project.version}">
        <feature>openhab-runtime-base</feature>
        <bundle start-level="80">mvn:org.openhab.binding/org.openhab.binding.rachio/${project.version}</bundle>
    </feature>
...
</features>
```

- Level A: no static code analysis

Open a command line (terminal) and hange to the binding directory, then run the following command:
...
mvn -DskipChecks=true install
```
The file org.openhab.binding.<binding>-<version>-SNAPSHOT.jar will be created in the sub folder 'target'
 
- Level B: with static code analysis (required to pass if you want to make your binding part of the official distribution)

##Involve the Community for testing
- General <a href="https://docs.openhab.org">openHAB Documentation</a>
- publish your binding on the Eclipse IoT Market Place
<a href="https://community.openhab.org/t/distributing-bindings-through-the-iot-marketplace/24491">Distributing bindings through the IoT Marketplace</a>
- openHAB community

##Other resources
* openHAB Community: <a href="https://community.openhab.org/t/getting-started-with-oh-dev/39007?u=markus7017">Getting started with OH Dev</a>
* <a href="https://community.openhab.org/c/apps-services/my-openhab">Lates topics in the Community</a>
* <a href="https://community.openhab.org/t/taming-openhab-2-logging/13976">Taming openHAB 2 Logging</a>
* <a href="https://community.openhab.org/t/influxdb-grafana-persistence-and-graphing/13761">Using Garfana for Charting</a>
* <a href="https://rachio.readme.io/v1.0/docs">Rachio API documentation</a> (specific for the Rachio Binding)
* <a href="https://support.rachio.com/hc/en-us/articles/115010541948-How-secure-is-my-Rachio-controller-">Rachio Security Concept</a>
* <a https://ip-ranges.amazonaws.com/ip-ranges.json>AWS's IP address ranges by region</a>

