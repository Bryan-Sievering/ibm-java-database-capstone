// java
package com.project.back_end.services;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // Generate JWT for a given identifier (e.g., username/email)
    public String generateToken(String identifier) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000); // 7 days

        return Jwts.builder()
                .subject(identifier)
                .issuedAt(now)
                .expiration(expiry)
                // 0.12.x recommended API: choose algorithm explicitly
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Extract identifier (subject) from token using 0.12.x parser API
    public String extractIdentifier(String token) {
        String raw = stripBearer(token);
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(raw)
                .getPayload();
        return claims.getSubject();
    }

    // Optional alias
    public String getEmailFromToken(String token) {
        return extractIdentifier(token);
    }

    // Validate token and ensure the subject exists for the given user type
    public boolean validateToken(String token, String userType) {
        try {
            String identifier = extractIdentifier(token);
            if (identifier == null || identifier.isEmpty() || userType == null) {
                return false;
            }

            switch (userType.toLowerCase()) {
                case "admin":
                    return adminRepository.findByUsername(identifier) != null;
                case "doctor":
                    return doctorRepository.findByEmail(identifier) != null;
                case "patient":
                    return patientRepository.findByEmail(identifier) != null;
                default:
                    return false;
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Signature invalid, token expired, malformed, etc.
            return false;
        }
    }

    private SecretKey getSigningKey() {
        // Ensure this secret is at least 32 bytes for HS256
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    private String stripBearer(String token) {
        if (token == null) return null;
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }
}
// 1. **@Component Annotation**
// The @Component annotation marks this class as a Spring component, meaning Spring will manage it as a bean within its application context.
// This allows the class to be injected into other Spring-managed components (like services or controllers) where it's needed.

// 2. **Constructor Injection for Dependencies**
// The constructor injects dependencies for `AdminRepository`, `DoctorRepository`, and `PatientRepository`,
// allowing the service to interact with the database and validate users based on their role (admin, doctor, or patient).
// Constructor injection ensures that the class is initialized with all required dependencies, promoting immutability and making the class testable.

// 3. **getSigningKey Method**
// This method retrieves the HMAC SHA key used to sign JWT tokens.
// It uses the `jwt.secret` value, which is provided from an external source (like application properties).
// The `Keys.hmacShaKeyFor()` method converts the secret key string into a valid `SecretKey` for signing and verification of JWTs.

// 4. **generateToken Method**
// This method generates a JWT token for a user based on their email.
// - The `subject` of the token is set to the user's email, which is used as an identifier.
// - The `issuedAt` is set to the current date and time.
// - The `expiration` is set to 7 days from the issue date, ensuring the token expires after one week.
// - The token is signed using the signing key generated by `getSigningKey()`, making it secure and tamper-proof.
// The method returns the JWT token as a string.

// 5. **extractEmail Method**
// This method extracts the user's email (subject) from the provided JWT token.
// - The token is first verified using the signing key to ensure it hasn’t been tampered with.
// - After verification, the token is parsed, and the subject (which represents the email) is extracted.
// This method allows the application to retrieve the user's identity (email) from the token for further use.

// 6. **validateToken Method**
// This method validates whether a provided JWT token is valid for a specific user role (admin, doctor, or patient).
// - It first extracts the email from the token using the `extractEmail()` method.
// - Depending on the role (`admin`, `doctor`, or `patient`), it checks the corresponding repository (AdminRepository, DoctorRepository, or PatientRepository)
//   to see if a user with the extracted email exists.
// - If a match is found for the specified user role, it returns true, indicating the token is valid.
// - If the role or user does not exist, it returns false, indicating the token is invalid.
// - The method gracefully handles any errors by returning false if the token is invalid or an exception occurs.
// This ensures secure access control based on the user's role and their existence in the system.

