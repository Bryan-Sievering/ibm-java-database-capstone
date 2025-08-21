// javascript
// adminDashboard.js - Admin: manage doctors (load, filter, add)

import { openModal } from "../components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./doctorServices.js";
import { createDoctorCard } from "../components/doctorCard.js";

// Attach button handler for "Add Doctor" (if the header renders it)
document.addEventListener("click", (e) => {
  const btn = e.target.closest("#addDocBtn");
  if (btn) {
    openModal("addDoctor");
  }
});

// Load doctor cards on page ready
document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();

  // Search and filter bindings
  const search = document.getElementById("searchBar");
  const time = document.getElementById("timeFilter"); // actual id in HTML
  const specialty = document.getElementById("specialtyFilter"); // actual id in HTML

  if (search) search.addEventListener("input", filterDoctorsOnChange);
  if (time) time.addEventListener("change", filterDoctorsOnChange);
  if (specialty) specialty.addEventListener("change", filterDoctorsOnChange);
});

/**
 * Fetch all doctors and render as cards
 */
async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (err) {
    console.error("Failed to load doctors:", err);
  }
}

/**
 * Handle filtering logic based on search and dropdowns
 */
async function filterDoctorsOnChange() {
  try {
    const nameInput = document.getElementById("searchBar");
    const timeSelect = document.getElementById("timeFilter");
    const specialtySelect = document.getElementById("specialtyFilter");

    const name = (nameInput?.value || "").trim();
    const time = timeSelect?.value || "all";
    const specialty = specialtySelect?.value || "all";

    const doctors = await filterDoctors(name, time, specialty);

    if (Array.isArray(doctors) && doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      const contentDiv = document.getElementById("content");
      if (contentDiv) {
        contentDiv.innerHTML = `<p class="empty-state">No doctors found with the given filters.</p>`;
      }
    }
  } catch (err) {
    console.error("Error filtering doctors:", err);
    alert("Could not filter doctors. Please try again.");
  }
}

/**
 * Render doctor cards to the content area
 * @param {Array<object>} doctors
 */
function renderDoctorCards(doctors = []) {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  contentDiv.innerHTML = "";

  if (!Array.isArray(doctors) || doctors.length === 0) {
    contentDiv.innerHTML = `<p class="empty-state">No doctors found.</p>`;
    return;
  }

  for (const doc of doctors) {
    const card = createDoctorCard(doc);
    contentDiv.appendChild(card);
  }
}

/**
 * Collect form data from the "Add Doctor" modal and submit to backend
 * Expose globally so the modal form can call it (e.g., onsubmit="return adminAddDoctor(event)")
 */
window.adminAddDoctor = async function adminAddDoctor(event) {
  try {
    if (event && typeof event.preventDefault === "function") {
      event.preventDefault();
    }

    // Retrieve inputs (support common IDs and fallbacks)
    const getVal = (id, altId) =>
      document.getElementById(id)?.value?.trim() ??
      document.getElementById(altId)?.value?.trim() ??
      "";

    const name = getVal("addDocName", "name");
    const specialty = getVal("addDocSpecialty", "specialty");
    const email = getVal("addDocEmail", "email");
    const password = getVal("addDocPassword", "password");
    const phone = getVal("addDocPhone", "phone");

    // Availability from checked checkboxes with name="availability"
    const availability = Array.from(
      document.querySelectorAll('input[name="availability"]:checked')
    ).map((el) => el.value);

    if (!name || !specialty || !email || !password || !phone) {
      alert("Please fill in all required fields.");
      return false;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in as admin to add a doctor.");
      return false;
    }

    const doctor = {
      name,
      specialty,
      email,
      password,
      phone,
      availableTimes: availability,
    };

    // Submit to backend
    const result = await saveDoctor(doctor, token);

    if (result?.success) {
      alert(result?.message || "Doctor added successfully.");
      // Close modal if available, then refresh list
      if (typeof window.closeModal === "function") {
        window.closeModal();
      }
      await loadDoctorCards();
      return true;
    } else {
      alert(result?.message || "Failed to add doctor.");
      return false;
    }
  } catch (err) {
    console.error("adminAddDoctor error:", err);
    alert("An error occurred while adding the doctor. Please try again.");
    return false;
  }
};

/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
