// java
package com.project.back_end.controllers;

import com.project.back_end.models.Appointment; // Adjust if your Appointment class is in a different package
import com.project.back_end.services.AppointmentService; // Adjust package if needed
import com.project.back_end.services.Service; // Adjust package if needed
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service;

    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    // GET /appointments/{date}/{patientName}/{token}
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable String date,
            @PathVariable String patientName,
            @PathVariable String token) {

        // Validate token for doctor
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            // Forward validation failure as-is
            return ResponseEntity.status(validation.getStatusCode())
                    .body(new HashMap<>(validation.getBody() != null ? Map.copyOf(validation.getBody()) : Map.of()));
        }

        // Parse date
        final LocalDate localDate;
        try {
            localDate = LocalDate.parse(date); // expects ISO-8601: yyyy-MM-dd
        } catch (DateTimeParseException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Invalid date format. Expected yyyy-MM-dd.");
            return ResponseEntity.badRequest().body(error);
        }

        // Normalize patient name (treat "all" or "-" as no filter)
        String pname = patientName;
        if ("all".equalsIgnoreCase(patientName) || "-".equals(patientName)) {
            pname = null;
        }

        Map<String, Object> result = appointmentService.getAppointment(pname, localDate, token);
        return ResponseEntity.ok(result);
    }

    // POST /appointments/{token}
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate token for patient
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        // Validate appointment details (doctor exists, time availability, etc.)
        int validationResult = service.validateAppointment(appointment);
        Map<String, String> response = new HashMap<>();
        if (validationResult == -1) {
            response.put("message", "Invalid doctor");
            return ResponseEntity.badRequest().body(response);
        }
        if (validationResult == 0) {
            response.put("message", "Requested time is not available");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        int booked = appointmentService.bookAppointment(appointment);
        if (booked == 1) {
            response.put("message", "Appointment booked successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Failed to book appointment");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // PUT /appointments/{token}
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(
            @PathVariable String token,
            @RequestBody Appointment appointment) {

        // Validate token for patient
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        // Delegate to AppointmentService (handles checks and responses)
        return appointmentService.updateAppointment(appointment);
    }

    // DELETE /appointments/{id}/{token}
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable long id,
            @PathVariable String token) {

        // Validate token for patient
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "patient");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.status(validation.getStatusCode()).body(validation.getBody());
        }

        // Delegate to AppointmentService (verifies ownership via token)
        return appointmentService.cancelAppointment(id, token);
    }
}

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("/appointments")` to set a base path for all appointment-related endpoints.
//    - This centralizes all routes that deal with booking, updating, retrieving, and canceling appointments.


// 2. Autowire Dependencies:
//    - Inject `AppointmentService` for handling the business logic specific to appointments.
//    - Inject the general `Service` class, which provides shared functionality like token validation and appointment checks.


// 3. Define the `getAppointments` Method:
//    - Handles HTTP GET requests to fetch appointments based on date and patient name.
//    - Takes the appointment date, patient name, and token as path variables.
//    - First validates the token for role `"doctor"` using the `Service`.
//    - If the token is valid, returns appointments for the given patient on the specified date.
//    - If the token is invalid or expired, responds with the appropriate message and status code.


// 4. Define the `bookAppointment` Method:
//    - Handles HTTP POST requests to create a new appointment.
//    - Accepts a validated `Appointment` object in the request body and a token as a path variable.
//    - Validates the token for the `"patient"` role.
//    - Uses service logic to validate the appointment data (e.g., check for doctor availability and time conflicts).
//    - Returns success if booked, or appropriate error messages if the doctor ID is invalid or the slot is already taken.


// 5. Define the `updateAppointment` Method:
//    - Handles HTTP PUT requests to modify an existing appointment.
//    - Accepts a validated `Appointment` object and a token as input.
//    - Validates the token for `"patient"` role.
//    - Delegates the update logic to the `AppointmentService`.
//    - Returns an appropriate success or failure response based on the update result.


// 6. Define the `cancelAppointment` Method:
//    - Handles HTTP DELETE requests to cancel a specific appointment.
//    - Accepts the appointment ID and a token as path variables.
//    - Validates the token for `"patient"` role to ensure the user is authorized to cancel the appointment.
//    - Calls `AppointmentService` to handle the cancellation process and returns the result.

