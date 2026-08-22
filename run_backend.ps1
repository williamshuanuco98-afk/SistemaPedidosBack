Set-Location $PSScriptRoot

# Stop any existing process on port 8080
$conns = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
foreach ($c in $conns) {
  if ($c.OwningProcess) {
    Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
  }
}
Start-Sleep -Seconds 1

# Auto-compilar automáticamente si hubo cambios en código Java o Frontend
& "$PSScriptRoot\compile_backend.ps1"

$m2Base = "$env:USERPROFILE\.m2\repository"


# List of required runtime jars (without legacy commons-logging)
$requiredJars = @(
  "org\springframework\boot\spring-boot\3.2.5\spring-boot-3.2.5.jar",
  "org\springframework\boot\spring-boot-autoconfigure\3.2.5\spring-boot-autoconfigure-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter\3.2.5\spring-boot-starter-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-web\3.2.5\spring-boot-starter-web-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-json\3.2.5\spring-boot-starter-json-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-tomcat\3.2.5\spring-boot-starter-tomcat-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-jdbc\3.2.5\spring-boot-starter-jdbc-3.2.5.jar",
  "org\springframework\boot\spring-boot-starter-logging\3.2.5\spring-boot-starter-logging-3.2.5.jar",
  "org\springframework\spring-web\6.1.6\spring-web-6.1.6.jar",
  "org\springframework\spring-webmvc\6.1.6\spring-webmvc-6.1.6.jar",
  "org\springframework\spring-context\6.1.6\spring-context-6.1.6.jar",
  "org\springframework\spring-core\6.1.6\spring-core-6.1.6.jar",
  "org\springframework\spring-beans\6.1.6\spring-beans-6.1.6.jar",
  "org\springframework\spring-aop\6.1.6\spring-aop-6.1.6.jar",
  "org\springframework\spring-expression\6.1.6\spring-expression-6.1.6.jar",
  "org\springframework\spring-jdbc\6.1.6\spring-jdbc-6.1.6.jar",
  "org\springframework\spring-tx\6.1.6\spring-tx-6.1.6.jar",
  "org\springframework\spring-jcl\6.1.6\spring-jcl-6.1.6.jar",
  "org\apache\tomcat\embed\tomcat-embed-core\10.1.20\tomcat-embed-core-10.1.20.jar",
  "org\apache\tomcat\embed\tomcat-embed-el\10.1.20\tomcat-embed-el-10.1.20.jar",
  "org\apache\tomcat\embed\tomcat-embed-websocket\10.1.20\tomcat-embed-websocket-10.1.20.jar",
  "jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar",
  "com\github\librepdf\openpdf\1.3.40\openpdf-1.3.40.jar",
  "com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar",
  "com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar",
  "org\slf4j\slf4j-api\2.0.13\slf4j-api-2.0.13.jar",
  "ch\qos\logback\logback-classic\1.4.14\logback-classic-1.4.14.jar",
  "ch\qos\logback\logback-core\1.4.14\logback-core-1.4.14.jar",
  "org\apache\logging\log4j\log4j-to-slf4j\2.21.1\log4j-to-slf4j-2.21.1.jar",
  "org\apache\logging\log4j\log4j-api\2.21.1\log4j-api-2.21.1.jar",
  "org\slf4j\jul-to-slf4j\2.0.13\jul-to-slf4j-2.0.13.jar",
  "org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar",
  "io\micrometer\micrometer-observation\1.12.5\micrometer-observation-1.12.5.jar",
  "io\micrometer\micrometer-commons\1.12.5\micrometer-commons-1.12.5.jar",
  "com\fasterxml\jackson\core\jackson-databind\2.15.4\jackson-databind-2.15.4.jar",
  "com\fasterxml\jackson\core\jackson-core\2.15.4\jackson-core-2.15.4.jar",
  "com\fasterxml\jackson\core\jackson-annotations\2.15.4\jackson-annotations-2.15.4.jar",
  "com\fasterxml\jackson\datatype\jackson-datatype-jdk8\2.15.4\jackson-datatype-jdk8-2.15.4.jar",
  "com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.15.4\jackson-datatype-jsr310-2.15.4.jar",
  "com\fasterxml\jackson\module\jackson-module-parameter-names\2.15.4\jackson-module-parameter-names-2.15.4.jar",
  "org\apache\poi\poi\5.2.4\poi-5.2.4.jar",
  "org\apache\poi\poi-ooxml\5.2.4\poi-ooxml-5.2.4.jar",
  "org\apache\poi\poi-ooxml-lite\5.2.4\poi-ooxml-lite-5.2.4.jar",
  "org\apache\xmlbeans\xmlbeans\5.1.1\xmlbeans-5.1.1.jar",
  "org\apache\commons\commons-compress\1.24.0\commons-compress-1.24.0.jar",
  "org\apache\commons\commons-collections4\4.4\commons-collections4-4.4.jar",
  "commons-io\commons-io\2.15.1\commons-io-2.15.1.jar"
)

$cpList = @("target/classes")
foreach ($j in $requiredJars) {
  $p = Join-Path $m2Base $j
  if (Test-Path $p) {
    $cpList += $p
  } else {
    $fileName = [System.IO.Path]::GetFileName($j)
    $found = Get-ChildItem -Path $m2Base -Recurse -Filter $fileName | Select-Object -First 1
    if ($found) { $cpList += $found.FullName }
  }
}

$cpString = $cpList -join ";"
Write-Host "Starting Spring Boot on port 8080..."
java -cp "$cpString" com.inplabel.pedidos.PedidosApplication



