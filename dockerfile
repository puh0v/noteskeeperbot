FROM eclipse-temurin:17-jdk

WORKDIR /app

# �������� jar-���� � ���������
COPY noteskeeperbot.jar app.jar

# ��������� ����� ����� � �������
ENTRYPOINT ["java", "-jar", "app.jar"]
