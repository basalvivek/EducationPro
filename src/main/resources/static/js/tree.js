/* ── Auth helpers ── */
function getToken() {
  return sessionStorage.getItem('edu_token') || localStorage.getItem('ep_token') || '';
}

function safeAuthHeaders(extra) {
  const token = getToken();
  const headers = { ...extra };
  if (token) headers['Authorization'] = 'Bearer ' + token;
  return headers;
}

async function safeJson(res) {
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  return { message: 'Server error (' + res.status + ')' };
}

/* ── State ── */
let treeData   = [];
let selectedId = null;

/* ── API ── */
async function loadTree() {
  const expandedIds = new Set(treeData.filter(n => n._expanded).map(n => n.id));
  const res = await fetch('/api/admin/course-nodes', {
    headers: safeAuthHeaders()
  });
  if (!res.ok) { showToast('Failed to load tree.', 'danger'); return; }
  treeData = await res.json();
  treeData.forEach(n => { if (expandedIds.has(n.id)) n._expanded = true; });
  refreshTree();
}

/* ── Build nested structure ── */
function buildTree(flat) {
  const map = {};
  flat.forEach(n => { map[n.id] = { ...n, children: [], expanded: n._expanded ?? false }; });
  const roots = [];
  flat.forEach(n => {
    if (n.parentId) {
      if (map[n.parentId]) map[n.parentId].children.push(map[n.id]);
    } else {
      roots.push(map[n.id]);
    }
  });
  return roots;
}

/* ── Render ── */
function refreshTree() {
  const nested = buildTree(treeData);
  const container = document.getElementById('courseTree');
  container.innerHTML = '';
  nested.forEach(node => container.appendChild(renderNode(node)));
}

function renderNode(node) {
  const isQuestion = node.type === 'QUESTION';

  const wrapper = document.createElement('div');
  wrapper.className = 'tree-node-wrapper';
  wrapper.dataset.id = node.id;

  const row = document.createElement('div');
  row.className = 'tree-node d-flex align-items-center px-2 py-1'
    + (node.id === selectedId ? ' tree-node--selected' : '');
  row.setAttribute('role', 'treeitem');
  row.setAttribute('aria-selected', node.id === selectedId ? 'true' : 'false');
  row.setAttribute('aria-expanded', node.expanded ? 'true' : 'false');

  const arrow = document.createElement('i');
  arrow.className = 'bi tree-arrow me-1'
    + (isQuestion ? ' invisible' : (node.expanded ? ' bi-chevron-down' : ' bi-chevron-right'));
  arrow.setAttribute('aria-hidden', 'true');
  arrow.addEventListener('click', e => { e.stopPropagation(); toggleExpand(node.id); });

  const icon = document.createElement('i');
  icon.className = 'bi ' + (isQuestion
    ? 'bi-patch-question-fill text-warning'
    : 'bi-folder2 text-primary') + ' me-2';
  icon.setAttribute('aria-hidden', 'true');

  const label = document.createElement('span');
  label.className = 'tree-label flex-grow-1 text-truncate';
  label.textContent = node.title;

  row.append(arrow, icon, label);
  row.addEventListener('click', () => selectNode(node));
  row.addEventListener('contextmenu', e => { e.preventDefault(); showContextMenu(e, node); });

  wrapper.appendChild(row);

  const childContainer = document.createElement('div');
  childContainer.className = 'tree-children ps-3' + (node.expanded ? '' : ' d-none');
  childContainer.setAttribute('role', 'group');
  if (node.children && node.children.length > 0) {
    node.children.forEach(child => childContainer.appendChild(renderNode(child)));
  }
  wrapper.appendChild(childContainer);
  return wrapper;
}

function toggleExpand(id) {
  const node = treeData.find(n => n.id === id);
  if (node) node._expanded = !node._expanded;
  const nested = buildTree(treeData);
  patchExpanded(nested, id);
  const container = document.getElementById('courseTree');
  container.innerHTML = '';
  nested.forEach(n => container.appendChild(renderNode(n)));
}

function patchExpanded(nodes, id) {
  nodes.forEach(n => {
    const flat = treeData.find(f => f.id === n.id);
    n.expanded = flat?._expanded ?? false;
    if (n.children) patchExpanded(n.children, id);
  });
}

function selectNode(node) {
  selectedId = node.id;
  refreshTree();
  openDetailPanel(node);
}

/* ── Context Menu ── */
let ctxTargetNode = null;

function showContextMenu(event, node) {
  ctxTargetNode = node;
  const menu = document.getElementById('treeContextMenu');
  const isQuestion = node.type === 'QUESTION';
  document.getElementById('ctxAddNode').parentElement.style.display     = isQuestion ? 'none' : 'block';
  document.getElementById('ctxAddQuestion').parentElement.style.display = isQuestion ? 'none' : 'block';
  menu.style.top     = event.clientY + 'px';
  menu.style.left    = event.clientX + 'px';
  menu.style.display = 'block';
}

document.addEventListener('click', () => {
  const menu = document.getElementById('treeContextMenu');
  if (menu) menu.style.display = 'none';
});

document.getElementById('ctxAddNode').addEventListener('click', e => {
  e.preventDefault();
  openAddNodeModal(ctxTargetNode, 'NODE');
});

document.getElementById('ctxAddQuestion').addEventListener('click', e => {
  e.preventDefault();
  openAddQuestionModal(ctxTargetNode);
});

/* ── Add Node Modal ── */
function openAddNodeModal(parentNode, type) {
  document.getElementById('newNodeType').value     = type;
  document.getElementById('newNodeParentId').value = parentNode ? parentNode.id : '';
  document.getElementById('addNodeModalTitle').textContent =
    type === 'QUESTION' ? 'Add Question' : 'Add Node';
  document.getElementById('newNodeTitle').value   = '';
  document.getElementById('newNodeDesc').value    = '';
  document.getElementById('newNodeTagLine').value = '';
  new bootstrap.Modal(document.getElementById('addNodeModal')).show();
}

document.getElementById('addRootBtn').addEventListener('click', () => {
  document.getElementById('newNodeType').value     = 'NODE';
  document.getElementById('newNodeParentId').value = '';
  document.getElementById('addNodeModalTitle').textContent = 'Add Course';
  document.getElementById('newNodeTitle').value   = '';
  document.getElementById('newNodeDesc').value    = '';
  document.getElementById('newNodeTagLine').value = '';
  new bootstrap.Modal(document.getElementById('addNodeModal')).show();
});

document.getElementById('saveNewNodeBtn').addEventListener('click', async () => {
  const title    = document.getElementById('newNodeTitle').value.trim();
  const desc     = document.getElementById('newNodeDesc').value.trim();
  const tagline  = document.getElementById('newNodeTagLine').value.trim();
  const type     = document.getElementById('newNodeType').value;
  const parentId = document.getElementById('newNodeParentId').value;

  if (!title) { showToast('Title is required.', 'warning'); return; }

  const body = { title, description: desc, tagline, type };
  if (parentId) body.parentId = parseInt(parentId);

  const res = await fetch('/api/admin/course-nodes', {
    method: 'POST',
    headers: safeAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  });

  if (res.ok) {
    bootstrap.Modal.getInstance(document.getElementById('addNodeModal')).hide();
    const created = await res.json();
    if (created.parentId) {
      const parent = treeData.find(n => n.id === created.parentId);
      if (parent) parent._expanded = true;
    }
    await loadTree();
    const newNode = treeData.find(n => n.id === created.id);
    if (newNode) selectNode(newNode);
    showToast('Node added.', 'success');
  } else {
    const data = await safeJson(res);
    showToast(data.message || 'Failed to add node.', 'danger');
  }
});

/* ── Add Question Modal ── */
function openAddQuestionModal(parentNode) {
  document.getElementById('qParentNodeId').value   = parentNode.id;
  document.getElementById('aqType').value          = '';
  document.getElementById('aqText').value          = '';
  document.getElementById('aqComplexityF').checked = true;
  document.getElementById('qTypeFields').innerHTML =
    'Select a question type above to configure its options.';
  // Clear right panel — filling a new question, not viewing an existing node
  selectedId = null;
  document.getElementById('detailPlaceholder').classList.remove('d-none');
  document.getElementById('detailForm').classList.add('d-none');
  refreshTree();
  new bootstrap.Modal(document.getElementById('addQuestionModal')).show();
}

document.getElementById('aqType').addEventListener('change', function () {
  renderQuestionTypeFields(this.value);
});

function renderQuestionTypeFields(type) {
  const el = document.getElementById('qTypeFields');
  switch (type) {
    case 'MCQ_SINGLE':
      el.innerHTML = tmplMcqSingle();
      attachOptionListeners('aqOptions', 'addOptionBtn');
      break;
    case 'MCQ_MULTIPLE':
      el.innerHTML = tmplMcqMultiple();
      attachOptionListeners('aqOptions', 'addOptionBtn');
      break;
    case 'TRUE_FALSE':
      el.innerHTML = tmplTrueFalse();
      break;
    case 'SHORT_ANSWER':
      el.innerHTML = tmplShortAnswer();
      break;
    case 'ESSAY':
      el.innerHTML = tmplEssay();
      break;
    case 'CODE':
      el.innerHTML = tmplCode();
      break;
    case 'IMAGE_BASED':
      el.innerHTML = tmplImageBased();
      attachImageListeners();
      break;
    default:
      el.innerHTML = 'Select a question type above to configure its options.';
  }
}

function tmplMcqSingle() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold">Answer Options <span class="text-danger">*</span></label>
      <div id="aqOptions" class="d-flex flex-column gap-2">
        ${[0,1,2,3].map(i => `
          <div class="d-flex align-items-center gap-2 option-row" data-idx="${i}">
            <input type="radio" name="mcqCorrect" class="form-check-input mt-0" value="${i}"
                   ${i===0?'checked':''} aria-label="Mark option ${i+1} correct"/>
            <input type="text" class="form-control form-control-sm" placeholder="Option ${i+1}" id="aqOpt${i}"/>
            <button type="button" class="btn btn-sm btn-outline-danger rm-opt-btn"
                    aria-label="Remove option ${i+1}" ${i<2?'disabled':''}>
              <i class="bi bi-x" aria-hidden="true"></i>
            </button>
          </div>`).join('')}
      </div>
      <button type="button" class="btn btn-sm btn-outline-secondary mt-2" id="addOptionBtn">
        <i class="bi bi-plus me-1" aria-hidden="true"></i>Add Option
      </button>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
      <input type="number" class="form-control" id="aqMarks" value="1"
             min="1" max="100" style="max-width:120px;"/>
    </div>`;
}

function tmplMcqMultiple() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold">Answer Options <span class="text-danger">*</span></label>
      <div class="form-text mb-2">Check all correct answers.</div>
      <div id="aqOptions" class="d-flex flex-column gap-2">
        ${[0,1,2,3].map(i => `
          <div class="d-flex align-items-center gap-2 option-row" data-idx="${i}">
            <input type="checkbox" class="form-check-input mt-0" id="aqChk${i}" value="${i}"
                   aria-label="Mark option ${i+1} correct"/>
            <input type="text" class="form-control form-control-sm" placeholder="Option ${i+1}" id="aqOpt${i}"/>
            <button type="button" class="btn btn-sm btn-outline-danger rm-opt-btn"
                    aria-label="Remove option ${i+1}" ${i<2?'disabled':''}>
              <i class="bi bi-x" aria-hidden="true"></i>
            </button>
          </div>`).join('')}
      </div>
      <button type="button" class="btn btn-sm btn-outline-secondary mt-2" id="addOptionBtn">
        <i class="bi bi-plus me-1" aria-hidden="true"></i>Add Option
      </button>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold">Partial Marking</label>
      <select class="form-select" id="aqPartialMarking" style="max-width:220px;">
        <option value="FULL_ONLY">Full mark only (all correct)</option>
        <option value="PER_OPTION">Per correct option</option>
      </select>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
      <input type="number" class="form-control" id="aqMarks" value="1"
             min="1" max="100" style="max-width:120px;"/>
    </div>`;
}

function tmplTrueFalse() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold d-block">Correct Answer <span class="text-danger">*</span></label>
      <div class="d-flex gap-3">
        <div class="form-check">
          <input class="form-check-input" type="radio" name="aqTFAnswer" id="aqTFTrue" value="TRUE" checked/>
          <label class="form-check-label" for="aqTFTrue">True</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="radio" name="aqTFAnswer" id="aqTFFalse" value="FALSE"/>
          <label class="form-check-label" for="aqTFFalse">False</label>
        </div>
      </div>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
      <input type="number" class="form-control" id="aqMarks" value="1"
             min="1" max="100" style="max-width:120px;"/>
    </div>`;
}

function tmplShortAnswer() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqModelAnswer">Model Answer</label>
      <textarea class="form-control" id="aqModelAnswer" rows="3"
                placeholder="Expected short answer…"></textarea>
    </div>
    <div class="mb-3 d-flex align-items-center gap-4">
      <div>
        <label class="form-label fw-semibold" for="aqWordLimit">Word Limit</label>
        <input type="number" class="form-control" id="aqWordLimit" value="50"
               min="0" max="1000" style="max-width:120px;"/>
      </div>
      <div>
        <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
        <input type="number" class="form-control" id="aqMarks" value="1"
               min="1" max="100" style="max-width:120px;"/>
      </div>
    </div>`;
}

function tmplEssay() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqModelAnswer">Model Answer / Key Points</label>
      <textarea class="form-control" id="aqModelAnswer" rows="4"
                placeholder="Key points expected in a strong answer…"></textarea>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqMarkingScheme">Marking Scheme</label>
      <textarea class="form-control" id="aqMarkingScheme" rows="3"
                placeholder="How marks are allocated across criteria…"></textarea>
    </div>
    <div class="mb-3 d-flex align-items-center gap-4">
      <div>
        <label class="form-label fw-semibold" for="aqWordLimit">Word Limit</label>
        <input type="number" class="form-control" id="aqWordLimit" value="500"
               min="0" max="5000" style="max-width:120px;"/>
      </div>
      <div>
        <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
        <input type="number" class="form-control" id="aqMarks" value="1"
               min="1" max="100" style="max-width:120px;"/>
      </div>
    </div>`;
}

function tmplCode() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqCodeLang">Programming Language</label>
      <select class="form-select" id="aqCodeLang" style="max-width:200px;">
        <option value="PYTHON">Python</option>
        <option value="JAVA">Java</option>
        <option value="JAVASCRIPT">JavaScript</option>
        <option value="SQL">SQL</option>
        <option value="HTML">HTML</option>
        <option value="CSS">CSS</option>
        <option value="OTHER">Other</option>
      </select>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqStarterCode">Starter Code</label>
      <textarea class="form-control font-monospace" id="aqStarterCode" rows="4"
                placeholder="Skeleton code provided to student…"
                style="font-size:0.8125rem;"></textarea>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold" for="aqExpectedOutput">Expected Output</label>
      <textarea class="form-control font-monospace" id="aqExpectedOutput" rows="3"
                placeholder="Expected program output for auto-grading…"
                style="font-size:0.8125rem;"></textarea>
    </div>
    <div class="mb-0">
      <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
      <input type="number" class="form-control" id="aqMarks" value="1"
             min="1" max="100" style="max-width:120px;"/>
    </div>`;
}

function tmplImageBased() {
  return `
    <div class="mb-3">
      <label class="form-label fw-semibold d-block">
        Answer Format <span class="text-danger">*</span>
      </label>
      <div class="d-flex gap-3">
        <div class="form-check">
          <input class="form-check-input" type="radio" name="aqImageAnswerType"
                 id="aqImgWritten" value="WRITTEN" checked/>
          <label class="form-check-label" for="aqImgWritten">Written</label>
        </div>
        <div class="form-check">
          <input class="form-check-input" type="radio" name="aqImageAnswerType"
                 id="aqImgMcq" value="MCQ"/>
          <label class="form-check-label" for="aqImgMcq">MCQ (4+ images)</label>
        </div>
      </div>
    </div>

    <!-- WRITTEN: single image upload -->
    <div id="imgWrittenPanel" class="mb-3">
      <label class="form-label fw-semibold">Question Image <span class="text-danger">*</span></label>
      ${imgUploadSlot('imgW0', 'Image alt text…')}
    </div>

    <!-- MCQ: 4+ image slots -->
    <div id="imgMcqPanel" class="mb-3" style="display:none;">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <label class="form-label fw-semibold mb-0">Option Images <span class="text-danger">*</span>
          <small class="text-muted fw-normal">(min 4)</small>
        </label>
        <button type="button" class="btn btn-sm btn-outline-secondary" id="addImgSlotBtn">
          + Add Image
        </button>
      </div>
      <div id="imgMcqSlots">
        ${imgUploadSlot('imgM0', 'Option A alt…', 0, true)}
        ${imgUploadSlot('imgM1', 'Option B alt…', 1, true)}
        ${imgUploadSlot('imgM2', 'Option C alt…', 2, true)}
        ${imgUploadSlot('imgM3', 'Option D alt…', 3, true)}
      </div>
    </div>

    <div class="mb-0">
      <label class="form-label fw-semibold">Marks <span class="text-danger">*</span></label>
      <input type="number" class="form-control" id="aqMarks" value="1"
             min="1" max="100" style="max-width:120px;"/>
    </div>`;
}

function imgUploadSlot(id, altPlaceholder, idx, removable) {
  const rmBtn = removable
    ? `<button type="button" class="btn btn-sm btn-outline-danger rm-img-btn ms-1"
               data-idx="${idx}" title="Remove">✕</button>`
    : '';
  return `
    <div class="img-slot border rounded p-2 mb-2" data-idx="${idx || 0}" id="slot_${id}">
      <div class="d-flex align-items-start gap-2">
        <div class="flex-grow-1">
          <input type="file" class="form-control form-control-sm mb-1 img-file-input"
                 id="${id}_file" accept="image/*" data-slot="${id}"/>
          <input type="text" class="form-control form-control-sm img-alt-input"
                 id="${id}_alt" placeholder="${altPlaceholder}"/>
          <input type="hidden" class="img-path-input" id="${id}_path" value=""/>
        </div>
        <div>
          <img id="${id}_preview" src="" alt="preview"
               style="width:64px;height:64px;object-fit:cover;border-radius:4px;display:none;border:1px solid #dee2e6;"/>
        </div>
        ${rmBtn}
      </div>
      <div class="img-upload-status small mt-1" id="${id}_status"></div>
    </div>`;
}

function attachImageListeners() {
  const writtenRadio = document.getElementById('aqImgWritten');
  const mcqRadio     = document.getElementById('aqImgMcq');
  const writtenPanel = document.getElementById('imgWrittenPanel');
  const mcqPanel     = document.getElementById('imgMcqPanel');

  function switchPanel() {
    const isMcq = document.getElementById('aqImgMcq')?.checked;
    if (writtenPanel) writtenPanel.style.display = isMcq ? 'none' : '';
    if (mcqPanel)     mcqPanel.style.display     = isMcq ? ''     : 'none';
  }
  // use click (more reliable than change inside Bootstrap modals)
  if (writtenRadio) writtenRadio.addEventListener('click', switchPanel);
  if (mcqRadio)     mcqRadio.addEventListener('click', switchPanel);
  switchPanel(); // apply initial state immediately

  // Add image slot button
  const addBtn = document.getElementById('addImgSlotBtn');
  if (addBtn) {
    addBtn.addEventListener('click', () => {
      const slots   = document.getElementById('imgMcqSlots');
      const idx     = slots.querySelectorAll('.img-slot').length;
      const id      = 'imgM' + idx;
      const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      const label   = 'Option ' + (letters[idx] || (idx + 1)) + ' alt…';
      const div     = document.createElement('div');
      div.innerHTML = imgUploadSlot(id, label, idx, true);
      slots.appendChild(div.firstElementChild);
      wireFileInput(id + '_file');
      wireRemoveBtn(slots);
    });
  }

  // Wire remove buttons in MCQ panel
  wireRemoveBtn(document.getElementById('imgMcqSlots'));

  // Wire all existing file inputs
  document.querySelectorAll('.img-file-input').forEach(inp => wireFileInput(inp.id));
}

function wireFileInput(inputId) {
  const inp = document.getElementById(inputId);
  if (!inp) return;
  inp.addEventListener('change', async () => {
    const file = inp.files[0];
    if (!file) return;
    const slotId = inp.dataset.slot;
    const status  = document.getElementById(slotId + '_status');
    const preview = document.getElementById(slotId + '_preview');
    const pathInp = document.getElementById(slotId + '_path');

    status.textContent = 'Uploading…';
    status.className   = 'img-upload-status small mt-1 text-muted';

    // Local preview immediately
    const reader = new FileReader();
    reader.onload = e => {
      preview.src     = e.target.result;
      preview.style.display = '';
    };
    reader.readAsDataURL(file);

    // Upload to server
    const fd = new FormData();
    fd.append('file', file);
    try {
      const token = getToken();
      const headers = {};
      if (token) headers['Authorization'] = 'Bearer ' + token;
      const res  = await fetch('/api/admin/upload', { method: 'POST', headers, body: fd });
      const data = await safeJson(res);
      if (res.ok && data.path) {
        pathInp.value      = data.path;
        preview.src        = data.path;
        status.textContent = '✓ Uploaded';
        status.className   = 'img-upload-status small mt-1 text-success';
      } else {
        status.textContent = '✗ ' + (data.error || 'Upload failed');
        status.className   = 'img-upload-status small mt-1 text-danger';
      }
    } catch (err) {
      status.textContent = '✗ Network error';
      status.className   = 'img-upload-status small mt-1 text-danger';
    }
  });
}

function wireRemoveBtn(container) {
  if (!container) return;
  container.addEventListener('click', e => {
    const btn = e.target.closest('.rm-img-btn');
    if (!btn) return;
    const slots = container.querySelectorAll('.img-slot');
    if (slots.length <= 4) { showToast('Minimum 4 images required.', 'warning'); return; }
    btn.closest('.img-slot').remove();
  });
}

function attachOptionListeners(containerId, addBtnId) {
  const container = document.getElementById(containerId);
  const addBtn    = document.getElementById(addBtnId);

  container.addEventListener('click', e => {
    const rmBtn = e.target.closest('.rm-opt-btn');
    if (!rmBtn) return;
    const rows = container.querySelectorAll('.option-row');
    if (rows.length <= 2) return;
    rmBtn.closest('.option-row').remove();
    container.querySelectorAll('.option-row').forEach((row, i) => {
      row.dataset.idx = i;
      const radio = row.querySelector('input[type=radio]');
      const chk   = row.querySelector('input[type=checkbox]');
      const txt   = row.querySelector('input[type=text]');
      const rm    = row.querySelector('.rm-opt-btn');
      if (radio) radio.value = i;
      if (chk)   { chk.value = i; chk.id = 'aqChk' + i; }
      if (txt)   { txt.id = 'aqOpt' + i; txt.placeholder = 'Option ' + (i + 1); }
      rm.disabled = container.querySelectorAll('.option-row').length <= 2;
    });
  });

  addBtn.addEventListener('click', () => {
    const rows   = container.querySelectorAll('.option-row');
    if (rows.length >= 6) { showToast('Maximum 6 options.', 'warning'); return; }
    const i      = rows.length;
    const isMulti = container.querySelector('input[type=checkbox]') !== null;
    const row    = document.createElement('div');
    row.className = 'd-flex align-items-center gap-2 option-row';
    row.dataset.idx = i;
    row.innerHTML = `
      ${isMulti
        ? `<input type="checkbox" class="form-check-input mt-0" id="aqChk${i}" value="${i}" aria-label="Mark option ${i+1} correct"/>`
        : `<input type="radio" name="mcqCorrect" class="form-check-input mt-0" value="${i}" aria-label="Mark option ${i+1} correct"/>`}
      <input type="text" class="form-control form-control-sm" placeholder="Option ${i+1}" id="aqOpt${i}"/>
      <button type="button" class="btn btn-sm btn-outline-danger rm-opt-btn" aria-label="Remove option ${i+1}">
        <i class="bi bi-x" aria-hidden="true"></i>
      </button>`;
    container.appendChild(row);
    container.querySelectorAll('.rm-opt-btn').forEach(btn => { btn.disabled = false; });
  });
}

function collectTypeData(type) {
  const data = {};
  const marks = document.getElementById('aqMarks');
  if (marks) data.marks = parseInt(marks.value) || 1;

  switch (type) {
    case 'MCQ_SINGLE': {
      const rows    = document.querySelectorAll('#aqOptions .option-row');
      const opts    = Array.from(rows).map(r => r.querySelector('input[type=text]')?.value.trim() || '');
      const checked = document.querySelector('input[name=mcqCorrect]:checked');
      data.options      = JSON.stringify(opts);
      data.correctIndex = checked ? parseInt(checked.value) : 0;
      break;
    }
    case 'MCQ_MULTIPLE': {
      const rows        = document.querySelectorAll('#aqOptions .option-row');
      const opts        = Array.from(rows).map(r => r.querySelector('input[type=text]')?.value.trim() || '');
      const correctIdxs = Array.from(rows)
        .filter(r => r.querySelector('input[type=checkbox]')?.checked)
        .map(r => parseInt(r.dataset.idx));
      const pm = document.getElementById('aqPartialMarking');
      data.options        = JSON.stringify(opts);
      data.correctIndices = JSON.stringify(correctIdxs);
      data.partialMarking = pm ? pm.value : 'FULL_ONLY';
      break;
    }
    case 'TRUE_FALSE': {
      const checked      = document.querySelector('input[name=aqTFAnswer]:checked');
      data.correctAnswer = checked ? checked.value : 'TRUE';
      break;
    }
    case 'SHORT_ANSWER': {
      const ma = document.getElementById('aqModelAnswer');
      const wl = document.getElementById('aqWordLimit');
      data.modelAnswer = ma ? ma.value.trim() : '';
      data.wordLimit   = wl ? (parseInt(wl.value) || 0) : 0;
      break;
    }
    case 'ESSAY': {
      const ma = document.getElementById('aqModelAnswer');
      const ms = document.getElementById('aqMarkingScheme');
      const wl = document.getElementById('aqWordLimit');
      data.modelAnswer   = ma ? ma.value.trim() : '';
      data.markingScheme = ms ? ms.value.trim() : '';
      data.wordLimit     = wl ? (parseInt(wl.value) || 0) : 0;
      break;
    }
    case 'CODE': {
      const lang = document.getElementById('aqCodeLang');
      const sc   = document.getElementById('aqStarterCode');
      const eo   = document.getElementById('aqExpectedOutput');
      data.codeLanguage   = lang ? lang.value : 'PYTHON';
      data.starterCode    = sc   ? sc.value  : '';
      data.expectedOutput = eo   ? eo.value  : '';
      break;
    }
    case 'IMAGE_BASED': {
      const iat = document.querySelector('input[name=aqImageAnswerType]:checked');
      const fmt = iat ? iat.value : 'WRITTEN';
      data.imageAnswerType = fmt;

      if (fmt === 'WRITTEN') {
        const pathInp = document.getElementById('imgW0_path');
        const altInp  = document.getElementById('imgW0_alt');
        data.imagePath = pathInp ? pathInp.value.trim() : '';
        data.imageAlt  = altInp  ? altInp.value.trim()  : '';
      } else {
        // MCQ: collect all slots
        const slots  = document.querySelectorAll('#imgMcqSlots .img-slot');
        const paths  = Array.from(slots).map(s => (s.querySelector('.img-path-input')?.value || '').trim());
        const alts   = Array.from(slots).map(s => (s.querySelector('.img-alt-input')?.value  || '').trim());
        data.imagePath = JSON.stringify(paths);
        data.imageAlt  = JSON.stringify(alts);
      }
      break;
    }
  }
  return data;
}

document.getElementById('saveNewQuestionBtn').addEventListener('click', async () => {
  const parentId   = document.getElementById('qParentNodeId').value;
  const type       = document.getElementById('aqType').value;
  const text       = document.getElementById('aqText').value.trim();
  const complexity = document.querySelector('input[name=aqComplexity]:checked')?.value || 'FOUNDATION';

  if (!type) { showToast('Select a question type.', 'warning'); return; }
  if (!text) { showToast('Question text is required.', 'warning'); return; }

  const typeData = collectTypeData(type);

  const body = {
    parentId:     parseInt(parentId),
    type:         'QUESTION',
    title:        text.length > 100 ? text.substring(0, 97) + '…' : text,
    questionText: text,
    questionType: type,
    complexity,
    ...typeData
  };

  const res = await fetch('/api/admin/course-nodes', {
    method: 'POST',
    headers: safeAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  });

  if (res.ok) {
    bootstrap.Modal.getInstance(document.getElementById('addQuestionModal')).hide();
    const created = await res.json();
    if (created.parentId) {
      const parent = treeData.find(n => n.id === created.parentId);
      if (parent) parent._expanded = true;
    }
    await loadTree();
    const newNode = treeData.find(n => n.id === created.id);
    if (newNode) selectNode(newNode);
    showToast('Question added.', 'success');
  } else {
    const data = await safeJson(res);
    showToast(data.message || 'Failed to add question.', 'danger');
  }
});

/* ── Detail Panel ── */
function openDetailPanel(node) {
  const isQuestion = node.type === 'QUESTION';
  document.getElementById('detailPlaceholder').classList.add('d-none');
  const form = document.getElementById('detailForm');
  form.classList.remove('d-none');

  const complexityOpts = ['FOUNDATION', 'INTERMEDIATE', 'HIGHER'].map(c => `
    <div class="form-check form-check-inline">
      <input class="form-check-input" type="radio" name="editComplexity"
             id="editComp${c}" value="${c}" ${(node.complexity || 'FOUNDATION') === c ? 'checked' : ''}/>
      <label class="form-check-label" for="editComp${c}">
        ${c.charAt(0) + c.slice(1).toLowerCase()}
      </label>
    </div>`).join('');

  form.innerHTML = `
    <div class="d-flex align-items-center gap-2 mb-3">
      <div style="width:28px;height:28px;border-radius:8px;background:${isQuestion?'#fef9c3':'#e8f0fe'};display:flex;align-items:center;justify-content:center;flex-shrink:0;">
        <i class="bi ${isQuestion?'bi-patch-question-fill':'bi-folder2'}" style="color:${isQuestion?'#b45309':'#0d6efd'};font-size:.8rem;" aria-hidden="true"></i>
      </div>
      <span style="font-size:.875rem;font-weight:700;color:#1a2332;">${isQuestion?'Question':'Node'} Details</span>
      <span class="badge ${isQuestion?'bg-warning text-dark':'bg-primary'} ms-auto" style="font-size:.65rem;">${node.type}</span>
    </div>

    <div class="card-form p-4 mb-3">
      <div class="mb-3">
        <label class="form-label fw-semibold label-sm" for="detailTitle">Title</label>
        <input type="text" class="form-control" id="detailTitle"
               value="${escHtml(node.title)}" maxlength="150"/>
      </div>
      <div class="mb-3">
        <label class="form-label fw-semibold label-sm" for="detailDesc">Description</label>
        <textarea class="form-control" id="detailDesc" rows="2"
                  maxlength="500">${escHtml(node.description || '')}</textarea>
      </div>
      ${!isQuestion ? `
      <div class="mb-3">
        <label class="form-label fw-semibold label-sm" for="detailTagline">Tagline</label>
        <input type="text" class="form-control" id="detailTagline"
               value="${escHtml(node.tagline || '')}" maxlength="100"/>
      </div>` : ''}
      ${isQuestion ? renderQuestionFields(node, complexityOpts) : ''}
    </div>

    <div class="d-flex gap-2">
      <button class="btn btn-primary fw-semibold px-4" onclick="saveNode(${node.id})">
        <i class="bi bi-save me-1" aria-hidden="true"></i>Save changes
      </button>
      <button class="btn btn-outline-danger fw-semibold px-4" onclick="confirmDeleteNode(${node.id})">
        <i class="bi bi-trash me-1" aria-hidden="true"></i>Delete
      </button>
    </div>
  `;

  if (isQuestion) {
    wireDetailTypeListeners(node);
    // Re-render type fields when type dropdown changes
    document.getElementById('questionType')?.addEventListener('change', function() {
      const phantom = { ...node, questionType: this.value };
      document.getElementById('detailTypeFields').innerHTML = renderDetailTypeFields(phantom, this.value);
      wireDetailTypeListeners(phantom);
    });
  }
}

function wireDetailTypeListeners(node) {
  const qt = node?.questionType;

  // IMAGE_BASED answer format switch in detail panel
  const detailWrittenRadio = document.getElementById('detailImgWritten');
  const detailMcqRadio     = document.getElementById('detailImgMcq');
  if (detailWrittenRadio && detailMcqRadio) {
    function switchDetailImgPanel() {
      const isMcq = document.getElementById('detailImgMcq')?.checked;
      const wp    = document.getElementById('detailImgWrittenPanel');
      const mp    = document.getElementById('detailImgMcqPanel');
      if (wp) wp.style.display = isMcq ? 'none' : '';
      if (mp) mp.style.display = isMcq ? ''     : 'none';
    }
    detailWrittenRadio.addEventListener('click', switchDetailImgPanel);
    detailMcqRadio.addEventListener('click', switchDetailImgPanel);
    switchDetailImgPanel(); // apply initial state
  }

  // MCQ add/remove option buttons
  const optsContainer = document.getElementById('detailMcqOpts');
  const addOptBtn     = document.getElementById('addDetailOptBtn');
  if (optsContainer) {
    optsContainer.addEventListener('click', e => {
      const btn = e.target.closest('.rm-detail-opt');
      if (!btn) return;
      const rows = optsContainer.querySelectorAll('.detail-opt-row');
      if (rows.length <= 2) return;
      btn.closest('.detail-opt-row').remove();
      optsContainer.querySelectorAll('.rm-detail-opt').forEach(b => {
        b.disabled = optsContainer.querySelectorAll('.detail-opt-row').length <= 2;
      });
    });
  }
  if (addOptBtn) {
    addOptBtn.addEventListener('click', () => {
      const rows = optsContainer.querySelectorAll('.detail-opt-row');
      if (rows.length >= 6) return;
      const i   = rows.length;
      const div = document.createElement('div');
      const isMulti = qt === 'MCQ_MULTIPLE';
      div.innerHTML = `<div class="input-group mb-1 detail-opt-row">
        <span class="input-group-text">
          ${isMulti
            ? `<input type="checkbox" id="detailChk${i}" value="${i}" aria-label="Mark option ${i+1} correct"/>`
            : `<input type="radio" name="detailCorrect" value="${i}" aria-label="Mark option ${i+1} correct"/>`}
        </span>
        <input type="text" class="form-control" id="detailOpt${i}" placeholder="Option ${i+1}"/>
        <button type="button" class="btn btn-outline-danger rm-detail-opt" aria-label="Remove">✕</button>
      </div>`;
      optsContainer.appendChild(div.firstElementChild);
      optsContainer.querySelectorAll('.rm-detail-opt').forEach(b => { b.disabled = false; });
    });
  }

  // WRITTEN image upload
  const detailImgFile = document.getElementById('detailImgFile');
  if (detailImgFile) {
    detailImgFile.addEventListener('change', async () => {
      const file = detailImgFile.files[0];
      if (!file) return;
      const status = document.getElementById('detailImgStatus');
      if (status) { status.textContent = 'Uploading…'; status.className = 'text-muted small'; }
      const fd = new FormData();
      fd.append('file', file);
      const token = getToken();
      const headers = token ? { 'Authorization': 'Bearer ' + token } : {};
      try {
        const res  = await fetch('/api/admin/upload', { method: 'POST', headers, body: fd });
        const data = await safeJson(res);
        if (res.ok && data.path) {
          document.getElementById('detailImgPath').value = data.path;
          // Refresh preview
          const existing = detailImgFile.closest('.mb-3').querySelector('img');
          if (existing) { existing.src = data.path; }
          else {
            const preview = document.createElement('img');
            preview.src = data.path;
            preview.style.cssText = 'max-width:200px;max-height:150px;object-fit:contain;border:1px solid #dee2e6;border-radius:4px;display:block;';
            preview.className = 'mb-2';
            detailImgFile.closest('.mb-3').insertBefore(preview, detailImgFile.closest('.d-flex'));
          }
          if (status) { status.textContent = '✓ Uploaded'; status.className = 'text-success small'; }
        } else {
          if (status) { status.textContent = '✗ ' + (data.error || 'Upload failed'); status.className = 'text-danger small'; }
        }
      } catch { if (status) { status.textContent = '✗ Network error'; status.className = 'text-danger small'; } }
    });
  }

  // MCQ image uploads
  document.querySelectorAll('.detail-mcq-img-file').forEach(inp => {
    inp.addEventListener('change', async () => {
      const file = inp.files[0];
      if (!file) return;
      const fd = new FormData();
      fd.append('file', file);
      const token = getToken();
      const headers = token ? { 'Authorization': 'Bearer ' + token } : {};
      try {
        const res  = await fetch('/api/admin/upload', { method: 'POST', headers, body: fd });
        const data = await safeJson(res);
        if (res.ok && data.path) {
          const row = inp.closest('.d-flex');
          row.querySelector('.detail-mcq-img-path').value = data.path;
          let img = row.querySelector('img');
          if (img) { img.src = data.path; }
          else {
            img = document.createElement('img');
            img.style.cssText = 'width:64px;height:64px;object-fit:cover;border:1px solid #dee2e6;border-radius:4px;';
            row.insertBefore(img, row.querySelector('.flex-grow-1'));
            img.src = data.path;
          }
        }
      } catch { /* silent */ }
    });
  });
}

function tryParseJson(str, fallback) {
  try { return JSON.parse(str); } catch (e) { return fallback; }
}

function renderQuestionFields(node, complexityOpts) {
  const typeMap = {
    MCQ_SINGLE:   'Multiple Choice — Single Answer',
    MCQ_MULTIPLE: 'Multiple Choice — Multiple Answers',
    TRUE_FALSE:   'True / False',
    SHORT_ANSWER: 'Short Answer',
    ESSAY:        'Essay',
    CODE:         'Code',
    IMAGE_BASED:  'Image Based'
  };
  const typeOptions = Object.entries(typeMap).map(([v, l]) =>
    `<option value="${v}" ${node.questionType === v ? 'selected' : ''}>${escHtml(l)}</option>`
  ).join('');

  return `
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm">Complexity</label>
      <div>${complexityOpts}</div>
    </div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm" for="questionText">Question Text</label>
      <textarea class="form-control" id="questionText" rows="3"
                maxlength="2000">${escHtml(node.questionText || '')}</textarea>
    </div>
    <div class="row g-3 mb-3">
      <div class="col-md-6">
        <label class="form-label fw-semibold label-sm" for="questionType">Question Type</label>
        <select class="form-select" id="questionType">${typeOptions}</select>
      </div>
      <div class="col-md-3">
        <label class="form-label fw-semibold label-sm" for="questionMarks">Marks</label>
        <input type="number" class="form-control" id="questionMarks"
               value="${node.marks || 1}" min="1" max="100"/>
      </div>
    </div>
    <div id="detailTypeFields">${renderDetailTypeFields(node, node.questionType)}</div>
    <div class="mb-3">
      <label class="form-label fw-semibold label-sm" for="questionExplanation">Explanation</label>
      <textarea class="form-control" id="questionExplanation" rows="2"
                placeholder="Shown after student answers…">${escHtml(node.explanation || '')}</textarea>
    </div>`;
}

function renderDetailTypeFields(node, qt) {
  if (!qt) return '';
  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

  switch (qt) {
    case 'MCQ_SINGLE': {
      const opts = tryParseJson(node.options, []);
      const ci   = node.correctIndex ?? 0;
      const rows = opts.map((opt, i) => `
        <div class="input-group mb-1 detail-opt-row">
          <span class="input-group-text">
            <input type="radio" name="detailCorrect" value="${i}" ${i === ci ? 'checked' : ''}
                   aria-label="Mark option ${i+1} correct"/>
          </span>
          <input type="text" class="form-control" id="detailOpt${i}"
                 value="${escHtml(opt)}" placeholder="Option ${i+1}"/>
          <button type="button" class="btn btn-outline-danger rm-detail-opt"
                  ${opts.length <= 2 ? 'disabled' : ''} aria-label="Remove">✕</button>
        </div>`).join('');
      return `<div class="mb-3">
        <label class="form-label fw-semibold label-sm">Options — select correct answer</label>
        <div id="detailMcqOpts">${rows}</div>
        <button type="button" class="btn btn-sm btn-outline-secondary mt-1"
                id="addDetailOptBtn">+ Add Option</button>
      </div>`;
    }
    case 'MCQ_MULTIPLE': {
      const opts = tryParseJson(node.options, []);
      const cis  = tryParseJson(node.correctIndices, []);
      const rows = opts.map((opt, i) => `
        <div class="input-group mb-1 detail-opt-row">
          <span class="input-group-text">
            <input type="checkbox" id="detailChk${i}" value="${i}"
                   ${cis.includes(i) ? 'checked' : ''} aria-label="Mark option ${i+1} correct"/>
          </span>
          <input type="text" class="form-control" id="detailOpt${i}"
                 value="${escHtml(opt)}" placeholder="Option ${i+1}"/>
          <button type="button" class="btn btn-outline-danger rm-detail-opt"
                  ${opts.length <= 2 ? 'disabled' : ''} aria-label="Remove">✕</button>
        </div>`).join('');
      return `<div class="mb-3">
        <label class="form-label fw-semibold label-sm">Options — check all correct answers</label>
        <div id="detailMcqOpts">${rows}</div>
        <button type="button" class="btn btn-sm btn-outline-secondary mt-1"
                id="addDetailOptBtn">+ Add Option</button>
      </div>`;
    }
    case 'TRUE_FALSE': {
      const ca = node.correctAnswer || 'TRUE';
      return `<div class="mb-3">
        <label class="form-label fw-semibold label-sm">Correct Answer</label>
        <div class="d-flex gap-3">
          <div class="form-check">
            <input class="form-check-input" type="radio" name="detailTF"
                   id="detailTFTrue" value="TRUE" ${ca === 'TRUE' ? 'checked' : ''}/>
            <label class="form-check-label" for="detailTFTrue">True</label>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="radio" name="detailTF"
                   id="detailTFFalse" value="FALSE" ${ca === 'FALSE' ? 'checked' : ''}/>
            <label class="form-check-label" for="detailTFFalse">False</label>
          </div>
        </div>
      </div>`;
    }
    case 'SHORT_ANSWER':
      return `
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailModelAnswer">Model Answer</label>
          <textarea class="form-control" id="detailModelAnswer" rows="3">${escHtml(node.modelAnswer || '')}</textarea>
        </div>
        <div class="row g-3 mb-3">
          <div class="col-md-8">
            <label class="form-label fw-semibold label-sm" for="detailMarkingScheme">Marking Scheme</label>
            <textarea class="form-control" id="detailMarkingScheme" rows="2">${escHtml(node.markingScheme || '')}</textarea>
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold label-sm" for="detailWordLimit">Word Limit</label>
            <input type="number" class="form-control" id="detailWordLimit"
                   value="${node.wordLimit || 0}" min="0"/>
          </div>
        </div>`;
    case 'ESSAY':
      return `
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailModelAnswer">Model Answer</label>
          <textarea class="form-control" id="detailModelAnswer" rows="4">${escHtml(node.modelAnswer || '')}</textarea>
        </div>
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailMarkingScheme">Marking Scheme</label>
          <textarea class="form-control" id="detailMarkingScheme" rows="3">${escHtml(node.markingScheme || '')}</textarea>
        </div>`;
    case 'CODE': {
      const langs = ['PYTHON','JAVA','JAVASCRIPT','SQL','HTML','CSS','OTHER'];
      const langOpts = langs.map(l =>
        `<option value="${l}" ${node.codeLanguage === l ? 'selected' : ''}>${l}</option>`).join('');
      return `
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailCodeLang">Language</label>
          <select class="form-select" id="detailCodeLang">${langOpts}</select>
        </div>
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailStarterCode">Starter Code</label>
          <textarea class="form-control font-monospace" id="detailStarterCode"
                    rows="4">${escHtml(node.starterCode || '')}</textarea>
        </div>
        <div class="mb-3">
          <label class="form-label fw-semibold label-sm" for="detailExpectedOutput">Expected Output</label>
          <textarea class="form-control font-monospace" id="detailExpectedOutput"
                    rows="3">${escHtml(node.expectedOutput || '')}</textarea>
        </div>`;
    }
    case 'IMAGE_BASED': {
      const fmt  = node.imageAnswerType || 'WRITTEN';
      const path = node.imagePath || '';
      const alt  = node.imageAlt  || '';

      // Written panel
      const preview = path
        ? `<img src="${escHtml(path)}" alt="${escHtml(alt)}"
                style="max-width:200px;max-height:150px;object-fit:contain;
                       border:1px solid #dee2e6;border-radius:4px;display:block;"
                class="mb-2" id="detailImgPreview"/>`
        : '<p class="text-muted small mb-1" id="detailImgPreview">No image uploaded yet.</p>';
      const writtenHtml = `<div id="detailImgWrittenPanel">
        <label class="form-label fw-semibold label-sm">Image</label>
        ${preview}
        <div class="d-flex gap-2 align-items-center mt-1">
          <input type="file" class="form-control form-control-sm" id="detailImgFile"
                 accept="image/*" style="max-width:260px;"/>
          <span class="small" id="detailImgStatus"></span>
        </div>
        <input type="text" class="form-control form-control-sm mt-1" id="detailImgAlt"
               value="${escHtml(alt)}" placeholder="Alt text"/>
        <input type="hidden" id="detailImgPath" value="${escHtml(path)}"/>
      </div>`;

      // MCQ panel
      const paths = tryParseJson(path, []);
      const alts  = tryParseJson(alt, []);
      const minSlots = Math.max(paths.length, 4);
      const slots = Array.from({ length: minSlots }, (_, i) => {
        const p  = paths[i] || '';
        const a  = alts[i]  || '';
        return `<div class="d-flex align-items-center gap-2 mb-2">
          <span class="badge bg-secondary">${letters[i] || (i+1)}</span>
          ${p ? `<img src="${escHtml(p)}" alt="${escHtml(a)}"
                      style="width:64px;height:64px;object-fit:cover;
                             border:1px solid #dee2e6;border-radius:4px;"/>` : ''}
          <div class="flex-grow-1">
            <input type="file" class="form-control form-control-sm mb-1 detail-mcq-img-file"
                   id="detailMcqImg${i}" accept="image/*" data-idx="${i}"/>
            <input type="text" class="form-control form-control-sm detail-mcq-img-alt"
                   value="${escHtml(a)}" placeholder="Alt ${letters[i]||i+1}"/>
            <input type="hidden" class="detail-mcq-img-path" value="${escHtml(p)}"/>
          </div>
        </div>`;
      }).join('');
      const mcqHtml = `<div id="detailImgMcqPanel">
        <label class="form-label fw-semibold label-sm">Option Images</label>
        <div id="detailMcqImgSlots">${slots}</div>
      </div>`;

      return `<div class="mb-3">
        <label class="form-label fw-semibold label-sm d-block">Answer Format</label>
        <div class="d-flex gap-3 mb-3">
          <div class="form-check">
            <input class="form-check-input" type="radio" name="detailImgFmt"
                   id="detailImgWritten" value="WRITTEN" ${fmt === 'WRITTEN' ? 'checked' : ''}/>
            <label class="form-check-label" for="detailImgWritten">Written</label>
          </div>
          <div class="form-check">
            <input class="form-check-input" type="radio" name="detailImgFmt"
                   id="detailImgMcq" value="MCQ" ${fmt === 'MCQ' ? 'checked' : ''}/>
            <label class="form-check-label" for="detailImgMcq">MCQ (4+ images)</label>
          </div>
        </div>
        ${writtenHtml}
        ${mcqHtml}
      </div>`;
    }
    default:
      return '';
  }
}

async function saveNode(id) {
  const body = {
    title:       document.getElementById('detailTitle').value.trim(),
    description: document.getElementById('detailDesc').value.trim(),
    tagline:     document.getElementById('detailTagline')?.value.trim() || ''
  };

  const node = treeData.find(n => n.id === id);
  if (node?.type === 'QUESTION') {
    body.questionText = document.getElementById('questionText').value.trim();
    body.questionType = document.getElementById('questionType').value;
    body.marks        = parseInt(document.getElementById('questionMarks').value) || 1;
    body.explanation  = document.getElementById('questionExplanation')?.value.trim() || '';
    body.complexity   = document.querySelector('input[name=editComplexity]:checked')?.value || 'FOUNDATION';

    const qt = body.questionType;
    if (qt === 'MCQ_SINGLE') {
      const rows = document.querySelectorAll('#detailMcqOpts .detail-opt-row');
      body.options      = JSON.stringify(Array.from(rows).map(r => r.querySelector('input[type=text]')?.value.trim() || ''));
      const checked     = document.querySelector('input[name=detailCorrect]:checked');
      body.correctIndex = checked ? parseInt(checked.value) : 0;
    } else if (qt === 'MCQ_MULTIPLE') {
      const rows         = document.querySelectorAll('#detailMcqOpts .detail-opt-row');
      body.options       = JSON.stringify(Array.from(rows).map(r => r.querySelector('input[type=text]')?.value.trim() || ''));
      const checked      = document.querySelectorAll('#detailMcqOpts input[type=checkbox]:checked');
      body.correctIndices = JSON.stringify(Array.from(checked).map(c => parseInt(c.value)));
    } else if (qt === 'TRUE_FALSE') {
      const tf = document.querySelector('input[name=detailTF]:checked');
      body.correctAnswer = tf ? tf.value : 'TRUE';
    } else if (qt === 'SHORT_ANSWER') {
      body.modelAnswer    = document.getElementById('detailModelAnswer')?.value.trim() || '';
      body.markingScheme  = document.getElementById('detailMarkingScheme')?.value.trim() || '';
      body.wordLimit      = parseInt(document.getElementById('detailWordLimit')?.value) || 0;
    } else if (qt === 'ESSAY') {
      body.modelAnswer   = document.getElementById('detailModelAnswer')?.value.trim() || '';
      body.markingScheme = document.getElementById('detailMarkingScheme')?.value.trim() || '';
    } else if (qt === 'CODE') {
      body.codeLanguage   = document.getElementById('detailCodeLang')?.value || '';
      body.starterCode    = document.getElementById('detailStarterCode')?.value.trim() || '';
      body.expectedOutput = document.getElementById('detailExpectedOutput')?.value.trim() || '';
    } else if (qt === 'IMAGE_BASED') {
      const fmtRadio = document.querySelector('input[name=detailImgFmt]:checked');
      const fmt = fmtRadio ? fmtRadio.value : (node.imageAnswerType || 'WRITTEN');
      body.imageAnswerType = fmt;
      if (fmt === 'WRITTEN') {
        body.imagePath = document.getElementById('detailImgPath')?.value || '';
        body.imageAlt  = document.getElementById('detailImgAlt')?.value.trim() || '';
      } else {
        const pathInputs = document.querySelectorAll('#detailMcqImgSlots .detail-mcq-img-path');
        const altInputs  = document.querySelectorAll('#detailMcqImgSlots .detail-mcq-img-alt');
        body.imagePath = JSON.stringify(Array.from(pathInputs).map(i => i.value));
        body.imageAlt  = JSON.stringify(Array.from(altInputs).map(i => i.value.trim()));
      }
    }
  }

  const res = await fetch(`/api/admin/course-nodes/${id}`, {
    method: 'PUT',
    headers: safeAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  });

  if (res.ok) {
    await loadTree();
    const updated = treeData.find(n => n.id === id);
    if (updated) selectNode(updated);
    showToast('Saved.', 'success');
  } else {
    const data = await safeJson(res);
    showToast(data.message || 'Save failed.', 'danger');
  }
}

async function confirmDeleteNode(id) {
  if (!confirm('Delete this node and all its children?')) return;
  const res = await fetch(`/api/admin/course-nodes/${id}`, {
    method: 'DELETE',
    headers: safeAuthHeaders()
  });
  if (res.ok || res.status === 204) {
    selectedId = null;
    document.getElementById('detailPlaceholder').classList.remove('d-none');
    document.getElementById('detailForm').classList.add('d-none');
    await loadTree();
    showToast('Deleted.', 'success');
  } else {
    showToast('Delete failed.', 'danger');
  }
}

/* ── Utilities ── */
function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function showToast(message, type = 'danger') {
  const toast = document.createElement('div');
  toast.className = `toast align-items-center text-white bg-${type} border-0 show`;
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body fw-semibold">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto"
              data-bs-dismiss="toast" aria-label="Close"></button>
    </div>`;
  document.getElementById('toastContainer').appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}

/* ── Drag-resize panel ── */
(function initResize() {
  const handle = document.getElementById('dragHandle');
  const panel  = document.getElementById('treePanel');
  if (!handle || !panel) return;

  let dragging = false;
  handle.addEventListener('mousedown', () => { dragging = true; });
  document.addEventListener('mousemove', e => {
    if (!dragging) return;
    const rect  = panel.getBoundingClientRect();
    const width = e.clientX - rect.left;
    if (width >= 240 && width <= 600) panel.style.width = width + 'px';
  });
  document.addEventListener('mouseup', () => { dragging = false; });
})();

/* ── Boot ── */
loadTree();
