param([string]$InstanceDirectory=(Join-Path $env:USERPROFILE 'curseforge/minecraft/Instances/26.3'))
$ErrorActionPreference='Stop'
$instance=Get-Content (Join-Path $InstanceDirectory 'minecraftinstance.json') -Raw | ConvertFrom-Json
if($instance.gameVersion -ne '26.3'){throw 'This build requires Minecraft 26.3.'}
$backup=Join-Path $PSScriptRoot ('installation-backup/'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $backup | Out-Null
$irisPath=Join-Path $InstanceDirectory 'config/iris.properties'
Copy-Item $irisPath (Join-Path $backup 'iris.properties')
$configPath=Join-Path $InstanceDirectory 'config/ridge-clouds.json'
if(Test-Path $configPath){Copy-Item $configPath $backup}
$modPath=Join-Path $InstanceDirectory 'mods/ridge-clouds-0.4.0+26.3.jar'
if(Test-Path $modPath){Copy-Item $modPath $backup}
Copy-Item (Join-Path $PSScriptRoot 'ridge-clouds-0.4.0+26.3.jar') $modPath -Force
$installed=@()
foreach($pack in Get-ChildItem (Join-Path $PSScriptRoot 'patched-shaderpacks') -Filter '*.zip'){
    $target=Join-Path $InstanceDirectory ('shaderpacks/'+$pack.Name)
    if(Test-Path $target){Copy-Item $target $backup}
    Copy-Item $pack.FullName $target -Force
    $originalName='ComplementaryUnbound_r5.9.3_CinematicWater.zip'
    $optionsPath=Join-Path $InstanceDirectory ('shaderpacks/'+$originalName+'.txt')
    $options=if(Test-Path $optionsPath){[IO.File]::ReadAllText($optionsPath)}else{''}
    foreach($setting in @('CLOUD_STYLE_DEFINE','CLOUD_SHADOWS','DOUBLE_REIM_CLOUDS')){$options=[regex]::Replace($options,"(?m)^$setting=.*(?:\r?\n|$)",'')}
    $options+="`nCLOUD_STYLE_DEFINE=4`nCLOUD_SHADOWS=false`nDOUBLE_REIM_CLOUDS=false`n"
    if($options -match '(?m)^CLOUD_QUALITY=0\s*$'){$options=[regex]::Replace($options,'(?m)^CLOUD_QUALITY=0\s*$','CLOUD_QUALITY=2')}
    [IO.File]::WriteAllText($target+'.txt',$options)
    $installed+=[pscustomobject]@{Name=$pack.Name;SHA256=(Get-FileHash $target).Hash}
}
if(!(Test-Path $configPath)){Copy-Item (Join-Path $PSScriptRoot 'ridge-clouds.json') $configPath}
$iris=[IO.File]::ReadAllText($irisPath)
$match=[regex]::Match($iris,'(?m)^shaderPack=(.*)\r?$')
$selected=$match.Groups[1].Value.Trim()
$patched='CinematicDrift_DynamicClouds.zip'
if(!(Test-Path (Join-Path $InstanceDirectory ('shaderpacks/'+$patched)))){throw "Patched active pack missing: $patched"}
$iris=[regex]::Replace($iris,'(?m)^shaderPack=.*',('shaderPack='+$patched))
[IO.File]::WriteAllText($irisPath,$iris)
[pscustomobject]@{Instance=$InstanceDirectory;Mod='ridge-clouds-0.4.0+26.3.jar';ModSHA256=(Get-FileHash $modPath).Hash;SelectedPack=$patched;Backup=$backup;Packs=$installed} | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $PSScriptRoot 'installation.json')
Write-Output "Installed Ridge Clouds with $($installed.Count) shader variants; selected $patched"
