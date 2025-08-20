## Architecture Summary

This Spring Boot application uses both MVC and REST controllers. Thymeleaf templates are used for the Admin and Doctor dashboards, while REST APIs serve other modules such as appointments and patient records. The application interacts with two databases: MySQL (for structured relational data such as patients, doctors, appointments, and admin users) and MongoDB (for flexible document data like prescriptions).

All controllers delegate requests to a centralized service layer, which handles business logic and validation. The service layer communicates with repository interfaces to access the database. MySQL uses JPA entities annotated with `@Entity`, while MongoDB uses document classes annotated with `@Document`. We will use both.

## Numbered Flow of Data and Control

1. A user accesses the application via a web dashboard (e.g., AdminDashboard) or through an API client (e.g., mobile app).
2. The request is routed to either a Thymeleaf-based controller or a REST controller based on the request path and type.
3. The controller validates the request and delegates it to the appropriate service method.
4. The service layer applies business logic, such as validating input or checking availability.
5. The service layer calls one or more repository interfaces to retrieve or persist data.
6. The repositories interact with either the MySQL database (via JPA) or MongoDB (via Spring Data MongoDB).
7. The resulting data is mapped to Java model objects, then either returned to Thymeleaf for rendering HTML or serialized to JSON for REST API responses.
