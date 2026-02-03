@echo off
chcp 65001 >nul
echo ====================================
echo 开始复制 JAR 包到 build 目录
echo ====================================
echo.

REM 创建 build 目录（如果不存在）
if not exist "build" mkdir "build"

REM 定义模块列表
set modules=aggregate ai article comment consumer file scheduler user

REM 遍历每个模块
for %%m in (%modules%) do (
    echo 处理模块: %%m
    
    REM 创建模块目录
    if not exist "build\%%m" mkdir "build\%%m"
    
    REM 查找并复制 exec.jar 文件
    if exist "%%m\target\*-exec.jar" (
        copy /Y "%%m\target\*-exec.jar" "build\%%m\"
        if errorlevel 1 (
            echo [错误] 复制 %%m 模块失败
        ) else (
            echo [成功] 已复制 %%m 模块的 JAR 包
        )
    ) else (
        echo [警告] 未找到 %%m 模块的 exec.jar 文件
    )
    echo.
)

echo ====================================
echo 复制完成！
echo ====================================
pause
