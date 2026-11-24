@echo off
REM Alternativa sin NetBeans: compila y ejecuta (requiere JDK instalado)
if not exist out mkdir out
javac -encoding UTF-8 -d out src\edo\*.java
if errorlevel 1 pause & exit /b 1
java -cp out edo.Main
