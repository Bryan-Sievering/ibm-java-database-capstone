// java
package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService;
    private final Service generalService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService,
                              Service generalService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
        this.generalService = generalService;
    }

    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();
        try {
            if (appointment.getId() == null) {
                response.put("message", "Appointment ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Appointment> existingOpt = appointmentRepository.findById(appointment.getId());
            if (existingOpt.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment existing = existingOpt.get();

            if (appointment.getPatient() == null || existing.getPatient() == null ||
                    !Objects.equals(existing.getPatient().getId(), appointment.getPatient().getId())) {
                response.put("message", "Patient mismatch for this appointment");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            int validation = generalService.validateAppointment(appointment);
            if (validation == -1) {
                response.put("message", "Invalid doctor");
                return ResponseEntity.badRequest().body(response);
            }
            if (validation == 0) {
                response.put("message", "Requested time is not available");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            appointment.setId(existing.getId());
            appointmentRepository.save(appointment);

            response.put("message", "Appointment updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Failed to update appointment");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> existingOpt = appointmentRepository.findById(id);
            if (existingOpt.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment existing = existingOpt.get();
            String emailFromToken = tokenService.getEmailFromToken(token);

            if (existing.getPatient() == null || existing.getPatient().getEmail() == null ||
                    !existing.getPatient().getEmail().equalsIgnoreCase(emailFromToken)) {
                response.put("message", "You are not authorized to cancel this appointment");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            appointmentRepository.delete(existing);
            response.put("message", "Appointment cancelled successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Failed to cancel appointment");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<String, Object> getAppointment(String pname, LocalDate date, String token) {
        Map<String, Object> result = new HashMap<>();
        try {
            String email = tokenService.getEmailFromToken(token);
            Doctor doctor = doctorRepository.findByEmail(email);
            if (doctor == null) {
                result.put("appointments", Collections.emptyList());
                result.put("message", "Doctor not found for provided token");
                return result;
            }

            LocalDateTime start = date.atTime(LocalTime.MIN);
            LocalDateTime end = date.atTime(LocalTime.MAX);

            List<Appointment> appointments;
            if (pname != null && !pname.isBlank()) {
                appointments = appointmentRepository
                        .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                                doctor.getId(), pname.trim(), start, end);
            } else {
                appointments = appointmentRepository
                        .findByDoctorIdAndAppointmentTimeBetween(doctor.getId(), start, end);
            }

            result.put("appointments", appointments);
            result.put("count", appointments.size());
            return result;
        } catch (Exception e) {
            result.put("appointments", Collections.emptyList());
            result.put("message", "Failed to retrieve appointments");
            return result;
        }
    }

    @Transactional
    public ResponseEntity<Map<String, String>> changeStatus(long id, int status) {
        Map<String, String> response = new HashMap<>();
        try {
            Optional<Appointment> existingOpt = appointmentRepository.findById(id);
            if (existingOpt.isEmpty()) {
                response.put("message", "Appointment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Appointment appointment = existingOpt.get();
            appointment.setStatus(status);
            appointmentRepository.save(appointment);

            response.put("message", "Appointment status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Failed to change appointment status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
// 1. **Add @Service Annotation**:
//    - To indicate that this class is a service layer class for handling business logic.
//    - The `@Service` annotation should be added before the class declaration to mark it as a Spring service component.
//    - Instruction: Add `@Service` above the class definition.

// 2. **Constructor Injection for Dependencies**:
//    - The `AppointmentService` class requires several dependencies like `AppointmentRepository`, `Service`, `TokenService`, `PatientRepository`, and `DoctorRepository`.
//    - These dependencies should be injected through the constructor.
//    - Instruction: Ensure constructor injection is used for proper dependency management in Spring.

// 3. **Add @Transactional Annotation for Methods that Modify Database**:
//    - The methods that modify or update the database should be annotated with `@Transactional` to ensure atomicity and consistency of the operations.
//    - Instruction: Add the `@Transactional` annotation above methods that interact with the database, especially those modifying data.

// 4. **Book Appointment Method**:
//    - Responsible for saving the new appointment to the database.
//    - If the save operation fails, it returns `0`; otherwise, it returns `1`.
//    - Instruction: Ensure that the method handles any exceptions and returns an appropriate result code.

// 5. **Update Appointment Method**:
//    - This method is used to update an existing appointment based on its ID.
//    - It validates whether the patient ID matches, checks if the appointment is available for updating, and ensures that the doctor is available at the specified time.
//    - If the update is successful, it saves the appointment; otherwise, it returns an appropriate error message.
//    - Instruction: Ensure proper validation and error handling is included for appointment updates.

// 6. **Cancel Appointment Method**:
//    - This method cancels an appointment by deleting it from the database.
//    - It ensures the patient who owns the appointment is trying to cancel it and handles possible errors.
//    - Instruction: Make sure that the method checks for the patient ID match before deleting the appointment.

// 7. **Get Appointments Method**:
//    - This method retrieves a list of appointments for a specific doctor on a particular day, optionally filtered by the patient's name.
//    - It uses `@Transactional` to ensure that database operations are consistent and handled in a single transaction.
//    - Instruction: Ensure the correct use of transaction boundaries, especially when querying the database for appointments.

// 8. **Change Status Method**:
//    - This method updates the status of an appointment by changing its value in the database.
//    - It should be annotated with `@Transactional` to ensure the operation is executed in a single transaction.
//    - Instruction: Add `@Transactional` before this method to ensure atomicity when updating appointment status.
