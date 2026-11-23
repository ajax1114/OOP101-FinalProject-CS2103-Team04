# -------------------------------------------------
# PackAtlases.ps1 – generates the four texture atlases
# -------------------------------------------------

# 1 – Locate the gdx‑tools JAR (Gradle cache)
$gdxJar = Get-ChildItem -Path "$HOME\.gradle\caches\modules-2\files-2.1" `
                         -Recurse -Filter "gdx-tools-*.jar" |
          Select-Object -First 1 -ExpandProperty FullName
if (-not $gdxJar) {
    Write-Error "gdx‑tools JAR not found – run the check in step 1 first."
    exit 1
}

# 2 – Helper function that calls TexturePacker
function Pack-Atlas {
    param(
        [Parameter(Mandatory)] [string] $src,   # folder with PNG frames
        [Parameter(Mandatory)] [string] $out,   # destination folder for .atlas/.png
        [Parameter(Mandatory)] [string] $name   # base name of the atlas (e.g. packed_wakwak)
    )
    Write-Host "Packing $src → $out\$name.atlas"
    java -cp $gdxJar com.badlogic.gdx.tools.texturepacker.TexturePacker $src $out $name
    if ($LASTEXITCODE -ne 0) {
        Write-Error "TexturePacker failed for $name"
        exit $LASTEXITCODE
    }
}

# 3 – Root folder that holds the animation sub‑folders
$animRoot = "C:\Users\MISD-NOD-MAIN\Desktop\DungeonBound\assets\ui\anim"

# 4 – Pack each animation set
Pack-Atlas "$animRoot\wakwak"   $animRoot "packed_wakwak"
Pack-Atlas "$animRoot\fireflies" $animRoot "packed_fireflies"
Pack-Atlas "$animRoot\torch"   $animRoot "packed_torch"
Pack-Atlas "$animRoot\moonhalo" $animRoot "packed_moonhalo"

Write-Host "`n[INFO] All atlases generated."