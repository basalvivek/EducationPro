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
async function safeJson(res) {
  const ct = res.headers.get('content-type') || '';
  return ct.includes('application/json') ? res.json() : { message: 'Server error (' + res.status + ')' };
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
  setTimeout(function() { el.remove(); }, 4000);
}
function escHtml(s) {
  return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ── Config (page-level override via window.EXAM_SUBMIT_LABEL) ─────────────────
var SUBMIT_LABEL = (typeof window !== 'undefined' && window.EXAM_SUBMIT_LABEL) || 'Submit / Approve';

// ── State ─────────────────────────────────────────────────────────────────────
var currentExam   = null;   // ExamDto
var examList      = [];     // ExamSummaryDto[]
var searchResults = [];     // QuestionSearchDto[]
var hasSearched   = false;  // tracks whether user has performed at least one search

// Cascade dropdown config: [dropdownId, label for empty-option, placeholder-when-disabled]
var CASCADE_LEVELS = [
  { id: 'filterClass',     label: 'All Classes',     placeholder: 'Loading…' },
  { id: 'filterSubject',   label: 'All Subjects',    placeholder: '— Select Class first —' },
  { id: 'filterExamBoard', label: 'All Exam Boards', placeholder: '— Select Subject first —' },
  { id: 'filterTopic',     label: 'All Topics',      placeholder: '— Select Exam Board first —' },
  { id: 'filterSubTopic',  label: 'All Sub Topics',  placeholder: '— Select Topic first —' }
];
// selected node ID at each level (null = none)
var selectedLevelId = [null, null, null, null, null];

// ── Boot ──────────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function() {
  Promise.all([loadExamList(), loadTreeNodes(null, 0)]).then(function() {
    bindEvents();
    var params  = new URLSearchParams(window.location.search);
    var examId  = params.get('examId');
    if (examId) {
      document.getElementById('examSelect').value = examId;
      loadExam(Number(examId));
    }
  });
});

// ── API ───────────────────────────────────────────────────────────────────────
async function loadExamList() {
  var res = await fetch('/api/admin/exams', { headers: authHeaders() });
  examList = res.ok ? await res.json() : [];
  renderExamSelect();
  renderExamList();
}

async function loadExam(id) {
  var res = await fetch('/api/admin/exams/' + id, { headers: authHeaders() });
  if (res.ok) {
    currentExam = await res.json();
    renderExamBuilder();
    renderQuestionPicker();
    renderExamList();
    document.getElementById('btnDeleteExam').disabled = false;
  } else {
    showToast('Failed to load exam.', 'danger');
  }
}

/**
 * Load NODE children for a cascade level.
 * level 0 = Class (root), 1 = Subject, 2 = Exam Board, 3 = Topic, 4 = Sub Topic.
 * parentNodeId = null → fetch root-level nodes.
 */
async function loadTreeNodes(parentNodeId, level) {
  if (level >= CASCADE_LEVELS.length) return;
  var url = '/api/admin/question-search/tree-nodes';
  if (parentNodeId != null) url += '?parentId=' + parentNodeId;
  var res = await fetch(url, { headers: authHeaders() });
  var nodes = res.ok ? await res.json() : [];

  var cfg = CASCADE_LEVELS[level];
  var el  = document.getElementById(cfg.id);
  if (!el) return;

  // Reset this level's selection state — the dropdown is being repopulated
  selectedLevelId[level] = null;

  el.innerHTML = '<option value="">' + cfg.label + '</option>';
  nodes.forEach(function(n) { el.add(new Option(n.title, n.id)); });
  el.disabled = false;

  // Clear and disable all levels below this one
  clearLevelsBelow(level);
}

function clearLevelsBelow(level) {
  for (var i = level + 1; i < CASCADE_LEVELS.length; i++) {
    selectedLevelId[i] = null;
    var cfg = CASCADE_LEVELS[i];
    var el  = document.getElementById(cfg.id);
    if (el) {
      el.innerHTML = '<option value="">' + cfg.placeholder + '</option>';
      el.disabled = true;
    }
  }
}

async function searchQuestions() {
  // Use deepest selected node ID
  var nodeId = null;
  for (var i = CASCADE_LEVELS.length - 1; i >= 0; i--) {
    if (selectedLevelId[i] != null) { nodeId = selectedLevelId[i]; break; }
  }
  var params = new URLSearchParams();
  if (nodeId != null) params.set('nodeId', nodeId);
  var complexity = (document.getElementById('filterComplexity') || {}).value;
  if (complexity) params.set('complexity', complexity);

  var res = await fetch('/api/admin/question-search?' + params.toString(), { headers: authHeaders() });
  searchResults = res.ok ? await res.json() : [];
  hasSearched = true;
  renderQuestionPicker();
}

// ── Render ─────────────────────────────────────────────────────────────────────
function renderExamSelect() {
  var sel = document.getElementById('examSelect');
  if (!sel) return;
  var cur = sel.value;
  while (sel.options.length > 1) sel.remove(1);
  examList.forEach(function(e) {
    var lbl = e.status === 'PENDING_APPROVAL' ? 'Pending' : e.status;
    sel.add(new Option(e.name + ' (' + lbl + ')', e.id));
  });
  if (cur) sel.value = cur;
}

// Sync examList summary entry from currentExam (avoids full API reload after add/remove/reorder)
function syncExamListEntry() {
  if (!currentExam) return;
  var idx = -1;
  for (var i = 0; i < examList.length; i++) {
    if (examList[i].id === currentExam.id) { idx = i; break; }
  }
  if (idx !== -1) {
    examList[idx].totalMarks    = currentExam.totalMarks;
    examList[idx].questionCount = (currentExam.questions || []).length;
  }
  renderExamList();
}

function renderExamList() {
  var container = document.getElementById('examListEntries');
  var countEl   = document.getElementById('examListCount');
  if (!container) return;

  var query = ((document.getElementById('examSearch') || {}).value || '').toLowerCase().trim();
  var filtered = examList.filter(function(e) {
    return !query || e.name.toLowerCase().indexOf(query) !== -1;
  });

  if (countEl) countEl.textContent = examList.length;

  if (!filtered.length) {
    container.innerHTML = '<div class="panel-empty"><i class="bi bi-journal-x"></i><p>' +
      (query ? 'No exams match your search.' : 'No exams yet.<br>Create one in the builder.') +
      '</p></div>';
    return;
  }

  container.innerHTML = filtered.map(function(e) {
    var isActive   = currentExam && currentExam.id === e.id;
    var statusCls = e.status === 'APPROVED' ? 'bg-success'
                  : e.status === 'PENDING_APPROVAL' ? 'bg-warning text-dark'
                  : 'bg-secondary';
    var statusLabel = e.status === 'PENDING_APPROVAL' ? 'Pending' : e.status;
    return '<div class="exam-card' + (isActive ? ' active' : '') + '" data-id="' + e.id + '">' +
      '<div class="d-flex justify-content-between align-items-start gap-1 mb-1">' +
        '<span class="exam-card__name" title="' + escHtml(e.name) + '">' + escHtml(e.name) + '</span>' +
        '<span class="badge ' + statusCls + ' flex-shrink-0">' + statusLabel + '</span>' +
      '</div>' +
      '<div class="exam-card__meta">' +
        '<span><i class="bi bi-patch-question me-1"></i>' + e.questionCount + ' Q</span>' +
        '<span><i class="bi bi-award me-1"></i>' + e.totalMarks + ' mk</span>' +
        '<span><i class="bi bi-clock me-1"></i>' + e.timeLimitMinutes + ' min</span>' +
      '</div>' +
    '</div>';
  }).join('');
}

function renderQuestionPicker() {
  var list = document.getElementById('questionPickerList');
  if (!list) return;
  if (!searchResults.length) {
    list.innerHTML = hasSearched
      ? '<div class="panel-empty"><i class="bi bi-search"></i><p>No questions match your filters.</p></div>'
      : '<div class="panel-empty"><i class="bi bi-funnel"></i><p>Select a class above<br>or click <strong>Apply Filter</strong>.</p></div>';
    return;
  }
  var examQIds = new Set((currentExam && currentExam.questions ? currentExam.questions : []).map(function(q) { return q.questionId; }));
  list.innerHTML = searchResults.map(function(q) {
    var inExam   = examQIds.has(q.id);
    var typeCol  = typeColor(q.questionType);
    var compCol  = complexityColor(q.complexity);
    var preview  = (q.questionText || '').substring(0, 70);
    return '<div class="picker-item" data-id="' + q.id + '">' +
      '<div class="flex-grow-1 overflow-hidden">' +
        '<div class="picker-item__title" title="' + escHtml(q.title) + '">' + escHtml(q.title) + '</div>' +
        '<div class="picker-item__preview">' + escHtml(preview) + (preview.length < (q.questionText || '').length ? '…' : '') + '</div>' +
        '<div class="picker-item__tags">' +
          '<span class="badge ' + typeCol + '">' + escHtml(q.questionType || '?') + '</span>' +
          (q.complexity ? '<span class="badge ' + compCol + '">' + escHtml(q.complexity) + '</span>' : '') +
          '<span class="badge bg-light text-dark border">' + q.marks + ' mk</span>' +
        '</div>' +
      '</div>' +
      '<button class="btn btn-sm ' + (inExam ? 'btn-secondary' : 'btn-outline-primary') + ' flex-shrink-0 add-q-btn" ' +
        'data-id="' + q.id + '" ' + (inExam ? 'disabled' : '') + ' title="' + (inExam ? 'Already added' : 'Add to exam') + '">' +
        (inExam ? '<i class="bi bi-check-lg"></i>' : '<i class="bi bi-plus-lg"></i>') +
      '</button>' +
    '</div>';
  }).join('');
}

function renderExamBuilder() {
  var qList = document.getElementById('examQuestionList');

  if (!currentExam) {
    qList.innerHTML = '<div class="panel-empty"><i class="bi bi-clipboard2-x"></i><p>No questions yet.<br>Use the picker on the left.</p></div>';
    document.getElementById('qCountBadge').textContent  = '0';
    document.getElementById('totalMarksDisplay').textContent = '0';
    var nullSubmit = document.getElementById('btnSubmit');
    nullSubmit.disabled   = true;
    nullSubmit.className  = 'btn btn-success';
    nullSubmit.innerHTML  = '<i class="bi bi-check-circle me-1"></i>' + SUBMIT_LABEL;
    document.getElementById('btnDeleteExam').disabled = true;
    return;
  }

  // Populate form fields
  document.getElementById('examName').value         = currentExam.name || '';
  document.getElementById('examDesc').value         = currentExam.description || '';
  document.getElementById('examTimeLimit').value    = currentExam.timeLimitMinutes || 60;
  document.getElementById('examTotalMarks').value   = currentExam.totalMarks || 0;
  document.getElementById('examPassMark').value     = currentExam.passMark != null ? currentExam.passMark : '';
  document.getElementById('examShuffleQ').checked   = !!currentExam.shuffleQuestions;
  document.getElementById('examShuffleOpt').checked = !!currentExam.shuffleOptions;
  document.getElementById('totalMarksDisplay').textContent = currentExam.totalMarks || 0;

  var status    = currentExam.status;
  var isDraft   = status === 'DRAFT';
  var isPending = status === 'PENDING_APPROVAL';
  var submitBtn = document.getElementById('btnSubmit');
  submitBtn.disabled = !isDraft;
  if (isDraft) {
    submitBtn.className = 'btn btn-success';
    submitBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i>' + SUBMIT_LABEL;
  } else if (isPending) {
    submitBtn.className = 'btn btn-warning';
    submitBtn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Awaiting Approval';
  } else {
    submitBtn.className = 'btn btn-secondary';
    submitBtn.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>Approved';
  }
  document.getElementById('btnDeleteExam').disabled = false;

  var qs = currentExam.questions || [];
  document.getElementById('qCountBadge').textContent = qs.length;

  if (!qs.length) {
    qList.innerHTML = '<div class="panel-empty"><i class="bi bi-clipboard2-x"></i><p>No questions yet.<br>Use the picker on the left.</p></div>';
    return;
  }

  qList.innerHTML = qs.map(function(q, i) {
    var preview = (q.questionText || '').substring(0, 65);
    return '<div class="exam-q-row" data-qid="' + q.questionId + '">' +
      '<span class="exam-q-row__num">' + (i + 1) + '</span>' +
      '<div class="exam-q-row__reorder">' +
        '<button class="btn btn-sm btn-outline-secondary move-up-btn" data-idx="' + i + '" ' + (i === 0 ? 'disabled' : '') + ' title="Move up"><i class="bi bi-chevron-up"></i></button>' +
        '<button class="btn btn-sm btn-outline-secondary move-dn-btn" data-idx="' + i + '" ' + (i === qs.length - 1 ? 'disabled' : '') + ' title="Move down"><i class="bi bi-chevron-down"></i></button>' +
      '</div>' +
      '<div class="flex-grow-1 overflow-hidden">' +
        '<div class="exam-q-row__title">' + escHtml(q.title) + '</div>' +
        '<div class="exam-q-row__preview">' + escHtml(preview) + (preview.length < (q.questionText || '').length ? '…' : '') + '</div>' +
        '<span class="badge ' + typeColor(q.questionType) + ' mt-1">' + escHtml(q.questionType || '') + '</span>' +
      '</div>' +
      '<span class="badge bg-light text-dark border flex-shrink-0">' + (q.marksOverride != null ? q.marksOverride : q.marks) + ' mk</span>' +
      '<button class="btn btn-sm btn-outline-danger rm-q-btn flex-shrink-0" data-qid="' + q.questionId + '" title="Remove"><i class="bi bi-x-lg"></i></button>' +
    '</div>';
  }).join('');
}

// ── Events ─────────────────────────────────────────────────────────────────────
function bindEvents() {

  // Exam selector (dropdown)
  document.getElementById('examSelect').addEventListener('change', function(e) {
    var id = e.target.value;
    if (id) {
      loadExam(Number(id));
    } else {
      currentExam = null;
      resetForm();
      renderQuestionPicker();
      renderExamList();
    }
  });

  // New exam
  document.getElementById('btnNewExam').addEventListener('click', function() {
    document.getElementById('examSelect').value = '';
    currentExam = null;
    resetForm();
    renderQuestionPicker();
    renderExamList();
  });

  // Saved exams panel — search
  var examSearchEl = document.getElementById('examSearch');
  if (examSearchEl) examSearchEl.addEventListener('input', renderExamList);

  // Saved exams panel — click card to load
  document.getElementById('examListEntries').addEventListener('click', function(e) {
    var card = e.target.closest('.exam-card');
    if (!card) return;
    var id = Number(card.dataset.id);
    document.getElementById('examSelect').value = id;
    loadExam(id);
  });

  // Delete exam
  document.getElementById('btnDeleteExam').addEventListener('click', async function() {
    if (!currentExam) return;
    if (!confirm('Delete "' + currentExam.name + '"? This cannot be undone.')) return;
    var res = await fetch('/api/admin/exams/' + currentExam.id, { method: 'DELETE', headers: authHeaders() });
    if (res.ok) {
      showToast('Exam deleted.', 'success');
      currentExam = null;
      await loadExamList();
      resetForm();
      renderQuestionPicker();
    } else {
      showToast('Delete failed.', 'danger');
    }
  });

  // Cascade dropdowns: each level change loads children for next level + auto-searches
  CASCADE_LEVELS.forEach(function(cfg, idx) {
    var el = document.getElementById(cfg.id);
    if (!el) return;
    el.addEventListener('change', function() {
      var val = this.value ? Number(this.value) : null;
      selectedLevelId[idx] = val;
      if (val && idx + 1 < CASCADE_LEVELS.length) {
        // Load children for next level
        loadTreeNodes(val, idx + 1).then(searchQuestions);
      } else {
        // Nothing selected or last level — just clear below and search
        clearLevelsBelow(idx);
        searchQuestions();
      }
    });
  });

  // Complexity is independent — just re-search
  var compEl = document.getElementById('filterComplexity');
  if (compEl) compEl.addEventListener('change', searchQuestions);

  // Manual apply button (re-runs search with current selections)
  document.getElementById('btnSearch').addEventListener('click', searchQuestions);

  // Refresh: reload root (Class) nodes and reset all cascade state
  document.getElementById('btnRefreshFilters').addEventListener('click', function() {
    selectedLevelId = [null, null, null, null, null];
    searchResults   = [];
    hasSearched     = false;
    loadTreeNodes(null, 0).then(function() {
      document.getElementById('questionPickerList').innerHTML =
        '<p class="text-muted small text-center mt-4 px-2">Click <strong>Apply Filter</strong> to load questions.</p>';
    });
  });

  // Save draft
  document.getElementById('btnSaveDraft').addEventListener('click', saveExam);

  // Submit / approve
  document.getElementById('btnSubmit').addEventListener('click', async function() {
    if (!currentExam) return;
    var res = await fetch('/api/admin/exams/' + currentExam.id + '/submit', { method: 'POST', headers: authHeaders() });
    if (res.ok) {
      currentExam = await res.json();
      await loadExamList();
      document.getElementById('examSelect').value = currentExam.id;
      renderExamBuilder();
      var msg = currentExam.status === 'PENDING_APPROVAL'
        ? 'Submitted for approval. Awaiting admin review.'
        : 'Exam approved.';
      showToast(msg, 'success');
    } else {
      var d = await safeJson(res);
      showToast(d.message || 'Submit failed.', 'danger');
    }
  });

  // Live total marks display
  document.getElementById('examTotalMarks').addEventListener('input', function() {
    document.getElementById('totalMarksDisplay').textContent = parseInt(this.value) || 0;
  });

  // Add question from picker (delegated)
  document.getElementById('questionPickerList').addEventListener('click', async function(e) {
    var btn = e.target.closest('.add-q-btn');
    if (!btn || btn.disabled) return;

    // Auto-save a new exam if none exists yet
    if (!currentExam) {
      var name = (document.getElementById('examName').value || '').trim();
      if (!name) {
        showToast('Enter an exam name first, then add questions.', 'warning');
        document.getElementById('examName').focus();
        return;
      }
      await saveExam();
      if (!currentExam) return; // save failed (e.g. server error)
    }

    var qid = Number(btn.dataset.id);
    var res = await fetch('/api/admin/exams/' + currentExam.id + '/questions', {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ questionId: qid })
    });
    if (res.ok) {
      currentExam = await res.json();
      renderExamBuilder();
      renderQuestionPicker();
      syncExamListEntry();
      showToast('Question added.', 'success');
    } else {
      var d = await safeJson(res);
      showToast(d.message || 'Failed to add question.', 'danger');
    }
  });

  // Remove / reorder in exam list (delegated)
  document.getElementById('examQuestionList').addEventListener('click', async function(e) {

    // Remove
    var rm = e.target.closest('.rm-q-btn');
    if (rm && currentExam) {
      var qid = Number(rm.dataset.qid);
      var res = await fetch('/api/admin/exams/' + currentExam.id + '/questions/' + qid, { method: 'DELETE', headers: authHeaders() });
      if (res.ok) {
        currentExam = await res.json();
        renderExamBuilder();
        renderQuestionPicker();
        syncExamListEntry();
        showToast('Question removed.', 'success');
      } else {
        showToast('Remove failed.', 'danger');
      }
      return;
    }

    // Move up
    var up = e.target.closest('.move-up-btn');
    if (up && currentExam) {
      var idx = Number(up.dataset.idx);
      if (idx === 0) return;
      var qs = currentExam.questions.slice();
      var tmp = qs[idx - 1]; qs[idx - 1] = qs[idx]; qs[idx] = tmp;
      await reorderExam(qs.map(function(q) { return q.questionId; }));
      return;
    }

    // Move down
    var dn = e.target.closest('.move-dn-btn');
    if (dn && currentExam) {
      var idx2 = Number(dn.dataset.idx);
      if (idx2 >= currentExam.questions.length - 1) return;
      var qs2 = currentExam.questions.slice();
      var tmp2 = qs2[idx2 + 1]; qs2[idx2 + 1] = qs2[idx2]; qs2[idx2] = tmp2;
      await reorderExam(qs2.map(function(q) { return q.questionId; }));
    }
  });
}

async function reorderExam(orderedIds) {
  var res = await fetch('/api/admin/exams/' + currentExam.id + '/questions/reorder', {
    method: 'PUT',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(orderedIds)
  });
  if (res.ok) {
    currentExam = await res.json();
    renderExamBuilder();
    syncExamListEntry();
  } else {
    showToast('Reorder failed.', 'danger');
  }
}

async function saveExam() {
  var name = (document.getElementById('examName').value || '').trim();
  if (!name) { showToast('Exam name is required.', 'warning'); return; }

  var passMark = parseInt(document.getElementById('examPassMark').value);
  var body = {
    name:             name,
    description:      (document.getElementById('examDesc').value || '').trim(),
    timeLimitMinutes: parseInt(document.getElementById('examTimeLimit').value) || 60,
    totalMarks:       parseInt(document.getElementById('examTotalMarks').value) || 0,
    passMark:         isNaN(passMark) || passMark <= 0 ? null : passMark,
    shuffleQuestions: document.getElementById('examShuffleQ').checked,
    shuffleOptions:   document.getElementById('examShuffleOpt').checked
  };

  var res;
  if (currentExam) {
    res = await fetch('/api/admin/exams/' + currentExam.id, {
      method: 'PUT',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(body)
    });
  } else {
    res = await fetch('/api/admin/exams', {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(body)
    });
  }

  if (res.ok) {
    currentExam = await res.json();
    await loadExamList();
    document.getElementById('examSelect').value = currentExam.id;
    renderExamBuilder();
    renderQuestionPicker();
    showToast('Saved as draft.', 'success');
  } else {
    var d = await safeJson(res);
    showToast(d.message || 'Save failed.', 'danger');
  }
}

function resetForm() {
  document.getElementById('examName').value      = '';
  document.getElementById('examDesc').value      = '';
  document.getElementById('examTimeLimit').value = '60';
  document.getElementById('examTotalMarks').value = '0';
  document.getElementById('examPassMark').value  = '';
  document.getElementById('examShuffleQ').checked  = false;
  document.getElementById('examShuffleOpt').checked = false;
  document.getElementById('totalMarksDisplay').textContent = '0';
  document.getElementById('qCountBadge').textContent = '0';
  var resetSubmit = document.getElementById('btnSubmit');
  resetSubmit.disabled   = true;
  resetSubmit.className  = 'btn btn-success';
  resetSubmit.innerHTML  = '<i class="bi bi-check-circle me-1"></i>' + SUBMIT_LABEL;
  document.getElementById('btnDeleteExam').disabled = true;
  document.getElementById('examQuestionList').innerHTML =
    '<div class="panel-empty"><i class="bi bi-clipboard2-x"></i><p>No questions yet.<br>Use the picker on the left.</p></div>';
}

// ── Style helpers ─────────────────────────────────────────────────────────────
function typeColor(t) {
  var m = {
    MCQ_SINGLE:   'bg-primary',
    MCQ_MULTIPLE: 'bg-info text-dark',
    TRUE_FALSE:   'bg-success',
    SHORT_ANSWER: 'bg-warning text-dark',
    ESSAY:        'bg-secondary',
    CODE:         'bg-dark',
    IMAGE_BASED:  'bg-purple'
  };
  return m[t] || 'bg-secondary';
}
function complexityColor(c) {
  if (c === 'FOUNDATION')  return 'bg-success';
  if (c === 'INTERMEDIATE') return 'bg-warning text-dark';
  return 'bg-danger';
}
