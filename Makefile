.PHONY: test run package clean

# Corre los tests con H2 (no necesita BD externa)
test:
	./mvnw clean test

# Corre el servicio con las variables de entorno de .env
run:
	export $$(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run

# Empaqueta el JAR (corre tests primero — si fallan, no genera el JAR)
package:
	./mvnw clean package

clean:
	./mvnw clean
