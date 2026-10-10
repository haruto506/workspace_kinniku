param([string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest', 'lintDebug'))
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (Test-Path '.tools/java') { $env:JAVA_HOME = (Get-ChildItem '.tools/java' -Directory | Select-Object -First 1).FullName }
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.tools/gradle-home'
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.tools/android-user'
if (Test-Path './gradlew.bat') { & ./gradlew.bat @Tasks --console=plain }
elseif (Test-Path '.tools/gradle/gradle-8.9/bin/gradle.bat') { & '.tools/gradle/gradle-8.9/bin/gradle.bat' @Tasks --console=plain }
else { throw 'Android Studio または Gradle 8.9 と JDK 17 を用意してください。README.md を参照してください。' }
exit $LASTEXITCODE
