// doctorServices.js
// Centralized API calls for doctor-related operations.

import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

/**
 * Fetch all doctors.
 * Returns an array of doctor objects. On failure, returns [].
 */
export async function getDoctors() {
  try {
    const res = await fetch(DOCTOR_API, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    if (!res.ok) {
      console.warn("getDoctors: non-OK response", res.status);
      return [];
    }

    const data = await res.json().catch(() => null);
    // Support either a raw array or an object with a 'doctors' field
    const doctors = Array.isArray(data) ? data : data?.doctors;
    return Array.isArray(doctors) ? doctors : [];
  } catch (err) {
    console.error("getDoctors error:", err);
    return [];
  }
}

/**
 * Delete a doctor by ID.
 * Returns { success: boolean, message: string, data?: any }
 */
export async function deleteDoctor(id, token) {
  if (!id) {
    return { success: false, message: "Invalid doctor id." };
  }
  if (!token) {
    return { success: false, message: "Missing authentication token." };
  }

  // Include token in query (path param alternative) and also send Authorization header for common backends
  const urlPath = `${DOCTOR_API}/${encodeURIComponent(id)}`;
  const url = `${urlPath}?token=${encodeURIComponent(token)}`;

  try {
    const res = await fetch(url, {
      method: "DELETE",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
    });

    const data = await res.json().catch(() => ({}));
    const success = res.ok && data?.success !== false;
    const message = data?.message || (success ? "Doctor deleted successfully." : "Failed to delete doctor.");

    return { success, message, data };
  } catch (err) {
    console.error("deleteDoctor error:", err);
    return { success: false, message: "Network or server error while deleting doctor." };
  }
}

/**
 * Save (create) a new doctor.
 * Returns { success: boolean, message: string, data?: any }
 */
export async function saveDoctor(doctor, token) {
  if (!doctor || typeof doctor !== "object") {
    return { success: false, message: "Invalid doctor payload." };
  }
  if (!token) {
    return { success: false, message: "Missing authentication token." };
  }

  // Include token as query (path param alternative) and also send Authorization header
  const url = `${DOCTOR_API}?token=${encodeURIComponent(token)}`;

  try {
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(doctor),
    });

    const data = await res.json().catch(() => ({}));
    const success = res.ok && data?.success !== false;
    const message = data?.message || (success ? "Doctor saved successfully." : "Failed to save doctor.");

    return { success, message, data };
  } catch (err) {
    console.error("saveDoctor error:", err);
    return { success: false, message: "Network or server error while saving doctor." };
  }
}

/**
 * Filter doctors using name, time, and specialty.
 * Returns an array of matching doctors. On failure, alerts and returns [].
 */
export async function filterDoctors(name, time, specialty) {
  // Use 'all' placeholder for empty params to align with path-based filtering
  const n = name && name.trim() ? name.trim() : "all";
  const t = time && time.trim() ? time.trim() : "all";
  const s = specialty && specialty.trim() ? specialty.trim() : "all";

  const url = `${DOCTOR_API}/filter/${encodeURIComponent(n)}/${encodeURIComponent(t)}/${encodeURIComponent(s)}`;

  try {
    const res = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/json",
      },
    });

    if (!res.ok) {
      console.warn("filterDoctors: non-OK response", res.status);
      // Per requirements, notify the user on failures
      if (typeof alert === "function") alert("Failed to filter doctors. Please try again.");
      return [];
    }

    const data = await res.json().catch(() => null);
    const list = Array.isArray(data) ? data : data?.doctors;
    return Array.isArray(list) ? list : [];
  } catch (err) {
    console.error("filterDoctors error:", err);
    if (typeof alert === "function") alert("An error occurred while filtering doctors.");
    return [];
  }
}

/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/
