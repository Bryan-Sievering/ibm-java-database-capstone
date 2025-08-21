// java
package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import com.project.back_end.services.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@org.springframework.stereotype.Service
public class Service {

    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    // 1) Validate token for a user role
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> body = new HashMap<>();
        try {
            boolean valid = tokenService.validateToken(token, user);
            if (!valid) {
                body.put("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }
            body.put("message", "Valid token");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Token validation failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 2) Validate admin login and return token
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> body = new HashMap<>();
        try {
            if (receivedAdmin == null || receivedAdmin.getUsername() == null || receivedAdmin.getPassword() == null) {
                body.put("message", "Username and password are required");
                return ResponseEntity.badRequest().body(body);
            }

            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            if (admin == null) {
                body.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            if (!Objects.equals(admin.getPassword(), receivedAdmin.getPassword())) {
                body.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            String token = tokenService.generateToken(admin.getUsername());
            body.put("token", token);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to validate admin");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 3) Filter doctors by name, specialty and time
    // java
// 3) Filter doctors by name, specialty and time
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        Map<String, Object> result = new HashMap<>();
        try {
            String n = (name != null && !name.isBlank()) ? name.trim().toLowerCase() : null;
            String s = (specialty != null && !specialty.isBlank()) ? specialty.trim() : null;
            String t = (time != null && !time.isBlank()) ? time.trim() : null;

            List<Doctor> doctors;

            if (t != null) {
                // Use existing filtering by time (and specialty if provided)
                Map<String, Object> map = doctorService.filterDoctorByTimeAndSpecility(s, t);
                @SuppressWarnings("unchecked")
                List<Doctor> base = (List<Doctor>) map.getOrDefault("doctors", Collections.emptyList());
                if (n != null) {
                    doctors = base.stream()
                            .filter(d -> d.getName() != null && d.getName().toLowerCase().contains(n))
                            .toList();
                } else {
                    doctors = base;
                }
            } else {
                // No time filter: derive base by specialty or use all, then optionally filter by name
                List<Doctor> base = (s != null)
                        ? doctorRepository.findBySpecialtyIgnoreCase(s)
                        : doctorRepository.findAll();

                if (n != null) {
                    doctors = base.stream()
                            .filter(d -> d.getName() != null && d.getName().toLowerCase().contains(n))
                            .toList();
                } else {
                    doctors = base;
                }
            }

            result.put("doctors", doctors);
            result.put("count", doctors != null ? doctors.size() : 0);
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    // 4) Validate whether the appointment time is available for the doctor
    // Returns: 1 -> valid, 0 -> time unavailable, -1 -> doctor doesn't exist
    public int validateAppointment(Appointment appointment) {
        try {
            if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null
                    || appointment.getAppointmentTime() == null) {
                return 0;
            }

            Long doctorId = appointment.getDoctor().getId();
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                return -1;
            }

            LocalDateTime requestedDateTime = appointment.getAppointmentTime();
            LocalDate date = requestedDateTime.toLocalDate();
            LocalTime requestedTime = requestedDateTime.toLocalTime();

            // Fetch availability via DoctorService; supports common slot formats.
            List<?> slots = doctorService.getDoctorAvailability(doctorId, date);
            if (slots == null || slots.isEmpty()) {
                return 0;
            }

            boolean available = slots.stream().anyMatch(slot -> {
                if (slot == null) return false;

                // Direct LocalTime match
                if (slot instanceof LocalTime lt) {
                    return lt.equals(requestedTime);
                }

                // LocalDateTime (start of slot)
                if (slot instanceof LocalDateTime ldt) {
                    return ldt.equals(requestedDateTime) || ldt.toLocalTime().equals(requestedTime);
                }

                // String formats like "HH:mm" or "HH:mm-HH:mm"
                if (slot instanceof String s) {
                    try {
                        if (s.contains("-")) {
                            String start = s.split("-", 2)[0].trim();
                            return LocalTime.parse(start).equals(requestedTime);
                        }
                        return LocalTime.parse(s.trim()).equals(requestedTime);
                    } catch (Exception ignored) {
                        return false;
                    }
                }

                // Fallback for objects with getStartTime() by convention
                try {
                    var m = slot.getClass().getMethod("getStartTime");
                    Object v = m.invoke(slot);
                    if (v instanceof LocalTime lt2) return lt2.equals(requestedTime);
                    if (v instanceof LocalDateTime ldt2) return ldt2.toLocalTime().equals(requestedTime);
                    if (v instanceof String ss) {
                        try {
                            return LocalTime.parse(ss.trim()).equals(requestedTime);
                        } catch (Exception ignored) {
                            return false;
                        }
                    }
                } catch (Exception ignored) { }

                return false;
            });

            return available ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // 5) Validate that the patient does not already exist (by email or phone)
    public boolean validatePatient(Patient patient) {
        try {
            if (patient == null) return false;
            String email = patient.getEmail();
            String phone = patient.getPhone();
            if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
                return false;
            }

            var existing = patientRepository.findByEmailOrPhone(email, phone);
            return existing == null; // true means not found -> valid to create
        } catch (Exception e) {
            return false;
        }
    }

    // 6) Validate patient login and return token
    // java
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> body = new HashMap<>();
        try {
            if (login == null || login.getIdentifier() == null || login.getPassword() == null) {
                body.put("message", "Identifier and password are required");
                return ResponseEntity.badRequest().body(body);
            }

            Patient patient = patientRepository.findByEmail(login.getIdentifier());
            if (patient == null || !Objects.equals(patient.getPassword(), login.getPassword())) {
                body.put("message", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
            }

            String token = tokenService.generateToken(patient.getEmail());
            body.put("token", token);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            body.put("message", "Failed to validate patient login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    // 7) Filter patient appointments using condition and/or doctor name
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        try {
            String email = tokenService.getEmailFromToken(token);
            if (email == null || email.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Patient patient = patientRepository.findByEmail(email);
            if (patient == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Patient not found for provided token");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Long patientId = patient.getId();

            boolean hasCondition = condition != null && !condition.isBlank();
            boolean hasName = name != null && !name.isBlank();

            if (hasCondition && hasName) {
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            } else if (hasCondition) {
                return patientService.filterByCondition(condition, patientId);
            } else if (hasName) {
                return patientService.filterByDoctor(name, patientId);
            } else {
                // No filters -> return all appointments for patient
                return patientService.getPatientAppointment(patientId, token);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to filter patient appointments");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
// 1. **@Service Annotation**
// The @Service annotation marks this class as a service component in Spring. This allows Spring to automatically detect it through component scanning
// and manage its lifecycle, enabling it to be injected into controllers or other services using @Autowired or constructor injection.

// 2. **Constructor Injection for Dependencies**
// The constructor injects all required dependencies (TokenService, Repositories, and other Services). This approach promotes loose coupling, improves testability,
// and ensures that all required dependencies are provided at object creation time.

// 3. **validateToken Method**
// This method checks if the provided JWT token is valid for a specific user. It uses the TokenService to perform the validation.
// If the token is invalid or expired, it returns a 401 Unauthorized response with an appropriate error message. This ensures security by preventing
// unauthorized access to protected resources.

// 4. **validateAdmin Method**
// This method validates the login credentials for an admin user.
// - It first searches the admin repository using the provided username.
// - If an admin is found, it checks if the password matches.
// - If the password is correct, it generates and returns a JWT token (using the admin’s username) with a 200 OK status.
// - If the password is incorrect, it returns a 401 Unauthorized status with an error message.
// - If no admin is found, it also returns a 401 Unauthorized.
// - If any unexpected error occurs during the process, a 500 Internal Server Error response is returned.
// This method ensures that only valid admin users can access secured parts of the system.

// 5. **filterDoctor Method**
// This method provides filtering functionality for doctors based on name, specialty, and available time slots.
// - It supports various combinations of the three filters.
// - If none of the filters are provided, it returns all available doctors.
// This flexible filtering mechanism allows the frontend or consumers of the API to search and narrow down doctors based on user criteria.

// 6. **validateAppointment Method**
// This method validates if the requested appointment time for a doctor is available.
// - It first checks if the doctor exists in the repository.
// - Then, it retrieves the list of available time slots for the doctor on the specified date.
// - It compares the requested appointment time with the start times of these slots.
// - If a match is found, it returns 1 (valid appointment time).
// - If no matching time slot is found, it returns 0 (invalid).
// - If the doctor doesn’t exist, it returns -1.
// This logic prevents overlapping or invalid appointment bookings.

// 7. **validatePatient Method**
// This method checks whether a patient with the same email or phone number already exists in the system.
// - If a match is found, it returns false (indicating the patient is not valid for new registration).
// - If no match is found, it returns true.
// This helps enforce uniqueness constraints on patient records and prevent duplicate entries.

// 8. **validatePatientLogin Method**
// This method handles login validation for patient users.
// - It looks up the patient by email.
// - If found, it checks whether the provided password matches the stored one.
// - On successful validation, it generates a JWT token and returns it with a 200 OK status.
// - If the password is incorrect or the patient doesn't exist, it returns a 401 Unauthorized with a relevant error.
// - If an exception occurs, it returns a 500 Internal Server Error.
// This method ensures only legitimate patients can log in and access their data securely.

// 9. **filterPatient Method**
// This method filters a patient's appointment history based on condition and doctor name.
// - It extracts the email from the JWT token to identify the patient.
// - Depending on which filters (condition, doctor name) are provided, it delegates the filtering logic to PatientService.
// - If no filters are provided, it retrieves all appointments for the patient.
// This flexible method supports patient-specific querying and enhances user experience on the client side.

