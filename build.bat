@echo off
echo 正在编译 UserInfoAPI 插件...
echo.

REM 检查是否安装了Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 Maven，请先安装 Maven 并添加到系统 PATH
    pause
    exit /b 1
)

REM 检查是否安装了Java 17+
java -version 2>&1 | findstr "17\|18\|19\|20\|21" >nul
if %errorlevel% neq 0 (
    echo 警告: 建议安装 Java 17 或更高版本以获得最佳兼容性
)

echo 清理旧的构建文件...
call mvn clean

echo.
echo 编译项目...
call mvn compile

if %errorlevel% neq 0 (
    echo.
    echo 编译失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo 打包插件...
call mvn package

if %errorlevel% neq 0 (
    echo.
    echo 打包失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo 构建成功！
echo.
echo 插件文件位置: target\userinfo-api-1.0.0.jar
echo.
echo 安装方法:
echo 1. 将 target\userinfo-api-1.0.0.jar 复制到你的 Paper 服务器的 plugins 文件夹
echo 2. 重启服务器或重新加载插件
echo.
pause