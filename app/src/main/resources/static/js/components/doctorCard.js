// doctorCard.js
// Creates a reusable card element for displaying doctor info and role-based actions.

import { showBookingOverlay } from "/js/components/loggedPatient.js";
import { deleteDoctor } from "/js/services/doctorServices.js";
import { getPatientData } from "/js/services/patientServices.js";

/**
 * Create and return a DOM element representing a doctor card.
 * @param {Object} doctor - Doctor data object.
 * @param {number|string} doctor.id
 * @param {string} doctor.name
 * @param {string} doctor.specialty
 * @param {string} doctor.email
 * @param {string} [doctor.phone]
 * @param {string[]} [doctor.availableTimes]
 */
export function createDoctorCard(doctor) {
  // Main card container
  const card = document.createElement("div");
  card.classList.add("doctor-card");
  if (doctor?.id != null) {
    card.dataset.doctorId = String(doctor.id);
  }

  // Current user role
  const role = localStorage.getItem("userRole");

  // Doctor info section
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.classList.add("doctor-name");
  name.textContent = doctor?.name || "Unnamed Doctor";

  const specialization = document.createElement("p");
  specialization.classList.add("doctor-specialty");
  specialization.textContent = `Specialty: ${doctor?.specialty ?? "—"}`;

  const email = document.createElement("p");
  email.classList.add("doctor-email");
  email.textContent = `Email: ${doctor?.email ?? "—"}`;

  const phone = document.createElement("p");
  phone.classList.add("doctor-phone");
  phone.textContent = `Phone: ${doctor?.phone ?? "—"}`;

  const availability = document.createElement("p");
  availability.classList.add("doctor-availability");
  const times = Array.isArray(doctor?.availableTimes) ? doctor.availableTimes : [];
  availability.textContent = `Available: ${times.length ? times.join(", ") : "—"}`;

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(phone);
  infoDiv.appendChild(availability);

  // Actions container
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  // Role-based actions
  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.classList.add("btn", "danger");
    removeBtn.textContent = "Delete";

    removeBtn.addEventListener("click", async () => {
      if (!doctor?.id) return;
      const ok = confirm("Are you sure you want to delete this doctor?");
      if (!ok) return;

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Missing admin token. Please log in again.");
        return;
        }

      try {
        removeBtn.disabled = true;
        await deleteDoctor(doctor.id, token);
        card.remove();
        alert("Doctor deleted successfully.");
      } catch (err) {
        console.error(err);
        alert("Failed to delete doctor. Please try again.");
        removeBtn.disabled = false;
      }
    });

    actionsDiv.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.classList.add("btn", "book-now");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Please log in to book an appointment.");
      // Optional: if a modal system exists, you could open login:
      if (typeof window.openModal === "function") {
        window.openModal("patientLogin");
      }
    });
    actionsDiv.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.type = "button";
    bookNow.classList.add("btn", "book-now");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please log in again.");
        window.location.href = "/";
        return;
      }
      try {
        const patientData = await getPatientData(token);
        if (typeof showBookingOverlay === "function") {
          showBookingOverlay(e, doctor, patientData);
        } else {
          console.warn("showBookingOverlay(...) not available.");
          alert("Booking UI is not available right now. Please try again later.");
        }
      } catch (err) {
        console.error(err);
        alert("Unable to start booking. Please try again.");
      }
    });
    actionsDiv.appendChild(bookNow);
  }

  // Final assembly
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}

/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/
