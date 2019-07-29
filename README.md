# cpi-gradle-plugin
This plugin provides an integration with SAP CPI platform.

## Tasks
The plugin has 3 tasks
1. `uploadIntegrationFlow` - upload IFlow to CPI
2. `deployIntegrationFlow` - deploy IFlow to CPI. Usually it makes sense to run this task after `uploadIntegrationFlow`
3. `downloadIntegrationFlow` - download IFlow from CPI to the repository.

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
* `uploadDraftVersions` - used only by `uploadIntegrationFlow` task. if true it will upload the IFlow with "Draft" version number. If false it will use
the number from MANIFEST file.