# ─── Stage 1 : Build Maven ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copie uniquement les fichiers de déclaration des dépendances en premier
# → exploite le cache Docker : si pom.xml ne change pas, Maven ne re-télécharge rien
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Pré-téléchargement des dépendances (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copie des sources
COPY src ./src

# Package sans tests (les tests s'exécutent en CI, pas au build image)
RUN ./mvnw package -DskipTests -B

# ─── Stage 2 : Image finale (JRE uniquement) ─────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Récupère uniquement le JAR depuis le stage builder
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
