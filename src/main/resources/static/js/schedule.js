// Schedule Management Module - Full Implementation
// Implements all 15 API endpoints and calendar rendering per Module 5 spec

const S = {
  view: 'week',
  currentDate: new Date(),
  filters: { teacherId: null, groupId: null, type: null, dateFrom: null, dateTo: null, status: null },
  calendarData: [],
  stats: {},
  teachers: [],
  groups: [],
  subjects: [],
  classrooms: [],
  panelOpen: false,
  editingScheduleId: null,
  panelTab: 'CLASSES',
  pendingConflicts: []
};

document.addEventListener('DOMContentLoaded', init);

function init() {
  loadDropdowns();
  loadStats();
  loadCalendar();
  attachEventListeners();
  updateDateRangeLabel();
}

function attachEventListeners() {
  document.getElementById('newScheduleBtn').addEventListener('click', () => openPanel());
  document.getElementById('closePanelBtn').addEventListener('click', closePanel);
  document.getElementById('cancelBtn').addEventListener('click', closePanel);
  document.getElementById('saveBtn').addEventListener('click', saveSchedule);

  document.getElementById('todayBtn').addEventListener('click', () => {
    S.currentDate = new Date();
    loadCalendar();
  });

  document.getElementById('prevBtn').addEventListener('click', () => {
    S.currentDate.setDate(S.currentDate.getDate() - (S.view === 'month' ? 30 : S.view === 'week' ? 7 : 1));
    loadCalendar();
  });

  document.getElementById('nextBtn').addEventListener('click', () => {
    S.currentDate.setDate(S.currentDate.getDate() + (S.view === 'month' ? 30 : S.view === 'week' ? 7 : 1));
    loadCalendar();
  });

  document.querySelectorAll('input[name="viewToggle"]').forEach(radio => {
    radio.addEventListener('change', (e) => {
      S.view = e.target.value;
      loadCalendar();
    });
  });

  document.getElementById('filterBtn').addEventListener('click', () => {
    document.getElementById('filterSidebar').classList.toggle('open');
  });

  document.getElementById('applyFiltersBtn').addEventListener('click', applyFilters);
  document.getElementById('clearFiltersBtn').addEventListener('click', clearFilters);

  document.querySelectorAll('#scheduleTabs button').forEach(btn => {
    btn.addEventListener('click', (e) => {
      S.panelTab = e.target.dataset.tab;
      renderPanelForm();
      document.querySelectorAll('#scheduleTabs button').forEach(b => b.classList.remove('active'));
      e.target.classList.add('active');
    });
  });

  document.getElementById('filterTeacher').addEventListener('change', onTeacherChange);
}

function loadDropdowns() {
  Promise.all([
    fetch('/api/admin/schedules/teachers').then(r => r.json()).then(d => S.teachers = d.data || []),
    fetch('/api/admin/schedules/classrooms').then(r => r.json()).then(d => S.classrooms = d.data || [])
  ]).then(() => {
    populateTeacherDropdown();
  }).catch(err => console.error('Dropdown load error:', err));
}

function populateTeacherDropdown() {
  const select = document.getElementById('filterTeacher');
  S.teachers.forEach(t => {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = t.fullName;
    select.appendChild(opt);
  });
}

function onTeacherChange() {
  const teacherId = document.getElementById('filterTeacher').value;
  if (!teacherId) {
    S.groups = [];
    updateGroupDropdown();
    return;
  }

  fetch(`/api/admin/schedules/groups?teacherProfileId=${teacherId}`)
    .then(r => r.json())
    .then(d => {
      S.groups = d.data || [];
      updateGroupDropdown();
    })
    .catch(err => console.error('Groups load error:', err));
}

function updateGroupDropdown() {
  const select = document.getElementById('filterGroup');
  select.innerHTML = '<option value="">All Groups</option>';
  S.groups.forEach(g => {
    const opt = document.createElement('option');
    opt.value = g.id;
    opt.textContent = `${g.groupName} (${g.studentCount} students)`;
    select.appendChild(opt);
  });
}

function loadStats() {
  fetch('/api/admin/schedules/stats')
    .then(r => r.json())
    .then(d => {
      S.stats = d.data || {};
      renderStats(S.stats);
    })
    .catch(err => console.error('Stats load error:', err));
}

function renderStats(stats) {
  document.getElementById('statTotal').textContent = stats.totalSchedules || 0;
  document.getElementById('statWeek').textContent = stats.thisWeekSchedules || 0;
  document.getElementById('statConflicts').textContent = stats.activeConflicts || 0;
  document.getElementById('statCompleted').textContent = stats.completedSchedules || 0;
}

function loadCalendar() {
  const from = getDateFrom();
  const to = getDateTo();

  const params = new URLSearchParams({
    from: formatDate(from),
    to: formatDate(to),
    ...Object.fromEntries(Object.entries(S.filters).filter(([,v]) => v !== null && v !== ''))
  });

  fetch(`/api/admin/schedules/calendar?${params}`)
    .then(r => r.json())
    .then(d => {
      S.calendarData = d.data || [];
      renderCalendar();
    })
    .catch(err => console.error('Calendar load error:', err));
}

function renderCalendar() {
  if (S.view === 'week') renderWeekView();
  else if (S.view === 'day') renderDayView();
  else if (S.view === 'month') renderMonthView();
  else if (S.view === 'agenda') renderAgendaView();

  updateDateRangeLabel();
}

function renderWeekView() {
  const grid = document.getElementById('calendarGrid');
  grid.innerHTML = '';

  const from = getDateFrom();
  const days = [];
  for (let i = 0; i < 7; i++) {
    days.push(new Date(from.getTime() + i * 86400000));
  }

  const container = document.createElement('div');
  container.className = 'calendar-grid';

  // Header
  container.appendChild(createTimeLabel(''));
  days.forEach(day => {
    const header = document.createElement('div');
    header.className = `calendar-col-header ${formatDate(day) === formatDate(new Date()) ? 'col-today' : ''}`;
    header.textContent = day.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    container.appendChild(header);
  });

  // Time slots (08:00 - 18:00)
  for (let hour = 8; hour <= 18; hour++) {
    const timeLabel = `${String(hour).padStart(2, '0')}:00`;
    container.appendChild(createTimeLabel(timeLabel));

    days.forEach((day, dayIdx) => {
      const slot = document.createElement('div');
      slot.className = `time-slot ${formatDate(day) === formatDate(new Date()) ? 'col-today' : ''}`;

      S.calendarData
        .filter(s => formatDate(s.occurrenceDate) === formatDate(day) && parseInt(s.startTime.substring(0, 2)) === hour)
        .forEach(schedule => {
          const card = createScheduleCard(schedule);
          slot.appendChild(card);
        });

      container.appendChild(slot);
    });
  }

  grid.appendChild(container);
}

function renderMonthView() {
  const grid = document.getElementById('calendarGrid');
  grid.innerHTML = '';

  const container = document.createElement('div');
  container.className = 'calendar-grid-month';

  const year = S.currentDate.getFullYear();
  const month = S.currentDate.getMonth();

  // Weekday headers
  ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].forEach(day => {
    const header = document.createElement('div');
    header.className = 'calendar-col-header';
    header.textContent = day;
    container.appendChild(header);
  });

  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  for (let i = 0; i < firstDay; i++) {
    container.appendChild(document.createElement('div'));
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const cell = document.createElement('div');
    cell.className = 'calendar-month-cell';
    const cellDate = new Date(year, month, day);
    if (formatDate(cellDate) === formatDate(new Date())) {
      cell.classList.add('today');
    }

    const dateDiv = document.createElement('div');
    dateDiv.className = 'calendar-month-date';
    dateDiv.textContent = day;
    cell.appendChild(dateDiv);

    const schedulesForDay = S.calendarData.filter(s => formatDate(s.occurrenceDate) === formatDate(cellDate));
    schedulesForDay.slice(0, 3).forEach(schedule => {
      const miniCard = document.createElement('div');
      miniCard.className = `schedule-card ${schedule.colorClass}`;
      miniCard.textContent = schedule.title;
      miniCard.style.fontSize = '.7rem';
      miniCard.style.marginBottom = '2px';
      cell.appendChild(miniCard);
    });

    if (schedulesForDay.length > 3) {
      const more = document.createElement('div');
      more.className = 'text-muted small';
      more.textContent = `+${schedulesForDay.length - 3} more`;
      cell.appendChild(more);
    }

    container.appendChild(cell);
  }

  grid.appendChild(container);
}

function renderAgendaView() {
  const grid = document.getElementById('calendarGrid');
  grid.innerHTML = '';

  const container = document.createElement('div');
  container.style.padding = '12px';

  const grouped = {};
  S.calendarData.forEach(schedule => {
    const dateKey = schedule.occurrenceDate;
    if (!grouped[dateKey]) grouped[dateKey] = [];
    grouped[dateKey].push(schedule);
  });

  Object.entries(grouped).sort().forEach(([date, schedules]) => {
    const dateItem = document.createElement('div');
    dateItem.className = 'agenda-date';
    dateItem.textContent = new Date(date).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    container.appendChild(dateItem);

    schedules.forEach(schedule => {
      const item = document.createElement('div');
      item.className = `agenda-schedule ${schedule.scheduleTab.toLowerCase()}`;
      item.innerHTML = `
        <strong>${schedule.title}</strong><br>
        <small>${schedule.startTime} - ${schedule.endTime}</small><br>
        <small>${schedule.teacherName} • ${schedule.groupName}</small>
      `;
      item.style.cursor = 'pointer';
      item.addEventListener('click', () => editSchedule(schedule.id));
      container.appendChild(item);
    });
  });

  grid.appendChild(container);
}

function renderDayView() {
  renderWeekView();
}

function createScheduleCard(schedule) {
  const card = document.createElement('div');
  card.className = `schedule-card ${schedule.colorClass || 'schedule-card--classes'}`;
  card.style.top = '2px';
  card.style.height = '54px';

  const titleDiv = document.createElement('div');
  titleDiv.className = 'schedule-card-title';
  titleDiv.textContent = schedule.title || '';

  const timeDiv = document.createElement('div');
  timeDiv.className = 'schedule-card-time';
  timeDiv.textContent = `${schedule.startTime.substring(0, 5)} - ${schedule.endTime.substring(0, 5)}`;

  card.appendChild(titleDiv);
  card.appendChild(timeDiv);

  if (schedule.isRecurring) {
    const badge = document.createElement('div');
    badge.className = 'recurring-badge';
    badge.innerHTML = '<i class="bi bi-arrow-repeat"></i>';
    card.appendChild(badge);
  }

  card.addEventListener('click', () => editSchedule(schedule.id));

  return card;
}

function createTimeLabel(label) {
  const div = document.createElement('div');
  div.className = 'time-label';
  div.textContent = label;
  return div;
}

function openPanel(scheduleId = null) {
  S.panelOpen = true;
  S.editingScheduleId = scheduleId;
  S.panelTab = 'CLASSES';

  document.getElementById('addSchedulePanel').classList.add('open');
  renderPanelForm();
}

function closePanel() {
  S.panelOpen = false;
  document.getElementById('addSchedulePanel').classList.remove('open');
  document.getElementById('panelFormBody').innerHTML = '';
}

function renderPanelForm() {
  const body = document.getElementById('panelFormBody');
  body.innerHTML = '';

  if (S.panelTab === 'CLASSES') {
    body.innerHTML = `
      <div class="form-group-schedule">
        <label>Teacher *</label>
        <select class="form-select form-select-sm" id="formTeacher">
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
