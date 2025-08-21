// index.js (role-based login handling)

import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

// API endpoints
const ADMIN_API = API_BASE_URL + "/admin";
const DOCTOR_API = API_BASE_URL + "/doctor/login";

// Ensure DOM is ready before wiring up events
window.addEventListener("load", () => {
  // Buttons that trigger login modals (if present on the page)
  const adminBtn = document.getElementById("adminLogin");
  if (adminBtn) {
    adminBtn.addEventListener("click", () => openModal("adminLogin"));
  }

  const doctorBtn = document.getElementById("doctorLogin");
  if (doctorBtn) {
    doctorBtn.addEventListener("click", () => openModal("doctorLogin"));
  }
});

/**
 * Admin login handler
 * Reads credentials, authenticates, and stores token/role.
 * Exposed globally for modal submit buttons to call.
 */
window.adminLoginHandler = async function adminLoginHandler() {
  try {
    const usernameInput =
      document.getElementById("adminUsername") || document.getElementById("username");
    const passwordInput =
      document.getElementById("adminPassword") || document.getElementById("password");

    const username = usernameInput?.value?.trim() || "";
    const password = passwordInput?.value || "";

    if (!username || !password) {
      alert("Please enter both username and password.");
      return;
    }

    const admin = { username, password };
    const res = await fetch(ADMIN_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(admin),
    });

    if (!res.ok) {
      alert("Invalid credentials!");
      return;
    }

    const data = await res.json().catch(() => ({}));
    const token = data.token || data.accessToken || data.jwt;
    if (!token) {
      alert("Login succeeded but token was not returned.");
      return;
    }

    localStorage.setItem("token", token);
    // Keep both keys for compatibility with other parts of the app
    localStorage.setItem("userRole", "admin");
    if (typeof window.selectRole === "function") {
      window.selectRole("admin");
    }

    // Optional: close modal if your modal system supports it
    if (typeof window.closeModal === "function") {
      window.closeModal();
    }
  } catch (err) {
    console.error(err);
    alert("An error occurred while trying to log in. Please try again.");
  }
};

/**
 * Doctor login handler
 * Reads credentials, authenticates, and stores token/role.
 * Exposed globally for modal submit buttons to call.
 */
window.doctorLoginHandler = async function doctorLoginHandler() {
  try {
    const emailInput =
      document.getElementById("doctorEmail") || document.getElementById("email");
    const passwordInput =
      document.getElementById("doctorPassword") || document.getElementById("password");

    const email = emailInput?.value?.trim() || "";
    const password = passwordInput?.value || "";

    if (!email || !password) {
      alert("Please enter both email and password.");
      return;
    }

    const doctor = { email, password };
    const res = await fetch(DOCTOR_API, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(doctor),
    });

    if (!res.ok) {
      alert("Invalid credentials!");
      return;
    }

    const data = await res.json().catch(() => ({}));
    const token = data.token || data.accessToken || data.jwt;
    if (!token) {
      alert("Login succeeded but token was not returned.");
      return;
    }

    localStorage.setItem("token", token);
    // Keep both keys for compatibility with other parts of the app
    localStorage.setItem("userRole", "doctor");
    if (typeof window.selectRole === "function") {
      window.selectRole("doctor");
    }

    // Optional: close modal if your modal system supports it
    if (typeof window.closeModal === "function") {
      window.closeModal();
    }
  } catch (err) {
    console.error(err);
    alert("An error occurred while trying to log in. Please try again.");
  }
};

/*
  Import the openModal function to handle showing login popups/modals
  Import the base API URL from the config file
  Define constants for the admin and doctor login API endpoints using the base URL

  Use the window.onload event to ensure DOM elements are available after page load
  Inside this function:
    - Select the "adminLogin" and "doctorLogin" buttons using getElementById
    - If the admin login button exists:
        - Add a click event listener that calls openModal('adminLogin') to show the admin login modal
    - If the doctor login button exists:
        - Add a click event listener that calls openModal('doctorLogin') to show the doctor login modal


  Define a function named adminLoginHandler on the global window object
  This function will be triggered when the admin submits their login credentials

  Step 1: Get the entered username and password from the input fields
  Step 2: Create an admin object with these credentials

  Step 3: Use fetch() to send a POST request to the ADMIN_API endpoint
    - Set method to POST
    - Add headers with 'Content-Type: application/json'
    - Convert the admin object to JSON and send in the body

  Step 4: If the response is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('admin') to proceed with admin-specific behavior

  Step 5: If login fails or credentials are invalid:
    - Show an alert with an error message

  Step 6: Wrap everything in a try-catch to handle network or server errors
    - Show a generic error message if something goes wrong


  Define a function named doctorLoginHandler on the global window object
  This function will be triggered when a doctor submits their login credentials

  Step 1: Get the entered email and password from the input fields
  Step 2: Create a doctor object with these credentials

  Step 3: Use fetch() to send a POST request to the DOCTOR_API endpoint
    - Include headers and request body similar to admin login

  Step 4: If login is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('doctor') to proceed with doctor-specific behavior

  Step 5: If login fails:
    - Show an alert for invalid credentials

  Step 6: Wrap in a try-catch block to handle errors gracefully
    - Log the error to the console
    - Show a generic error message
*/
