// Module 5: Curriculum Scheduler

var scheduleState = {
  schedules: [],
  selectedSchedule: null,
  currentSession: null
};

function openScheduleModal() {
  var modal = new bootstrap.Modal(document.getElementById('scheduleModal'));
  renderScheduleForm();
  modal.show();
}

function renderScheduleForm() {
  var form = document.getElementById('scheduleForm');
  form.innerHTML = `
    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label fw-600">Teacher *</label>
        <select class="form-select" id="teacherSelect" required>
          <option value="">Select teacher</option>
        </select>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Group *</label>
        <select class="form-select" id="groupSelect" required>
          <option value="">Select group</option>
        </select>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Subject *</label>
        <select class="form-select" id="subjectSelect" required>
          <option value="">Select subject</option>
        </select>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Schedule Type *</label>
        <select class="form-select" id="scheduleTypeSelect" required>
          <option value="">Select type</option>
          <option value="REGULAR_CLASS">Regular Class</option>
          <option value="REVISION_SESSION">Revision Session</option>
          <option value="EXTRA_CLASS">Extra Class</option>
          <option value="PRACTICAL_SESSION">Practical Session</option>
          <option value="EXAM_PREPARATION">Exam Preparation</option>
          <option value="PARENT_SESSION">Parent Session</option>
          <option value="WORKSHOP">Workshop</option>
        </select>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Date Mode *</label>
        <div class="btn-group w-100" role="group">
          <input type="radio" class="btn-check" name="dateMode" id="singleDay" value="SINGLE_DAY" checked>
          <label class="btn btn-outline-primary" for="singleDay">Single Day</label>
          <input type="radio" class="btn-check" name="dateMode" id="multiDay" value="MULTIPLE_DAYS">
          <label class="btn btn-outline-primary" for="multiDay">Multiple Days</label>
          <input type="radio" class="btn-check" name="dateMode" id="recurring" value="RECURRING">
          <label class="btn btn-outline-primary" for="recurring">Recurring</label>
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Schedule Date *</label>
        <input type="date" class="form-control" id="scheduleDate" required>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">Start Time *</label>
        <input type="time" class="form-control" id="startTime" required>
      </div>
      <div class="col-md-6">
        <label class="form-label fw-600">End Time *</label>
        <input type="time" class="form-control" id="endTime" required>
      </div>
      <div class="col-12">
        <label class="form-label fw-600">Topic *</label>
        <input type="text" class="form-control" id="topic" maxlength="255" required placeholder="e.g. Algebra Basics">
      </div>
      <div class="col-12">
        <label class="form-label fw-600">Description</label>
        <textarea class="form-control" id="description" rows="4" placeholder="Lesson details..."></textarea>
      </div>
      <div class="col-12">
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="enableAttendance">
          <label class="form-check-label" for="enableAttendance">Enable Attendance</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="notifyStudents">
          <label class="form-check-label" for="notifyStudents">Notify Students</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="checkbox" id="notifyParents">
          <label class="form-check-label" for="notifyParents">Notify Parents</label>
        </div>
      </div>
    </div>
  `;
}

function saveSchedule() {
  var teacherId = document.getElementById('teacherSelect').value;
  var groupId = document.getElementById('groupSelect').value;
  var scheduleDate = document.getElementById('scheduleDate').value;
  var startTime = document.getElementById('startTime').value;
  var endTime = document.getElementById('endTime').value;
  var topic = document.getElementById('topic').value;

  if (!teacherId || !groupId || !scheduleDate || !startTime || !endTime || !topic) {
    showToast('Please fill all required fields', 'warning');
    return;
  }

  var payload = {
    sessionId: scheduleState.currentSession,
    teacherId: parseInt(teacherId),
    groupId: parseInt(groupId),
    scheduleType: document.getElementById('scheduleTypeSelect').value,
    dateMode: document.querySelector('input[name="dateMode"]:checked').value,
    scheduleDate: scheduleDate,
    startTime: startTime,
    endTime: endTime,
    topic: topic,
    description: document.getElementById('description').value,
    enableAttendance: document.getElementById('enableAttendance').checked,
    notifyStudents: document.getElementById('notifyStudents').checked,
    notifyParents: document.getElementById('notifyParents').checked
  };

  fetch('/api/admin/schedules', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('ep_token')
    },
    body: JSON.stringify(payload)
  })
  .then(function (r) {
    if (!r.ok) throw new Error('HTTP ' + r.status);
    return r.json();
  })
  .then(function (data) {
    showToast('Schedule created successfully', 'success');
    bootstrap.Modal.getInstance(document.getElementById('scheduleModal')).hide();
    loadSchedules();
  })
  .catch(function (e) {
    console.error('Error:', e);
    showToast('Failed to create schedule', 'danger');
  });
}

function loadSchedules() {
  if (!scheduleState.currentSession) {
    document.getElementById('scheduleCalendar').innerHTML = '<div class="text-muted">Select a session first</div>';
    return;
  }

  fetch('/api/admin/schedules/session/' + scheduleState.currentSession, {
    headers: {
      'Authorization': 'Bearer ' + localStorage.getItem('ep_token')
    }
  })
  .then(function (r) { return r.json(); })
  .then(function (schedules) {
    scheduleState.schedules = schedules;
    renderCalendar();
  })
  .catch(function (e) {
    console.error('Error loading schedules:', e);
    showToast('Failed to load schedules', 'danger');
  });
}

function renderCalendar() {
  var calendar = document.getElementById('scheduleCalendar');
  if (!scheduleState.schedules.length) {
    calendar.innerHTML = '<div class="text-muted">No schedules created yet</div>';
    return;
  }

  var html = '<div class="row g-3">';
  scheduleState.schedules.forEach(function (schedule) {
    html += `
      <div class="col-md-6">
        <div class="card" style="border-left:4px solid ${getColorForType(schedule.scheduleType)}">
          <div class="card-body">
            <h6 class="card-title">${escHtml(schedule.topic)}</h6>
            <p class="card-text small mb-2">
              <strong>${escHtml(schedule.teacherName)}</strong><br>
              ${escHtml(schedule.groupName)}<br>
              ${schedule.scheduleDate} ${schedule.startTime} - ${schedule.endTime}
            </p>
          </div>
        </div>
      </div>
    `;
  });
  html += '</div>';
  calendar.innerHTML = html;
}

function getColorForType(type) {
  var colors = {
    'REGULAR_CLASS': '#0d6efd',
    'REVISION_SESSION': '#198754',
    'EXTRA_CLASS': '#6f42c1',
    'PRACTICAL_SESSION': '#fd7e14',
    'EXAM_PREPARATION': '#dc3545'
  };
  return colors[type] || '#6c757d';
}

function showToast(message, type) {
  var container = document.getElementById('toastContainer');
  var toast = document.createElement('div');
  toast.className = 'toast align-items-center text-white bg-' + type + ' border-0 show';
  toast.innerHTML = '<div class="d-flex"><div class="toast-body">' + escHtml(message) + '</div></div>';
  container.appendChild(toast);
  setTimeout(function () { toast.remove(); }, 3000);
}

function escHtml(text) {
  var div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Initialize
document.addEventListener('DOMContentLoaded', function () {
  console.log('Schedule module loaded');
});
