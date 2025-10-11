@echo off
gradlew clean
gradlew cleanBuildCache
rd /s /q .gradle
rd /s /q app\build
rd /s /q build
echo Clean complete!
pause