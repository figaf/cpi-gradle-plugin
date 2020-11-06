# cpi-gradle-plugin
This plugin provides an integration with SAP CPI platform. It can be used as standalone plugin.

The easist way to use the plugin is to use the Figaf DevOps Tool, since it will create the folder structure you need to manage your SAP CPI. You can see how to configure the tool here https://figaf.com/sap-cpi-development-in-git-and-debug-your-groovy-scripts/. There is a free trial that allows you to get started. 

## Requirements

Gradle 4.10 or later.

## Getting started

You need to organize modular structure, where each separate IFlow/Value Mapping folder is a Gradle module.
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
│   ├── valueMappingTechnicalName
│   │   ├── META-INF
│   │   │   └── MANIFEST.MF
│   │   ├── value_mapping.xml
│   │   └── ...
│   └── ...  
├── ...
├── build.gradle
└── gradle.properties
```
You can download IFlow/Value Mapping archives from CPI manually and then unpack them to the project or just use `downloadArtifact` task 
to fetch and automatically unpack bundled IFlow/Value Mapping. Just create a high-level folder structure for needed IFlow: 
`packageTechnicalName/iflowTechnicalName`, Value Mapping: `packageTechnicalName/valueMappingTechnicalName`, register that 
folder as a module in `settings.gradle` (see later) and run `downloadArtifact` task.

build.gradle
```
plugins {
    id 'com.figaf.cpi-plugin' version '2.0.RELEASE' apply false
}

configure(subprojects.findAll()) { sub ->

    apply plugin: 'idea'
    apply plugin: 'groovy'

    repositories {
        mavenLocal()
        jcenter()
    }

    if (sub.name.startsWith("iflow-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "CPI_IFLOW"
        }

    } else if (sub.name.startsWith("vm-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "VALUE_MAPPING"
        }
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

include "package1TechnicalName"
project (":package1TechnicalName").projectDir = file("package1TechnicalName")

include "package2TechnicalName"
project (":package2TechnicalName").projectDir = file("package2TechnicalName")

include "package1TechnicalName:iflow-iFlow1TechnicalName"
project (":package1TechnicalName:iflow-iFlow1TechnicalName").projectDir = file("package1TechnicalName/iFlow1TechnicalName")

include "package1TechnicalName:iflow-iFlow2TechnicalName"
project (":package1TechnicalName:iflow-iFlow2TechnicalName").projectDir = file("package1TechnicalName/iFlow2TechnicalName")

include "package2TechnicalName:iflow-iFlow3TechnicalName"
project (":package2TechnicalName:iflow-iFlow3TechnicalName").projectDir = file("package2TechnicalName/iFlow3TechnicalName")

include "package2TechnicalName:iflow-iFlow4TechnicalName"
project (":package2TechnicalName:iflow-iFlow4TechnicalName").projectDir = file("package2TechnicalName/iFlow4TechnicalName")

include "package2TechnicalName:vm-valueMappingTechnicalName"
project (":package2TechnicalName:vm-valueMappingTechnicalName").projectDir = file("package2TechnicalName/valueMappingTechnicalName")
```

gradle.properties
```
cpiUrl=https://pxxxx-tmn.hci.eu1.hana.ondemand.com
cpiUsername=S00000000
cpiPassword=123456
cloudPlatformType=NEO
```

## Tasks
The plugin has 3 tasks
1. `uploadArtifact` - builds bundled model of IFlow/Value Mapping and uploads it to CPI.
2. `deployArtifact` - deploys IFlow/Value Mapping on CPI. Usually it makes sense to run this task after `uploadArtifact`.
3. `downloadArtifact` - downloads IFlow/Value Mapping bundled model from CPI and unpacks it to module folder.

## Configuration
The tasks can be configured through an extension `cpiPlugin` which accepts several parameters:
* `url`* - basic path to the CPI agent. Example: `https://pxxxx-tmn.hci.eu1.hana.ondemand.com`
* `username`* - CPI username. Example: `S00000000`
* `password`* - CPI password. Example: `123456`
* `platformType`* - Cloud platform type. `NEO` or `CLOUD_FOUNDRY`. Default value: `NEO`.
* `sourceFilePath`* - path to the directory with the IFlow/Value Mapping. Default value: `$project.projectDir` which means
that root directory of the IFlow/Value Mapping will be taken. In most cases this parameter shouldn't be overridden but it can be any valid path.
Example: `C:\some\path`
* `artifactType`* - Type of artifact for which the tasks will be executed. `CPI_IFLOW` or `VALUE_MAPPING`.
* `packageTechnicalName` - package technical name. By default the name of the parent folder is used. If your project structure is not standard
you can define this parameter directly. Example: `Test`
* `packageExternalId` - package Id on CPI. By default the plugin looks for it in CPI automatically using `packageTechnicalName` but if you know
this value, you can define it directly. Example: `4b551fb3c16442c4b33a8d61e0fd3477`
* `artifactTechnicalName` - IFlow/Value Mapping technical name. By default the name of the folder is used. If your project structure is not standard
you can define this parameter directly. Example: `MyIFlow`
* `artifactExternalId` - IFlow/Value Mapping Id on CPI. By default the plugin looks for it in CPI automatically using `artifactTechnicalName` but if you know
this value, you can define it directly. Example: `2f0d56be14c44a50a3c1e5c1bebc23fe`
* `waitForStartup` - used only by `deployArtifact` task. If this parameter is true, the plugin will not only deploy the IFlow/Value Mapping but also wait until it's successfully started.
Default value: `false`. 
* `ignoreFilesList` - list of files (or directories) which shouldn't be added to the archive when the plugin executes `uploadArtifact` task and shouldn't be modified when the plugin executes `downloadArtifact` task.
The plugin always adds to this list the following paths: `src/test`, `build.gradle`, `gradle.properties`, `settings.gradle`. Example: `["somefile.txt", "somefolder"]`
* `uploadDraftVersion` - used only by `uploadArtifact` task. If true it will upload the IFlow/Value Mapping with "Draft" version number. If false it will use
the number from MANIFEST file. Default value: `false`.
