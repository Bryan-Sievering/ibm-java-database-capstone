// java
package com.project.back_end.services;

import com.project.back_end.models.Doctor;
import com.project.back_end.DTO.Login;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                return Collections.emptyList();
            }

            Doctor doctor = doctorOpt.get();
            List<String> allSlots = doctor.getAvailableTimes() == null
                    ? Collections.emptyList()
                    : new ArrayList<>(doctor.getAvailableTimes());

            if (allSlots.isEmpty()) {
                return allSlots;
            }

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            // Build a set of booked slot strings from appointments
            var appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    doctorId, start, end
            );

            Set<String> bookedSlots = appointments.stream()
                    .map(a -> toSlot(a.getAppointmentTime()))
                    .collect(Collectors.toSet());

            // Available = all doctor slots minus booked
            return allSlots.stream()
                    .filter(s -> !bookedSlots.contains(s))
                    .sorted(this::compareSlots)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            Doctor existing = doctorRepository.findByEmail(doctor.getEmail());
            if (existing != null) {
                return -1; // already exists
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public int updateDoctor(Doctor doctor) {
        try {
            if (doctor.getId() == null) {
                return -1;
            }
            Optional<Doctor> existing = doctorRepository.findById(doctor.getId());
            if (existing.isEmpty()) {
                return -1;
            }
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        try {
            List<Doctor> doctors = doctorRepository.findAll();
            // Force-initialize lazy collections while the transaction is open
            doctors.forEach(d -> {
                List<String> slots = d.getAvailableTimes();
                if (slots != null) { slots.size(); }
            });
            return doctors;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    @Transactional
    public int deleteDoctor(long id) {
        try {
            Optional<Doctor> existing = doctorRepository.findById(id);
            if (existing.isEmpty()) {
                return -1;
            }
            // Delete appointments for that doctor first
            appointmentRepository.deleteAllByDoctorId(id);
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> validateDoctor(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            String identifier = login.getIdentifier(); // email for doctors
            Doctor doctor = doctorRepository.findByEmail(identifier);
            if (doctor == null || doctor.getPassword() == null
                    || !doctor.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }


            String token = tokenService.generateToken(doctor.getEmail());
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Doctor> doctors;
            if (name == null || name.isBlank()) {
                doctors = doctorRepository.findAll();
            } else {
                doctors = doctorRepository.findByNameLike(name.trim());
            }
            result.put("doctors", doctors);
            result.put("count", doctors.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to retrieve doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        try {
            String safeName = name == null ? "" : name.trim();
            String safeSpec = specialty == null ? "" : specialty.trim();

            List<Doctor> base = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(safeName, safeSpec);
            List<Doctor> filtered = filterDoctorByTime(base, amOrPm);

            result.put("doctors", filtered);
            result.put("count", filtered.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        try {
            String safeName = name == null ? "" : name.trim();
            List<Doctor> base = safeName.isBlank()
                    ? doctorRepository.findAll()
                    : doctorRepository.findByNameLike(safeName);
            List<Doctor> filtered = filterDoctorByTime(base, amOrPm);

            result.put("doctors", filtered);
            result.put("count", filtered.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specilty) {
        Map<String, Object> result = new HashMap<>();
        try {
            String safeName = name == null ? "" : name.trim();
            String safeSpec = specilty == null ? "" : specilty.trim();
            List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(safeName, safeSpec);

            result.put("doctors", doctors);
            result.put("count", doctors.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specilty, String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        try {
            String safeSpec = specilty == null ? "" : specilty.trim();
            List<Doctor> base = doctorRepository.findBySpecialtyIgnoreCase(safeSpec);
            List<Doctor> filtered = filterDoctorByTime(base, amOrPm);

            result.put("doctors", filtered);
            result.put("count", filtered.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorBySpecility(String specilty) {
        Map<String, Object> result = new HashMap<>();
        try {
            String safeSpec = specilty == null ? "" : specilty.trim();
            List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(safeSpec);

            result.put("doctors", doctors);
            result.put("count", doctors.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Doctor> base = doctorRepository.findAll();
            List<Doctor> filtered = filterDoctorByTime(base, amOrPm);

            result.put("doctors", filtered);
            result.put("count", filtered.size());
            return result;
        } catch (Exception e) {
            result.put("doctors", Collections.emptyList());
            result.put("count", 0);
            result.put("message", "Failed to filter doctors");
            return result;
        }
    }

    // Helper: filter by AM/PM based on the start time of each slot
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || amOrPm.isBlank()) {
            return doctors;
        }
        String period = amOrPm.trim().toUpperCase(Locale.ROOT);
        boolean wantAM = "AM".equals(period);
        boolean wantPM = "PM".equals(period);

        if (!wantAM && !wantPM) {
            return doctors;
        }

        return doctors.stream()
                .filter(d -> {
                    List<String> slots = d.getAvailableTimes();
                    if (slots == null || slots.isEmpty()) return false;
                    for (String slot : slots) {
                        LocalTime start = parseSlotStart(slot);
                        if (start == null) continue;
                        if (wantAM && start.isBefore(LocalTime.NOON)) return true;
                        if (wantPM && !start.isBefore(LocalTime.NOON)) return true; // 12:00 and after -> PM
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    // Build slot string from appointment start time (assumes 1-hour slots)
    private String toSlot(LocalDateTime start) {
        LocalDateTime end = start.plusHours(1);
        return start.toLocalTime().format(SLOT_TIME) + "-" + end.toLocalTime().format(SLOT_TIME);
    }

    private LocalTime parseSlotStart(String slot) {
        try {
            if (slot == null || !slot.contains("-")) return null;
            String start = slot.split("-", 2)[0].trim();
            return LocalTime.parse(start, SLOT_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    // Sort comparator for slot strings like "09:00-10:00"
    private int compareSlots(String a, String b) {
        LocalTime sa = parseSlotStart(a);
        LocalTime sb = parseSlotStart(b);
        if (sa == null && sb == null) return 0;
        if (sa == null) return 1;
        if (sb == null) return -1;
        return sa.compareTo(sb);
    }
}

// 1. **Add @Service Annotation**:
//    - This class should be annotated with `@Service` to indicate that it is a service layer class.
//    - The `@Service` annotation marks this class as a Spring-managed bean for business logic.
//    - Instruction: Add `@Service` above the class declaration.

// 2. **Constructor Injection for Dependencies**:
//    - The `DoctorService` class depends on `DoctorRepository`, `AppointmentRepository`, and `TokenService`.
//    - These dependencies should be injected via the constructor for proper dependency management.
//    - Instruction: Ensure constructor injection is used for injecting dependencies into the service.

// 3. **Add @Transactional Annotation for Methods that Modify or Fetch Database Data**:
//    - Methods like `getDoctorAvailability`, `getDoctors`, `findDoctorByName`, `filterDoctorsBy*` should be annotated with `@Transactional`.
//    - The `@Transactional` annotation ensures that database operations are consistent and wrapped in a single transaction.
//    - Instruction: Add the `@Transactional` annotation above the methods that perform database operations or queries.

// 4. **getDoctorAvailability Method**:
//    - Retrieves the available time slots for a specific doctor on a particular date and filters out already booked slots.
//    - The method fetches all appointments for the doctor on the given date and calculates the availability by comparing against booked slots.
//    - Instruction: Ensure that the time slots are properly formatted and the available slots are correctly filtered.

// 5. **saveDoctor Method**:
//    - Used to save a new doctor record in the database after checking if a doctor with the same email already exists.
//    - If a doctor with the same email is found, it returns `-1` to indicate conflict; `1` for success, and `0` for internal errors.
//    - Instruction: Ensure that the method correctly handles conflicts and exceptions when saving a doctor.

// 6. **updateDoctor Method**:
//    - Updates an existing doctor's details in the database. If the doctor doesn't exist, it returns `-1`.
//    - Instruction: Make sure that the doctor exists before attempting to save the updated record and handle any errors properly.

// 7. **getDoctors Method**:
//    - Fetches all doctors from the database. It is marked with `@Transactional` to ensure that the collection is properly loaded.
//    - Instruction: Ensure that the collection is eagerly loaded, especially if dealing with lazy-loaded relationships (e.g., available times). 

// 8. **deleteDoctor Method**:
//    - Deletes a doctor from the system along with all appointments associated with that doctor.
//    - It first checks if the doctor exists. If not, it returns `-1`; otherwise, it deletes the doctor and their appointments.
//    - Instruction: Ensure the doctor and their appointments are deleted properly, with error handling for internal issues.

// 9. **validateDoctor Method**:
//    - Validates a doctor's login by checking if the email and password match an existing doctor record.
//    - It generates a token for the doctor if the login is successful, otherwise returns an error message.
//    - Instruction: Make sure to handle invalid login attempts and password mismatches properly with error responses.

// 10. **findDoctorByName Method**:
//    - Finds doctors based on partial name matching and returns the list of doctors with their available times.
//    - This method is annotated with `@Transactional` to ensure that the database query and data retrieval are properly managed within a transaction.
//    - Instruction: Ensure that available times are eagerly loaded for the doctors.


// 11. **filterDoctorsByNameSpecilityandTime Method**:
//    - Filters doctors based on their name, specialty, and availability during a specific time (AM/PM).
//    - The method fetches doctors matching the name and specialty criteria, then filters them based on their availability during the specified time period.
//    - Instruction: Ensure proper filtering based on both the name and specialty as well as the specified time period.

// 12. **filterDoctorByTime Method**:
//    - Filters a list of doctors based on whether their available times match the specified time period (AM/PM).
//    - This method processes a list of doctors and their available times to return those that fit the time criteria.
//    - Instruction: Ensure that the time filtering logic correctly handles both AM and PM time slots and edge cases.


// 13. **filterDoctorByNameAndTime Method**:
//    - Filters doctors based on their name and the specified time period (AM/PM).
//    - Fetches doctors based on partial name matching and filters the results to include only those available during the specified time period.
//    - Instruction: Ensure that the method correctly filters doctors based on the given name and time of day (AM/PM).

// 14. **filterDoctorByNameAndSpecility Method**:
//    - Filters doctors by name and specialty.
//    - It ensures that the resulting list of doctors matches both the name (case-insensitive) and the specified specialty.
//    - Instruction: Ensure that both name and specialty are considered when filtering doctors.


// 15. **filterDoctorByTimeAndSpecility Method**:
//    - Filters doctors based on their specialty and availability during a specific time period (AM/PM).
//    - Fetches doctors based on the specified specialty and filters them based on their available time slots for AM/PM.
//    - Instruction: Ensure the time filtering is accurately applied based on the given specialty and time period (AM/PM).

// 16. **filterDoctorBySpecility Method**:
//    - Filters doctors based on their specialty.
//    - This method fetches all doctors matching the specified specialty and returns them.
//    - Instruction: Make sure the filtering logic works for case-insensitive specialty matching.

// 17. **filterDoctorsByTime Method**:
//    - Filters all doctors based on their availability during a specific time period (AM/PM).
//    - The method checks all doctors' available times and returns those available during the specified time period.
//    - Instruction: Ensure proper filtering logic to handle AM/PM time periods.

