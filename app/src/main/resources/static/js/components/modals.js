// javascript
// Helpers to control modal visibility reliably
const HIDE_CLASSES = ['hidden', 'invisible', 'opacity-0', 'pointer-events-none', 'modal--hidden', 'is-hidden'];

function showModalBase() {
  const m = document.getElementById('modal');
  const c = document.getElementById('modal-content') || m?.querySelector('.modal-content');
  if (!m) return;

  m.removeAttribute('aria-hidden');
  HIDE_CLASSES.forEach(cls => m.classList.remove(cls));
  if (c) HIDE_CLASSES.forEach(cls => c.classList.remove(cls));

  m.style.display = 'block';
  m.style.visibility = 'visible';
  m.style.pointerEvents = 'auto';
  if (!m.style.zIndex) m.style.zIndex = '9999';

  requestAnimationFrame(() => {
    m.classList.add('is-open');
    m.style.opacity = '1';
    if (c) {
      c.style.opacity = '1';
      c.style.transform = 'none';
    }
  });
}

function hideModalBase() {
  const m = document.getElementById('modal');
  const c = document.getElementById('modal-content') || m?.querySelector('.modal-content');
  if (!m) return;

  m.classList.remove('is-open');
  m.style.opacity = '0';
  if (c) c.style.opacity = '0';

  const done = () => {
    m.style.display = 'none';
    m.style.pointerEvents = 'none';
    m.setAttribute('aria-hidden', 'true');
    HIDE_CLASSES.forEach(cls => m.classList.add(cls));
    if (c) HIDE_CLASSES.forEach(cls => c.classList.add(cls));
    m.removeEventListener('transitionend', done);
  };

  m.addEventListener('transitionend', done, { once: true });
  setTimeout(done, 300); // fallback if no transitionend
}

export function openModal(type) {
  // Ensure the modal DOM exists on the current page
  ensureModalDOM();

  let modalContent = '';
  if (type === 'addDoctor') {
    modalContent = `
         <h2>Add Doctor</h2>
         <input type="text" id="doctorName" placeholder="Doctor Name" class="input-field">
         <select id="specialization" class="input-field select-dropdown">
             <option value="">Specialization</option>
             <option value="cardiologist">Cardiologist</option>
             <option value="dermatologist">Dermatologist</option>
             <option value="neurologist">Neurologist</option>
             <option value="pediatrician">Pediatrician</option>
             <option value="orthopedic">Orthopedic</option>
             <option value="gynecologist">Gynecologist</option>
             <option value="psychiatrist">Psychiatrist</option>
             <option value="dentist">Dentist</option>
             <option value="ophthalmologist">Ophthalmologist</option>
             <option value="ent">ENT Specialist</option>
             <option value="urologist">Urologist</option>
             <option value="oncologist">Oncologist</option>
             <option value="gastroenterologist">Gastroenterologist</option>
             <option value="general">General Physician</option>
        </select>
        <input type="email" id="doctorEmail" placeholder="Email" class="input-field">
        <input type="password" id="doctorPassword" placeholder="Password" class="input-field">
        <input type="text" id="doctorPhone" placeholder="Mobile No." class="input-field">
        <div class="availability-container">
          <label class="availabilityLabel">Select Availability:</label>
          <div class="checkbox-group">
              <label><input type="checkbox" name="availability" value="09:00-10:00"> 9:00 AM - 10:00 AM</label>
              <label><input type="checkbox" name="availability" value="10:00-11:00"> 10:00 AM - 11:00 AM</label>
              <label><input type="checkbox" name="availability" value="11:00-12:00"> 11:00 AM - 12:00 PM</label>
              <label><input type="checkbox" name="availability" value="12:00-13:00"> 12:00 PM - 1:00 PM</label>
          </div>
        </div>
        <button class="dashboard-btn" id="saveDoctorBtn">Save</button>
      `;
  } else if (type === 'patientLogin') {
    modalContent = `
        <h2>Patient Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="loginBtn">Login</button>
      `;
  } else if (type === "patientSignup") {
    modalContent = `
      <h2>Patient Signup</h2>
      <input type="text" id="name" placeholder="Name" class="input-field">
      <input type="email" id="email" placeholder="Email" class="input-field">
      <input type="password" id="password" placeholder="Password" class="input-field">
      <input type="text" id="phone" placeholder="Phone" class="input-field">
      <input type="text" id="address" placeholder="Address" class="input-field">
      <button class="dashboard-btn" id="signupBtn">Signup</button>
    `;
  } else if (type === 'adminLogin') {
    modalContent = `
        <h2>Admin Login</h2>
        <input type="text" id="username" name="username" placeholder="Username" class="input-field">
        <input type="password" id="password" name="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="adminLoginBtn">Login</button>
      `;
  } else if (type === 'doctorLogin') {
    modalContent = `
        <h2>Doctor Login</h2>
        <input type="text" id="email" placeholder="Email" class="input-field">
        <input type="password" id="password" placeholder="Password" class="input-field">
        <button class="dashboard-btn" id="doctorLoginBtn">Login</button>
      `;
  }

  const modalBody = document.getElementById('modal-body');
  const modal = document.getElementById('modal');
  const closeBtn = document.getElementById('closeModal');

  modalBody.innerHTML = modalContent;

  // Show using the base helper to avoid opacity:0 issues
  showModalBase();

  if (closeBtn) {
    closeBtn.onclick = () => {
      hideModalBase();
    };
  }

  // Close on backdrop click (bind once)
  if (!modal.dataset.boundBackdrop) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) hideModalBase();
    });
    modal.dataset.boundBackdrop = '1';
  }

  // Hook handlers from window.* (if defined elsewhere)
  if (type === "patientSignup") {
    const el = document.getElementById("signupBtn");
    if (el && typeof window.signupPatient === "function") el.addEventListener("click", window.signupPatient);
  }
  if (type === "patientLogin") {
    const el = document.getElementById("loginBtn");
    if (el && typeof window.loginPatient === "function") el.addEventListener("click", window.loginPatient);
  }
  if (type === 'addDoctor') {
    const el = document.getElementById('saveDoctorBtn');
    if (el && typeof window.adminAddDoctor === "function") el.addEventListener('click', window.adminAddDoctor);
  }
  if (type === 'adminLogin') {
    const el = document.getElementById('adminLoginBtn');
    if (el && typeof window.adminLoginHandler === "function") el.addEventListener('click', window.adminLoginHandler);
  }
  if (type === 'doctorLogin') {
    const el = document.getElementById('doctorLoginBtn');
    if (el && typeof window.doctorLoginHandler === "function") el.addEventListener('click', window.doctorLoginHandler);
  }
}

// Create the modal container if it doesn't exist
function ensureModalDOM() {
  let modal = document.getElementById('modal');
  if (modal) return;

  modal = document.createElement('div');
  modal.id = 'modal';
  modal.style.cssText = 'position:fixed;inset:0;display:none;background:rgba(0,0,0,.5);z-index:9999;opacity:0;transition:opacity 150ms ease;';
  modal.setAttribute('aria-hidden', 'true');

  const content = document.createElement('div');
  content.id = 'modal-content';
  content.style.cssText = 'background:#fff;max-width:520px;margin:10vh auto;padding:16px;border-radius:8px;position:relative;transition:opacity 150ms ease, transform 150ms ease;opacity:0;transform:scale(0.98);';

  const close = document.createElement('button');
  close.id = 'closeModal';
  close.textContent = '×';
  close.setAttribute('aria-label', 'Close');
  close.style.cssText = 'position:absolute;top:8px;right:8px;font-size:24px;background:none;border:none;cursor:pointer;';

  const body = document.createElement('div');
  body.id = 'modal-body';

  content.appendChild(close);
  content.appendChild(body);
  modal.appendChild(content);
  document.body.appendChild(modal);

  // Close on ESC (bind once at creation)
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.style.display !== 'none') {
      hideModalBase();
    }
  });
}

// Keep openModal available for other modules that call it
if (typeof window !== "undefined") {
  window.openModal = openModal;
}