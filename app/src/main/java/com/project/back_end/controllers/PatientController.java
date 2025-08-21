// java
package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;    // Adjust if your DTO is in a different package
import com.project.back_end.models.Patient;  // Adjust if your model is in a different package
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final Service service;

    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    // 1) Get Patient Details
    // GET /patient/{token}
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatient(@PathVariable String token) {
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>();
            if (validation.getBody() != null) body.putAll(validation.getBody());
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }
        return patientService.getPatientDetails(token);
    }

    // 2) Create a New Patient
    // POST /patient
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        Map<String, String> body = new HashMap<>();

        boolean okToCreate = service.validatePatient(patient);
        if (!okToCreate) {
            body.put("message", "Patient with email id or phone no already exist");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        int res = patientService.createPatient(patient);
        if (res == 1) {
            body.put("message", "Signup successful");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } else {
            body.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 3) Patient Login
    // POST /patient/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        return service.validatePatientLogin(login);
    }

    // 4) Get Patient Appointments
    // GET /patient/{id}/{user}/{token}
    @GetMapping("/{id}/{user}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointment(
            @PathVariable Long id,
            @PathVariable String user,
            @PathVariable String token) {

        // Validate token based on provided role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>();
            if (validation.getBody() != null) body.putAll(validation.getBody());
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }

        // Delegate to PatientService (verifies identity when needed)
        return patientService.getPatientAppointment(id, token);
    }

    // 5) Filter Patient Appointments
    // GET /patient/filter/{condition}/{name}/{token}
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointment(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token) {

        // Validate token for patient role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>();
            if (validation.getBody() != null) body.putAll(validation.getBody());
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }

        // Delegate filtering to shared service
        return service.filterPatient(condition, name, token);
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.


