@echo off
REM Arranca los 3 servicios, cada uno en su propia ventana.
REM Catalogo :8081  |  Inventario :8082  |  Composite API + Dashboard :8080
start "catalog-service :8081" cmd /k mvnw.cmd -pl catalog-service spring-boot:run
start "inventory-service :8082" cmd /k mvnw.cmd -pl inventory-service spring-boot:run
start "composite-api :8080" cmd /k mvnw.cmd -pl composite-api spring-boot:run
