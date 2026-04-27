# Help

This file is intentionally a small supplement to the main repository docs.

- Use `README.md` for running and using the service.
- Use `STRUCTURE.md` for package ownership and entry points.
- Use `ARCHITECTURE.md` for the end-to-end flow and diagrams.
- Use `QUALITY.md` for Maven gates, CI workflows, and current stats.

## Useful reference documentation

- [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
- [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/maven-plugin)
- [Create an OCI image](https://docs.spring.io/spring-boot/4.0.6/maven-plugin/build-image.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.6/reference/actuator/index.html)
- [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.6/reference/data/sql.html#data.sql.jpa-and-spring-data)
- [Flyway Migration](https://docs.spring.io/spring-boot/4.0.6/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- [Spring for Apache Kafka](https://docs.spring.io/spring-boot/4.0.6/reference/messaging/kafka.html)
- [Validation](https://docs.spring.io/spring-boot/4.0.6/reference/io/validation.html)
- [Spring Web MVC](https://docs.spring.io/spring-boot/4.0.6/reference/web/servlet.html)
- [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.0.6/reference/testing/testcontainers.html#testing.testcontainers)
- [Testcontainers Kafka module](https://java.testcontainers.org/modules/kafka/)

## Notes specific to this repository

- The repository uses `compose.yaml` for the optional local Docker stack.
- Spring Docker Compose auto-start is disabled in the base application configuration; local container startup is explicit.
- Swagger/OpenAPI is enabled in local-oriented profiles, not in the base default profile.
- The supported no-RocketMQ review path is the `local-fallback` profile, where settlement payloads are logged.
- The final local verification path is `spotless:check`, `verify`, `test-compile`, and `clean verify`, followed by the committed stats refresh script in `utils/test-stats/extractor.ps1`.
