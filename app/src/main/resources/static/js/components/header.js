// header.js

(function () {
  function isHomePage() {
    const p = window.location.pathname || "/";
    return p === "/" || p.endsWith("/index.html");
  }

  function getRole() {
    // Primary key per spec: userRole
    let role = localStorage.getItem("userRole");

    // Optional fallback if some pages set 'role'
    if (!role) {
      const legacy = localStorage.getItem("role");
      if (legacy) {
        role = legacy;
        localStorage.setItem("userRole", legacy);
      }
    }
    return role;
  }

  function getToken() {
    return localStorage.getItem("token");
  }

  function renderHeader() {
    const headerDiv = document.getElementById("header");
    if (!headerDiv) return;

    // Do not display role-based header on the homepage
    if (isHomePage()) {
      try {
        localStorage.removeItem("userRole");
        localStorage.removeItem("token");
      } catch (_) {}
      headerDiv.innerHTML = `
        <header class="header">
          <div class="logo-section">
            <img src="/assets/images/logo/logo.png" alt="Smart Clinic Logo" class="logo-img" />
            <span class="logo-title">Smart Clinic</span>
          </div>
        </header>`;
      return;
    }

    const role = getRole();
    const token = getToken();

    // Guard: if privileged roles are missing a token, treat as expired/invalid session
    if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
      try {
        localStorage.removeItem("userRole");
      } catch (_) {}
      alert("Session expired or invalid login. Please log in again.");
      window.location.href = "/";
      return;
    }

    let headerContent = `
      <header class="header">
        <div class="logo-section" style="cursor:pointer" id="logoHome">
          <img src="/assets/images/logo/logo.png" alt="Smart Clinic Logo" class="logo-img" />
          <span class="logo-title">Smart Clinic</span>
        </div>
        <nav class="nav-actions">`;

    // Role-specific actions
    if (role === "admin") {
      headerContent += `
        <button id="addDocBtn" class="adminBtn">Add Doctor</button>
        <a id="logoutBtn" href="#" class="nav-link">Logout</a>`;
    } else if (role === "doctor") {
      headerContent += `
        <button id="doctorHome" class="adminBtn">Home</button>
        <a id="logoutBtn" href="#" class="nav-link">Logout</a>`;
    } else if (role === "patient" || !role) {
      headerContent += `
        <button id="patientLogin" class="adminBtn">Login</button>
        <button id="patientSignup" class="adminBtn">Sign Up</button>`;
    } else if (role === "loggedPatient") {
      headerContent += `
        <button id="loggedPatientHome" class="adminBtn">Home</button>
        <button id="patientAppointments" class="adminBtn">Appointments</button>
        <a id="logoutPatientBtn" href="#" class="nav-link">Logout</a>`;
    }

    headerContent += `</nav></header>`;

    headerDiv.innerHTML = headerContent;
    attachHeaderButtonListeners();
  }

  function attachHeaderButtonListeners() {
    const byId = (id) => document.getElementById(id);

    const logoHome = byId("logoHome");
    if (logoHome) {
      logoHome.addEventListener("click", () => {
        const role = localStorage.getItem("userRole");
        // Simple smart home routing based on role
        if (role === "admin") window.location.href = "/pages/adminDashboard.html";
        else if (role === "doctor") window.location.href = "/pages/doctorDashboard.html";
        else if (role === "loggedPatient") window.location.href = "/pages/loggedPatientDashboard.html";
        else window.location.href = "/";
      });
    }

    const addDocBtn = byId("addDocBtn");
        if (addDocBtn) {
          addDocBtn.addEventListener("click", async () => {
            try {
              // If openModal isn't on window yet, dynamically import the module
              if (typeof window.openModal !== "function") {
                const mod = await import("/js/components/modals.js"); // adjust path to your modals.js
                const fn = mod?.openModal || window.openModal;
                if (typeof fn === "function") {
                  fn("addDoctor");
                } else {
                  console.warn("openModal not available after dynamic import.");
                  alert("Unable to open the Add Doctor dialog right now.");
                }
              } else {
                window.openModal("addDoctor");
              }
            } catch (err) {
              console.error("Failed to open Add Doctor modal:", err);
              alert("Failed to open the dialog. Please try again.");
            }
          });
        }


    const doctorHome = byId("doctorHome");
    if (doctorHome) {
      doctorHome.addEventListener("click", () => {
        window.location.href = "/pages/doctorDashboard.html";
      });
    }

    const patientLogin = byId("patientLogin");
    if (patientLogin) {
      patientLogin.addEventListener("click", () => {
        localStorage.setItem("userRole", "patient");
        if (typeof window.openModal === "function") {
          window.openModal("patientLogin");
        } else {
          console.warn("openModal('patientLogin') not available.");
        }
      });
    }

    const patientSignup = byId("patientSignup");
    if (patientSignup) {
      patientSignup.addEventListener("click", () => {
        localStorage.setItem("userRole", "patient");
        if (typeof window.openModal === "function") {
          window.openModal("patientSignup");
        } else {
          console.warn("openModal('patientSignup') not available.");
        }
      });
    }

    const loggedPatientHome = byId("loggedPatientHome");
    if (loggedPatientHome) {
      loggedPatientHome.addEventListener("click", () => {
        window.location.href = "/pages/loggedPatientDashboard.html";
      });
    }

    const patientAppointments = byId("patientAppointments");
    if (patientAppointments) {
      patientAppointments.addEventListener("click", () => {
        window.location.href = "/pages/patientAppointments.html";
      });
    }

    const logoutBtn = byId("logoutBtn");
    if (logoutBtn) {
      logoutBtn.addEventListener("click", (e) => {
        e.preventDefault();
        logout();
      });
    }

    const logoutPatientBtn = byId("logoutPatientBtn");
    if (logoutPatientBtn) {
      logoutPatientBtn.addEventListener("click", (e) => {
        e.preventDefault();
        logoutPatient();
      });
    }
  }

  // Logout helpers
  function logout() {
    try {
      localStorage.removeItem("token");
      localStorage.removeItem("userRole");
    } catch (_) {}
    window.location.href = "/";
  }

  function logoutPatient() {
    try {
      localStorage.removeItem("token");
      localStorage.setItem("userRole", "patient");
    } catch (_) {}
    window.location.href = "/pages/patientDashboard.html";
  }

  // Expose functions if needed globally
  window.renderHeader = renderHeader;
  window.logout = logout;
  window.logoutPatient = logoutPatient;

  // Initialize after DOM is ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", renderHeader);
  } else {
    renderHeader();
  }
})();

/*
  Step-by-Step Explanation of Header Section Rendering

  This code dynamically renders the header section of the page based on the user's role, session status, and available actions (such as login, logout, or role-switching).

  1. Define the `renderHeader` Function

     * The `renderHeader` function is responsible for rendering the entire header based on the user's session, role, and whether they are logged in.

  2. Select the Header Div

     * The `headerDiv` variable retrieves the HTML element with the ID `header`, where the header content will be inserted.
       ```javascript
       const headerDiv = document.getElementById("header");
       ```

  3. Check if the Current Page is the Root Page

     * The `window.location.pathname` is checked to see if the current page is the root (`/`). If true, the user's session data (role) is removed from `localStorage`, and the header is rendered without any user-specific elements (just the logo and site title).
       ```javascript
       if (window.location.pathname.endsWith("/")) {
         localStorage.removeItem("userRole");
         headerDiv.innerHTML = `
           <header class="header">
             <div class="logo-section">
               <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
               <span class="logo-title">Hospital CMS</span>
             </div>
           </header>`;
         return;
       }
       ```

  4. Retrieve the User's Role and Token from LocalStorage

     * The `role` (user role like admin, patient, doctor) and `token` (authentication token) are retrieved from `localStorage` to determine the user's current session.
       ```javascript
       const role = localStorage.getItem("userRole");
       const token = localStorage.getItem("token");
       ```

  5. Initialize Header Content

     * The `headerContent` variable is initialized with basic header HTML (logo section), to which additional elements will be added based on the user's role.
       ```javascript
       let headerContent = `<header class="header">
         <div class="logo-section">
           <img src="../assets/images/logo/logo.png" alt="Hospital CRM Logo" class="logo-img">
           <span class="logo-title">Hospital CMS</span>
         </div>
         <nav>`;
       ```

  6. Handle Session Expiry or Invalid Login

     * If a user with a role like `loggedPatient`, `admin`, or `doctor` does not have a valid `token`, the session is considered expired or invalid. The user is logged out, and a message is shown.
       ```javascript
       if ((role === "loggedPatient" || role === "admin" || role === "doctor") && !token) {
         localStorage.removeItem("userRole");
         alert("Session expired or invalid login. Please log in again.");
         window.location.href = "/";   or a specific login page
         return;
       }
       ```

  7. Add Role-Specific Header Content

     * Depending on the user's role, different actions or buttons are rendered in the header:
       - **Admin**: Can add a doctor and log out.
       - **Doctor**: Has a home button and log out.
       - **Patient**: Shows login and signup buttons.
       - **LoggedPatient**: Has home, appointments, and logout options.
       ```javascript
       else if (role === "admin") {
         headerContent += `
           <button id="addDocBtn" class="adminBtn" onclick="openModal('addDoctor')">Add Doctor</button>
           <a href="#" onclick="logout()">Logout</a>`;
       } else if (role === "doctor") {
         headerContent += `
           <button class="adminBtn"  onclick="selectRole('doctor')">Home</button>
           <a href="#" onclick="logout()">Logout</a>`;
       } else if (role === "patient") {
         headerContent += `
           <button id="patientLogin" class="adminBtn">Login</button>
           <button id="patientSignup" class="adminBtn">Sign Up</button>`;
       } else if (role === "loggedPatient") {
         headerContent += `
           <button id="home" class="adminBtn" onclick="window.location.href='/pages/loggedPatientDashboard.html'">Home</button>
           <button id="patientAppointments" class="adminBtn" onclick="window.location.href='/pages/patientAppointments.html'">Appointments</button>
           <a href="#" onclick="logoutPatient()">Logout</a>`;
       }
       ```



  9. Close the Header Section



  10. Render the Header Content

     * Insert the dynamically generated `headerContent` into the `headerDiv` element.
       ```javascript
       headerDiv.innerHTML = headerContent;
       ```

  11. Attach Event Listeners to Header Buttons

     * Call `attachHeaderButtonListeners` to add event listeners to any dynamically created buttons in the header (e.g., login, logout, home).
       ```javascript
       attachHeaderButtonListeners();
       ```


  ### Helper Functions

  13. **attachHeaderButtonListeners**: Adds event listeners to login buttons for "Doctor" and "Admin" roles. If clicked, it opens the respective login modal.

  14. **logout**: Removes user session data and redirects the user to the root page.

  15. **logoutPatient**: Removes the patient's session token and redirects to the patient dashboard.

  16. **Render the Header**: Finally, the `renderHeader()` function is called to initialize the header rendering process when the page loads.
*/
   
