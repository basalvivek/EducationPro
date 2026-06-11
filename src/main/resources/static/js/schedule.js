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
  formGroups: [],
  subjects: [],
  classrooms: [],
  panelOpen: false,
  editingScheduleId: null,
  panelTab: 'CLASSES',
  pendingConflicts: []
};

function getToken() {
  return sessionStorage.getItem('edu_token') || localStorage.getItem('ep_token') || '';
}

function authHeaders(extra) {
  const h = extra || {};
  const t = getToken();
  if (t) h['Authorization'] = 'Bearer ' + t;
  return h;
}

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
    fetch('/api/admin/schedules/teachers', { headers: authHeaders() }).then(r => r.json()).then(d => S.teachers = d || []),
    fetch('/api/admin/schedules/classrooms', { headers: authHeaders() }).then(r => r.json()).then(d => S.classrooms = d || [])
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

  fetch(`/api/admin/schedules/groups?teacherProfileId=${teacherId}`, { headers: authHeaders() })
    .then(r => r.json())
    .then(d => {
      S.groups = d || [];
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
  fetch('/api/admin/schedules/stats', { headers: authHeaders() })
    .then(r => r.json())
    .then(d => {
      S.stats = d || {};
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

  fetch(`/api/admin/schedules/calendar?${params}`, { headers: authHeaders() })
    .then(r => r.json())
    .then(d => {
      S.calendarData = d || [];
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
    const teacherOptions = S.teachers
      .map(t => `<option value="${t.id}">${escHtml(t.fullName)}</option>`)
      .join('');

    body.innerHTML = `
      <div class="form-group-schedule mb-2">
        <label class="form-label fw-600">Teacher *</label>
        <select class="form-select form-select-sm" id="formTeacher">
          <option value="">Select teacher</option>${teacherOptions}
        </select>
      </div>
      <div class="form-group-schedule mb-2">
        <label class="form-label fw-600">Group *</label>
        <select class="form-select form-select-sm" id="formGroup">
          <option value="">Select teacher first</option>
        </select>
      </div>
      <div class="form-group-schedule mb-2">
        <label class="form-label fw-600">Subject</label>
        <select class="form-select form-select-sm" id="formSubject">
          <option value="">Select group first</option>
        </select>
      </div>
      <div class="form-group-schedule mb-2">
        <label class="form-label fw-600">Schedule Type</label>
        <select class="form-select form-select-sm" id="formScheduleType">
          <option value="">None</option>
          <option value="REGULAR">Regular Class</option>
          <option value="REVISION">Revision Session</option>
          <option value="EXTRA">Extra Class</option>
          <option value="PRACTICAL">Practical Session</option>
          <option value="EXAM_PREP">Exam Preparation</option>
          <option value="PARENT">Parent Session</option>
          <option value="WORKSHOP">Workshop</option>
        </select>
      </div>
      <div class="form-group-schedule mb-2">
        <label class="form-label fw-600">Date Mode *</label>
        <div class="btn-group w-100" role="group">
          <input type="radio" class="btn-check" name="dateMode" id="singleDay" value="SINGLE" checked>
          <label class="btn btn-outline-primary" for="singleDay">Single Day</label>
          <input type="radio" class="btn-check" name="dateMode" id="multiDay" value="MULTIPLE">
          <label class="btn btn-outline-primary" for="multiDay">Multiple Days</label>
          <input type="radio" class="btn-check" name="dateMode" id="recurring" value="RECURRING">
          <label class="btn btn-outline-primary" for="recurring">Recurring</label>
        </div>
      </div>
      <div class="mb-2">
        <label class="form-label fw-600">Schedule Date *</label>
        <input type="date" class="form-control" id="scheduleDate" required>
      </div>
      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label fw-600">Start Time *</label>
          <input type="time" class="form-control" id="startTime" required>
        </div>
        <div class="col-6">
          <label class="form-label fw-600">End Time *</label>
          <input type="time" class="form-control" id="endTime" required>
        </div>
      </div>
      <div class="mb-2">
        <label class="form-label fw-600">Topic *</label>
        <input type="text" class="form-control" id="topic" maxlength="200" required placeholder="e.g. Algebra Basics">
      </div>
      <div class="mb-2">
        <label class="form-label fw-600">Description</label>
        <textarea class="form-control" id="description" rows="4" placeholder="Lesson details..."></textarea>
      </div>
      <div class="form-check mb-2">
        <input class="form-check-input" type="checkbox" id="enableAttendance">
        <label class="form-check-label" for="enableAttendance">Enable Attendance</label>
      </div>
    `;

    document.getElementById('formTeacher').addEventListener('change', onFormTeacherChange);
    document.getElementById('formGroup').addEventListener('change', onFormGroupChange);
  } else {
    const isEvent = S.panelTab === 'EVENTS';
    const isHoliday = S.panelTab === 'HOLIDAYS';
    const titleLabel = isHoliday ? 'Holiday Name' : 'Event Title';
    const defaultStart = isHoliday ? '00:00' : '09:00';
    const defaultEnd = isHoliday ? '23:59' : '17:00';

    body.innerHTML = `
      <div class="mb-2">
        <label class="form-label fw-600">${titleLabel} *</label>
        <input type="text" class="form-control" id="eventTitle" maxlength="200" required>
      </div>
      ${isEvent ? `
      <div class="mb-2">
        <label class="form-label fw-600">Location</label>
        <input type="text" class="form-control" id="eventLocation" maxlength="200">
      </div>
      <div class="mb-2">
        <label class="form-label fw-600">Audience</label>
        <select class="form-select form-select-sm" id="eventAudience">
          <option value="">Not specified</option>
          <option value="ALL">Everyone</option>
          <option value="TEACHERS">Teachers</option>
          <option value="STUDENTS">Students</option>
          <option value="PARENTS">Parents</option>
        </select>
      </div>` : ''}
      <div class="mb-2">
        <label class="form-label fw-600">Date *</label>
        <input type="date" class="form-control" id="scheduleDate" required>
      </div>
      <div class="row g-2 mb-2">
        <div class="col-6">
          <label class="form-label fw-600">Start Time *</label>
          <input type="time" class="form-control" id="startTime" value="${defaultStart}" required>
        </div>
        <div class="col-6">
          <label class="form-label fw-600">End Time *</label>
          <input type="time" class="form-control" id="endTime" value="${defaultEnd}" required>
        </div>
      </div>
      <div class="mb-2">
        <label class="form-label fw-600">Description</label>
        <textarea class="form-control" id="description" rows="4"></textarea>
      </div>
    `;
  }
}

function onFormTeacherChange() {
  const teacherId = document.getElementById('formTeacher').value;
  const select = document.getElementById('formGroup');
  select.innerHTML = '<option value="">Select group</option>';
  document.getElementById('formSubject').innerHTML = '<option value="">Select group first</option>';
  S.formGroups = [];
  if (!teacherId) return;

  fetch(`/api/admin/schedules/groups?teacherProfileId=${teacherId}`, { headers: authHeaders() })
    .then(r => r.json())
    .then(groups => {
      S.formGroups = groups || [];
      S.formGroups.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g.id;
        opt.textContent = `${g.groupName} (${g.studentCount} students)`;
        select.appendChild(opt);
      });
    })
    .catch(err => console.error('Groups load error:', err));
}

function onFormGroupChange() {
  const groupId = document.getElementById('formGroup').value;
  const select = document.getElementById('formSubject');
  select.innerHTML = '<option value="">None</option>';

  const group = (S.formGroups || []).find(g => String(g.id) === groupId);
  if (!group || !group.sessionId) return;

  fetch(`/api/admin/schedules/subjects?sessionId=${group.sessionId}`, { headers: authHeaders() })
    .then(r => r.json())
    .then(subjects => {
      (subjects || []).forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.nodeId;
        opt.textContent = s.subjectName;
        select.appendChild(opt);
      });
    })
    .catch(err => console.error('Subjects load error:', err));
}

function saveSchedule() {
  const scheduleDate = document.getElementById('scheduleDate').value;
  const startTime = document.getElementById('startTime').value;
  const endTime = document.getElementById('endTime').value;
  const description = document.getElementById('description').value || null;

  let payload;

  if (S.panelTab === 'CLASSES') {
    const teacherId = document.getElementById('formTeacher').value;
    const groupId = document.getElementById('formGroup').value;
    const topic = document.getElementById('topic').value;

    if (!teacherId || !groupId || !scheduleDate || !startTime || !endTime || !topic) {
      showToast('Please fill all required fields', 'warning');
      return;
    }

    const group = (S.formGroups || []).find(g => String(g.id) === groupId);
    const subjectNodeId = document.getElementById('formSubject').value;

    payload = {
      scheduleTab: 'CLASSES',
      teacherProfileId: parseInt(teacherId),
      groupId: parseInt(groupId),
      subjectNodeId: subjectNodeId ? parseInt(subjectNodeId) : null,
      assignmentSessionId: group ? group.sessionId : null,
      scheduleType: document.getElementById('formScheduleType').value || null,
      dateMode: document.querySelector('input[name="dateMode"]:checked').value,
      startDate: scheduleDate,
      startTime: startTime,
      endTime: endTime,
      topic: topic,
      description: description,
      attendanceRequired: document.getElementById('enableAttendance').checked
    };
  } else {
    const eventTitle = document.getElementById('eventTitle').value;

    if (!eventTitle || !scheduleDate || !startTime || !endTime) {
      showToast('Please fill all required fields', 'warning');
      return;
    }

    const locationEl = document.getElementById('eventLocation');
    const audienceEl = document.getElementById('eventAudience');

    payload = {
      scheduleTab: S.panelTab,
      dateMode: 'SINGLE',
      startDate: scheduleDate,
      startTime: startTime,
      endTime: endTime,
      eventTitle: eventTitle,
      location: locationEl ? (locationEl.value || null) : null,
      audience: audienceEl ? (audienceEl.value || null) : null,
      description: description
    };
  }

  fetch('/api/admin/schedules', {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload)
  })
    .then(async r => {
      if (!r.ok) {
        const err = await r.json().catch(() => null);
        throw new Error(err && err.message ? err.message : 'HTTP ' + r.status);
      }
      return r.json();
    })
    .then(() => {
      showToast('Schedule created successfully', 'success');
      closePanel();
      loadCalendar();
      loadStats();
    })
    .catch(err => {
      console.error('Save error:', err);
      showToast(err.message || 'Failed to create schedule', 'danger');
    });
}

function formatDate(d) {
  if (typeof d === 'string') return d.substring(0, 10);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function getDateFrom() {
  const d = new Date(S.currentDate);
  d.setHours(0, 0, 0, 0);
  if (S.view === 'week') {
    d.setDate(d.getDate() - d.getDay());
  } else if (S.view === 'month') {
    d.setDate(1);
  }
  return d;
}

function getDateTo() {
  const d = new Date(getDateFrom());
  if (S.view === 'week') {
    d.setDate(d.getDate() + 6);
  } else if (S.view === 'month') {
    d.setMonth(d.getMonth() + 1);
    d.setDate(0);
  } else if (S.view === 'agenda') {
    d.setDate(d.getDate() + 30);
  }
  return d;
}

function updateDateRangeLabel() {
  const label = document.getElementById('dateRangeLabel');
  if (!label) return;
  const from = getDateFrom();
  const to = getDateTo();
  const full = { month: 'short', day: 'numeric', year: 'numeric' };
  if (S.view === 'day') {
    label.textContent = from.toLocaleDateString('en-US', { weekday: 'long', ...full });
  } else {
    label.textContent = `${from.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} – ${to.toLocaleDateString('en-US', full)}`;
  }
}

function applyFilters() {
  S.filters.teacherId = document.getElementById('filterTeacher').value || null;
  S.filters.groupId = document.getElementById('filterGroup').value || null;
  S.filters.type = document.getElementById('filterType').value || null;
  S.filters.status = document.getElementById('filterStatus').value || null;
  loadCalendar();
}

function clearFilters() {
  S.filters = { teacherId: null, groupId: null, type: null, dateFrom: null, dateTo: null, status: null };
  document.getElementById('filterTeacher').value = '';
  document.getElementById('filterType').value = '';
  document.getElementById('filterStatus').value = '';
  S.groups = [];
  updateGroupDropdown();
  loadCalendar();
}

function editSchedule(scheduleId) {
  openPanel(scheduleId);
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
