// javascript
// patientDashboard.js
import { getDoctors } from './services/doctorServices.js';
import { openModal } from './components/modals.js';
import { createDoctorCard } from './components/doctorCard.js';
import { filterDoctors } from './services/doctorServices.js';
import { patientSignup, patientLogin } from './services/patientServices.js';

// Initialize reliably whether DOMContentLoaded has already fired or not
window.addEventListener("load", init);


function init() {
  console.debug("[patientDashboard] init on", location.pathname);
  loadDoctorCards();

  const btn = document.getElementById("patientSignup");
  if (btn) btn.addEventListener("click", () => openModal("patientSignup"));

  const loginBtn = document.getElementById("patientLogin");
  if (loginBtn) loginBtn.addEventListener("click", () => openModal("patientLogin"));

  const searchEl = document.getElementById("searchBar");
  if (searchEl) searchEl.addEventListener("input", filterDoctorsOnChange);
  const timeEl = document.getElementById("filterTime");
  if (timeEl) timeEl.addEventListener("change", filterDoctorsOnChange);
  const specEl = document.getElementById("filterSpecialty");
  if (specEl) specEl.addEventListener("change", filterDoctorsOnChange);
}

function getDoctorContainer() {
  const el =
    document.getElementById("content") ||
    document.getElementById("doctors") ||
    document.getElementById("doctorCards") ||
    document.querySelector("[data-role='doctor-list']");
  if (!el) {
    console.warn(
      "[patientDashboard] Doctor container not found. Add an element with id='content' (or #doctors, #doctorCards, or [data-role='doctor-list'])."
    );
  }
  return el;
}

function loadDoctorCards() {
  console.debug("[patientDashboard] loading doctors...");
  getDoctors()
    .then((data) => {
      const doctors = Array.isArray(data) ? data : Array.isArray(data?.doctors) ? data.doctors : [];
      console.debug("[patientDashboard] doctors received:", doctors.length);

      const contentDiv = getDoctorContainer();
      if (!contentDiv) return;

      contentDiv.innerHTML = "";

      let appended = 0;
      doctors.forEach((doctor) => {
        try {
          const card = createDoctorCard(doctor);
          if (card) {
            contentDiv.appendChild(card);
            appended++;
          }
        } catch (e) {
          console.error("createDoctorCard failed for doctor:", doctor, e);
        }
      });

      if (appended === 0) {
        contentDiv.innerHTML = "<p>No doctors available right now.</p>";
      }
    })
    .catch((error) => {
      console.error("Failed to load doctors:", error);
      const contentDiv = getDoctorContainer();
      if (contentDiv) {
        contentDiv.innerHTML = "<p>Failed to load doctors. Please try again later.</p>";
      }
    });
}

function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar")?.value?.trim() || "";
  const filterTime = document.getElementById("filterTime")?.value || "";
  const filterSpecialty = document.getElementById("filterSpecialty")?.value || "";

  const name = searchBar.length > 0 ? searchBar : "all";
  const time = filterTime.length > 0 ? filterTime : "all";
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : "all";

  filterDoctors(name, time, specialty)
    .then((response) => {
      const doctors = Array.isArray(response) ? response : Array.isArray(response?.doctors) ? response.doctors : [];
      const contentDiv = getDoctorContainer();
      if (!contentDiv) return;

      contentDiv.innerHTML = "";

      let appended = 0;
      doctors.forEach((doctor) => {
        try {
          const card = createDoctorCard(doctor);
          if (card) {
            contentDiv.appendChild(card);
            appended++;
          }
        } catch (e) {
          console.error("createDoctorCard failed for doctor:", doctor, e);
        }
      });

      if (appended === 0) {
        contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
      }
    })
    .catch((error) => {
      console.error("Failed to filter doctors:", error);
      alert("❌ An error occurred while filtering doctors.");
    });
}

window.signupPatient = async function () {
  try {
    const name = document.getElementById("name").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const phone = document.getElementById("phone").value;
    const address = document.getElementById("address").value;

    const data = { name, email, password, phone, address };
    const { success, message } = await patientSignup(data);
    if (success) {
      alert(message);
      document.getElementById("modal").style.display = "none";
      window.location.reload();
    } else alert(message);
  } catch (error) {
    console.error("Signup failed:", error);
    alert("❌ An error occurred while signing up.");
  }
};

window.loginPatient = async function () {
  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const data = { email, password };
    console.log("loginPatient :: ", data);
    const response = await patientLogin(data);
    console.log("Status Code:", response.status);
    console.log("Response OK:", response.ok);
    if (response.ok) {
      const result = await response.json();
      console.log(result);
      selectRole("loggedPatient");
      localStorage.setItem("token", result.token);
      window.location.href = "/pages/loggedPatientDashboard.html";
    } else {
      alert("❌ Invalid credentials!");
    }
  } catch (error) {
    alert("❌ Failed to Login : " + error);
    console.log("Error :: loginPatient :: ", error);
  }
};
