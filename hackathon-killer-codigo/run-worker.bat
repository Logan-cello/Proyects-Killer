@echo off
REM Uso: run-worker.bat "Agent B" DEVELOPER MANUAL
java -jar hk-worker\target\hk-worker-0.1.0-SNAPSHOT.jar --name %1 --role %2 --adapter %3 --repo . --test-cmd "mvn -q test"
