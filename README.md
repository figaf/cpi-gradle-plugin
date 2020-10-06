# cpi-gradle-plugin
This plugin provides an integration with SAP CPI platform. It can be used as standalone plugin.

The easist way to use the plugin is to use the Figaf DevOps Tool, since it will create the folder structure you need to manage your SAP CPI. You can see how to configure the tool here https://figaf.com/sap-cpi-development-in-git-and-debug-your-groovy-scripts/. There is a free trial that allows you to get started. 

## Requirements

Gradle 4.10 or later.

## Getting started

You need to organize modular structure, where each separate IFlow folder is a Gradle module.
Default project structure:
```
rootProject
├── package1TechnicalName
│   ├── iFlow1TechnicalName
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF 
│   │   ├── src   
│   │   │   └── ...
│   │   └── ...
│   ├── iFlow2TechnicalName
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF
│   │   ├── src   
│   │   │   └── ...
│   │   │   
│   │   └── ...  
│   └── ...     
├── package2TechnicalName
│   ├── iFlow3TechnicalName
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF
│   │   ├── src   
│   │   │   └── ...
│   │   └── ...
│   ├── iFlow4TechnicalName
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF 
│   │   ├── src   
│   │   │   └── ...  
│   │   └── ...  
│   └── ...  
├── ...
├── build.gradle
└── gradle.properties
```
You can download IFlow archives from CPI manually and then unpack them to the project or just use `downloadIntegrationFlow` task 
to fetch and automatically unpack bundled IFlow. Just create a high-level folder structure for needed IFlow: 
`packageTechnicalName/iflowTechnicalName`, register that folder as a module in `settings.gradle` (see later) and run 
`downloadIntegrationFlow` task.

build.gradle
```
plugins {
    id 'com.figaf.cpi-plugin' version '1.0.RELEASE' apply false
}

subprojects { sub->

    apply plugin: 'com.figaf.cpi-plugin'

    cpiPlugin {
        url = cpiUrl
        username = cpiUsername
        password = cpiPassword
        platformType = cpiPlatformType
        waitForStartup = true
        sourceFilePath = "$project.projectDir".toString()
        uploadDraftVersion = true
    }
}
```

settings.gradle
```
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

include "package1TechnicalName-iFlow1TechnicalName"
project (":package1TechnicalName-iFlow1TechnicalName").projectDir = file("package1TechnicalName/iFlow1TechnicalName")

include "package1TechnicalName-iFlow2TechnicalName"
project (":package1TechnicalName-iFlow2TechnicalName").projectDir = file("package1TechnicalName/iFlow2TechnicalName")

include "package2TechnicalName-iFlow3TechnicalName"
project (":package2TechnicalName-iFlow3TechnicalName").projectDir = file("package2TechnicalName/iFlow3TechnicalName")

include "package2TechnicalName-iFlow4TechnicalName"
project (":package2TechnicalName-iFlow4TechnicalName").projectDir = file("package2TechnicalName/iFlow4TechnicalName")
```

gradle.properties
```
cpiUrl=https://pxxxx-tmn.hci.eu1.hana.ondemand.com
cpiUsername=S00000000
cpiPassword=123456
cpiPlatformType=NEO
```

## Tasks
The plugin has 3 tasks
1. `uploadIntegrationFlow` - builds bundled model of IFlow and uploads it to CPI.
2. `deployIntegrationFlow` - deploys IFlow on CPI. Usually it makes sense to run this task after `uploadIntegrationFlow`.
3. `downloadIntegrationFlow` - downloads IFlow bundled model from CPI and unpacks it to module folder.

## Configuration
The tasks can be configured through an extension `cpiPlugin` which accepts several parameters:
* `url`* - basic path to the CPI agent. Example: `https://pxxxx-tmn.hci.eu1.hana.ondemand.com`
* `username`* - CPI username. Example: `S00000000`
* `password`* - CPI password. Example: `123456`
* `sourceFilePath`* - path to the directory with the IFlow. Default value: `$project.projectDir` which means
that root directory of the IFlow will be taken. In most cases this parameter shouldn't be overridden but it can be any valid path.
Example: `C:\some\path`
* `packageTechnicalName` - package technical name. By default the name of the parent folder is used. If your project structure is not standard
you can define this parameter directly. Example: `Test`
* `packageExternalId` - package Id on CPI. By default the plugin looks for it in CPI automatically using `packageTechnicalName` but if you know
this value, you can define it directly. Example: `4b551fb3c16442c4b33a8d61e0fd3477`
* `integrationFlowTechnicalName` - integration flow technical name. By default the name of the folder is used. If your project structure is not standard
you can define this parameter directly. Example: `MyIFlow`
* `integrationFlowExternalId` - IFlow Id on CPI. By default the plugin looks for it in CPI automatically using `integrationFlowTechnicalName` but if you know
this value, you can define it directly. Example: `2f0d56be14c44a50a3c1e5c1bebc23fe`
* `waitForStartup` - used only by `deployIntegrationFlow` task. If this parameter is true, the plugin will not only deploy the IFlow but also wait until it's successfully started.
Default value: `false`. 
* `ignoreFilesList` - list of files (or directories) which shouldn't be added to the archive when the plugin executes `uploadIntegrationFlow` task and shouldn't be modified when the plugin executes `downloadIntegrationFlow` task.
The plugin always adds to this list the following paths: `src/test`, `build.gradle`, `gradle.properties`, `settings.gradle`. Example: `["somefile.txt", "somefolder"]`
* `uploadDraftVersion` - used only by `uploadIntegrationFlow` task. if true it will upload the IFlow with "Draft" version number. If false it will use
the number from MANIFEST file. Default value: `false`.
