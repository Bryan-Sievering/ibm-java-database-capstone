// javascript
// Consolidated index services: correct-per-role login modals with safe fallbacks.
(() => {
  // ---- API base and endpoints ----
  // Set API_BASE to your backend base path (must match your server's configuration, e.g., "/api")
  const API_BASE =
    window.API_BASE ||
    document.querySelector('meta[name="api-base"]')?.content ||
    ""; // was "/api"


  // Join base + tail ensuring a single slash between them
  function joinUrl(base, tail) {
    if (!base) return tail;
    const b = base.endsWith("/") ? base.slice(0, -1) : base;
    const t = tail.startsWith("/") ? tail : `/${tail}`;
    return `${b}${t}`;
  }

  const API = {
    // Keep doctor/patient as-is
    doctorLogin: joinUrl(API_BASE, "/doctor/login"),
    patientLogin: joinUrl(API_BASE, "/patient/login"),

    // Admin: try several candidates to be backward-compatible
    // Priority:
    //  1) Explicit override via window.ADMIN_LOGIN_URL
    //  2) {API_BASE}/admin  (e.g., "/api/admin")
    //  3) {API_BASE}admin   (handles configs where api.path lacks a trailing slash on server)
    //  4) "/admin"          (legacy)
    //  5) "/admin/login"    (legacy alt)
    adminLoginCandidates: [
      typeof window.ADMIN_LOGIN_URL === "string" ? window.ADMIN_LOGIN_URL : null,
      joinUrl(API_BASE, "/admin"),
      `${API_BASE}admin`,
      "/admin",
      "/admin/login",
    ].filter(Boolean),
  };

  // ---- Helpers ----
  const qs = (id) => document.getElementById(id);

  function ensureModalEl() {
    const modal = qs("modal");
    if (!modal) {
      alert("Modal container with id='modal' not found on this page.");
      return null;
    }
    return modal;
  }

  function showModal() {
    const modal = ensureModalEl();
    if (!modal) return;
    modal.style.display = "block";
  }

  function hideModal() {
    const modal = ensureModalEl();
    if (!modal) return;
    modal.style.display = "none";
  }

  function safeOpenModal(name) {
    // Try app's modal system first
    if (typeof window.openModal === "function") {
      try {
        window.openModal(name);
        return;
      } catch {}
    }
    // Fallback visibility only
    showModal();
  }

  function safeCloseModal() {
    if (typeof window.closeModal === "function") {
      try {
        window.closeModal();
        return;
      } catch {}
    }
    hideModal();
  }

  function storeAuth(role, token) {
    try {
      localStorage.setItem("token", token);
      localStorage.setItem("userRole", role);
    } catch {}
    if (typeof window.selectRole === "function") {
      window.selectRole(role);
    }
  }

  function goToDashboard(role, token) {
    if (role === "admin") {
      window.location.href = `/adminDashboard/${encodeURIComponent(token)}`;
      return;
    }
    if (role === "doctor") {
      window.location.href = `/doctorDashboard/${encodeURIComponent(token)}`;
      return;
    }
    if (role === "patient") {
      window.location.href = "/pages/loggedPatientDashboard.html";
      return;
    }
  }

  // Flexible login fetch: tries multiple URLs until one works
  async function loginFetchFlexible(urls, payload) {
    const tried = [];
    // Allow a cached successful endpoint to be used first for speed
    const cachedKey = "adminLogin.lastGoodUrl";
    const lastGood = localStorage.getItem(cachedKey);
    const list = Array.isArray(urls) ? [...urls] : [urls];
    if (lastGood && !list.includes(lastGood)) list.unshift(lastGood);

    for (const url of list) {
      try {
        tried.push(url);
        const res = await fetch(url, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });

        // If the route exists but credentials are wrong, surface the message
        if (res.status === 400 || res.status === 401) {
          let message = "Invalid credentials";
          try {
            const data = await res.json();
            if (data && data.message) message = data.message;
          } catch {}
          throw new Error(message);
        }

        // Not found or method not allowed -> try next candidate
        if (res.status === 404 || res.status === 405) {
          continue;
        }

        // Any other non-OK -> try to read server message, else try next
        if (!res.ok) {
          try {
            const data = await res.json();
            if (data && data.message) {
              // If server responded with a meaningful error, stop here
              throw new Error(data.message);
            }
          } catch {}
          continue;
        }

        // OK -> parse token
        const data = await res.json().catch(() => ({}));
        const token = data.token || data.accessToken || data.jwt;
        if (!token) throw new Error("Login succeeded but token is missing.");
        // Cache the working endpoint
        localStorage.setItem(cachedKey, url);
        return token;
      } catch (err) {
        // For network failures, continue to next candidate
        if (err instanceof TypeError) {
          continue;
        }
        // For explicit credential errors, stop immediately
        if (String(err.message || "").toLowerCase().includes("invalid")) {
          throw err;
        }
        // Otherwise, try the next candidate
        continue;
      }
    }
    throw new Error("Login endpoint not found. Please try again later.");
  }

  async function loginFetch(url, payload) {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      let message = "Invalid credentials";
      try {
        const data = await res.json();
        if (data && data.message) message = data.message;
      } catch {}
      throw new Error(message);
    }

    const data = await res.json().catch(() => ({}));
    const token = data.token || data.accessToken || data.jwt;
    if (!token) throw new Error("Login succeeded but token is missing.");
    return token;
  }

  // ---- Fallback renderers (replace modal content per role) ----
  function renderFallbackLogin(modal, role) {
    modal.dataset.currentRole = role;

    const commonHead = `
      <div class="modal-backdrop" style="position:fixed;inset:0;background:rgba(0,0,0,0.5);"></div>
      <div class="modal-dialog" style="position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:#fff;border-radius:8px;min-width:320px;max-width:90%;box-shadow:0 10px 30px rgba(0,0,0,0.3);">
        <div class="modal-content" style="padding:20px;position:relative;">
          <button type="button" class="modal-close" data-modal-close aria-label="Close" style="position:absolute;right:10px;top:10px;border:none;background:transparent;font-size:20px;cursor:pointer;">×</button>
    `;

    const commonFoot = `
        </div>
      </div>
    `;

    if (role === "admin") {
      modal.innerHTML = `
        ${commonHead}
          <h2 style="margin-top:0;">Admin Login</h2>
          <form id="adminLoginForm">
            <div style="margin-bottom:10px;">
              <label for="adminUsername">Username</label>
              <input type="text" id="adminUsername" required style="display:block;width:100%;padding:8px;margin-top:4px;">
            </div>
            <div style="margin-bottom:16px;">
              <label for="adminPassword">Password</label>
              <input type="password" id="adminPassword" required style="display:block;width:100%;padding:8px;margin-top:4px;">
            </div>
            <button type="submit" style="padding:10px 16px;cursor:pointer;">Log in</button>
          </form>
        ${commonFoot}
      `;
      modal.querySelector("#adminLoginForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        await window.adminLoginHandler();
      });
      showModal();
      return;
    }

    if (role === "doctor") {
      modal.innerHTML = `
        ${commonHead}
          <h2 style="margin-top:0;">Doctor Login</h2>
          <form id="doctorLoginForm">
            <div style="margin-bottom:10px;">
              <label for="doctorEmail">Email</label>
              <input type="email" id="doctorEmail" required style="display:block;width:100%;padding:8px;margin-top:4px;">
            </div>
            <div style="margin-bottom:16px;">
              <label for="doctorPassword">Password</label>
              <input type="password" id="doctorPassword" required style="display:block;width:100%;padding:8px;margin-top:4px;">
            </div>
            <button type="submit" style="padding:10px 16px;cursor:pointer;">Log in</button>
          </form>
        ${commonFoot}
      `;
      modal.querySelector("#doctorLoginForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        await window.doctorLoginHandler();
      });
      showModal();
      return;
    }

    // patient
    modal.innerHTML = `
      ${commonHead}
        <h2 style="margin-top:0;">Patient Login</h2>
        <form id="patientLoginForm">
          <div style="margin-bottom:10px;">
            <label for="patientEmail">Email</label>
            <input type="email" id="patientEmail" required style="display:block;width:100%;padding:8px;margin-top:4px;">
          </div>
          <div style="margin-bottom:16px;">
            <label for="patientPassword">Password</label>
            <input type="password" id="patientPassword" required style="display:block;width:100%;padding:8px;margin-top:4px;">
          </div>
          <button type="submit" style="padding:10px 16px;cursor:pointer;">Log in</button>
        </form>
      ${commonFoot}
    `;
    modal.querySelector("#patientLoginForm").addEventListener("submit", async (e) => {
      e.preventDefault();
      await window.patientLoginHandler();
    });
    showModal();
  }

  function ensureLoginModal(role) {
    const modal = ensureModalEl();
    if (!modal) return;

    // First, try app’s modal system with a named modal if available
    const modalName = role + "Login";
    safeOpenModal(modalName);

    // After this tick, if the modal doesn't contain the correct form, inject fallback for that role
    setTimeout(() => {
      const hasCorrectForm =
        (role === "admin" && modal.querySelector("#adminLoginForm")) ||
        (role === "doctor" && modal.querySelector("#doctorLoginForm")) ||
        (role === "patient" && modal.querySelector("#patientLoginForm"));

      if (!hasCorrectForm || modal.dataset.currentRole !== role) {
        renderFallbackLogin(modal, role);
      }
      showModal();
    }, 0);
  }

  // ---- Global close wiring (X button, backdrop) ----
  document.addEventListener(
    "click",
    (e) => {
      const modal = qs("modal");
      if (!modal) return;

      if (
        e.target.matches("[data-modal-close]") ||
        e.target.closest("[data-modal-close]") ||
        e.target.matches(".modal-close") ||
        e.target.closest(".modal-close") ||
        (e.target.getAttribute && e.target.getAttribute("aria-label") === "Close")
      ) {
        e.preventDefault();
        safeCloseModal();
        return;
      }

      if (e.target === modal || e.target.closest(".modal-backdrop")) {
        e.preventDefault();
        safeCloseModal();
      }
    },
    true
  );

  // ---- Button wiring (IDs or data attributes) ----
  function wireButtonToRole(el, role) {
    if (!el) return;
    // Prevent default navigation and force correct role modal
    el.addEventListener(
      "click",
      (e) => {
        e.preventDefault();
        e.stopImmediatePropagation();
        ensureLoginModal(role);
      },
      { capture: true }
    );
    // Neutralize anchor hrefs so they don't navigate
    if (el.tagName && el.tagName.toLowerCase() === "a") {
      el.dataset.originalHref = el.getAttribute("href") || "";
      el.setAttribute("href", "#");
      el.setAttribute("role", "button");
    }
    // Remove inline onclick if present to avoid redirects
    if (el.getAttribute && el.getAttribute("onclick")) {
      el.removeAttribute("onclick");
    }
  }

  window.addEventListener("DOMContentLoaded", () => {
    // Preferred IDs
    wireButtonToRole(qs("btnAdmin"), "admin");
    wireButtonToRole(qs("btnDoctor"), "doctor");
    wireButtonToRole(qs("btnPatient"), "patient");

    // Optional: support data-login="admin|doctor|patient" on elements
    document.querySelectorAll("[data-login]").forEach((el) => {
      const role = (el.getAttribute("data-login") || "").toLowerCase();
      if (role === "admin" || role === "doctor" || role === "patient") {
        wireButtonToRole(el, role);
      }
    });
  });

  // ---- Guard: block navigating to logged patient page without token ----
  document.addEventListener(
    "click",
    (e) => {
      const a = e.target.closest("a");
      if (!a) return;
      const href = (a.getAttribute("href") || "").trim();
      if (!href) return;

      const token = localStorage.getItem("token");
      // Only guard the patient dashboard page
      const isLoggedPatientPage = /\/pages\/loggedPatientDashboard\.html(\?|#|$)/i.test(href);
      if (isLoggedPatientPage && !token) {
        e.preventDefault();
        e.stopImmediatePropagation();
        ensureLoginModal("patient");
      }
    },
    true
  );

  // ---- Optional: react to a custom role:selected event ----
  document.addEventListener("role:selected", (e) => {
    const role = (e?.detail?.role || "").toLowerCase();
    if (role === "admin" || role === "doctor" || role === "patient") {
      ensureLoginModal(role);
    }
  });

  // ---- Login handlers ----
  window.adminLoginHandler = async function adminLoginHandler() {
    try {
      const username = (qs("adminUsername") || qs("username"))?.value?.trim();
      const password = (qs("adminPassword") || qs("password"))?.value || "";
      if (!username || !password) {
        alert("Please enter username and password.");
        return false;
      }

      const token = await loginFetchFlexible(API.adminLoginCandidates, { username, password });
      storeAuth("admin", token);
      safeCloseModal();
      goToDashboard("admin", token);
      return true;
    } catch (err) {
      console.error("adminLoginHandler:", err);
      alert("❌ " + err.message);
      return false;
    }
  };

  window.doctorLoginHandler = async function doctorLoginHandler() {
    try {
      const identifier =
        (qs("doctorEmail") || qs("doctorIdentifier") || qs("email") || qs("identifier"))?.value?.trim() || "";
      const password = (qs("doctorPassword") || qs("password"))?.value || "";
      if (!identifier || !password) {
        alert("Please enter email and password.");
        return false;
      }

      const token = await loginFetch(API.doctorLogin, { identifier, password });
      storeAuth("doctor", token);
      safeCloseModal();
      goToDashboard("doctor", token);
      return true;
    } catch (err) {
      console.error("doctorLoginHandler:", err);
      alert("❌ " + err.message);
      return false;
    }
  };

  window.patientLoginHandler = async function patientLoginHandler() {
    try {
      const identifier =
        (qs("patientEmail") || qs("email") || qs("identifier"))?.value?.trim() || "";
      const password = (qs("patientPassword") || qs("password"))?.value || "";
      if (!identifier || !password) {
        alert("Please enter email and password.");
        return false;
      }

      const token = await loginFetch(API.patientLogin, { identifier, password });
      storeAuth("patient", token);
      safeCloseModal();
      goToDashboard("patient", token);
      return true;
    } catch (err) {
      console.error("patientLoginHandler:", err);
      alert("❌ " + err.message);
      return false;
    }
  };


  // Expose if needed
  window.goToDashboard = goToDashboard;
})();

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
