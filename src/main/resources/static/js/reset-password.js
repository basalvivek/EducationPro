document.getElementById('resetForm').addEventListener('submit', async e => {
  e.preventDefault();
  const alertEl = document.getElementById('resetAlert');
  alertEl.classList.add('d-none');

  const token   = document.getElementById('tokenInput').value;
  const newPwd  = document.getElementById('newPassword').value;
  const confirm = document.getElementById('confirmPassword').value;

  if (newPwd !== confirm) {
    alertEl.textContent = 'Passwords do not match.';
    alertEl.classList.remove('d-none');
    return;
  }

  try {
    const res  = await fetch('/auth/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, newPassword: newPwd, confirmPassword: confirm })
    });
    const data = await res.json();

    if (res.ok) {
      alertEl.className = 'alert alert-success';
      alertEl.textContent = data.message;
      alertEl.classList.remove('d-none');
      setTimeout(() => window.location.href = '/auth/login', 2000);
    } else {
      alertEl.className = 'alert alert-danger';
      alertEl.textContent = data.message || 'Reset failed.';
      alertEl.classList.remove('d-none');
    }
  } catch {
    alertEl.className = 'alert alert-danger';
    alertEl.textContent = 'Network error. Please try again.';
    alertEl.classList.remove('d-none');
  }
});
