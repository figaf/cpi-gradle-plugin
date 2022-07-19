@echo off
for /d %%p in (*) do (
 echo processing of %%p folder
 for /d %%a in ("%%p\*") do (
  if exist %%a\META-INF\MANIFEST.MF (

   rem search IntegrationFlow
   find /c "SAP-BundleType: IntegrationFlow" %%a\META-INF\MANIFEST.MF > nul && (
    if not exist %%p\IntegrationFlow (mkdir %%p\IntegrationFlow)
	move /y %%a %%p\IntegrationFlow > nul
   ) || (
   
    rem search ValueMapping
    find /c "SAP-BundleType: ValueMapping" %%a\META-INF\MANIFEST.MF > nul && ( 
	 if not exist %%p\ValueMapping (mkdir %%p\ValueMapping)
	 move /y %%a %%p\ValueMapping > nul
    ) || (
	
	  rem search MessageMapping
	  find /c "SAP-BundleType: MessageMapping" %%a\META-INF\MANIFEST.MF > nul && ( 
	   if not exist %%p\MessageMapping (mkdir %%p\MessageMapping)
	   move /y %%a %%p\MessageMapping > nul
	  ) || (
	  
	   rem search ScriptCollection
       find /c "SAP-BundleType: ScriptCollection" %%a\META-INF\MANIFEST.MF > nul && ( 
	    if not exist %%p\ScriptCollection (mkdir %%p\ScriptCollection)
		move /y %%a %%p\ScriptCollection > nul
       )
	  
	  )
	  
	)
	
   )
  )
 )
)
pause
