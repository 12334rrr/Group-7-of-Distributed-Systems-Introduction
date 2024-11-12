@echo off
setlocal

REM 定义相关路径
set SOURCE_DIR=G:\大三上\分布式系统导论\实验\实验一\实验1资料（含参考代码)\分布式应用设计与开发\JAVA代码\SP_C7_Jv\Java\TimeServiceClient_Library
set DEST_DIR=G:\大三上\分布式系统导论\实验\实验一\实验1资料（含参考代码)\分布式应用设计与开发\JAVA代码\SP_C7_Jv\Java\TimeServiceClient_GUI_uses_library\dist\lib
set JAR_FILE=TimeServiceClient_Library.jar

REM 切换到源目录
cd /d "%SOURCE_DIR%"

REM 执行 ant clean 和 ant jar
echo Running ant clean...
ant clean
if %errorlevel% neq 0 (
    echo "ant clean failed. Exiting."
    pause
    exit /b %errorlevel%
)

echo Running ant jar...
ant jar
if %errorlevel% neq 0 (
    echo "ant jar failed. Exiting."
    pause
    exit /b %errorlevel%
)

REM 检查 JAR 文件是否生成成功
if not exist "%SOURCE_DIR%\dist\%JAR_FILE%" (
    echo "JAR file not found in the source directory. Exiting."
    pause
    exit /b 1
)

REM 创建目标目录（如果不存在）
if not exist "%DEST_DIR%" (
    mkdir "%DEST_DIR%"
)

REM 复制 JAR 文件到目标目录
echo Copying JAR file to destination directory...
copy "%SOURCE_DIR%\dist\%JAR_FILE%" "%DEST_DIR%"
if %errorlevel% neq 0 (
    echo "Copy failed. Exiting."
    pause
    exit /b %errorlevel%
)

echo "Build and copy completed successfully."
endlocal

REM 保持窗口打开
cmd /k
