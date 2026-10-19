@echo off
REM Compila todo y arranca el coordinador en http://localhost:8080
call mvn -q -DskipTests package
java -jar hk-coordinator\target\hk-coordinator-0.1.0-SNAPSHOT.jar
