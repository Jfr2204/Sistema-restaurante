# Sistema de Gestión de Restaurante – Proyecto Académico de Bases de Datos
Sistema de escritorio desarrollado en Java como proyecto académico para la asignatura de Base de datos. El sistema permite gestionar información de un restaurante mediante operaciones CRUD, consultas SQL avanzadas e integración entre una interfaz gráfica y una base de datos relacional utilizando JDBC.

# Objetivos del Proyecto
Este proyecto fue desarrollado con fines educativos para aplicar conceptos fundamentales de:
- Modelado relacional
- Diseño de bases de datos
- Integridad referencial
- SQL
- JDBC
- Consultas agregadas
- Validaciones de negocio
- Interacción entre aplicación y base de datos

# Tecnologías Utilizadas
- Java
- Java Swing
- PostgreSQL
- JDBC
- NetBeans

# Funcionalidades
## Gestión de datos
- Alta de platos
- Eliminación de mozos
- Validación de datos de entrada
- Carga inicial de información desde archivo SQL

## Consultas generales
- Listado de mozos
- Listado de platos
- Mozos libres
- Cantidad de mesas por mozo
- Platos más consumidos
- Platos nunca consumidos
- Estadísticas de precios

## Consultas parametrizadas
- Mesas asignadas a un mozo
- Platos consumidos en una mesa
- Cantidad de platos consumidos
- Consultas entre rangos de fechas

# Conceptos de Base de Datos Aplicados
## DDL
- CREATE TABLE
- definición de restricciones
- claves primarias y foráneas
## DML
- INSERT
- UPDATE
- DELETE
## Consultas SQL
- JOIN
- GROUP BY
- COUNT
- AVG
- MAX
- MIN
- subconsultas
- filtros por fechas
- consultas parametrizadas

# Estructura General
/src  
 ├── MainFrame.java  
 ├── MainFrame.form  
 └── Archivos/  
 &emsp;&emsp;└── Inserta_Datos.sql.txt

# Configuración de la Base de Datos
El proyecto utiliza PostgreSQL.
Por defecto:

DB_NAME = "RestoFreaky"  
DB_USER = "postgres"  
DB_PWD = "admin"

Modificar estos valores según la configuración local antes de ejecutar el proyecto.
