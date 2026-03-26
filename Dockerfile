FROM amazoncorretto:17-al2023-headless
WORKDIR /app
COPY build/libs/payment-demo-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod", "--server.address=0.0.0.0"]