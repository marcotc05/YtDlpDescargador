$files = @{
    "app\proguard-rules.pro" = @"
-keep class com.yausername.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class org.apache.commons.io.** { *; }
-keep class org.apache.commons.lang3.** { *; }
-keep class java.util.zip.** { *; }
-keep class java.util.jar.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn java.beans.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.apache.commons.**
-dontwarn org.slf4j.**
-dontwarn com.fasterxml.jackson.databind.ext.**
"@

    "app\src\main\res\values\strings.xml" = @"
<resources>
    <string name="app_name">The Downloader of All</string>
</resources>
"@

    "app\src\main\res\drawable\ic_launcher_background.xml" = @"
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#14163A" android:pathData="M0,0h108v108h-108z" /></vector>
"@

    "app\src\main\res\drawable\ic_launcher_foreground.xml" = @"
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108"><path android:fillColor="#FFFFFF" android:pathData="M38,28 L85,54 L38,80 Z" /><path android:fillColor="#3A35F0" android:pathData="M58,48 L58,62 M52,56 L58,62 L64,56" android:strokeColor="#3A35F0" android:strokeWidth="4" android:strokeLineCap="round" android:strokeLineJoin="round" /></vector>
"@

    "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"@
}

foreach ($file in $files.Keys) {
    $dir = Split-Path $file -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    Set-Content -Path $file -Value $files[$file] -Encoding utf8
}
Copy-Item "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" "app\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml" -Force

# Parche a AndroidManifest
$manifestPath = "app\src\main\AndroidManifest.xml"
$manifest = Get-Content $manifestPath -Raw
if ($manifest -notmatch 'extractNativeLibs="true"') {
    $manifest = $manifest -replace '<application', '<application android:extractNativeLibs="true"'
    Set-Content -Path $manifestPath -Value $manifest -Encoding utf8
}

# Parche a build.gradle.kts
$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
if ($gradle -notmatch 'useLegacyPackaging = true') {
    $gradle = $gradle -replace 'jniLibs\s*\{', 'jniLibs { useLegacyPackaging = true'
    Set-Content -Path $gradlePath -Value $gradle -Encoding utf8
}

# Parche a DownloadManager (Paridad .bat)
$dmPath = "app\src\main\java\app\ytdlp\descargador\engine\DownloadManager.kt"
if (Test-Path $dmPath) {
    $dm = Get-Content $dmPath -Raw
    $dm = $dm -replace '(job\.options\.forEach\s*\{[^}]+\})', @"
$1
        val hasFormat = job.options.any { it.contains("-f") }
        if (!hasFormat) {
            request.addOption("-f", "bestvideo+bestaudio/best")
            request.addOption("--merge-output-format", "mp4")
        }
"@
    Set-Content -Path $dmPath -Value $dm -Encoding utf8
}

git add -A
git commit -m "fix(v3.0): R8 native libs, paridad .bat, branding e iconos"
git push origin main
