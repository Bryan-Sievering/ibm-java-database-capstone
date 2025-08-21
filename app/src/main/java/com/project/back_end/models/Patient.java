package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "name cannot be null")
    @Size(min = 3, max = 100, message = "name must be between 3 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotNull(message = "email cannot be null")
    @Email(message = "email must be a valid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull(message = "password cannot be null")
    @Size(min = 6, message = "password must be at least 6 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @NotNull(message = "phone cannot be null")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    @Column(nullable = false, length = 10)
    private String phone;

    @NotNull(message = "address cannot be null")
    @Size(max = 255, message = "address must be at most 255 characters")
    @Column(nullable = false, length = 255)
    private String address;

    public Patient() {
    }

    public Patient(Long id, String name, String email, String password, String phone, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
// @Entity annotation:
//    - Marks the class as a JPA entity, meaning it represents a table in the database.
//    - Required for persistence frameworks (e.g., Hibernate) to map the class to a database table.

// 1. 'id' field:
//    - Type: private Long
//    - Description:
//      - Represents the unique identifier for each patient.
//      - The @Id annotation marks it as the primary key.
//      - The @GeneratedValue(strategy = GenerationType.IDENTITY) annotation auto-generates the ID value when a new record is inserted into the database.

// 2. 'name' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's full name.
//      - The @NotNull annotation ensures that the patient's name is required.
//      - The @Size(min = 3, max = 100) annotation ensures that the name length is between 3 and 100 characters. 
//      - Provides validation for correct input and user experience.


// 3. 'email' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's email address.
//      - The @NotNull annotation ensures that an email address must be provided.
//      - The @Email annotation validates that the email address follows a valid email format (e.g., patient@example.com).

// 4. 'password' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's password for login authentication.
//      - The @NotNull annotation ensures that a password must be provided.
//      - The @Size(min = 6) annotation ensures that the password must be at least 6 characters long.

// 5. 'phone' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's phone number.
//      - The @NotNull annotation ensures that a phone number must be provided.
//      - The @Pattern(regexp = "^[0-9]{10}$") annotation validates that the phone number must be exactly 10 digits long.

// 6. 'address' field:
//    - Type: private String
//    - Description:
//      - Represents the patient's address.
//      - The @NotNull annotation ensures that the address must be provided.
//      - The @Size(max = 255) annotation ensures that the address does not exceed 255 characters in length, providing validation for the address input.


// 7. Getters and Setters:
//    - Standard getter and setter methods are provided for all fields: id, name, email, password, phone, and address.
//    - These methods allow access and modification of the fields of the Patient class.

