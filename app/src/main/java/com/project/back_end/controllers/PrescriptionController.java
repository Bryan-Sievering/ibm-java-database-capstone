// java
package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;            // Adjust if your model package differs
import com.project.back_end.services.AppointmentService;   // Adjust package if needed
import com.project.back_end.services.PrescriptionService;  // Adjust package if needed
import com.project.back_end.services.Service;               // Adjust package if needed
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${api.path}" + "prescription")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service;
    private final AppointmentService appointmentService;

    public PrescriptionController(PrescriptionService prescriptionService,
                                  Service service,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.service = service;
        this.appointmentService = appointmentService;
    }

    // 1) Save Prescription
    // POST /prescription/{token}
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable String token,
            @Valid @RequestBody Prescription prescription) {

        // Validate token for doctor role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, String> body = validation.getBody() != null ? validation.getBody() : new HashMap<>();
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }

        // Save prescription
        ResponseEntity<Map<String, String>> saveResponse = prescriptionService.savePrescription(prescription);

        // If saved successfully, optionally update appointment status (e.g., to "Completed" = 1)
        if (saveResponse.getStatusCode().is2xxSuccessful() && prescription.getAppointmentId() != null) {
            try {
                appointmentService.changeStatus(prescription.getAppointmentId(), 1);
            } catch (Exception ignored) {
                // Best-effort status update; do not fail the main request if this part fails
            }
        }

        return saveResponse;
    }

    // 2) Get Prescription by Appointment ID
    // GET /prescription/{appointmentId}/{token}
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {

        // Validate token for doctor role
        ResponseEntity<Map<String, String>> validation = service.validateToken(token, "doctor");
        if (!validation.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> body = new HashMap<>();
            if (validation.getBody() != null) body.putAll(validation.getBody());
            return ResponseEntity.status(validation.getStatusCode()).body(body);
        }

        // Fetch prescription
        return prescriptionService.getPrescription(appointmentId);
    }
}
    
// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller.
//    - Use `@RequestMapping("${api.path}prescription")` to set the base path for all prescription-related endpoints.
//    - This controller manages creating and retrieving prescriptions tied to appointments.


// 2. Autowire Dependencies:
//    - Inject `PrescriptionService` to handle logic related to saving and fetching prescriptions.
//    - Inject the shared `Service` class for token validation and role-based access control.
//    - Inject `AppointmentService` to update appointment status after a prescription is issued.


// 3. Define the `savePrescription` Method:
//    - Handles HTTP POST requests to save a new prescription for a given appointment.
//    - Accepts a validated `Prescription` object in the request body and a doctor’s token as a path variable.
//    - Validates the token for the `"doctor"` role.
//    - If the token is valid, updates the status of the corresponding appointment to reflect that a prescription has been added.
//    - Delegates the saving logic to `PrescriptionService` and returns a response indicating success or failure.


// 4. Define the `getPrescription` Method:
//    - Handles HTTP GET requests to retrieve a prescription by its associated appointment ID.
//    - Accepts the appointment ID and a doctor’s token as path variables.
//    - Validates the token for the `"doctor"` role using the shared service.
//    - If the token is valid, fetches the prescription using the `PrescriptionService`.
//    - Returns the prescription details or an appropriate error message if validation fails.

