FROM eclipse-temurin:17-jdk

WORKDIR /app

# Копируем jar-файл в контейнер
COPY noteskeeperbot.jar app.jar

# Указываем точку входа и профиль
ENTRYPOINT ["java", "-jar", "app.jar"]
