## MySQL Database Design

### Table: patients
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(50), Not Null
- last_name: VARCHAR(50), Not Null
- email: VARCHAR(100), Unique, Not Null
- phone_number: VARCHAR(15), Not Null
- date_of_birth: DATE
- gender: VARCHAR(10)

### Table: doctors
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(50), Not Null
- last_name: VARCHAR(50), Not Null
- specialization: VARCHAR(100)
- email: VARCHAR(100), Unique, Not Null
- phone_number: VARCHAR(15)
- available_from: TIME
- available_to: TIME

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key → doctors(id)
- patient_id: INT, Foreign Key → patients(id)
- appointment_time: DATETIME, Not Null
- status: INT (0 = Scheduled, 1 = Completed, 2 = Cancelled)

### Table: admins
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(50), Unique, Not Null
- password_hash: VARCHAR(255), Not Null


## MongoDB Collection Design

### Collection: prescriptions

```json
{
  "_id": "ObjectId('64abc123456')",
  "patientId": 101,
  "appointmentId": 202,
  "doctorId": 12,
  "medication": "Amoxicillin",
  "dosage": "250mg",
  "instructions": "Take one capsule every 8 hours for 7 days.",
  "issuedAt": "2025-08-01T10:00:00Z",
  "refillAllowed": true,
  "pharmacy": {
    "name": "CVS",
    "location": "Downtown Ave"
  },
  "tags": ["antibiotic", "infection"]
}
