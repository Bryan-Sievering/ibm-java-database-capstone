// java
package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 1) Saves a new patient
    @Transactional
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 2) Retrieves appointments for a patient, verifying token identity
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> body = new HashMap<>();
        try {
            String email = tokenService.getEmailFromToken(token);
            Patient fromToken = patientRepository.findByEmail(email);

            if (fromToken == null || !Objects.equals(fromToken.getId(), id)) {
                body.put("message", "Unauthorized: token does not match the requested patient");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            List<Appointment> appointments = appointmentRepository.findByPatientId(id);
            List<AppointmentDTO> dtos = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            body.put("appointments", dtos);
            body.put("count", dtos.size());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to retrieve patient appointments");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 3) Filters by condition (past/future)
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (condition == null) {
                body.put("message", "Condition is required: past or future");
                return ResponseEntity.badRequest().body(body);
            }

            int status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1;
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0;
            } else {
                body.put("message", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.badRequest().body(body);
            }

            List<Appointment> appointments =
                    appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);

            List<AppointmentDTO> dtos = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            body.put("appointments", dtos);
            body.put("count", dtos.size());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to filter appointments by condition");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 4) Filters by doctor name for a specific patient
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (name == null || name.isBlank()) {
                body.put("message", "Doctor name is required");
                return ResponseEntity.badRequest().body(body);
            }

            List<Appointment> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientId(name.trim(), patientId);

            List<AppointmentDTO> dtos = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            body.put("appointments", dtos);
            body.put("count", dtos.size());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to filter appointments by doctor");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 5) Filters by doctor name and condition for a specific patient
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> body = new HashMap<>();
        try {
            if (name == null || name.isBlank()) {
                body.put("message", "Doctor name is required");
                return ResponseEntity.badRequest().body(body);
            }

            int status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1;
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0;
            } else {
                body.put("message", "Invalid condition. Use 'past' or 'future'.");
                return ResponseEntity.badRequest().body(body);
            }

            List<Appointment> appointments =
                    appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name.trim(), patientId, status);

            List<AppointmentDTO> dtos = appointments.stream()
                    .map(this::toAppointmentDTO)
                    .collect(Collectors.toList());

            body.put("appointments", dtos);
            body.put("count", dtos.size());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to filter appointments by doctor and condition");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 6) Fetches patient details from token
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> body = new HashMap<>();
        try {
            String email = tokenService.getEmailFromToken(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null) {
                body.put("message", "Patient not found for provided token");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            }

            body.put("patient", patient);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to fetch patient details");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // Helper: map Appointment -> AppointmentDTO using the all-args constructor
    private AppointmentDTO toAppointmentDTO(Appointment a) {
        var d = a.getDoctor();
        var p = a.getPatient();

        Long doctorId = d != null ? d.getId() : null;
        String doctorName = d != null ? d.getName() : null;

        Long patientId = p != null ? p.getId() : null;
        String patientName = p != null ? p.getName() : null;
        String patientEmail = p != null ? p.getEmail() : null;
        String patientPhone = p != null ? p.getPhone() : null;
        String patientAddress = p != null ? p.getAddress() : null;

        LocalDateTime time = a.getAppointmentTime();
        int status = a.getStatus();

        return new AppointmentDTO(
                a.getId(),
                doctorId,
                doctorName,
                patientId,
                patientName,
                patientEmail,
                patientPhone,
                patientAddress,
                time,
                status
        );
    }
}
// 1. **Add @Service Annotation**:
//    - The `@Service` annotation is used to mark this class as a Spring service component. 
//    - It will be managed by Spring's container and used for business logic related to patients and appointments.
//    - Instruction: Ensure that the `@Service` annotation is applied above the class declaration.

// 2. **Constructor Injection for Dependencies**:
//    - The `PatientService` class has dependencies on `PatientRepository`, `AppointmentRepository`, and `TokenService`.
//    - These dependencies are injected via the constructor to maintain good practices of dependency injection and testing.
//    - Instruction: Ensure constructor injection is used for all the required dependencies.

// 3. **createPatient Method**:
//    - Creates a new patient in the database. It saves the patient object using the `PatientRepository`.
//    - If the patient is successfully saved, the method returns `1`; otherwise, it logs the error and returns `0`.
//    - Instruction: Ensure that error handling is done properly and exceptions are caught and logged appropriately.

// 4. **getPatientAppointment Method**:
//    - Retrieves a list of appointments for a specific patient, based on their ID.
//    - The appointments are then converted into `AppointmentDTO` objects for easier consumption by the API client.
//    - This method is marked as `@Transactional` to ensure database consistency during the transaction.
//    - Instruction: Ensure that appointment data is properly converted into DTOs and the method handles errors gracefully.

// 5. **filterByCondition Method**:
//    - Filters appointments for a patient based on the condition (e.g., "past" or "future").
//    - Retrieves appointments with a specific status (0 for future, 1 for past) for the patient.
//    - Converts the appointments into `AppointmentDTO` and returns them in the response.
//    - Instruction: Ensure the method correctly handles "past" and "future" conditions, and that invalid conditions are caught and returned as errors.

// 6. **filterByDoctor Method**:
//    - Filters appointments for a patient based on the doctor's name.
//    - It retrieves appointments where the doctor’s name matches the given value, and the patient ID matches the provided ID.
//    - Instruction: Ensure that the method correctly filters by doctor's name and patient ID and handles any errors or invalid cases.

// 7. **filterByDoctorAndCondition Method**:
//    - Filters appointments based on both the doctor's name and the condition (past or future) for a specific patient.
//    - This method combines filtering by doctor name and appointment status (past or future).
//    - Converts the appointments into `AppointmentDTO` objects and returns them in the response.
//    - Instruction: Ensure that the filter handles both doctor name and condition properly, and catches errors for invalid input.

// 8. **getPatientDetails Method**:
//    - Retrieves patient details using the `tokenService` to extract the patient's email from the provided token.
//    - Once the email is extracted, it fetches the corresponding patient from the `patientRepository`.
//    - It returns the patient's information in the response body.
    //    - Instruction: Make sure that the token extraction process works correctly and patient details are fetched properly based on the extracted email.

// 9. **Handling Exceptions and Errors**:
//    - The service methods handle exceptions using try-catch blocks and log any issues that occur. If an error occurs during database operations, the service responds with appropriate HTTP status codes (e.g., `500 Internal Server Error`).
//    - Instruction: Ensure that error handling is consistent across the service, with proper logging and meaningful error messages returned to the client.

// 10. **Use of DTOs (Data Transfer Objects)**:
//    - The service uses `AppointmentDTO` to transfer appointment-related data between layers. This ensures that sensitive or unnecessary data (e.g., password or private patient information) is not exposed in the response.
//    - Instruction: Ensure that DTOs are used appropriately to limit the exposure of internal data and only send the relevant fields to the client.

