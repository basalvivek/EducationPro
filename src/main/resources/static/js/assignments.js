'use strict';

// ── Auth helpers ───────────────────────────────────────────────────────────────
function getToken() {
  return sessionStorage.getItem('edu_token') || localStorage.getItem('ep_token') || '';
}
function authHeaders(extra) {
  const h = { ...extra };
  const t = getToken();
  if (t) h['Authorization'] = 'Bearer ' + t;
  return h;
}

// ── Toast ──────────────────────────────────────────────────────────────────────
function showToast(msg, type) {
  type = type || 'success';
  const el = document.createElement('div');
  el.className = 'toast align-items-center text-white bg-' + type + ' border-0 show';
  el.setAttribute('role', 'alert');
  el.innerHTML = '<div class="d-flex"><div class="toast-body fw-semibold">' + escHtml(msg) +
    '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
  document.getElementById('toastContainer').appendChild(el);
  setTimeout(function () { el.remove(); }, 4000);
}
function escHtml(s) {
  return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── State ──────────────────────────────────────────────────────────────────────
var S = {
  nodes:            [],
  students:         [],
  teachers:         [],
  selectedCourseId: null,
  maxPerGroup:      30,
  groups:           [],   // {id, name, desc, period, studentIds:[]}
  assignments:      {},   // teacherId -> groupId
  nextGroupId:          1,
  activatedTeacherIds:  [],  // teachers added via Assign Teachers modal
  // Scope selection (radio in info panel)
  scopeNodes:           { course: null, subject: null, board: null },
  selectedScopeKey:     null   // 'course' | 'subject' | 'board'
};

var GROUP_COLORS = ['success', 'primary', 'purple'];

function groupBadgeClass(color) {
  if (color === 'purple') return '';
  return 'bg-' + color + '-subtle text-' + color + '-emphasis border border-' + color + '-subtle';
}
function groupBadgeStyle(color, over) {
  var base = 'font-size:.7rem;';
  if (over) return base;
  if (color === 'purple') return base + 'background:#ede9fe;color:#5b21b6;border:1px solid #c4b5fd;';
  return base;
}

// ── Boot ───────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
  var name   = sessionStorage.getItem('edu_name') || '';
  var nameEl = document.getElementById('topbarName');
  var avEl   = document.getElementById('topbarAvatar');
  if (name) { if (nameEl) nameEl.textContent = name; if (avEl) avEl.textContent = name.charAt(0).toUpperCase(); }

  Promise.all([
    fetch('/api/admin/course-nodes', { headers: authHeaders() }).then(function (r) { return r.json(); }),
    fetch('/api/admin/students',     { headers: authHeaders() }).then(function (r) { return r.json(); }),
    fetch('/api/admin/teachers',     { headers: authHeaders() }).then(function (r) { return r.json(); }),
    fetch('/api/admin/assignments/latest', { headers: authHeaders() }).then(function (r) {
      return r.status === 204 ? null : r.json();
    })
  ]).then(function (results) {
    S.nodes    = results[0];
    S.students = results[1];
    S.teachers = results[2];
    populateCourseDropdown();
    if (results[3]) restoreSession(results[3]);
    renderTeachers();
    updateSummary();
  }).catch(function (e) {
    showToast('Failed to load data. Please refresh.', 'danger');
  });
});

// ── Card 1: Course Selection ───────────────────────────────────────────────────
function populateCourseDropdown() {
  var sel = document.getElementById('courseSelect');
  var courses = S.nodes.filter(function (n) { return !n.parentId && n.type === 'NODE'; });
  courses.forEach(function (c) {
    var opt = document.createElement('option');
    opt.value = c.id;
    opt.textContent = c.title;
    sel.appendChild(opt);
  });
}

function onCourseChange() {
  var id = parseInt(document.getElementById('courseSelect').value, 10);
  S.selectedCourseId = id || null;

  var panel   = document.getElementById('courseInfoPanel');
  var card2   = document.getElementById('card2');
  var card3   = document.getElementById('card3');

  var strip = document.getElementById('cardStatStrip');

  if (!id) {
    panel.style.display = 'none';
    if (strip) strip.style.display = 'none';
    card2.classList.add('card-locked');
    card3.classList.add('card-locked');
    S.scopeNodes = { course: null, subject: null, board: null };
    S.selectedScopeKey = null;
    updateCard2ScopeDisplay();
    return;
  }

  var course   = S.nodes.find(function (n) { return n.id === id; });
  var subjects = S.nodes.filter(function (n) { return n.parentId === id && n.type === 'NODE'; });
  var subIds   = subjects.map(function (s) { return s.id; });
  var boards   = S.nodes.filter(function (n) { return subIds.indexOf(n.parentId) !== -1 && n.type === 'NODE'; });
  var boardIds = boards.map(function (b) { return b.id; });
  var topics   = S.nodes.filter(function (n) { return boardIds.indexOf(n.parentId) !== -1 && n.type === 'NODE'; });
  var topicIds = topics.map(function (t) { return t.id; });
  var questions = S.nodes.filter(function (n) { return topicIds.indexOf(n.parentId) !== -1 && n.type === 'QUESTION'; });

  // Course row
  setText('infoCourseName', course.title);
  setText('infoCourseDesc', course.description || '');

  // Subject row
  var firstSub = subjects[0];
  setText('infoSubjectName', firstSub ? firstSub.title : '—');
  setText('infoSubjectDesc', firstSub && firstSub.description ? firstSub.description : '');
  if (subjects.length > 1) {
    var badge = document.getElementById('infoSubjectBadge');
    badge.textContent = subjects.length + ' subjects';
    badge.style.display = '';
  } else {
    document.getElementById('infoSubjectBadge').style.display = 'none';
  }

  // Exam Board row
  var firstBoard = boards[0];
  setText('infoBoardName', firstBoard ? firstBoard.title : '—');
  setText('infoBoardDesc', firstBoard && firstBoard.description ? firstBoard.description : '');

  // Stats
  setText('infoTopicCount',    topics.length);
  setText('infoQuestionCount', questions.length);
  setText('infoStudentCount',  S.students.length);

  // Academic period (current + next year)
  var year = new Date().getFullYear();
  setText('infoAcademicPeriod', year + '–' + (year + 1));

  if (strip) strip.style.display = '';

  // Store scope node candidates
  S.scopeNodes.course  = { id: course.id, title: course.title };
  S.scopeNodes.subject = firstSub   ? { id: firstSub.id,   title: firstSub.title   } : null;
  S.scopeNodes.board   = firstBoard ? { id: firstBoard.id, title: firstBoard.title  } : null;

  // Enable/disable radios based on availability
  setRadioEnabled('scopeRadio_subject', !!firstSub);
  setRadioEnabled('scopeRadio_board',   !!firstBoard);

  // Auto-select Course (L1) by default
  selectScope('course');

  panel.style.display = '';
  card2.classList.remove('card-locked');
  card3.classList.remove('card-locked');

  // Populate subject filter in Card 3
  var subFilter = document.getElementById('subjectFilter');
  subFilter.innerHTML = '<option value="">— All Subjects —</option>';
  subjects.forEach(function (s) {
    var opt = document.createElement('option');
    opt.value = s.id;
    opt.textContent = s.title;
    subFilter.appendChild(opt);
  });
}

function setText(id, val) {
  var el = document.getElementById(id);
  if (el) el.textContent = val;
}

// ── Scope radio helpers ────────────────────────────────────────────────────────
function setRadioEnabled(radioId, enabled) {
  var el = document.getElementById(radioId);
  if (el) { el.disabled = !enabled; el.closest('.selectable-row').style.opacity = enabled ? '' : '0.4'; }
}

function selectScope(key) {
  var radio = document.getElementById('scopeRadio_' + key);
  if (!radio || radio.disabled) return;
  radio.checked = true;
  S.selectedScopeKey = key;
  ['course', 'subject', 'board'].forEach(function (k) {
    var row = document.getElementById('scopeRow_' + k);
    if (row) row.classList.toggle('scope-selected', k === key);
  });
  updateCard2ScopeDisplay();
}

function onScopeRowClick(key) {
  var radio = document.getElementById('scopeRadio_' + key);
  if (radio && radio.disabled) return;
  selectScope(key);
}

function onScopeRadioChange(key) {
  selectScope(key);
}

function updateCard2ScopeDisplay() {
  var el   = document.getElementById('card2ScopeDisplay');
  var hint = document.getElementById('card2ScopeHint');
  if (!el) return;
  var node = S.selectedScopeKey ? S.scopeNodes[S.selectedScopeKey] : null;
  if (node) {
    el.value = node.title;
    el.style.color = '#1a2332';
    if (hint) hint.textContent = { course: 'Level: Course (L1)', subject: 'Level: Subject (L2)', board: 'Level: Exam Board (L3)' }[S.selectedScopeKey] || '';
  } else {
    el.value = '';
    if (hint) hint.textContent = 'Select Course, Subject, or Exam Board from panel';
  }
}

// ── Card 2: Create Groups ──────────────────────────────────────────────────────
function openCreateGroupModal() {
  if (!S.selectedCourseId) { showToast('Select a course first', 'warning'); return; }
  if (!S.selectedScopeKey || !S.scopeNodes[S.selectedScopeKey]) {
    showToast('Select Course, Subject, or Exam Board from the info panel', 'warning'); return;
  }

  var max = parseInt(document.getElementById('maxStudentsInput').value, 10);
  if (!max || max < 1) { showToast('Enter a valid Max Students per Group', 'warning'); return; }
  S.maxPerGroup = max;

  var scopeNode = S.scopeNodes[S.selectedScopeKey];
  var courseNameEl = document.getElementById('modalCourseName');
  if (courseNameEl) courseNameEl.value = scopeNode.title;
  setText('modalMaxStudents', max);
  setText('modalSelCount',    '0');
  setText('modalMaxCount',    max);

  document.getElementById('groupNameInput').value  = '';
  document.getElementById('groupDescInput').value  = '';
  var year = new Date().getFullYear();
  document.getElementById('groupPeriodInput').value = year + '–' + (year + 1);
  document.getElementById('studentSearchModal').value = '';
  document.getElementById('groupNameInput').classList.remove('is-invalid');
  document.getElementById('maxStudentsWarning').style.display = 'none';

  renderModalStudents('');

  new bootstrap.Modal(document.getElementById('createGroupModal')).show();
}

function renderModalStudents(search) {
  var list     = document.getElementById('studentCheckList');
  var filtered = S.students.filter(function (s) {
    if (!search) return true;
    return (s.firstName + ' ' + s.lastName).toLowerCase().indexOf(search.toLowerCase()) !== -1;
  });

  list.innerHTML = '';
  filtered.forEach(function (s) {
    var initials = ((s.firstName || '?')[0] + (s.lastName || '?')[0]).toUpperCase();
    var div = document.createElement('div');
    div.className = 'student-check-row d-flex align-items-center gap-2 py-2 px-2 rounded';
    div.innerHTML =
      '<input class="form-check-input flex-shrink-0" type="checkbox" value="' + s.id + '" ' +
        'id="msc_' + s.id + '" onchange="onStudentCheck()">' +
      '<div class="student-avatar-sm flex-shrink-0">' + escHtml(initials) + '</div>' +
      '<div class="flex-grow-1 min-width-0">' +
        '<div class="fw-medium small text-truncate">' + escHtml(s.firstName) + ' ' + escHtml(s.lastName) + '</div>' +
        '<div class="text-muted" style="font-size:.72rem;">' + escHtml(s.gradeYear || '') + (s.className ? ' · ' + escHtml(s.className) : '') + '</div>' +
      '</div>' +
      '<span class="badge bg-light text-dark border flex-shrink-0" style="font-size:.7rem;">' + escHtml(s.studentId || '') + '</span>';
    list.appendChild(div);
  });
}

function onStudentCheck() {
  var checked = document.querySelectorAll('#studentCheckList input[type=checkbox]:checked').length;
  setText('modalSelCount', checked);

  document.querySelectorAll('#studentCheckList input[type=checkbox]:not(:checked)').forEach(function (cb) {
    cb.disabled = checked >= S.maxPerGroup;
  });

  var warn = document.getElementById('maxStudentsWarning');
  warn.style.display = checked >= S.maxPerGroup ? '' : 'none';
}

function saveGroup() {
  var name   = document.getElementById('groupNameInput').value.trim();
  var desc   = document.getElementById('groupDescInput').value.trim();
  var period = document.getElementById('groupPeriodInput').value.trim();

  var nameInp = document.getElementById('groupNameInput');
  if (!name) { nameInp.classList.add('is-invalid'); return; }
  nameInp.classList.remove('is-invalid');

  var selectedIds = Array.from(document.querySelectorAll('#studentCheckList input[type=checkbox]:checked'))
    .map(function (cb) { return parseInt(cb.value, 10); });

  S.groups.push({ id: S.nextGroupId++, name: name, desc: desc, period: period, studentIds: selectedIds });

  bootstrap.Modal.getInstance(document.getElementById('createGroupModal')).hide();

  renderGroups();
  updateSummary();

  var el = document.getElementById('groupSuccessMsg');
  el.textContent = 'Group "' + name + '" created successfully.';
  el.style.display = '';
  setTimeout(function () { el.style.display = 'none'; }, 6000);
}

// ── Groups Panel ───────────────────────────────────────────────────────────────
function renderGroups() {
  var container = document.getElementById('groupsContainer');
  var empty     = document.getElementById('groupsEmpty');
  var chipEl    = document.getElementById('groupCountChip');

  chipEl.textContent = S.groups.length + ' Group' + (S.groups.length !== 1 ? 's' : '');

  if (S.groups.length === 0) {
    empty.style.display     = '';
    container.style.display = 'none';
    return;
  }
  empty.style.display     = 'none';
  container.style.display = '';
  container.innerHTML = '';

  S.groups.forEach(function (g, i) {
    var color    = GROUP_COLORS[i % GROUP_COLORS.length];
    var students = S.students.filter(function (s) { return g.studentIds.indexOf(s.id) !== -1; });
    var chipsHtml = students.slice(0, 10).map(function (s) {
      var initials = ((s.firstName || '?')[0] + (s.lastName || '?')[0]).toUpperCase();
      return '<span class="student-chip chip-' + color + '" title="' + escHtml(s.firstName + ' ' + s.lastName) + '">' + escHtml(initials) + '</span>';
    }).join('');
    if (students.length > 10) chipsHtml += '<span class="student-chip chip-muted">+' + (students.length - 10) + '</span>';

    var pct   = Math.round((students.length / S.maxPerGroup) * 100);
    var over  = students.length > S.maxPerGroup;

    var card = document.createElement('div');
    card.className = 'group-card';
    card.style.borderLeftColor = 'var(--group-' + color + ')';
    card.innerHTML =
      '<div class="d-flex align-items-start justify-content-between mb-1">' +
        '<div>' +
          '<div class="fw-semibold small">' + escHtml(g.name) + '</div>' +
          '<div class="text-muted" style="font-size:.72rem;">' + escHtml(g.period) + '</div>' +
        '</div>' +
        '<span class="badge ' + (over ? 'bg-danger' : groupBadgeClass(color)) + '" style="' + groupBadgeStyle(color, over) + '">' +
          students.length + '/' + S.maxPerGroup +
        '</span>' +
      '</div>' +
      '<div class="progress mb-2" style="height:4px;">' +
        '<div class="progress-bar" style="width:' + Math.min(pct, 100) + '%;background:var(--group-' + color + ');"></div>' +
      '</div>' +
      '<div class="mb-2">' +
        '<input class="form-control form-control-sm" style="font-size:.8rem;" placeholder="Search students…" ' +
          'oninput="filterGroupChips(' + g.id + ', this.value)">' +
      '</div>' +
      '<div class="student-chips-area" id="chips_' + g.id + '">' + chipsHtml + '</div>';
    container.appendChild(card);
  });
}

function filterGroupsPanel(search) {
  var cards = document.querySelectorAll('#groupsContainer .group-card');
  cards.forEach(function (card, i) {
    var name = S.groups[i] ? S.groups[i].name.toLowerCase() : '';
    card.style.display = (!search || name.indexOf(search.toLowerCase()) !== -1) ? '' : 'none';
  });
}

function filterGroupChips(groupId, search) {
  var g = S.groups.find(function (x) { return x.id === groupId; });
  if (!g) return;
  var i     = S.groups.indexOf(g);
  var color = GROUP_COLORS[i % GROUP_COLORS.length];
  var list  = S.students.filter(function (s) { return g.studentIds.indexOf(s.id) !== -1; });
  if (search) list = list.filter(function (s) {
    return (s.firstName + ' ' + s.lastName).toLowerCase().indexOf(search.toLowerCase()) !== -1;
  });
  var html = list.slice(0, 10).map(function (s) {
    var initials = ((s.firstName || '?')[0] + (s.lastName || '?')[0]).toUpperCase();
    return '<span class="student-chip chip-' + color + '">' + escHtml(initials) + '</span>';
  }).join('');
  var el = document.getElementById('chips_' + groupId);
  if (el) el.innerHTML = html;
}

// ── Teachers Panel ─────────────────────────────────────────────────────────────
function renderTeachers() {
  var tbody = document.getElementById('teachersBody');
  tbody.innerHTML = '';

  var active = S.teachers.filter(function (t) {
    return S.activatedTeacherIds.indexOf(t.id) !== -1;
  });

  if (!active.length) {
    tbody.innerHTML =
      '<tr><td colspan="3" class="text-center text-muted py-5 small">' +
        '<div class="mb-2"><i class="bi bi-person-badge" style="font-size:1.6rem;color:#c4b5fd;"></i></div>' +
        'No teachers assigned yet.<br>' +
        '<span style="font-size:.75rem;">Use <strong>Assign Teachers</strong> or <strong>Assign Group</strong> to add teachers.</span>' +
      '</td></tr>';
    return;
  }

  active.forEach(function (t) {
    var assignedGroupId = S.assignments[t.id];
    var assignedGroup   = assignedGroupId ? S.groups.find(function (g) { return g.id === assignedGroupId; }) : null;
    var initials        = ((t.firstName || '?')[0] + (t.lastName || '?')[0]).toUpperCase();

    var assignmentHtml;
    if (assignedGroup) {
      assignmentHtml =
        '<div class="d-flex align-items-center gap-2">' +
          '<span class="badge bg-primary-subtle text-primary-emphasis border border-primary-subtle">' + escHtml(assignedGroup.name) + '</span>' +
          '<button class="btn btn-xs btn-outline-secondary" style="padding:.25rem .35rem;font-size:.65rem;" title="Edit" onclick="openEditAssignmentModal(' + t.id + ', ' + assignedGroupId + ')">' +
            '<i class="bi bi-pencil"></i>' +
          '</button>' +
          '<button class="btn btn-xs btn-outline-danger" style="padding:.25rem .35rem;font-size:.65rem;" title="Delete" onclick="deleteAssignment(' + t.id + ')">' +
            '<i class="bi bi-trash"></i>' +
          '</button>' +
        '</div>';
    } else {
      var opts = S.groups.map(function (g) {
        return '<option value="' + g.id + '">' + escHtml(g.name) + '</option>';
      }).join('');
      assignmentHtml =
        '<select class="form-select form-select-sm" style="min-width:130px; font-size:.8rem;" ' +
          'onchange="assignTeacher(' + t.id + ', this.value)">' +
          '<option value="">— Assign group —</option>' + opts +
        '</select>';
    }

    var tr = document.createElement('tr');
    tr.innerHTML =
      '<td>' +
        '<div class="d-flex align-items-center gap-2">' +
          '<div class="teacher-avatar-sm flex-shrink-0">' + escHtml(initials) + '</div>' +
          '<div>' +
            '<div class="fw-medium small">' + escHtml(t.firstName) + ' ' + escHtml(t.lastName) + '</div>' +
            '<div class="text-muted" style="font-size:.72rem;">' + escHtml(t.teacherId || '') + '</div>' +
          '</div>' +
        '</div>' +
      '</td>' +
      '<td class="small text-muted">' + escHtml(t.department || t.designation || '—') + '</td>' +
      '<td>' + assignmentHtml + '</td>' +
      '<td></td>';
    tbody.appendChild(tr);
  });
}

function assignTeacher(teacherId, groupId) {
  if (groupId) S.assignments[teacherId] = parseInt(groupId, 10);
  else delete S.assignments[teacherId];
  renderTeachers();
  updateSummary();
}

// ── Card 3: Assign Teachers Modal ─────────────────────────────────────────────
function openAssignTeachersModal() {
  if (!S.selectedCourseId) { showToast('Select a course first', 'warning'); return; }

  var subjectId = document.getElementById('subjectFilter').value;
  var subjectName = '';
  if (subjectId) {
    var subjectNode = S.nodes.find(function (n) { return n.id === parseInt(subjectId, 10); });
    subjectName = subjectNode ? subjectNode.title : '';
  }
  var subjectEl = document.getElementById('modalAssignSubject');
  if (subjectEl) subjectEl.value = subjectName || 'All Subjects';

  document.getElementById('teacherSearchModal').value = '';
  renderModalTeachers('');

  new bootstrap.Modal(document.getElementById('assignTeachersModal')).show();
}

function renderModalTeachers(search) {
  var list     = document.getElementById('teacherCheckList');
  var filtered = S.teachers.filter(function (t) {
    if (!search) return true;
    var name = (t.firstName + ' ' + t.lastName).toLowerCase();
    var dept = (t.department || t.designation || '').toLowerCase();
    var q    = search.toLowerCase();
    return name.indexOf(q) !== -1 || dept.indexOf(q) !== -1 || (t.teacherId || '').toLowerCase().indexOf(q) !== -1;
  });

  list.innerHTML = '';

  if (!filtered.length) {
    list.innerHTML = '<div class="text-center text-muted py-4 small">No teachers found.</div>';
    updateModalTeacherCount();
    return;
  }

  filtered.forEach(function (t) {
    var initials   = ((t.firstName || '?')[0] + (t.lastName || '?')[0]).toUpperCase();
    var isActive   = S.activatedTeacherIds.indexOf(t.id) !== -1;
    var dept       = t.department || t.designation || '';

    var div = document.createElement('div');
    div.className = 'teacher-check-row d-flex align-items-center gap-2 py-2 px-2 rounded' + (isActive ? ' is-activated' : '');
    div.innerHTML =
      '<input class="form-check-input flex-shrink-0" type="checkbox" value="' + t.id + '" ' +
        'id="mtc_' + t.id + '"' + (isActive ? ' checked' : '') + ' onchange="onTeacherCheck()">' +
      '<div class="teacher-avatar-sm flex-shrink-0">' + escHtml(initials) + '</div>' +
      '<div class="flex-grow-1 min-width-0">' +
        '<div class="fw-medium small text-truncate">' + escHtml(t.firstName) + ' ' + escHtml(t.lastName) + '</div>' +
        '<div class="text-muted" style="font-size:.72rem;">' + escHtml(dept) + '</div>' +
      '</div>' +
      '<span class="badge bg-light text-dark border flex-shrink-0" style="font-size:.7rem;">' + escHtml(t.teacherId || '') + '</span>';
    list.appendChild(div);
  });

  updateModalTeacherCount();
}

function onTeacherCheck() {
  updateModalTeacherCount();
}

function updateModalTeacherCount() {
  var count = document.querySelectorAll('#teacherCheckList input[type=checkbox]:checked').length;
  var badge = document.getElementById('modalTeacherSelBadge');
  var btn   = document.getElementById('modalAssignCount');
  if (badge) badge.textContent = count + ' selected';
  if (btn)   btn.textContent   = count;
}

function confirmAssignTeachers() {
  var checked = Array.from(document.querySelectorAll('#teacherCheckList input[type=checkbox]:checked'))
    .map(function (cb) { return parseInt(cb.value, 10); });

  // Merge into activatedTeacherIds (no duplicates)
  checked.forEach(function (id) {
    if (S.activatedTeacherIds.indexOf(id) === -1) S.activatedTeacherIds.push(id);
  });

  bootstrap.Modal.getInstance(document.getElementById('assignTeachersModal')).hide();

  renderTeachers();
  updateSummary();

  var subjectId = document.getElementById('subjectFilter').value;
  var subject = 'all subjects';
  if (subjectId) {
    var subjectNode = S.nodes.find(function (n) { return n.id === parseInt(subjectId, 10); });
    subject = subjectNode ? subjectNode.title : 'all subjects';
  }
  showToast(checked.length + ' teacher(s) assigned for ' + subject, 'success');
}

// ── Summary ────────────────────────────────────────────────────────────────────
function updateSummary() {
  setText('sumStudents',  S.students.length);
  setText('sumGroups',    S.groups.length);
  setText('sumAssigned',  Object.keys(S.assignments).length);
  var assignedGroupIds = Object.values(S.assignments);
  var unassigned = S.groups.filter(function (g) {
    return assignedGroupIds.indexOf(g.id) === -1;
  }).length;
  setText('sumUnassigned', unassigned);
}

// ── Restore saved session ─────────────────────────────────────────────────────
function restoreSession(session) {
  var sel = document.getElementById('courseSelect');
  sel.value = session.courseNodeId;
  onCourseChange();

  if (session.scopeLevel) selectScope(session.scopeLevel);

  var maxInp = document.getElementById('maxStudentsInput');
  if (maxInp) maxInp.value = session.maxPerGroup;
  S.maxPerGroup = session.maxPerGroup;

  S.groups = (session.groups || []).map(function (g) {
    return { id: g.id, name: g.name, desc: g.description || '', period: g.period || '', studentIds: g.studentIds || [] };
  });
  var maxId = S.groups.reduce(function (m, g) { return Math.max(m, g.id); }, 0);
  S.nextGroupId = maxId + 1;

  S.activatedTeacherIds = [];
  S.assignments = {};
  (session.teacherAssignments || []).forEach(function (ta) {
    if (S.activatedTeacherIds.indexOf(ta.teacherId) === -1) S.activatedTeacherIds.push(ta.teacherId);
    if (ta.groupId) S.assignments[ta.teacherId] = ta.groupId;
  });

  renderGroups();

  var banner = document.getElementById('groupSuccessMsg');
  if (banner) {
    banner.textContent = 'Restored ' + S.groups.length + ' group(s) from last saved session (' + session.status + ').';
    banner.style.display = '';
    setTimeout(function () { banner.style.display = 'none'; }, 6000);
  }
}

// ── Actions ────────────────────────────────────────────────────────────────────
function buildSavePayload(status) {
  var scopeNode = S.selectedScopeKey ? S.scopeNodes[S.selectedScopeKey] : null;
  return {
    courseNodeId: S.selectedCourseId,
    scopeNodeId:  scopeNode ? scopeNode.id : null,
    scopeLevel:   S.selectedScopeKey,
    maxPerGroup:  S.maxPerGroup,
    status:       status,
    groups: S.groups.map(function (g) {
      return { localId: g.id, name: g.name, description: g.desc, period: g.period, studentIds: g.studentIds };
    }),
    teacherAssignments: S.activatedTeacherIds.map(function (tid) {
      return { teacherId: tid, groupLocalId: S.assignments[tid] || null };
    })
  };
}

function postSave(status, toastType, btnId) {
  if (!S.selectedCourseId) { showToast('Select a course first', 'warning'); return; }
  if (status === 'SAVED' && !S.groups.length) {
    showToast('Create at least one group before saving', 'warning'); return;
  }
  var btn = document.getElementById(btnId);
  var origLabel = btn ? btn.innerHTML : '';
  if (btn) { btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Saving…'; }

  fetch('/api/admin/assignments', {
    method:  'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body:    JSON.stringify(buildSavePayload(status))
  })
  .then(function (r) {
    if (!r.ok) throw new Error('HTTP ' + r.status);
    return r.json();
  })
  .then(function (data) {
    showToast(data.message || 'Saved', toastType);
  })
  .catch(function () {
    showToast('Failed to save. Please try again.', 'danger');
  })
  .finally(function () {
    if (btn) { btn.disabled = false; btn.innerHTML = origLabel; }
  });
}

function openEditAssignmentModal(teacherId, currentGroupId) {
  var teacher = S.teachers.find(function (t) { return t.id === teacherId; });
  if (!teacher) return;

  var groupOptions = S.groups.map(function (g) {
    return (g.id === currentGroupId ? '✓ ' : '') + g.name;
  }).join('\n');

  var groupList = '';
  S.groups.forEach(function (g) {
    groupList += (g.id === currentGroupId ? '[CURRENT] ' : '') + g.name + '\n';
  });

  var selectedName = S.groups.find(function (g) { return g.id === currentGroupId; });
  selectedName = selectedName ? selectedName.name : 'Unknown';

  var newGroupName = prompt(
    'Edit assignment for: ' + teacher.firstName + ' ' + teacher.lastName + '\n\n' +
    'Current group: ' + selectedName + '\n\n' +
    'Select new group:\n' + groupList,
    selectedName
  );

  if (newGroupName) {
    var newGroup = S.groups.find(function (g) { return g.name === newGroupName; });
    if (newGroup) {
      S.assignments[teacherId] = newGroup.id;
      renderTeachers();
      updateSummary();
      showToast('Assignment updated to ' + newGroupName, 'success');
    }
  }
}

function deleteAssignment(teacherId) {
  if (confirm('Remove this teacher assignment?')) {
    delete S.assignments[teacherId];
    renderTeachers();
    updateSummary();
    showToast('Assignment removed', 'success');
  }
}

function saveAsDraft()   { postSave('DRAFT', 'info',    'btnSaveDraft'); }
function reviewAndSave() { postSave('SAVED', 'success', 'btnReviewSave'); }

// ── Assign Group Modal ──────────────────────────────────────────────────────────
var agPairs = [];  // {teacherId, groupId}

function openAssignGroupModal() {
  if (!S.selectedCourseId) { showToast('Select a course first', 'warning'); return; }
  if (!S.groups.length) { showToast('Create at least one group first', 'warning'); return; }

  agPairs = [];
  populateAgDropdowns();
  renderAgPairs();

  new bootstrap.Modal(document.getElementById('assignGroupModal')).show();
}

function populateAgDropdowns() {
  var teacherSel = document.getElementById('agTeacherSelect');
  var groupSel = document.getElementById('agGroupSelect');

  teacherSel.innerHTML = '<option value="">— Choose Teacher —</option>';
  S.teachers.forEach(function (t) {
    var opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = escHtml(t.firstName + ' ' + t.lastName);
    teacherSel.appendChild(opt);
  });

  groupSel.innerHTML = '<option value="">— Choose Group —</option>';
  S.groups.forEach(function (g) {
    var opt = document.createElement('option');
    opt.value = g.id;
    opt.textContent = escHtml(g.name);
    groupSel.appendChild(opt);
  });
}

function addTeacherGroupPair() {
  var teacherId = parseInt(document.getElementById('agTeacherSelect').value, 10);
  var groupId = parseInt(document.getElementById('agGroupSelect').value, 10);

  if (!teacherId || !groupId) { showToast('Select both teacher and group', 'warning'); return; }

  if (agPairs.find(function (p) { return p.teacherId === teacherId && p.groupId === groupId; })) {
    showToast('This pair already in list', 'warning'); return;
  }

  if (agPairs.find(function (p) { return p.teacherId === teacherId; })) {
    showToast('Teacher already assigned in this batch. Remove or edit existing.', 'warning'); return;
  }

  agPairs.push({ teacherId: teacherId, groupId: groupId });
  document.getElementById('agTeacherSelect').value = '';
  document.getElementById('agGroupSelect').value = '';
  renderAgPairs();
}

function removeAgPair(index) {
  agPairs.splice(index, 1);
  renderAgPairs();
}

function renderAgPairs() {
  var list = document.getElementById('agPairsList');
  setText('agPairCount', agPairs.length);

  if (!agPairs.length) {
    list.innerHTML = '<div class="text-center text-muted py-3 small">No assignments yet. Add combinations above.</div>';
    return;
  }

  list.innerHTML = '';
  agPairs.forEach(function (pair, i) {
    var teacher = S.teachers.find(function (t) { return t.id === pair.teacherId; });
    var group = S.groups.find(function (g) { return g.id === pair.groupId; });
    var teacherName = teacher ? (teacher.firstName + ' ' + teacher.lastName) : 'Unknown';
    var groupName = group ? group.name : 'Unknown';

    var div = document.createElement('div');
    div.className = 'd-flex align-items-center justify-content-between p-2 mb-2 rounded bg-light border';
    div.innerHTML =
      '<div>' +
        '<div class="fw-medium small">' + escHtml(teacherName) + '</div>' +
        '<div class="text-muted" style="font-size:.72rem;">→ ' + escHtml(groupName) + '</div>' +
      '</div>' +
      '<button class="btn btn-sm btn-outline-danger" onclick="removeAgPair(' + i + ')">' +
        '<i class="bi bi-trash"></i>' +
      '</button>';
    list.appendChild(div);
  });
}

function saveAssignGroups() {
  if (!agPairs.length) { showToast('Add at least one assignment', 'warning'); return; }

  agPairs.forEach(function (pair) {
    S.assignments[pair.teacherId] = pair.groupId;
    if (S.activatedTeacherIds.indexOf(pair.teacherId) === -1) {
      S.activatedTeacherIds.push(pair.teacherId);
    }
  });

  bootstrap.Modal.getInstance(document.getElementById('assignGroupModal')).hide();
  renderTeachers();
  updateSummary();

  showToast(agPairs.length + ' assignment(s) saved', 'success');
}
