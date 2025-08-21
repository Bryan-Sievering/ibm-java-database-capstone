// java
package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Find a doctor by email
    Doctor findByEmail(String email);

    // Find doctors by partial name (LIKE + CONCAT for flexible pattern)
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(@Param("name") String name);

    // Filter by partial name (case-insensitive) and exact specialty (case-insensitive)
    @Query("""
           SELECT d
           FROM Doctor d
           WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))
             AND LOWER(d.specialty) = LOWER(:specialty)
           """)
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(@Param("name") String name,
                                                                      @Param("specialty") String specialty);

    // Find by specialty ignoring case using Spring Data convention
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
   // 1. Extend JpaRepository:
//    - The repository extends JpaRepository<Doctor, Long>, which gives it basic CRUD functionality.
//    - This allows the repository to perform operations like save, delete, update, and find without needing to implement these methods manually.
//    - JpaRepository also includes features like pagination and sorting.

// Example: public interface DoctorRepository extends JpaRepository<Doctor, Long> {}

// 2. Custom Query Methods:

//    - **findByEmail**:
//      - This method retrieves a Doctor by their email.
//      - Return type: Doctor
//      - Parameters: String email

//    - **findByNameLike**:
//      - This method retrieves a list of Doctors whose name contains the provided search string (case-sensitive).
//      - The `CONCAT('%', :name, '%')` is used to create a pattern for partial matching.
//      - Return type: List<Doctor>
//      - Parameters: String name

//    - **findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase**:
//      - This method retrieves a list of Doctors where the name contains the search string (case-insensitive) and the specialty matches exactly (case-insensitive).
//      - It combines both fields for a more specific search.
//      - Return type: List<Doctor>
//      - Parameters: String name, String specialty

//    - **findBySpecialtyIgnoreCase**:
//      - This method retrieves a list of Doctors with the specified specialty, ignoring case sensitivity.
//      - Return type: List<Doctor>
//      - Parameters: String specialty

// 3. @Repository annotation:
//    - The @Repository annotation marks this interface as a Spring Data JPA repository.
//    - Spring Data JPA automatically implements this repository, providing the necessary CRUD functionality and custom queries defined in the interface.
