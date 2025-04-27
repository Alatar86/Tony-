@echo off
echo Rebuilding the frontend application...
cd frontend
mvn clean compile
echo.
echo Frontend rebuilt successfully.
echo You can now run the frontend with: mvn javafx:run
echo. 