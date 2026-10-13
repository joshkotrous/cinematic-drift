param([string]$MinecraftDirectory="$env:APPDATA/.minecraft",[string]$InstanceDirectory=(Join-Path $env:USERPROFILE 'curseforge/minecraft/Instances/26.3'),[string]$JavaHome='C:/Program Files/Android/Android Studio/jbr')
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$buildPath=Join-Path $projectRoot 'build'
New-Item -ItemType Directory -Force -Path "$buildPath/classes" | Out-Null
$version=Get-Content (Join-Path $MinecraftDirectory 'versions/26.3/26.3.json') -Raw | ConvertFrom-Json
$jars=@((Join-Path $MinecraftDirectory 'versions/26.3/26.3.jar'))
foreach($lib in $version.libraries){if($lib.downloads.artifact){$jar=Join-Path "$MinecraftDirectory/libraries" $lib.downloads.artifact.path;if(Test-Path $jar){$jars+=$jar}}}
$jars+=Get-ChildItem "$MinecraftDirectory/libraries/net/fabricmc/fabric-loader/0.19.5" -Filter '*.jar' | ForEach-Object FullName
$jars+=Get-ChildItem "$MinecraftDirectory/libraries/net/fabricmc/sponge-mixin/0.17.4+mixin.0.8.7" -Filter '*.jar' | ForEach-Object FullName
$jars+=Join-Path $InstanceDirectory 'mods/iris-fabric-1.11.7+mc26.3.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
New-Item -ItemType Directory -Force -Path "$buildPath/deps" | Out-Null
$apiArchive=[IO.Compression.ZipFile]::OpenRead((Join-Path $InstanceDirectory 'mods/fabric-api-0.162.0+26.3.jar'))
try {foreach($entry in $apiArchive.Entries | Where-Object FullName -Match 'META-INF/jars/fabric-(command-api|api-base).*jar'){$dest=Join-Path "$buildPath/deps" ([IO.Path]::GetFileName($entry.FullName));[IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$dest,$true);$jars+=$dest}}finally{$apiArchive.Dispose()}
$sources=Get-ChildItem "$projectRoot/src/main/java" -Recurse -Filter '*.java' | ForEach-Object FullName
& "$JavaHome/bin/javac.exe" --release 25 -proc:none -classpath ($jars -join ';') -d "$buildPath/classes" @sources
if($LASTEXITCODE -ne 0){throw 'Java compilation failed'}
Copy-Item "$projectRoot/src/main/resources/*" "$buildPath/classes" -Recurse -Force
& "$JavaHome/bin/jar.exe" --create --file "$projectRoot/../ridge-clouds-0.4.0+26.3.jar" -C "$buildPath/classes" .
if($LASTEXITCODE -ne 0){throw 'Jar packaging failed'}
Write-Output 'Built ridge-clouds-0.4.0+26.3.jar'
