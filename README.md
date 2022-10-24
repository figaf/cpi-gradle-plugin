# cpi-gradle-plugin
This plugin provides an integration with SAP CPI platform. It can be used as standalone plugin. 
Supported artifact types: IFlow, Value Mapping, Message Mapping, Script Collection.

The easiest way to use the plugin is to use the Figaf DevOps Tool, since it will create the folder structure you need to manage your SAP CPI. You can see how to configure the tool here https://figaf.com/sap-cpi-development-in-git-and-debug-your-groovy-scripts/. There is a free trial that allows you to get started. 

## Requirements

Gradle 4.10 or later.

## Getting started

You need to organize modular structure, where each separate artifact folder is a Gradle module.
Default project structure:
```
rootProject
├── package1TechnicalName
│   ├── IntegrationFlow
│   │   ├── iFlow1TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   ├── iFlow2TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   │   
│   │   │   └── ...  
│   │   └── ...     
│   ├── ValueMapping
│   │   ├── valueMapping1TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF
│   │   │   ├── value_mapping.xml
│   │   │   └── ...
│   │   └── ...
│   ├── MessageMapping
│   │   ├── messageMapping1TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   └── ...     
│   ├── ScriptCollection
│   │   ├── scriptCollection1TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   └── ...     
├── package2TechnicalName
│   ├── IntegrationFlow
│   │   ├── iFlow3TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   ├── iFlow4TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   │   
│   │   │   └── ...  
│   │   └── ...     
│   ├── ValueMapping
│   │   ├── valueMapping2TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF
│   │   │   ├── value_mapping.xml
│   │   │   └── ...
│   │   └── ...
│   ├── MessageMapping
│   │   ├── messageMapping2TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   └── ...     
│   ├── ScriptCollection
│   │   ├── scriptCollection2TechnicalName
│   │   │   ├── META-INF
│   │   │   │   └── MANIFEST.MF 
│   │   │   ├── src   
│   │   │   │   └── ...
│   │   │   └── ...
│   │   └── ...     
├── ...
├── build.gradle
└── gradle.properties
```
NOTE: If you have old project structure then set `false` value to useSeparateFolderForEachArtifactType property to continue use old structure. 
If you want to use new structure follow these steps, new structure allows processing artifacts with different types and the same names.
1) Set `true` value to useSeparateFolderForEachArtifactType property.
2) Move move-artifacts script from scripts_of_updating_repository folder to project folder and run it.
3) Compile UpdateSettingsGradle.java and run it with one argument - absolute path to settings.gradle file.

You can download artifact archives from CPI manually and then unpack them to the project or just use `downloadArtifact` task 
to fetch and automatically unpack bundled artifact. Just create a high-level folder structure for needed IFlow: 
`packageTechnicalName/IntegrationFlow/iflowTechnicalName`, 
Value Mapping: `packageTechnicalName/ValueMapping/valueMappingTechnicalName`,
Message Mapping: `packageTechnicalName/MessageMapping/messageMappingTechnicalName`,
Script Collection: `packageTechnicalName/ScriptCollection/scriptCollectionTechnicalName`,register that 
folder as a module in `settings.gradle` (see later) and run `downloadArtifact` task.

build.gradle
```
buildscript {
    repositories {
        maven { url "https://jitpack.io" }
        mavenCentral()
    }
}

plugins {
    id 'com.figaf.cpi-plugin' version '2.11.RELEASE' apply false
}

configure(subprojects.findAll()) { sub ->

    apply plugin: 'idea'
    apply plugin: 'groovy'

    repositories {
        mavenLocal()
        mavenCentral()
    }
    
    if (sub.name.startsWith("iflow-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        sourceSets {
            test {
                groovy {
                    srcDirs = ['src/test/groovy','src/main/resources/script']
                }
            }
        }

        dependencies {
            testImplementation project(":common")
            testImplementation fileTree(dir: 'src/main/resources/lib', include: '*.jar')
        }

        test {
            dependsOn ':common:test'
            useJUnitPlatform()
        }

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            loginPageUrl = "$project.loginPageUrl"
            ssoUrl = "$project.ssoUrl"
            useCustomIdp = "$project.useCustomIdp".toBoolean()
            samlUrl = "$project.samlUrl"
            idpName = "$project.idpName"
            idpApiClientId = "$project.idpApiClientId"
            idpApiClientSecret = "$project.idpApiClientSecret"
            oauthTokenUrl = "$project.oauthTokenUrl"
            authenticationType = "$project.authenticationType"
            publicApiUrl = "$project.publicApiUrl"
            publicApiClientId = "$project.publicApiClientId"
            publicApiClientSecret = "$project.publicApiClientSecret"
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "CPI_IFLOW"
            useSeparateFolderForEachArtifactType = true
            httpClientsFactory = new com.figaf.integration.common.factory.HttpClientsFactory(
                project.hasProperty('connectionSettings.useProxyForConnections') ? project.property('connectionSettings.useProxyForConnections').toBoolean() : false,
                project.hasProperty('connectionSettings.connectionRequestTimeout') ? project.property('connectionSettings.connectionRequestTimeout').toInteger() : 300000,
                project.hasProperty('connectionSettings.connectTimeout') ? project.property('connectionSettings.connectTimeout').toInteger() : 300000,
                project.hasProperty('connectionSettings.socketTimeout') ? project.property('connectionSettings.socketTimeout').toInteger() : 300000
            )
        }

    } else if (sub.name.startsWith("vm-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            loginPageUrl = "$project.loginPageUrl"
            ssoUrl = "$project.ssoUrl"
            useCustomIdp = "$project.useCustomIdp".toBoolean()
            samlUrl = "$project.samlUrl"
            idpName = "$project.idpName"
            idpApiClientId = "$project.idpApiClientId"
            idpApiClientSecret = "$project.idpApiClientSecret"
            oauthTokenUrl = "$project.oauthTokenUrl"
            authenticationType = "$project.authenticationType"
            publicApiUrl = "$project.publicApiUrl"
            publicApiClientId = "$project.publicApiClientId"
            publicApiClientSecret = "$project.publicApiClientSecret"
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "VALUE_MAPPING"
            useSeparateFolderForEachArtifactType = true
        }
    } else if (sub.name.startsWith("sc-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            loginPageUrl = "$project.loginPageUrl"
            ssoUrl = "$project.ssoUrl"
            useCustomIdp = "$project.useCustomIdp".toBoolean()
            samlUrl = "$project.samlUrl"
            idpName = "$project.idpName"
            idpApiClientId = "$project.idpApiClientId"
            idpApiClientSecret = "$project.idpApiClientSecret"
            oauthTokenUrl = "$project.oauthTokenUrl"
            authenticationType = "$project.authenticationType"
            publicApiUrl = "$project.publicApiUrl"
            publicApiClientId = "$project.publicApiClientId"
            publicApiClientSecret = "$project.publicApiClientSecret"
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "SCRIPT_COLLECTION"
            useSeparateFolderForEachArtifactType = true
        }
    } else if (sub.name.startsWith("mm-")) {

        apply plugin: 'com.figaf.cpi-plugin'

        cpiPlugin {
            url = cpiUrl
            username = cpiUsername
            password = cpiPassword
            platformType = cloudPlatformType
            loginPageUrl = "$project.loginPageUrl"
            ssoUrl = "$project.ssoUrl"
            useCustomIdp = "$project.useCustomIdp".toBoolean()
            samlUrl = "$project.samlUrl"
            idpName = "$project.idpName"
            idpApiClientId = "$project.idpApiClientId"
            idpApiClientSecret = "$project.idpApiClientSecret"
            oauthTokenUrl = "$project.oauthTokenUrl"
            authenticationType = "$project.authenticationType"
            publicApiUrl = "$project.publicApiUrl"
            publicApiClientId = "$project.publicApiClientId"
            publicApiClientSecret = "$project.publicApiClientSecret"
            waitForStartup = true
            sourceFilePath = "$project.projectDir".toString()
            uploadDraftVersion = true
            artifactType = "CPI_MESSAGE_MAPPING"
            useSeparateFolderForEachArtifactType = true
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
project (":package1TechnicalName:iflow-iFlow1TechnicalName").projectDir = file("package1TechnicalName/IntegrationFlow/iFlow1TechnicalName")

include "package1TechnicalName:iflow-iFlow2TechnicalName"
project (":package1TechnicalName:iflow-iFlow2TechnicalName").projectDir = file("package1TechnicalName/IntegrationFlow/iFlow2TechnicalName")

include "package2TechnicalName:iflow-iFlow3TechnicalName"
project (":package2TechnicalName:iflow-iFlow3TechnicalName").projectDir = file("package2TechnicalName/IntegrationFlow/iFlow3TechnicalName")

include "package2TechnicalName:iflow-iFlow4TechnicalName"
project (":package2TechnicalName:iflow-iFlow4TechnicalName").projectDir = file("package2TechnicalName/IntegrationFlow/iFlow4TechnicalName")

include "package2TechnicalName:vm-valueMappingTechnicalName"
project (":package2TechnicalName:vm-valueMappingTechnicalName").projectDir = file("package2TechnicalName/ValueMapping/valueMappingTechnicalName")

include "package2TechnicalName:mm-messageMappingTechnicalName"
project (":package2TechnicalName:mm-messageMappingTechnicalName").projectDir = file("package2TechnicalName/MessageMapping/messageMappingTechnicalName")

include "package2TechnicalName:sc-valueMappingTechnicalName"
project (":package2TechnicalName:sc-scriptCollectionTechnicalName").projectDir = file("package2TechnicalName/ScriptCollection/scriptCollectionTechnicalName")
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
1. `uploadArtifact` - builds bundled model of artifact and uploads it to CPI.
2. `deployArtifact` - deploys artifact on CPI. Usually it makes sense to run this task after `uploadArtifact`.
3. `downloadArtifact` - downloads artifact bundled model from CPI and unpacks it to module folder.

## Configuration
The tasks can be configured through an extension `cpiPlugin` which accepts several parameters:
* `url`* - basic path to the CPI agent. Example: `https://pxxxx-tmn.hci.eu1.hana.ondemand.com`
* `username`* - CPI username. Example: `S00000000`
* `password`* - CPI password. Example: `123456`
* `platformType`* - Cloud platform type. `NEO` or `CLOUD_FOUNDRY`. Default value: `NEO`.
* `sourceFilePath`* - path to the directory with the artifact. Default value: `$project.projectDir` which means
that root directory of the artifact will be taken. In most cases this parameter shouldn't be overridden but it can be any valid path.
Example: `C:\some\path`
* `artifactType`* - Type of artifact for which the tasks will be executed. `CPI_IFLOW`, `VALUE_MAPPING`, `CPI_MESSAGE_MAPPING`, `SCRIPT_COLLECTION`.
* `packageTechnicalName` - package technical name. By default the name of the parent folder is used. If your project structure is not standard
you can define this parameter directly. Example: `Test`
* `packageExternalId` - package Id on CPI. By default the plugin looks for it in CPI automatically using `packageTechnicalName` but if you know
this value, you can define it directly. Example: `4b551fb3c16442c4b33a8d61e0fd3477`
* `artifactTechnicalName` - artifact technical name. By default the name of the folder is used. If your project structure is not standard
you can define this parameter directly. Example: `MyIFlow`
* `artifactExternalId` - artifact Id on CPI. By default the plugin looks for it in CPI automatically using `artifactTechnicalName` but if you know
this value, you can define it directly. Example: `2f0d56be14c44a50a3c1e5c1bebc23fe`
* `waitForStartup` - used only by `deployArtifact` task. If this parameter is true, the plugin will not only deploy the artifact but also wait until it's successfully started.
Default value: `false`. 
* `ignoreFilesList` - list of files (or directories) which shouldn't be added to the archive when the plugin executes `uploadArtifact` task and shouldn't be modified when the plugin executes `downloadArtifact` task.
The plugin always adds to this list the following paths: `src/test`, `build.gradle`, `gradle.properties`, `settings.gradle`. Example: `["somefile.txt", "somefolder"]`
* `uploadDraftVersion` - used only by `uploadArtifact` task. If true it will upload the artifact with "Draft" version number. If false it will use
the number from MANIFEST file. Default value: `false`.
* `httpClientsFactory` - configuration for http requests. Its constructor has the following parameters: `useProxyForConnections`, `connectionRequestTimeout`, `connectTimeout`, `socketTimeout`.
If not provided it will use the following default values: `false`, `300000`, `300000`, `300000`.
* `useSeparateFolderForEachArtifactType`* - if true then package folder should contain types folders with artifacts, if false then package folder should contain only artifacts. Default value: `false`.
