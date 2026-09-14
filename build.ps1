param([string]$Jdk = 'D:\.jdks\ms-21.0.7')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$timer = [Diagnostics.Stopwatch]::StartNew()
# 每次使用独立输出目录，避免残留 class 混入成品。
$output = Join-Path $PSScriptRoot ('build\run-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff'))
$classes = Join-Path $output 'classes'
New-Item -ItemType Directory -Force $classes | Out-Null
$dependencies = @(Get-Content '.deps\classpath-local.txt') + @(
    (Get-Item '.deps\cobblemon-1.7.0.jar').FullName,
    (Get-Item '.deps\architectury-13.0.8.jar').FullName
)
foreach ($dependency in $dependencies) {
    if (!(Test-Path -LiteralPath $dependency)) { throw "缺失依赖：$dependency" }
}
$arguments = @('--release', '21', '-encoding', 'UTF-8', '-proc:none', '-d', ('"' + $classes.Replace('\','/') + '"'), '-classpath', ('"' + (($dependencies -join ';').Replace('\','/')) + '"'))
$arguments += Get-ChildItem 'src\main\java' -Recurse -Filter '*.java' | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
$argfile = Join-Path $output 'javac.args'
$arguments | Set-Content $argfile -Encoding utf8
Write-Progress -Activity '重新构建' -Status '编译 Java 21 源码' -PercentComplete 0
& "$Jdk\bin\javac.exe" '-J-Duser.language=en' "@$argfile" *> (Join-Path $output 'compile.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $output 'compile.log'); throw '主源码编译失败' }
Write-Host "1/3 主源码编译完成，JDK 21，结束时间 $(Get-Date -Format s)，预计剩余 $([math]::Round($timer.Elapsed.TotalSeconds * 0.3)) 秒"
Write-Progress -Activity '重新构建' -Status '编译 Java 9 多版本类' -PercentComplete 70
& "$Jdk\bin\javac.exe" '-J-Duser.language=en' --release 9 -encoding UTF-8 -proc:none -d "$classes\META-INF\versions\9" 'src\java9\java\Logger.java' *> (Join-Path $output 'compile-java9.log')
if ($LASTEXITCODE -ne 0) { Get-Content (Join-Path $output 'compile-java9.log'); throw '多版本源码编译失败' }
Write-Host "2/3 多版本源码编译完成，release 9，结束时间 $(Get-Date -Format s)"
Copy-Item 'src\main\resources\*' -Destination $classes -Recurse -Force
New-Item -ItemType Directory -Force 'build\libs' | Out-Null
& "$Jdk\bin\jar.exe" --create --file 'build\libs\cobblebattle-neoforge-0.1.3-rebuilt.jar' --no-manifest -C $classes .
if ($LASTEXITCODE -ne 0) { throw '打包失败' }
Write-Progress -Activity '重新构建' -Completed
Write-Host "3/3 构建完成，结束时间 $(Get-Date -Format s)，耗时 $([math]::Round($timer.Elapsed.TotalSeconds, 1)) 秒，日志：$output"
