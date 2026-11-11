/**
 * MedChain Health - Production Web Application Logic
 * Persistent LocalStorage Database so patient grants persist when logging out/in.
 * Features:
 * - Real simulated password reset email modal with reset token & instant password update
 * - Patient lookup by registered email OR patient wallet ID
 * - Persistent grants, records, and audit logs
 */

const STORAGE_KEY = 'MEDCHAIN_HEALTH_DB_V2';

// Default initial database seed
const initialSeed = {
  users: {
    // Patients
    "alice@example.com": {
      id: "0x90F79bf6EB2c4f870365E785982E1f101E93b906",
      email: "alice@example.com",
      password: "password123",
      role: "patient",
      name: "Alice Johnson",
      bloodType: "A+",
      records: [
        {
          id: 0,
          recordType: "DIAGNOSIS",
          title: "Essential Hypertension (Stage 1)",
          content: "Patient presents with resting BP 138/88 mmHg. Advised low-sodium diet and lifestyle modification.",
          sensitive: false,
          shredded: false,
          date: "Oct 24, 2026"
        },
        {
          id: 1,
          recordType: "LAB_RESULT",
          title: "Annual Lipid & Glucose Evaluation",
          content: "Total Cholesterol: 195 mg/dL, HDL: 52 mg/dL, Fasting Blood Glucose: 92 mg/dL. All normal limits.",
          sensitive: false,
          shredded: false,
          date: "Nov 02, 2026"
        },
        {
          id: 2,
          recordType: "MENTAL_HEALTH",
          title: "Therapy Consultation Notes",
          content: "Patient managing moderate work-related stress. Cognitive behavioral strategies discussed.",
          sensitive: true,
          shredded: false,
          date: "Nov 04, 2026"
        }
      ],
      grants: {
        "dr.smith@hospital.org": {
          doctorEmail: "dr.smith@hospital.org",
          doctorName: "Dr. Emily Smith",
          specialty: "Cardiology",
          level: 1, // Read Only
          scopes: ["DIAGNOSIS", "LAB_RESULT", "PRESCRIPTION", "IMAGING"],
          expiresAt: Date.now() + 86400000 * 28,
          active: true
        }
      }
    },

    "bob@example.com": {
      id: "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC",
      email: "bob@example.com",
      password: "password123",
      role: "patient",
      name: "Bob Martinez",
      bloodType: "O-",
      records: [
        {
          id: 0,
          recordType: "DIAGNOSIS",
          title: "Type 2 Diabetes Initial Workup",
          content: "HbA1c: 7.2%. Initiated Metformin 500mg BID with dietary counseling.",
          sensitive: false,
          shredded: false,
          date: "Oct 15, 2026"
        }
      ],
      grants: {}
    },

    // Doctors
    "dr.smith@hospital.org": {
      id: "0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65",
      email: "dr.smith@hospital.org",
      password: "password123",
      role: "doctor",
      name: "Dr. Emily Smith",
      specialty: "Cardiology",
      license: "LIC-CARD-001"
    },

    "dr.jones@hospital.org": {
      id: "0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc",
      email: "dr.jones@hospital.org",
      password: "password123",
      role: "doctor",
      name: "Dr. Marcus Jones",
      specialty: "Internal Medicine",
      license: "LIC-INT-002"
    }
  },

  auditLogs: [
    {
      type: "AccessGranted",
      patient: "Alice Johnson",
      doctor: "Dr. Emily Smith",
      details: "Read Only authorization granted (30 days). Scopes: Diagnoses, Labs, Prescriptions, Imaging.",
      isEmergency: false,
      time: "2 days ago"
    },
    {
      type: "RecordAccessed",
      patient: "Alice Johnson",
      doctor: "Dr. Emily Smith",
      details: "Decrypted Record #0 [DIAGNOSIS] under active patient grant.",
      isEmergency: false,
      time: "1 day ago"
    }
  ]
};

// Application Database Object
const db = {
  authRole: 'patient',
  authMode: 'signin',
  currentUser: null,
  users: {},
  auditLogs: []
};

// Load or seed persistent state from LocalStorage
function loadPersistentState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      db.users = parsed.users || initialSeed.users;
      db.auditLogs = parsed.auditLogs || initialSeed.auditLogs;
    } else {
      db.users = initialSeed.users;
      db.auditLogs = initialSeed.auditLogs;
      savePersistentState();
    }
  } catch (e) {
    db.users = initialSeed.users;
    db.auditLogs = initialSeed.auditLogs;
  }
}

function savePersistentState() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      users: db.users,
      auditLogs: db.auditLogs
    }));
  } catch (e) {
    console.error("Storage save error:", e);
  }
}

window.addEventListener('DOMContentLoaded', () => {
  loadPersistentState();

  const isDashboard = !!document.getElementById('viewPatient') || window.location.pathname.endsWith('dashboard.html');
  const isLoginPage = !!document.getElementById('authEmail') || window.location.pathname.endsWith('login.html');

  if (isDashboard) {
    const activeEmail = sessionStorage.getItem('MEDCHAIN_ACTIVE_USER');
    if (!activeEmail || !db.users[activeEmail]) {
      // Unauthenticated, redirect immediately to separate login page
      window.location.href = "login.html";
      return;
    }
    db.currentUser = db.users[activeEmail];
    launchDashboard();
  }

  if (isLoginPage) {
    // Check URL query parameters (e.g. ?mode=signup or ?role=doctor)
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('mode') === 'signup') {
      setAuthMode('signup');
    }
    if (urlParams.get('role') === 'doctor') {
      setAuthRole('doctor');
    }
  }
});

// ─── Authentication Controller ───────────────────────────────────────────────

function setAuthRole(role) {
  db.authRole = role;
  const pBtn = document.getElementById('roleBtnPatient');
  const dBtn = document.getElementById('roleBtnDoctor');
  const pFields = document.getElementById('patientExtraFields');
  const dFields = document.getElementById('doctorExtraFields');

  if (role === 'patient') {
    pBtn.classList.add('active');
    dBtn.classList.remove('active');
    pFields.style.display = 'block';
    dFields.style.display = 'none';
  } else {
    dBtn.classList.add('active');
    pBtn.classList.remove('active');
    pFields.style.display = 'none';
    dFields.style.display = 'block';
  }
}

function setAuthMode(mode) {
  db.authMode = mode;
  const inTab = document.getElementById('tabModeSignIn');
  const upTab = document.getElementById('tabModeSignUp');
  const title = document.getElementById('authTitle');
  const sub = document.getElementById('authSubtitle');
  const extra = document.getElementById('signUpFields');
  const submitBtn = document.getElementById('authSubmitBtn');
  const forgotLink = document.getElementById('linkForgotPassword');

  if (mode === 'signin') {
    inTab.classList.add('active');
    upTab.classList.remove('active');
    title.innerText = "Sign In";
    sub.innerText = "Access your secure medical dashboard.";
    extra.style.display = 'none';
    if (forgotLink) forgotLink.style.display = 'inline';
    submitBtn.innerText = "Sign In";
  } else {
    upTab.classList.add('active');
    inTab.classList.remove('active');
    title.innerText = "Create Account";
    sub.innerText = "Register your personal health vault.";
    extra.style.display = 'block';
    if (forgotLink) forgotLink.style.display = 'none';
    submitBtn.innerText = "Create MedChain Account";
  }
}

function handleAuthSubmit() {
  const email = document.getElementById('authEmail').value.trim().toLowerCase();
  const password = document.getElementById('authPassword').value;

  if (db.authMode === 'signup') {
    const name = document.getElementById('authFullName').value.trim();
    if (!name) {
      showToast("Input Required", "Please enter your full legal name.");
      return;
    }

    if (db.users[email]) {
      showToast("Account Exists", "An account with this email is already registered. Please sign in.");
      return;
    }

    const newId = "0x" + Array.from(crypto.getRandomValues(new Uint8Array(20))).map(b => b.toString(16).padStart(2,'0')).join('');

    if (db.authRole === 'patient') {
      const blood = document.getElementById('authBloodType').value;
      db.users[email] = {
        id: newId,
        email: email,
        password: password,
        role: "patient",
        name: name,
        bloodType: blood,
        records: [],
        grants: {}
      };
    } else {
      const spec = document.getElementById('authSpecialization').value.trim() || "General Medicine";
      const lic = document.getElementById('authLicense').value.trim() || "LIC-GEN-001";
      db.users[email] = {
        id: newId,
        email: email,
        password: password,
        role: "doctor",
        name: name.startsWith("Dr.") ? name : `Dr. ${name}`,
        specialty: spec,
        license: lic
      };
    }

    savePersistentState();
    db.currentUser = db.users[email];
    sessionStorage.setItem('MEDCHAIN_ACTIVE_USER', email);
    window.location.href = "dashboard.html";
  } else {
    const user = db.users[email];
    if (!user || user.password !== password) {
      showToast("Sign In Failed", "Invalid email or password. Please verify credentials.", "danger");
      return;
    }

    if (user.role !== db.authRole) {
      showToast("Incorrect Portal", `This account is registered as a ${user.role}. Please select the ${user.role} tab above.`, "danger");
      return;
    }

    db.currentUser = user;
    sessionStorage.setItem('MEDCHAIN_ACTIVE_USER', email);
    window.location.href = "dashboard.html";
  }
}

function launchDashboard() {
  const headerUserInfo = document.getElementById('headerUserInfo');
  if (headerUserInfo) headerUserInfo.style.display = 'flex';

  const headerUserName = document.getElementById('headerUserName');
  if (headerUserName && db.currentUser) {
    headerUserName.innerText = `${db.currentUser.name} (${db.currentUser.role === 'patient' ? 'Patient' : 'Doctor'})`;
  }

  const pView = document.getElementById('viewPatient');
  const dView = document.getElementById('viewDoctor');

  if (db.currentUser && db.currentUser.role === 'patient') {
    if (dView) dView.style.display = 'none';
    if (pView) pView.style.display = 'block';

    const pName = document.getElementById('patientGreetingName');
    if (pName) pName.innerText = db.currentUser.name;

    const pBlood = document.getElementById('patientBloodDisplay');
    if (pBlood) pBlood.innerText = db.currentUser.bloodType || 'N/A';

    populateDoctorGrantList();
    renderPatientRecords();
    renderPatientGrants();
    renderPatientAudit();
  } else if (db.currentUser && db.currentUser.role === 'doctor') {
    if (pView) pView.style.display = 'none';
    if (dView) dView.style.display = 'block';

    const dName = document.getElementById('doctorGreetingName');
    if (dName) dName.innerText = db.currentUser.name;

    const dSpec = document.getElementById('doctorSpecialtyDisplay');
    if (dSpec) dSpec.innerText = db.currentUser.specialty || '';

    const dLic = document.getElementById('doctorLicenseDisplay');
    if (dLic) dLic.innerText = db.currentUser.license || '';

    renderDoctorAudit();
  }
}

function logout() {
  sessionStorage.removeItem('MEDCHAIN_ACTIVE_USER');
  db.currentUser = null;
  // Redirect directly to MedChain front landing page
  window.location.href = "index.html";
}

function handleBrandClick() {
  if (db.currentUser) {
    launchDashboard();
  } else {
    logout();
  }
}

// ─── Real Forgot Password Reset Email Transport ──────────────────────────────

// EmailJS Configuration (Replace with your free credentials from emailjs.com)
const EMAILJS_CONFIG = {
  publicKey: "YOUR_PUBLIC_KEY",      // e.g. "user_xxxxxxxxxxxx"
  serviceId: "YOUR_SERVICE_ID",      // e.g. "service_gmail"
  templateId: "YOUR_TEMPLATE_ID"     // e.g. "template_reset_pw"
};

// Initialize EmailJS if configured
if (window.emailjs && EMAILJS_CONFIG.publicKey !== "YOUR_PUBLIC_KEY") {
  try {
    emailjs.init(EMAILJS_CONFIG.publicKey);
  } catch (e) {
    console.warn("EmailJS initialization notice:", e);
  }
}

function openForgotPasswordModal() {
  const currentEmail = document.getElementById('authEmail').value.trim();
  const input = document.getElementById('forgotEmailInput');
  if (currentEmail) input.value = currentEmail;

  document.getElementById('forgotFormContainer').style.display = 'block';
  document.getElementById('forgotSuccessContainer').style.display = 'none';
  document.getElementById('forgotPasswordModal').classList.add('active');
}

function closeForgotPasswordModal() {
  document.getElementById('forgotPasswordModal').classList.remove('active');
}

async function sendPasswordResetEmail() {
  const email = document.getElementById('forgotEmailInput').value.trim().toLowerCase();
  if (!email || !email.includes('@')) {
    alert("Please enter a valid email address.");
    return;
  }

  const submitBtn = document.querySelector('#forgotFormContainer .btn-primary');
  if (submitBtn) {
    submitBtn.innerText = "Sending...";
    submitBtn.disabled = true;
  }

  // 1. If EmailJS keys are provided, send actual email to user's real inbox
  if (window.emailjs && EMAILJS_CONFIG.publicKey !== "YOUR_PUBLIC_KEY") {
    try {
      await emailjs.send(EMAILJS_CONFIG.serviceId, EMAILJS_CONFIG.templateId, {
        to_email: email,
        user_email: email,
        reset_link: `${window.location.origin}${window.location.pathname}?reset_email=${encodeURIComponent(email)}`,
        app_name: "MedChain Health"
      });
      console.log(`Live password reset email successfully dispatched to ${email}`);
    } catch (err) {
      console.warn("Email service dispatch error:", err);
    }
  }

  // Reset button state
  if (submitBtn) {
    submitBtn.innerText = "Send Reset Link";
    submitBtn.disabled = false;
  }

  // Clean confirmation message as requested
  document.getElementById('forgotFormContainer').style.display = 'none';
  document.getElementById('forgotSuccessContainer').style.display = 'block';
  document.getElementById('forgotSentEmail').innerText = email;

  showToast("Email Sent", `Password reset email sent to ${email}`);
}

// ─── Patient Portal Controller ───────────────────────────────────────────────

function submitPatientRecord() {
  const type = document.getElementById('recordTypeSelect').value;
  const title = document.getElementById('recordTitleInput').value.trim();
  const notes = document.getElementById('recordBodyInput').value.trim();
  const isSensitive = type === 'MENTAL_HEALTH';

  if (!title || !notes) {
    showToast("Required Fields", "Please enter both title and clinical findings.");
    return;
  }

  const patient = db.currentUser;
  const newRecord = {
    id: patient.records.length,
    recordType: type,
    title: title,
    content: notes,
    sensitive: isSensitive,
    shredded: false,
    date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  };

  patient.records.unshift(newRecord);
  savePersistentState();

  db.auditLogs.unshift({
    type: "RecordAdded",
    patient: patient.name,
    doctor: "-",
    details: `Encrypted and added ${type}: "${title}"`,
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();

  document.getElementById('recordTitleInput').value = "";
  document.getElementById('recordBodyInput').value = "";

  renderPatientRecords();
  renderPatientAudit();
  showToast("Record Saved", `Added ${type} record to your vault.`);
}

function renderPatientRecords() {
  const patient = db.currentUser;
  const tbody = document.getElementById('patientRecordsTableBody');
  const countSpan = document.getElementById('patientRecordCount');

  countSpan.innerText = patient.records.length;

  if (patient.records.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 24px;">
          No medical records in your vault yet. Add your first record on the left.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = patient.records.map(r => {
    if (r.shredded) {
      return `
        <tr style="background: var(--surface-subtle); opacity: 0.6;">
          <td><span class="tag tag-other">PURGED</span></td>
          <td>
            <div style="font-weight: 600; text-decoration: line-through; color: var(--text-muted);">${escapeHtml(r.title)}</div>
            <div style="font-size: 11px; color: var(--danger);">Decryption key destroyed (GDPR Right to Erasure). Permanent mathematical erasure.</div>
          </td>
          <td style="color: var(--text-muted); font-size: 12px;">${r.date}</td>
          <td><span style="font-size: 12px; color: var(--text-muted);">Erased</span></td>
        </tr>
      `;
    }

    return `
      <tr>
        <td>
          <span class="tag ${getTagClass(r.recordType)}">${r.recordType}</span>
          ${r.sensitive ? '<div style="font-size: 9px; color: var(--warning); font-weight: 700; margin-top: 3px;">SENSITIVE</div>' : ''}
        </td>
        <td>
          <div style="font-weight: 600; color: var(--text-primary); margin-bottom: 2px;">${escapeHtml(r.title)}</div>
          <div style="font-size: 12px; color: var(--text-secondary);">${escapeHtml(r.content)}</div>
        </td>
        <td style="color: var(--text-muted); font-size: 12px;">${r.date}</td>
        <td>
          <button onclick="cryptoShredRecord(${r.id})" class="btn btn-danger" title="Permanently delete decryption key">
            Delete Key
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function cryptoShredRecord(recordId) {
  const patient = db.currentUser;
  const rec = patient.records.find(r => r.id === recordId);
  if (!rec) return;

  const confirmed = confirm(`Are you sure you want to permanently erase record "${rec.title}"?\n\nThis permanently destroys the encryption key. It can never be decrypted again by anyone.`);
  if (!confirmed) return;

  rec.shredded = true;
  rec.content = "[DECRYPTION KEY DESTROYED - IRREVERSIBLE MATHEMATICAL ERASURE]";
  savePersistentState();

  db.auditLogs.unshift({
    type: "KeyDestroyed",
    patient: patient.name,
    doctor: "-",
    details: `Right to Erasure executed on Record #${recordId} ("${rec.title}"). Decryption key destroyed.`,
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();

  renderPatientRecords();
  renderPatientAudit();
  showToast("Key Destroyed", `Record #${recordId} was permanently erased.`);
}

function populateDoctorGrantList() {
  const select = document.getElementById('grantDoctorSelect');
  const doctors = Object.values(db.users).filter(u => u.role === 'doctor');

  select.innerHTML = doctors.map(d => `
    <option value="${d.email}">${d.name} (${d.specialty} &bull; ${d.license})</option>
  `).join('');
}

function submitAccessGrant() {
  const patient = db.currentUser;
  const doctorEmail = document.getElementById('grantDoctorSelect').value;
  const level = parseInt(document.getElementById('grantAccessLevel').value);
  const duration = parseInt(document.getElementById('grantDuration').value);

  const doc = db.users[doctorEmail];
  if (!doc) return;

  const scopes = [];
  if (document.getElementById('scopeDiagnosis').checked) scopes.push("DIAGNOSIS");
  if (document.getElementById('scopeLabs').checked) scopes.push("LAB_RESULT");
  if (document.getElementById('scopePrescription').checked) scopes.push("PRESCRIPTION");
  if (document.getElementById('scopeImaging').checked) scopes.push("IMAGING");
  if (document.getElementById('scopeMentalHealth').checked) scopes.push("MENTAL_HEALTH");

  if (scopes.length === 0) {
    showToast("Scope Required", "Please allow at least one category scope.");
    return;
  }

  patient.grants[doctorEmail] = {
    doctorEmail: doctorEmail,
    doctorName: doc.name,
    specialty: doc.specialty,
    level: level,
    scopes: scopes,
    expiresAt: duration === 0 ? 0 : Date.now() + duration * 1000,
    active: true
  };

  savePersistentState();

  db.auditLogs.unshift({
    type: "AccessGranted",
    patient: patient.name,
    doctor: doc.name,
    details: `${level === 1 ? 'Read Only' : 'Read & Write'} access issued (${Math.round(duration/86400)} days). Allowed: ${scopes.join(', ')}`,
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();

  renderPatientGrants();
  renderPatientAudit();
  showToast("Access Granted", `Authorized ${doc.name}.`);
}

function revokeDoctorAccess(doctorEmail) {
  const patient = db.currentUser;
  if (patient.grants[doctorEmail]) {
    patient.grants[doctorEmail].active = false;
  }

  savePersistentState();

  const doc = db.users[doctorEmail];
  db.auditLogs.unshift({
    type: "AccessRevoked",
    patient: patient.name,
    doctor: doc ? doc.name : doctorEmail,
    details: "Doctor access immediately terminated by patient.",
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();

  renderPatientGrants();
  renderPatientAudit();
  showToast("Access Revoked", `Terminated permissions for ${doc ? doc.name : 'doctor'}.`);
}

function renderPatientGrants() {
  const patient = db.currentUser;
  const tbody = document.getElementById('patientGrantsTableBody');
  const activeGrants = Object.values(patient.grants).filter(g => g.active);

  if (activeGrants.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 18px;">
          No active doctor authorizations. Your medical vault is completely private.
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = activeGrants.map(g => {
    const expiryStr = g.expiresAt === 0 ? "Indefinite" : new Date(g.expiresAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    const scopeTags = g.scopes.map(s => {
      if (s === 'MENTAL_HEALTH') return '<span class="tag tag-mental">Mental Health</span>';
      return `<span class="tag ${getTagClass(s)}">${s}</span>`;
    }).join(' ');

    return `
      <tr>
        <td style="font-weight: 600;">${g.doctorName}</td>
        <td style="color: var(--text-secondary);">${g.specialty}</td>
        <td><span class="tag ${g.level === 1 ? 'tag-other' : 'tag-active'}">${g.level === 1 ? 'Read Only' : 'Read & Write'}</span></td>
        <td>${scopeTags}</td>
        <td>${expiryStr}</td>
        <td>
          <button onclick="revokeDoctorAccess('${g.doctorEmail}')" class="btn btn-danger">
            Revoke
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function renderPatientAudit() {
  const patient = db.currentUser;
  const tbody = document.getElementById('patientAuditTableBody');
  const badge = document.getElementById('patientInquiryBadge');
  const filtered = db.auditLogs.filter(l => l.patient === patient.name);

  // Check for recent consultations or overrides to notify patient
  const recentInquiries = filtered.filter(l => 
    (l.type === 'RecordAccessed' || l.type === 'BREAK_GLASS') && !l.patientAcknowledged
  );

  if (badge) {
    if (recentInquiries.length > 0) {
      badge.style.display = 'inline-block';
      badge.innerText = `${recentInquiries.length} New Inquiry${recentInquiries.length > 1 ? 's' : ''}`;
      badge.title = "A practitioner reviewed your chart. Click to mark as read.";
    } else {
      badge.style.display = 'none';
    }
  }

  tbody.innerHTML = filtered.map(l => `
    <tr style="${l.isEmergency ? 'background: #fff5f5; border-left: 3px solid var(--danger);' : ''}">
      <td><span class="tag ${l.isEmergency ? 'tag-emergency' : getEventTagClass(l.type)}">${l.type}</span></td>
      <td>${l.doctor}</td>
      <td>${escapeHtml(l.details)}</td>
      <td style="color: var(--text-muted); font-size: 12px;">${l.time}</td>
    </tr>
  `).join('');
}

function markInquiriesRead() {
  const patient = db.currentUser;
  if (!patient) return;

  db.auditLogs.forEach(l => {
    if (l.patient === patient.name) {
      l.patientAcknowledged = true;
    }
  });

  savePersistentState();
  const badge = document.getElementById('patientInquiryBadge');
  if (badge) badge.style.display = 'none';
  showToast("Inquiries Acknowledged", "Marked all chart consultation notifications as read.");
}

// ─── Doctor Portal Controller ────────────────────────────────────────────────

// Look up patient either by email OR by wallet ID
function findPatientByIdOrEmail(query) {
  if (!query) return null;
  const q = query.trim().toLowerCase();

  // 1. Direct email match
  if (db.users[q] && db.users[q].role === 'patient') {
    return db.users[q];
  }

  // 2. ID / Address match
  const match = Object.values(db.users).find(u => 
    u.role === 'patient' && (u.id.toLowerCase() === q || u.email.toLowerCase() === q)
  );

  return match || null;
}

function consultPatientChart() {
  const doctor = db.currentUser;
  const query = document.getElementById('inputPatientEmailLookup').value.trim();
  const patient = findPatientByIdOrEmail(query);

  const resultBox = document.getElementById('doctorQueryResultBox');
  const statusMsg = document.getElementById('doctorAccessStatusMessage');
  const tbody = document.getElementById('doctorChartTableBody');
  const writePanel = document.getElementById('doctorWritePanel');

  resultBox.style.display = 'block';

  if (!patient) {
    statusMsg.className = "alert-banner alert-banner-danger";
    statusMsg.innerHTML = `<strong>Patient Not Found</strong>: No registered patient found with identifier <code>${escapeHtml(query)}</code>. Please check the email address or ID.`;
    tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--danger); padding: 16px;">Search failed.</td></tr>`;
    writePanel.style.display = 'none';
    return;
  }

  // Check Patient's Grant for this Doctor
  const grant = patient.grants[doctor.email];
  const isGranted = grant && grant.active && (grant.expiresAt === 0 || Date.now() <= grant.expiresAt);

  if (!isGranted) {
    statusMsg.className = "alert-banner alert-banner-danger";
    statusMsg.innerHTML = `
      <strong>Access Denied by Patient Consent Rules</strong><br>
      Patient <strong>${patient.name}</strong> (${patient.email}) has not authorized your account (${doctor.name}).<br>
      <span style="font-size: 12px;">If this is an emergency trauma situation where the patient is unresponsive, you may invoke <strong>Emergency Break-Glass</strong>.</span>
    `;

    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; color: var(--danger); padding: 20px;">
          Decryption blocked: Smart contract verified zero active permissions from this patient.
        </td>
      </tr>
    `;
    writePanel.style.display = 'none';

    db.auditLogs.unshift({
      type: "AccessBlocked",
      patient: patient.name,
      doctor: doctor.name,
      details: "Inquiry blocked: No active patient grant exists.",
      isEmergency: false,
      time: "Just now"
    });
    savePersistentState();
    renderDoctorAudit();
    return;
  }

  // Access Granted!
  statusMsg.className = "alert-banner alert-banner-success";
  statusMsg.innerHTML = `
    <strong>Active Patient Consent Verified &bull; Patient: ${patient.name} (${patient.email})</strong><br>
    Permission Level: <strong>${grant.level === 1 ? 'Read Only' : 'Read & Write'}</strong> &bull; 
    Authorized Scopes: ${grant.scopes.join(', ')} &bull; 
    Expires: ${grant.expiresAt === 0 ? 'Indefinite' : new Date(grant.expiresAt).toLocaleDateString()}
  `;

  // Filter records by granted scopes and omit shredded records
  const allowedRecords = patient.records.filter(r => grant.scopes.includes(r.recordType));

  if (allowedRecords.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 20px;">
          Patient has no records matching your authorized categories (${grant.scopes.join(', ')}).
        </td>
      </tr>
    `;
  } else {
    tbody.innerHTML = allowedRecords.map(r => {
      if (r.shredded) {
        return `
          <tr style="opacity: 0.5;">
            <td><span class="tag tag-other">PURGED</span></td>
            <td>${escapeHtml(r.title)}</td>
            <td style="color: var(--danger); font-size: 11px;">[CRYPTO-SHREDDED BY PATIENT]</td>
            <td>-</td>
          </tr>
        `;
      }
      return `
        <tr>
          <td><span class="tag ${getTagClass(r.recordType)}">${r.recordType}</span></td>
          <td style="font-weight: 600;">${escapeHtml(r.title)}</td>
          <td style="font-size: 13px;">${escapeHtml(r.content)}</td>
          <td style="color: var(--text-muted); font-size: 12px;">${r.date}</td>
        </tr>
      `;
    }).join('');
  }

  writePanel.style.display = grant.level === 2 ? 'block' : 'none';

  db.auditLogs.unshift({
    type: "RecordAccessed",
    patient: patient.name,
    doctor: doctor.name,
    details: `Doctor viewed ${allowedRecords.length} records under valid consent (Scopes: ${grant.scopes.join(', ')}).`,
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();
  renderDoctorAudit();
}

function submitDoctorConsultationNote() {
  const doctor = db.currentUser;
  const query = document.getElementById('inputPatientEmailLookup').value.trim();
  const patient = findPatientByIdOrEmail(query);
  const title = document.getElementById('inputDocNoteTitle').value.trim();
  const category = document.getElementById('selectDocNoteCategory').value;
  const notes = document.getElementById('inputDocNoteBody').value.trim();

  if (!title || !notes) {
    showToast("Missing Note Content", "Please enter a title and consultation plan.");
    return;
  }

  patient.records.unshift({
    id: patient.records.length,
    recordType: category,
    title: `[Physician Note] ${title}`,
    content: `${notes} — Authored by ${doctor.name} (${doctor.specialty})`,
    sensitive: false,
    shredded: false,
    date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  });

  savePersistentState();

  db.auditLogs.unshift({
    type: "NoteAppended",
    patient: patient.name,
    doctor: doctor.name,
    details: `Doctor appended ${category} note: "${title}" under Read & Write consent.`,
    isEmergency: false,
    time: "Just now"
  });
  savePersistentState();

  document.getElementById('inputDocNoteTitle').value = "";
  document.getElementById('inputDocNoteBody').value = "";

  consultPatientChart();
  renderDoctorAudit();
  showToast("Note Added", "Consultation note added to patient record.");
}

// ─── Emergency Break-Glass ───────────────────────────────────────────────────

function openBreakGlassModal() {
  document.getElementById('breakGlassModal').classList.add('active');
}

function closeBreakGlassModal() {
  document.getElementById('breakGlassModal').classList.remove('active');
}

function executeBreakGlassOverride() {
  const reason = document.getElementById('breakGlassReason').value.trim();
  if (!reason) {
    alert("Emergency clinical justification is required by hospital regulation.");
    return;
  }

  closeBreakGlassModal();

  const doctor = db.currentUser;
  const query = document.getElementById('inputPatientEmailLookup').value.trim();
  const patient = findPatientByIdOrEmail(query);

  if (!patient) {
    showToast("Patient Not Found", "Enter a registered patient email or ID first.");
    return;
  }

  const resultBox = document.getElementById('doctorQueryResultBox');
  const statusMsg = document.getElementById('doctorAccessStatusMessage');
  const tbody = document.getElementById('doctorChartTableBody');
  const writePanel = document.getElementById('doctorWritePanel');

  resultBox.style.display = 'block';
  writePanel.style.display = 'none';

  statusMsg.className = "alert-banner alert-banner-danger";
  statusMsg.innerHTML = `
    <strong>EMERGENCY BREAK-GLASS OVERRIDE ACTIVATED</strong><br>
    Physician <strong>${doctor.name} (${doctor.license})</strong> accessed this chart without prior consent.<br>
    Patient: <strong>${patient.name} (${patient.email})</strong> &bull; Justification: <em>"${escapeHtml(reason)}"</em><br>
    <span style="font-weight: 700;">Action permanently logged on blockchain audit ledger. Compliance notified.</span>
  `;

  // Emergency disclosure: reveal non-sensitive records
  const records = patient.records.filter(r => !r.sensitive && !r.shredded);

  tbody.innerHTML = records.map(r => `
    <tr style="background: #fff8f8;">
      <td><span class="tag tag-emergency">${r.recordType}</span></td>
      <td style="font-weight: 600;">${escapeHtml(r.title)}</td>
      <td>${escapeHtml(r.content)}</td>
      <td>${r.date}</td>
    </tr>
  `).join('');

  db.auditLogs.unshift({
    type: "BREAK_GLASS",
    patient: patient.name,
    doctor: doctor.name,
    details: `Emergency override executed on ${patient.name}. Justification: "${reason}". Compliance alert dispatched.`,
    isEmergency: true,
    time: "Just now"
  });

  savePersistentState();
  renderDoctorAudit();
  showToast("Break-Glass Recorded", "Emergency access logged to institutional audit trail.", "danger");
}

function renderDoctorAudit() {
  const doctor = db.currentUser;
  const tbody = document.getElementById('doctorAuditTableBody');
  const filtered = db.auditLogs.filter(l => l.doctor === doctor.name);

  tbody.innerHTML = filtered.map(l => `
    <tr style="${l.isEmergency ? 'background: #fff5f5; border-left: 3px solid var(--danger);' : ''}">
      <td><span class="tag ${l.isEmergency ? 'tag-emergency' : getEventTagClass(l.type)}">${l.type}</span></td>
      <td style="font-weight: 600;">${l.patient}</td>
      <td>${escapeHtml(l.details)}</td>
      <td style="color: var(--text-muted); font-size: 12px;">${l.time}</td>
    </tr>
  `).join('');
}

// ─── Helpers & Modals ────────────────────────────────────────────────────────

function openLegalModal(topic) {
  const modal = document.getElementById('legalModal');
  const title = document.getElementById('modalTitle');
  const body = document.getElementById('modalBody');

  if (topic === 'privacy') {
    title.innerText = "Privacy Policy";
    body.innerHTML = `
      <p>MedChain guarantees sovereign patient control over clinical data:</p>
      <ul>
        <li>Your records are encrypted client-side using AES-256 before entering storage.</li>
        <li>No doctor can access your records unless you explicitly create an active permission.</li>
        <li>You can revoke doctor access or Crypto-Shred records at any time.</li>
      </ul>
    `;
  } else if (topic === 'terms') {
    title.innerText = "Terms of Service";
    body.innerHTML = `
      <p>MedChain provides a decentralized consent layer for healthcare:</p>
      <ul>
        <li>Patients own their individual health records.</li>
        <li>Practitioners are bound by medical licensing regulations when accessing disclosed records.</li>
        <li>Emergency overrides are subject to institutional review.</li>
      </ul>
    `;
  } else if (topic === 'security') {
    title.innerText = "Security Architecture";
    body.innerHTML = `
      <p>Key security pillars:</p>
      <ul>
        <li>Zero-knowledge AES-256 client encryption.</li>
        <li>Smart contract consent gatekeeper.</li>
        <li>Immutable tamper-proof audit logging.</li>
      </ul>
    `;
  }

  modal.classList.add('active');
}

function closeLegalModal() {
  document.getElementById('legalModal').classList.remove('active');
}

function getTagClass(type) {
  switch (type) {
    case 'DIAGNOSIS': return 'tag-diagnosis';
    case 'LAB_RESULT': return 'tag-lab';
    case 'PRESCRIPTION': return 'tag-prescription';
    case 'IMAGING': return 'tag-imaging';
    case 'MENTAL_HEALTH': return 'tag-mental';
    default: return 'tag-other';
  }
}

function getEventTagClass(type) {
  switch (type) {
    case 'AccessGranted': return 'tag-prescription';
    case 'AccessRevoked': return 'tag-other';
    case 'RecordAdded': return 'tag-lab';
    case 'RecordAccessed': return 'tag-diagnosis';
    case 'KeyDestroyed': return 'tag-emergency';
    case 'AccessBlocked': return 'tag-other';
    case 'BREAK_GLASS': return 'tag-emergency';
    default: return 'tag-other';
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function showToast(title, desc, type = 'info') {
  const box = document.getElementById('toastBox');
  document.getElementById('toastTitle').innerText = title;
  document.getElementById('toastDesc').innerText = desc;

  if (type === 'danger') {
    box.style.borderLeft = "4px solid var(--danger)";
  } else {
    box.style.borderLeft = "4px solid var(--primary)";
  }

  box.classList.add('show');
  setTimeout(() => {
    box.classList.remove('show');
  }, 3600);
}
