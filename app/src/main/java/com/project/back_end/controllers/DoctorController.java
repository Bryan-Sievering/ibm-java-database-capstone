// java
package com.project.back_end.controllers;

import com.project.back_end.models.Doctor; // Adjust if your Doctor class is in a different package
import com.project.back_end.DTO.Login;  // Adjust if your Login DTO is in a different package
import com.project.back_end.services.DoctorService; // Adjust package if needed
import com.project.back_end.services.Service;       // Adjust package if needed
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service;

    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // 1) Get Doctor Availability
    // GET /doctor/availability/{user}/{doctorId}/{date}/{token}
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String token) {

        // Validate token for the provided user role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, user);
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>();
            if (validation.getBody() != null) {
                body.putAll(validation.getBody());
            }
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }

        // Parse date (expects yyyy-MM-dd)
        final LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Invalid date format. Expected yyyy-MM-dd.");
            return ResponseEntity.badRequest().body(error);
        }

        List<String> availability = doctorService.getDoctorAvailability(doctorId, localDate);
        Map<String, Object> result = new HashMap<>();
        result.put("availability", availability);
        result.put("count", availability != null ? availability.size() : 0);
        return ResponseEntity.ok(result);
    }

    // 2) Get List of Doctors
    // GET /doctor
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctor() {
        List<Doctor> doctors = doctorService.getDoctors();
        Map<String, Object> result = new HashMap<>();
        result.put("doctors", doctors);
        result.put("count", doctors != null ? doctors.size() : 0);
        return ResponseEntity.ok(result);
    }

    // 3) Add New Doctor
    // POST /doctor/{token}
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token) {

        // Only admin can add doctors
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        int result = doctorService.saveDoctor(doctor);
        Map<String, String> body = new HashMap<>();
        if (result == 1) {
            body.put("message", "Doctor added to db");
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } else if (result == -1) {
            body.put("message", "Doctor already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } else {
            body.put("message", "Some internal error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 4) Doctor Login
    // POST /doctor/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
        return doctorService.validateDoctor(login);
    }

    // 5) Update Doctor Details
    // PUT /doctor/{token}
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @RequestBody Doctor doctor,
            @PathVariable String token) {

        // Only admin can update doctors
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        int result = doctorService.updateDoctor(doctor);
        Map<String, String> body = new HashMap<>();
        if (result == 1) {
            body.put("message", "Doctor updated");
            return ResponseEntity.ok(body);
        } else if (result == -1) {
            body.put("message", "Doctor not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } else {
            body.put("message", "Some internal error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 6) Delete Doctor
    // DELETE /doctor/{id}/{token}
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable long id,
            @PathVariable String token) {

        // Only admin can delete doctors
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "admin");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        int result = doctorService.deleteDoctor(id);
        Map<String, String> body = new HashMap<>();
        if (result == 1) {
            body.put("message", "Doctor deleted successfully");
            return ResponseEntity.ok(body);
        } else if (result == -1) {
            body.put("message", "Doctor not found with id");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        } else {
            body.put("message", "Some internal error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 7) Filter Doctors
    // GET /doctor/filter/{name}/{time}/{speciality}
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filter(
            @PathVariable String name,
            @PathVariable String time,
            @PathVariable("speciality") String specialty) {

        // Normalize placeholders like "all" to null so the service treats them as no-filter
        String n = "all".equalsIgnoreCase(name) ? null : name;
        String t = "all".equalsIgnoreCase(time) ? null : time;
        String s = "all".equalsIgnoreCase(specialty) ? null : specialty;

        Map<String, Object> result = service.filterDoctor(n, s, t);
        return ResponseEntity.ok(result);
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
//    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
//    - This class manages doctor-related functionalities such as registration, login, updates, and availability.


// 2. Autowire Dependencies:
//    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
//    - Inject the shared `Service` class for general-purpose features like token validation and filtering.


// 3. Define the `getDoctorAvailability` Method:
//    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
//    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
//    - First validates the token against the user type.
//    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.


// 4. Define the `getDoctor` Method:
//    - Handles HTTP GET requests to retrieve a list of all doctors.
//    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.


// 5. Define the `saveDoctor` Method:
//    - Handles HTTP POST requests to register a new doctor.
//    - Accepts a validated `Doctor` object in the request body and a token for authorization.
//    - Validates the token for the `"admin"` role before proceeding.
//    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.


// 6. Define the `doctorLogin` Method:
//    - Handles HTTP POST requests for doctor login.
//    - Accepts a validated `Login` DTO containing credentials.
//    - Delegates authentication to the `DoctorService` and returns login status and token information.


// 7. Define the `updateDoctor` Method:
//    - Handles HTTP PUT requests to update an existing doctor's information.
//    - Accepts a validated `Doctor` object and a token for authorization.
//    - Token must belong to an `"admin"`.
//    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.


// 8. Define the `deleteDoctor` Method:
//    - Handles HTTP DELETE requests to remove a doctor by ID.
//    - Requires both doctor ID and an admin token as path variables.
//    - If the doctor exists, deletes the record and returns a success message; otherwise, responds with a not found or error message.


// 9. Define the `filter` Method:
//    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
//    - Accepts `name`, `time`, and `speciality` as path variables.
//    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.