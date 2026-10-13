param([string]$InstanceDirectory=(Join-Path $env:USERPROFILE 'curseforge/minecraft/Instances/26.3'),[string]$DestinationDirectory='')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
if(!$DestinationDirectory){$DestinationDirectory=Join-Path $PSScriptRoot '../patched-shaderpacks'}
New-Item -ItemType Directory -Force -Path $DestinationDirectory | Out-Null
$ridgeShader=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'ridgeClouds.glsl'))
$manifest=@()
foreach($pack in Get-ChildItem (Join-Path $InstanceDirectory 'shaderpacks') -Filter 'ComplementaryUnbound_r5.9.3_CinematicWater.zip') {
    $source=[IO.Compression.ZipFile]::OpenRead($pack.FullName)
    try {
        $entry=$source.GetEntry('shaders/lib/atmospherics/clouds/mainClouds.glsl');if(!$entry){throw "Unsupported layout: $($pack.Name)"}
        $reader=[IO.StreamReader]::new($entry.Open());$main=$reader.ReadToEnd();$originalMain=$main;$reader.Dispose()
        if($main -notmatch 'cameraPosOffset'){throw "Expected Complementary r5.9.3 signature: $($pack.Name)"}
        $includes='(?s)#ifdef CLOUDS_REIMAGINED\s*#include "/lib/atmospherics/clouds/reimaginedClouds.glsl"\s*#endif\s*#ifdef CLOUDS_UNBOUND\s*#include "/lib/atmospherics/clouds/unboundClouds.glsl"\s*#endif'
        if(![regex]::IsMatch($main,$includes)){throw "Cloud include anchor missing: $($pack.Name)"}
        $main=[regex]::Replace($main,$includes,'#include "/lib/atmospherics/clouds/ridgeClouds.glsl"',1)
        $camera='(?s)#if IRIS_VERSION >= 10800.*?cameraPos \+= cameraPosOffset;'
        if(![regex]::IsMatch($main,$camera)){throw "Camera anchor missing: $($pack.Name)"}
        $cameraReplacement=@'
vec3 cameraPos = vec3(vec2(cameraPositionInt.xz - ridgeOrigin).x + cameraPositionBestFract.x,
                              cameraPosition.y,
                              vec2(cameraPositionInt.xz - ridgeOrigin).y + cameraPositionBestFract.z);
    cameraPos += cameraPosOffset;
'@
        $main=[regex]::Replace($main,$camera,$cameraReplacement,1)
        $main=$main.Replace('vec4 clouds = vec4(0.0);',"vec4 clouds = vec4(0.0);`n    if (ridgeParams.y < 0.5) return clouds;")
        $main="#if CLOUD_STYLE_DEFINE == 4`n"+$main+"`n#else`n"+$originalMain+"`n#endif`n";
        $outName='CinematicDrift_DynamicClouds.zip';$outPath=Join-Path $DestinationDirectory $outName
        if(Test-Path $outPath){Remove-Item -LiteralPath $outPath}
        $target=[IO.Compression.ZipFile]::Open($outPath,[IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach($item in $source.Entries){if($item.FullName.EndsWith('/')){continue}
                $new=$target.CreateEntry($item.FullName,[IO.Compression.CompressionLevel]::Optimal)
                $stream=$new.Open()
                if($item.FullName -eq 'shaders/lib/atmospherics/clouds/mainClouds.glsl'){$bytes=[Text.Encoding]::UTF8.GetBytes($main);$stream.Write($bytes,0,$bytes.Length)}
                elseif($item.FullName -eq 'shaders/program/gbuffers_clouds.glsl'){
$reader=[IO.StreamReader]::new($item.Open());$vanilla=$reader.ReadToEnd();$reader.Dispose();$vanilla=$vanilla.Replace('#if CLOUD_STYLE_DEFINE == 50','#if CLOUD_STYLE_DEFINE == 50 && defined OVERWORLD').Replace('#if CLOUD_STYLE_DEFINE != 50','#if CLOUD_STYLE_DEFINE != 50 || !defined OVERWORLD');$bytes=[Text.Encoding]::UTF8.GetBytes($vanilla);$stream.Write($bytes,0,$bytes.Length)
}
elseif($item.FullName -eq 'shaders/lib/common.glsl'){
$reader=[IO.StreamReader]::new($item.Open());$common=$reader.ReadToEnd();$reader.Dispose();$common=$common.Replace('#define CLOUD_STYLE_DEFINE -1 //[-1 0 1 3 50]','#define CLOUD_STYLE_DEFINE 4 //[50 3 4]');$common=[regex]::Replace($common,'(#define SHADER_STYLE)\s+\d+\s*//\[1 4\]','$1 4');$common=$common.Replace('#if CLOUD_STYLE == 3','#if CLOUD_STYLE == 3 || CLOUD_STYLE == 4');$bytes=[Text.Encoding]::UTF8.GetBytes($common);$stream.Write($bytes,0,$bytes.Length)
}
elseif($item.FullName -eq 'shaders/shaders.properties'){
                    $propReader=[IO.StreamReader]::new($item.Open());$props=$propReader.ReadToEnd().Replace("`r","");$propReader.Dispose()
                     $props=[regex]::Replace($props,'(?m)^\s*screen\.INFORMATION=.*\n?','');$props=[regex]::Replace($props,'(?m)^(\s*screen=.*)$',{param($match) $match.Value.Replace('[INFORMATION]','').Replace('SHADER_STYLE','')});$props=[regex]::Replace($props,'(?m)^(\s*sliders\s*=.*)$','$1 RIDGE_DENSITY_MULT RIDGE_MOUNTAIN_WIND RIDGE_WIND_RISE');$props=[regex]::Replace($props,'(?m)^\s*screen\.CLOUD_SETTINGS=.*$','screen.CLOUD_SETTINGS=CLOUD_STYLE_DEFINE CLOUD_ALT1 CLOUD_SPEED_MULT CLOUD_QUALITY CLOUD_UNBOUND_AMOUNT CLOUD_UNBOUND_SIZE_MULT CLOUD_UNBOUND_RAIN_ADD [CLOUD_COLOR_SETTINGS] [RIDGE_CLOUD_SETTINGS]');$props=[regex]::Replace($props,'(?m)^\s*screen\.CLOUD_(UNBOUND|REIMAGINED)_SETTINGS=.*\n?','');$props+="`nscreen.RIDGE_CLOUD_SETTINGS=RIDGE_DENSITY_MULT RIDGE_MOUNTAIN_WIND RIDGE_WIND_RISE`nscreen.RIDGE_CLOUD_SETTINGS.columns=2`n# Ridge Clouds: prevent vanilla and Distant Horizons duplicate clouds.`n#if CLOUD_STYLE_DEFINE == 50`nclouds=fancy`n#else`nclouds=off`n#endif`ndhClouds=off`n"
                    $bytes=[Text.Encoding]::UTF8.GetBytes($props);$stream.Write($bytes,0,$bytes.Length)
                }elseif($item.FullName -eq 'shaders/lang/en_US.lang'){
$reader=[IO.StreamReader]::new($item.Open());$lang=$reader.ReadToEnd().Replace("`r","");$reader.Dispose();$lang=[regex]::Replace($lang,'(?m)^(option.CLOUD_STYLE_DEFINE(?:\.comment)?|value.CLOUD_STYLE_DEFINE\.[^=]+)=.*\n?','');$lang+="`noption.CLOUD_STYLE_DEFINE=Cloud Mode`noption.CLOUD_STYLE_DEFINE.comment=Vanilla Minecraft clouds, original volumetric clouds, or terrain-responsive dynamic clouds.`nvalue.CLOUD_STYLE_DEFINE.50=Vanilla`nvalue.CLOUD_STYLE_DEFINE.3=Volumetric`nvalue.CLOUD_STYLE_DEFINE.4=Dynamic`n";$lang+="`nscreen.RIDGE_CLOUD_SETTINGS=Dynamic Clouds`noption.RIDGE_DENSITY_MULT=Cloud Density Multiplier`noption.RIDGE_DENSITY_MULT.comment=Relative to Complementary and its weather response. 100 preserves the original density.`noption.RIDGE_MOUNTAIN_WIND=Mountain Wind Gain`noption.RIDGE_MOUNTAIN_WIND.comment=Extra wind on exposed high ground. 1 gives up to twice the base speed; 0 disables the gain.`noption.RIDGE_WIND_RISE=Wind Elevation Range`noption.RIDGE_WIND_RISE.comment=Blocks above sea level over which the mountain wind gain reaches its maximum.`n";$bytes=[Text.Encoding]::UTF8.GetBytes($lang);$stream.Write($bytes,0,$bytes.Length)
}else{$inputStream=$item.Open();$inputStream.CopyTo($stream);$inputStream.Dispose()}
                $stream.Dispose()
            }
            $new=$target.CreateEntry('shaders/lib/atmospherics/clouds/ridgeClouds.glsl');$stream=$new.Open();$bytes=[Text.Encoding]::UTF8.GetBytes($ridgeShader);$stream.Write($bytes,0,$bytes.Length);$stream.Dispose()
        }finally{$target.Dispose()}
        $manifest+=[pscustomobject]@{Original=$pack.Name;Patched=$outName;OriginalSHA256=(Get-FileHash $pack.FullName).Hash;PatchedSHA256=(Get-FileHash $outPath).Hash}
        Write-Output "Patched $($pack.Name)"
    } finally {$source.Dispose()}
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $DestinationDirectory 'manifest.json')
