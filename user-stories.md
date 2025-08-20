# User Stories

## Admin User Stories

### Admin User Story 1
Title:
As an admin, I want to log into the portal with my username and password, so that I can manage the platform securely.

Acceptance Criteria:

Admins can enter credentials on a secure login form

Login is validated against stored admin records

Admin is redirected to the dashboard after successful login

Priority: High
Story Points: 3

Notes:

Should use encrypted passwords

Login failures should be logged


### Admin User Story 2
Title:
As an admin, I want to log out of the portal, so that I can protect system access.

Acceptance Criteria:

Logout button is available on all admin pages

Clicking logout ends the session

User is redirected to the login page

Priority: High
Story Points: 2

Notes:

Session tokens should be invalidated

Consider implementing session timeout


### Admin User Story 3
Title:
As an admin, I want to add doctors to the portal, so that they can manage appointments and patients.

Acceptance Criteria:

Admin can enter new doctor details in a form

Doctor profile is saved to the database

Success confirmation is shown

Priority: High
Story Points: 3

Notes:

Doctor ID should be unique

Validate email and specialty fields


### Admin User Story 4
Title:
As an admin, I want to delete a doctor's profile from the portal, so that I can remove inactive or incorrect entries.

Acceptance Criteria:

Admin can view list of doctors

Delete action prompts confirmation

Doctor is removed from the system

Priority: Medium
Story Points: 3

Notes:

Handle cascade deletes carefully

Consider soft delete (e.g., inactive flag)


### Admin User Story 5
Title:
As an admin, I want to run a stored procedure in MySQL CLI to get the number of appointments per month, so that I can track platform usage.

Acceptance Criteria:

Stored procedure returns correct appointment count by month

Accessible only by admin

Output is readable and can be exported

Priority: Medium
Story Points: 5

Notes:

Procedure name: get_monthly_appointments()

Optional: integrate result view into admin dashboard

## Patient User Stories

### Patient User Story 1
Title:
As a patient, I want to view a list of doctors without logging in, so that I can explore my options before registering.

Acceptance Criteria:

A public page displays available doctors

Doctor profiles include specialties and availability

No login is required to access the page

Priority: High
Story Points: 3

Notes:

Consider basic filters like specialty or location


### Patient User Story 2
Title:
As a patient, I want to sign up using my email and password, so that I can book appointments.

Acceptance Criteria:

Sign-up form collects email and password

Validations for email format and password strength

On success, patient profile is created and user is logged in

Priority: High
Story Points: 3

Notes:

Implement email uniqueness check

Store password securely (hashing)


### Patient User Story 3
Title:
As a patient, I want to log into the portal, so that I can manage my bookings.

Acceptance Criteria:

Login form accepts email and password

Valid credentials allow access to the dashboard

Incorrect credentials show an error message

Priority: High
Story Points: 2

Notes:

Add "Remember me" option

Use session management


### Patient User Story 4
Title:
As a patient, I want to log out of the portal, so that I can secure my account.

Acceptance Criteria:

Logout button available on all pages

Session ends upon logout

User is redirected to homepage or login page

Priority: High
Story Points: 2

Notes:

Consider auto-logout after inactivity


### Patient User Story 5
Title:
As a patient, I want to book an hour-long appointment with a doctor, so that I can get a consultation.

Acceptance Criteria:

Appointment booking form allows doctor and time selection

Booking confirms availability

Confirmation message shown after success

Priority: High
Story Points: 4

Notes:

Prevent overlapping appointments

Send email confirmation (optional)


### Patient User Story 6
Title:
As a patient, I want to view my upcoming appointments, so that I can prepare accordingly.

Acceptance Criteria:

Dashboard displays upcoming confirmed appointments

Each appointment shows doctor, date, and time

Past appointments are not shown

Priority: Medium
Story Points: 3

Notes:

Allow sorting or filtering by date


## Doctor User Stories

### Doctor User Story 1
Title:
As a doctor, I want to log into the portal, so that I can manage my appointments.

Acceptance Criteria:

Login form accepts valid email and password

Redirect to the doctor's dashboard upon successful login

Show error for incorrect credentials

Priority: High
Story Points: 2

Notes:

Login must be secure using HTTPS


### Doctor User Story 2
Title:
As a doctor, I want to log out of the portal, so that I can protect my data.

Acceptance Criteria:

Logout button available on every page

Session is terminated securely

Redirect to the login or home page after logout

Priority: High
Story Points: 1

Notes:

Consider automatic session timeout after inactivity


### Doctor User Story 3
Title:
As a doctor, I want to view my appointment calendar, so that I can stay organized.

Acceptance Criteria:

Calendar displays appointments in daily/weekly views

Each appointment shows patient name and time

Past appointments are clearly marked

Priority: High
Story Points: 4

Notes:

Consider calendar color coding or time blocking


### Doctor User Story 4
Title:
As a doctor, I want to mark my unavailability, so that patients only see available slots.

Acceptance Criteria:

Interface to block out dates/times

Unavailable slots hidden from patient booking view

Changes take effect immediately

Priority: High
Story Points: 4

Notes:

Validate that changes don’t overlap with existing appointments


### Doctor User Story 5
Title:
As a doctor, I want to update my profile with specialization and contact info, so that patients have up-to-date information.

Acceptance Criteria:

Profile form allows editing of name, specialty, and contact details

Changes are saved and reflected on patient view

Form has validation for required fields

Priority: Medium
Story Points: 3

Notes:

Consider preview before submitting changes


### Doctor User Story 6
Title:
As a doctor, I want to view patient details for upcoming appointments, so that I can be prepared.

Acceptance Criteria:

Each appointment includes patient name, reason for visit, and any notes

Accessible via the appointment calendar

Read-only access to patient profile

Priority: High
Story Points: 3

Notes:

Ensure patient confidentiality and role-based access
