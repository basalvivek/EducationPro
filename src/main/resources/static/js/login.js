/* ── Role configuration ── */
const ROLES = {
  admin: {
    label:      'Admin Portal',
    badgeClass: 'bg-primary',
    btnClass:   'btn-primary',
    borderVar:  '#0d6efd',
    bgVar:      'var(--bg-admin)',
    subtitle:   'Platform management & oversight',
    features: [
      { icon: 'bi-people-fill',    text: 'Manage all users & roles' },
      { icon: 'bi-diagram-3-fill', text: 'Design & publish course trees' },
      { icon: 'bi-check2-circle',  text: 'Approve teacher submissions' },
      { icon: 'bi-bar-chart-fill', text: 'Platform-wide analytics' },
    ]
  },
  teacher: {
    label:      'Teacher Portal',
    badgeClass: 'bg-success',
    btnClass:   'btn-success',
    borderVar:  '#198754',
    bgVar:      'var(--bg-teacher)',
    subtitle:   'Create, submit & manage your courses',
    features: [
      { icon: 'bi-journal-plus',   text: 'Build & submit courses for approval' },
      { icon: 'bi-people',         text: 'View enrolled students' },
      { icon: 'bi-chat-dots-fill', text: 'Grade & give feedback' },
      { icon: 'bi-calendar3',      text: 'Schedule classes & events' },
    ]
  },
  student: {
    label:      'Student Portal',
    badgeClass: 'bg-purple',
    btnClass:   'btn-purple',
    borderVar:  '#6f42c1',
    bgVar:      'var(--bg-student)',
    subtitle:   'Learn, practise & grow',
    features: [
      { icon: 'bi-book-fill',           text: 'Access enrolled courses' },
      { icon: 'bi-patch-question-fill', text: 'Take quizzes & assignments' },
      { icon: 'bi-graph-up-arrow',      text: 'Track your progress' },
      { icon: 'bi-award-fill',          text: 'Earn certificates' },
    ]
  },
  parent: {
    label:      'Parent Portal',
    badgeClass: 'bg-orange',
    btnClass:   'btn-orange',
    borderVar:  '#fd7e14',
    bgVar:      'var(--bg-parent)',
    subtitle:   "Monitor your child's journey",
    features: [
      { icon: 'bi-person-check-fill', text: "View child's progress & grades" },
      { icon: 'bi-bell-fill',         text: 'Receive activity notifications' },
      { icon: 'bi-calendar-event',    text: 'See upcoming assessments' },
      { icon: 'bi-chat-left-text',    text: 'Message teachers directly' },
    ]
  }
};

let activeRole = 'admin';

function applyRole(role) {
  const cfg = ROLES[role];
  activeRole = role;

  const badge = document.getElementById('roleBadge');
  badge.className = `badge ${cfg.badgeClass} fs-6 mb-3`;
  badge.textContent = cfg.label;

  document.getElementById('leftPanel').style.background = cfg.bgVar;
  document.getElementById('panelTitle').textContent = cfg.label;
  document.getElementById('panelSubtitle').textContent = cfg.subtitle;

  const list = document.getElementById('featureList');
  list.innerHTML = cfg.features.map(f => `
    <li class="d-flex align-items-start mb-3">
      <i class="bi ${f.icon} fs-4 me-3 mt-1" style="color:${cfg.borderVar}"></i>
      <span class="fs-6">${f.text}</span>
    </li>`).join('');

  const btn = document.getElementById('loginBtn');
  btn.className = `btn ${cfg.btnClass} btn-lg w-100 fw-semibold`;
  btn.textContent = `Sign in as ${role.charAt(0).toUpperCase() + role.slice(1)}`;

  document.getElementById('forgotLink').style.color = cfg.borderVar;
  document.getElementById('roleInput').value = role;

  document.querySelectorAll('.role-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.role === role);
  });
}

/* ── Toggle password visibility ── */
document.getElementById('togglePass').addEventListener('click', () => {
  const pwd = document.getElementById('password');
  const icon = document.querySelector('#togglePass i');
  if (pwd.type === 'password') {
    pwd.type = 'text';
    icon.className = 'bi bi-eye-slash';
  } else {
    pwd.type = 'password';
    icon.className = 'bi bi-eye';
  }
});

/* ── Pill click handlers ── */
document.querySelectorAll('.role-btn').forEach(btn => {
  btn.addEventListener('click', () => applyRole(btn.dataset.role));
});

/* ── Login form — AJAX submit, store JWT, redirect ── */
document.getElementById('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  if (!form.checkValidity()) {
    form.classList.add('was-validated');
    return;
  }

  const alertEl = document.getElementById('loginAlert');
  alertEl.classList.add('d-none');

  const payload = {
    email:    document.getElementById('email').value.trim(),
    password: document.getElementById('password').value,
    role:     activeRole.toUpperCase()
  };

  try {
    const res  = await fetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (res.ok) {
      const payload = data.data;
      localStorage.setItem('ep_token', payload.token);
      sessionStorage.setItem('edu_name', payload.name || '');
      window.location.href = payload.redirectTo;
    } else {
      alertEl.textContent = data.message || 'Login failed.';
      alertEl.classList.remove('d-none');
    }
  } catch {
    alertEl.textContent = 'Network error. Please try again.';
    alertEl.classList.remove('d-none');
  }
});

/* ── Forgot password modal ── */
document.getElementById('forgotLink').addEventListener('click', e => {
  e.preventDefault();
  new bootstrap.Modal(document.getElementById('forgotModal')).show();
});

document.getElementById('sendResetBtn').addEventListener('click', async () => {
  const email   = document.getElementById('resetEmail').value.trim();
  const alertEl = document.getElementById('resetAlert');
  if (!email) { showResetAlert('Please enter your email.', 'danger'); return; }

  const res  = await fetch('/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, role: activeRole.toUpperCase() })
  });
  const data = await res.json();

  if (res.ok) {
    showResetAlert('Check your inbox — link sent!', 'success');
  } else {
    showResetAlert(data.message || 'Something went wrong.', 'danger');
  }
});

function showResetAlert(msg, type) {
  const el = document.getElementById('resetAlert');
  el.className = `alert alert-${type}`;
  el.textContent = msg;
}

/* ── Init ── */
applyRole('admin');
