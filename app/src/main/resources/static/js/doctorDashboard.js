// javascript
// doctorDashboard.js – Managing Appointments for Doctor

import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

// Globals
let tableBody;
let selectedDate; // 'YYYY-MM-DD'
let token;
let patientName = null; // For search filtering

// Utility: format Date to 'YYYY-MM-DD'
function formatDateYYYYMMDD(dateObj = new Date()) {
  const y = dateObj.getFullYear();
  const m = String(dateObj.getMonth() + 1).padStart(2, "0");
  const d = String(dateObj.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// Utility: render a single-row message into the table
function renderMessageRow(message) {
  if (!tableBody) return;
  const tr = document.createElement("tr");
  const td = document.createElement("td");
  td.colSpan = 6; // Adjust if your table has a different number of columns
  td.className = "table-message";
  td.textContent = message;
  tr.appendChild(td);
  tableBody.appendChild(tr);
}

// Core: load and render appointments
async function loadAppointments() {
  try {
    if (!tableBody) {
      tableBody = document.getElementById("patientTableBody");
    }
    if (!tableBody) return;

    tableBody.innerHTML = "";

    const nameFilter = patientName && patientName.trim() ? patientName.trim() : "null";
    const appointments = await getAllAppointments(selectedDate, nameFilter, token);

    if (!Array.isArray(appointments) || appointments.length === 0) {
      renderMessageRow("No Appointments found for today.");
      return;
    }

    for (const appt of appointments) {
      // Extract patient info safely
      const p = appt?.patient || {};
      const patient = {
        id: p.id ?? appt.patientId ?? null,
        name: p.name ?? appt.name ?? "",
        phone: p.phone ?? appt.phone ?? "",
        email: p.email ?? appt.email ?? "",
      };

      // Create row via component
      const row = createPatientRow(patient, appt);
      if (typeof row === "string") {
        tableBody.insertAdjacentHTML("beforeend", row);
      } else if (row instanceof HTMLElement) {
        tableBody.appendChild(row);
      } else if (row && row.el instanceof HTMLElement) {
        // Fallback if component returns wrapped object
        tableBody.appendChild(row.el);
      } else {
        // Graceful fallback if component returned something unexpected
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${patient.name || "-"}</td><td>${patient.phone || "-"}</td><td>${patient.email || "-"}</td>`;
        tableBody.appendChild(tr);
      }
    }
  } catch (err) {
    console.error("Error loading appointments:", err);
    renderMessageRow("Error loading appointments. Try again later.");
  }
}

// Wire up UI events after DOM is ready
document.addEventListener("DOMContentLoaded", async () => {
  // Initialize references and defaults
  tableBody = document.getElementById("patientTableBody");
  selectedDate = formatDateYYYYMMDD(new Date());
  token = localStorage.getItem("token") || "";

  // Optional: if your app uses renderContent to set up layout
  if (typeof window.renderContent === "function") {
    try {
      await window.renderContent();
    } catch (_) {
      // Ignore renderContent errors to avoid blocking appointments
    }
  }

  // Search bar
  const searchInput = document.getElementById("searchBar");
  if (searchInput) {
    searchInput.addEventListener("input", () => {
      const val = searchInput.value.trim();
      patientName = val.length ? val : "null";
      loadAppointments();
    });
  }

  // "Today" button
  const todayBtn = document.getElementById("todayButton");
  const datePicker = document.getElementById("datePicker");

  if (todayBtn) {
    todayBtn.addEventListener("click", () => {
      selectedDate = formatDateYYYYMMDD(new Date());
      if (datePicker) datePicker.value = selectedDate;
      loadAppointments();
    });
  }

  // Date picker change
  if (datePicker) {
    // Initialize picker to today on first load
    if (!datePicker.value) {
      datePicker.value = selectedDate;
    }
    datePicker.addEventListener("change", () => {
      selectedDate = datePicker.value || formatDateYYYYMMDD(new Date());
      loadAppointments();
    });
  }

  // Initial load: show today's appointments by default
  loadAppointments();
});

/*
  Import getAllAppointments to fetch appointments from the backend
  Import createPatientRow to generate a table row for each patient appointment


  Get the table body where patient rows will be added
  Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  Get the saved token from localStorage (used for authenticated API calls)
  Initialize patientName to null (used for filtering by name)


  Add an 'input' event listener to the search bar
  On each keystroke:
    - Trim and check the input value
    - If not empty, use it as the patientName for filtering
    - Else, reset patientName to "null" (as expected by backend)
    - Reload the appointments list with the updated filter


  Add a click listener to the "Today" button
  When clicked:
    - Set selectedDate to today's date
    - Update the date picker UI to match
    - Reload the appointments for today


  Add a change event listener to the date picker
  When the date changes:
    - Update selectedDate with the new value
    - Reload the appointments for that specific date


  Function: loadAppointments
  Purpose: Fetch and display appointments based on selected date and optional patient name

  Step 1: Call getAllAppointments with selectedDate, patientName, and token
  Step 2: Clear the table body content before rendering new rows

  Step 3: If no appointments are returned:
    - Display a message row: "No Appointments found for today."

  Step 4: If appointments exist:
    - Loop through each appointment and construct a 'patient' object with id, name, phone, and email
    - Call createPatientRow to generate a table row for the appointment
    - Append each row to the table body

  Step 5: Catch and handle any errors during fetch:
    - Show a message row: "Error loading appointments. Try again later."


  When the page is fully loaded (DOMContentLoaded):
    - Call renderContent() (assumes it sets up the UI layout)
    - Call loadAppointments() to display today's appointments by default
*/
