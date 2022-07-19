#!/bin/sh
for dir in ./* 
do
 if [ -d "$dir" ]
 then
  echo processing of $dir folder
  for artifact in $dir/*
  do
   if [ -f "$artifact/META-INF/MANIFEST.MF" ]
   then

    #search IntegrationFlow
    count=$(grep -c "SAP-BundleType: IntegrationFlow" $artifact/META-INF/MANIFEST.MF)
    if [ $count -gt 0 ]
    then
     if ! [ -d "$dir/IntegrationFlow" ]; then mkdir "$dir/IntegrationFlow"; fi;
     mv "$artifact" "$dir/IntegrationFlow/"
     continue
    fi

    #search ValueMapping
    count=$(grep -c "SAP-BundleType: ValueMapping" $artifact/META-INF/MANIFEST.MF)
    if [ $count -gt 0 ]
    then
     if ! [ -d "$dir/ValueMapping" ]; then mkdir "$dir/ValueMapping"; fi;
     mv "$artifact" "$dir/ValueMapping/"
     continue
    fi

    #search MessageMapping
    count=$(grep -c "SAP-BundleType: MessageMapping" $artifact/META-INF/MANIFEST.MF)
    if [ $count -gt 0 ]
    then
     if ! [ -d "$dir/MessageMapping" ]; then mkdir "$dir/MessageMapping"; fi;
     mv "$artifact" "$dir/MessageMapping/"
     continue
    fi

    #search ScriptCollection
    count=$(grep -c "SAP-BundleType: ScriptCollection" $artifact/META-INF/MANIFEST.MF)
    if [ $count -gt 0 ]
    then
     if ! [ -d "$dir/ScriptCollection" ]; then mkdir "$dir/ScriptCollection"; fi;
     mv "$artifact" "$dir/ScriptCollection/"
     continue
    fi

   fi
  done
 fi
done
