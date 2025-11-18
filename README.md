# PRIMS - Sistema de Gestión

## Descripción
PRIMS es una aplicación de escritorio desarrollada en Java con JavaFX que proporciona [describe la funcionalidad principal de tu sistema].

## Requisitos Previos

### Software Necesario
- **Java Development Kit (JDK)**: 11 o superior
- **JavaFX SDK**: 17 o superior
- **Maven** o **Gradle** (opcional, si usas gestor de dependencias)
- **MySQL**: 8.0 o superior (basado en las librerías incluidas)
- **IDE recomendado**: IntelliJ IDEA, Eclipse o NetBeans

### Requisitos del Sistema
- **Sistema Operativo**: Windows 10/11, Linux (Ubuntu 18.04+), macOS 10.14+
- **RAM**: Mínimo 4GB (recomendado 8GB)
- **Espacio en disco**: 500MB para la aplicación + espacio para la base de datos

## Dependencias del Proyecto

El proyecto utiliza las siguientes librerías (incluidas en `/lib`):

### Framework y UI
- `antlr4-runtime-4.13.1.jar` - Parser generator
- `fontbox-3.0.6.jar` - Manejo de fuentes
- `pdfbox-app-3.0.6.jar` - Generación y manipulación de PDFs

### Persistencia de Datos
- `hibernate-core-6.4.4.Final.jar` - ORM (Object-Relational Mapping)
- `hibernate-commons-annotations-6.0.6.Final.jar`
- `jakarta.persistence-api-3.1.0.jar`
- `jakarta.transaction-api-2.0.1.jar`
- `jakarta.xml.bind-api-3.0.1.jar`

### Conectores de Base de Datos
- `mysql-connector-j-8.3.0.jar` - Conector MySQL
- `mysql-connector-java-8.0.29.jar` - Conector MySQL (versión alternativa)

### Utilidades
- `byte-buddy-1.14.10.jar` - Manipulación de bytecode
- `classmate-1.5.1.jar` - Reflexión de tipos
- `commons-logging-1.3.5.jar` - Sistema de logging
- `gson-2.13.1.jar` - Serialización/Deserialización JSON
- `jandex-3.2.7.jar` - Indexación de clases
- `jaxb-runtime-3.0.1.jar` - Binding XML
- `jboss-logging-3.5.0.Final.jar` - Framework de logging
- `slf4j-api-2.0.17.jar` - API de logging

### Librerías Personalizadas
- `ebaycalls.jar` - [Describe su función]
- `ebaysdkcore.jar` - [Describe su función]
- `helper.jar` - Utilidades auxiliares

## Instalación

### 1. Clonar o Descargar el Proyecto
```bash
git clone https://github.com/tu-usuario/prims.git
cd prims
```

O descarga el archivo ZIP y descomprímelo.

### 2. Configurar Java y JavaFX

#### Verificar instalación de Java
```bash
java -version
```
Debe mostrar Java 11 o superior.

#### Descargar JavaFX (si no está incluido)
1. Descarga JavaFX SDK desde: https://openjfx.io/
2. Extrae el SDK en una ubicación conocida (ej: `C:\javafx-sdk` o `/opt/javafx-sdk`)

### 3. Configurar la Base de Datos MySQL

#### Instalar MySQL
Descarga e instala MySQL desde: https://dev.mysql.com/downloads/

#### Crear la base de datos
```sql
CREATE DATABASE prims_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'prims_user'@'localhost' IDENTIFIED BY 'tu_contraseña_segura';
GRANT ALL PRIVILEGES ON prims_db.* TO 'prims_user'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Configurar Archivo de Conexión

Crea o edita el archivo de configuración de base de datos (generalmente en `src/config/database.properties` o similar):

```properties
db.url=jdbc:mysql://localhost:3306/prims_db?useSSL=false&serverTimezone=UTC
db.username=prims_user
db.password=tu_contraseña_segura
db.driver=com.mysql.cj.jdbc.Driver

# Configuración de Hibernate
hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
hibernate.show_sql=true
hibernate.hbm2ddl.auto=update
```

### 5. Compilar el Proyecto

#### Usando el IDE (IntelliJ IDEA / Eclipse / NetBeans)

1. Abre el proyecto en tu IDE
2. Configura las librerías del proyecto:
   - Añade todas las JAR de la carpeta `/lib` al classpath
   - Configura JavaFX SDK en las librerías del proyecto
3. Configura la clase principal (`Main.java`)
4. Ejecuta el proyecto desde el IDE

#### Usando Línea de Comandos (sin Maven/Gradle)

**En Windows:**
```bash
# Compilar
javac --module-path "C:\ruta\a\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "lib\*;src" -d bin src\tu\paquete\*.java

# Ejecutar
java --module-path "C:\ruta\a\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\*" tu.paquete.Main
```

**En Linux/macOS:**
```bash
# Compilar
javac --module-path "/ruta/a/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml -cp "lib/*:src" -d bin src/tu/paquete/*.java

# Ejecutar
java --module-path "/ruta/a/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml -cp "bin:lib/*" tu.paquete.Main
```

### 6. Crear Ejecutable JAR (Opcional)

Crea un archivo `manifest.txt`:
```
Main-Class: tu.paquete.Main
Class-Path: lib/antlr4-runtime-4.13.1.jar lib/byte-buddy-1.14.10.jar lib/classmate-1.5.1.jar lib/commons-logging-1.3.5.jar lib/ebaycalls.jar lib/ebaysdkcore.jar lib/fontbox-3.0.6.jar lib/gson-2.13.1.jar lib/helper.jar lib/hibernate-commons-annotations-6.0.6.Final.jar lib/hibernate-core-6.4.4.Final.jar lib/jakarta.persistence-api-3.1.0.jar lib/jakarta.transaction-api-2.0.1.jar lib/jakarta.xml.bind-api-3.0.1.jar lib/jandex-3.2.7.jar lib/jaxb-runtime-3.0.1.jar lib/jboss-logging-3.5.0.Final.jar lib/mysql-connector-j-8.3.0.jar lib/mysql-connector-java-8.0.29.jar lib/pdfbox-app-3.0.6.jar lib/slf4j-api-2.0.17.jar
```

Crear el JAR:
```bash
jar cfm PRIMS.jar manifest.txt -C bin . lib/
```

Ejecutar el JAR:
```bash
java --module-path "/ruta/a/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml -jar PRIMS.jar
```

## Verificación de la Instalación

### Verificar conexión a la base de datos
Al iniciar la aplicación, verifica en los logs que aparezca:
```
Hibernate: 
    create table...
Conexión exitosa a la base de datos
```

### Pruebas básicas
1. La interfaz gráfica debe cargar correctamente
2. Verifica que puedas acceder al menú principal
3. Realiza una operación básica de CRUD (Crear, Leer, Actualizar, Eliminar)

## Solución de Problemas

### Error: [Problema común 1]
**Solución:** [Explicación de cómo resolverlo]

### Error: [Problema común 2]
**Solución:** [Explicación de cómo resolverlo]

## Documentación Adicional

- [Guía de Usuario](docs/user-guide.md)
- [Documentación de API](docs/api.md)
- [Guía de Contribución](CONTRIBUTING.md)

## Soporte

Si encuentras problemas durante la instalación:
- Abre un issue en: [URL del repositorio]
- Contacta al equipo: [email de soporte]
- Consulta la documentación completa: [URL de docs]

## Licencia

[Tipo de licencia - MIT, Apache, etc.]

## Autores

- [Nombre del autor/equipo]
- [Información de contacto]

---

**Última actualización:** [Fecha]
