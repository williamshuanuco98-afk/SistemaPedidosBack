$targetJars = @(
  "org\springframework\boot\spring-boot\3.2.5\spring-boot-3.2.5.jar",
  "org\springframework\boot\spring-boot-autoconfigure\3.2.5\spring-boot-autoconfigure-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-web\3.2.5\spring-boot-starter-web-3.2.5.jar",
  "org\apache\tomcat\embed\tomcat-embed-core\10.1.20\tomcat-embed-core-10.1.20.jar",
  "org\springframework\boot\spring-boot-starter-jdbc\3.2.5\spring-boot-starter-jdbc-3.2.5.jar",
  "org\springframework\spring-web\6.1.6\spring-web-6.1.6.jar",
  "org\springframework\spring-webmvc\6.1.6\spring-webmvc-6.1.6.jar",
  "org\springframework\spring-context\6.1.6\spring-context-6.1.6.jar",
  "org\springframework\spring-core\6.1.6\spring-core-6.1.6.jar",
  "org\springframework\spring-beans\6.1.6\spring-beans-6.1.6.jar",
  "org\springframework\spring-jdbc\6.1.6\spring-jdbc-6.1.6.jar",
  "org\springframework\spring-tx\6.1.6\spring-tx-6.1.6.jar",
  "jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar",
  "com\github\librepdf\openpdf\1.3.40\openpdf-1.3.40.jar",
  "com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar",
  "com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar",
  "org\slf4j\slf4j-api\2.0.13\slf4j-api-2.0.13.jar",
  "com\fasterxml\jackson\core\jackson-databind\2.15.4\jackson-databind-2.15.4.jar",
  "com\fasterxml\jackson\core\jackson-core\2.15.4\jackson-core-2.15.4.jar",
  "com\fasterxml\jackson\core\jackson-annotations\2.15.4\jackson-annotations-2.15.4.jar",
  "org\apache\poi\poi\5.2.4\poi-5.2.4.jar",
  "org\apache\poi\poi-ooxml\5.2.4\poi-ooxml-5.2.4.jar",
  "org\apache\poi\poi-ooxml-lite\5.2.4\poi-ooxml-lite-5.2.4.jar",
  "org\apache\xmlbeans\xmlbeans\5.1.1\xmlbeans-5.1.1.jar",
  "org\apache\commons\commons-compress\1.24.0\commons-compress-1.24.0.jar",
  "org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar",
  "commons-io\commons-io\2.15.1\commons-io-2.15.1.jar"
)

$m2Base = "C:\Users\User\.m2\repository"
$cpList = @()
foreach ($j in $targetJars) {
  $p = Join-Path $m2Base $j
  if (Test-Path $p) {
    $cpList += $p
  } else {
    # Find matching jar
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($j).Split('-')[0]
    $found = Get-ChildItem -Path $m2Base -Recurse -Filter "*$baseName*.jar" | Select-Object -First 1
    if ($found) { $cpList += $found.FullName }
  }
}

$cpString = $cpList -join ";"
$sources = (Get-ChildItem -Path src\main\java -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
Set-Content -Path "sources.txt" -Value $sources
Set-Content -Path "options.txt" -Value @("-parameters", "--release", "21", "-cp", $cpString, "-d", "target/classes")

javac "@options.txt" "@sources.txt"
if ($LASTEXITCODE -eq 0) {
  if (Test-Path "src\main\resources") {
    Copy-Item -Path "src\main\resources\*" -Destination "target\classes\" -Recurse -Force
  }
  Write-Host ">>> COMPILATION SUCCEEDED! <<<"
} else {
  Write-Host ">>> COMPILATION FAILED! <<<"
}
