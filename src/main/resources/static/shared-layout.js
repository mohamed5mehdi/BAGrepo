// =====================================================
// shared-layout.js — Shared Logic for Actor Dashboards
// =====================================================

const userId   = localStorage.getItem('userId');
const userName = localStorage.getItem('userName') || 'User';
const userRole = localStorage.getItem('userRole') || '';

if (!userId) { window.location.href = '/'; }

document.addEventListener('DOMContentLoaded', () => {
    const el = (id) => document.getElementById(id);
    if (el('sidebarName'))   el('sidebarName').innerText = userName;
    if (el('sidebarRole'))   el('sidebarRole').innerText = userRole.replace('ROLE_', '');
    if (el('sidebarAvatar')) el('sidebarAvatar').src =
        `https://ui-avatars.com/api/?name=${encodeURIComponent(userName)}&background=1976d2&color=fff`;
    loadRequests();
});

function toggleSidebar() {
    document.querySelector('.sidebar').classList.toggle('collapsed');
}

function toggleTheme() {
    const h = document.documentElement;
    h.setAttribute('data-theme', h.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
}

function statusBadge(status) {
    const map = {
        EN_ATTENTE_N1:        ['#1976d2','Pending N+1'],
        EN_ATTENTE_TECH:      ['#0097a7','Technical Validation'],
        EN_ATTENTE_AMG:       ['#f57c00','AMG Review'],
        EN_ATTENTE_ACHAT:     ['#f57c00','Purchase Treatment'],
        EN_ATTENTE_DAF:       ['#e64a19','DAF Approval'],
        EN_ATTENTE_DG:        ['#b71c1c','DG Approval'],
        VALIDEE:              ['#2e7d32','Validated'],
        PO_CREE:              ['#43a047','PO Created'],
        REJETEE:              ['#c62828','Rejected'],
    };
    const [color, label] = map[status] || ['#9e9e9e', status];
    return `<span style="padding:4px 12px;border-radius:20px;font-size:0.78rem;font-weight:600;
            background:${color}22;color:${color};border:1px solid ${color}55">${label}</span>`;
}

function buildStepper(currentStatus) {
    const steps = [
        { id: 'EN_ATTENTE_N1',    label: 'N+1' },
        { id: 'EN_ATTENTE_TECH',  label: 'Tech' },
        { id: 'EN_ATTENTE_ACHAT', label: 'Purchase' },
        { id: 'EN_ATTENTE_AMG',   label: 'AMG' },
        { id: 'EN_ATTENTE_DAF',   label: 'DAF' },
        { id: 'EN_ATTENTE_DG',    label: 'DG' },
        { id: 'PO_CREE',          label: 'PO' }
    ];

    const statusOrder = [
        'EN_ATTENTE_N1', 'EN_ATTENTE_TECH', 'EN_ATTENTE_ACHAT', 
        'EN_ATTENTE_AMG', 'EN_ATTENTE_DAF', 'EN_ATTENTE_DG', 'VALIDEE', 'PO_CREE'
    ];
    
    let currentIndex = statusOrder.indexOf(currentStatus);
    if (currentStatus === 'REJETEE') currentIndex = -1;

    return steps.map((s, i) => {
        const stepStatusIndex = statusOrder.indexOf(s.id);
        let cls = '';
        if (currentIndex >= stepStatusIndex) cls = 'active';
        if (currentStatus === 'VALIDEE' && s.id !== 'PO_CREE') cls = 'active';
        if (currentStatus === 'PO_CREE') cls = 'active';

        return `
            <div class="step ${cls}">
                <div class="step-num">${i + 1}</div>
                <div class="step-label">${s.label}</div>
            </div>
        `;
    }).join('<div class="step-line"></div>');
}

let currentDa = null;

function loadRequests() {
    fetch('/api/da-headers')
        .then(r => r.json())
        .then(list => renderTable(list))
        .catch(() => {
            const tb = document.getElementById('tbodyRequests') || document.getElementById('requestsTableBody');
            if (tb) tb.innerHTML = `<tr><td colspan="8" class="no-data">Unable to load data.</td></tr>`;
        });
}

function getDaCategory(da) {
    if (da.details && da.details.length > 0 && da.details[0].subFamily) {
        const f = da.details[0].subFamily.family?.libelle || '';
        const sf = da.details[0].subFamily.libelle || '';
        return f + (sf ? ' / ' + sf : '');
    }
    return '—';
}

function getDaTotal(da) {
    if (!da.details) return 0;
    return da.details.reduce((sum, det) => sum + (det.totalPrice || 0), 0);
}

function renderTable(list) {
    const tb = document.getElementById('tbodyRequests') || document.getElementById('requestsTableBody');
    if (!tb) return;

    const filtered = filterByRole(list, userRole);
    if (filtered.length === 0) {
        tb.innerHTML = `<tr><td colspan="8" class="no-data">No requests pending.</td></tr>`;
        updateKPIs(list, filtered);
        return;
    }

    tb.innerHTML = filtered.map(da => {
        const cat = getDaCategory(da);
        return `
        <tr class="table-row" onclick="openModal(${JSON.stringify(da).replace(/"/g,'&quot;')})">
            <td>${da.oidDa}</td>
            <td class="da-number">DA-${String(da.oidDa).padStart(3, '0')}</td>
            <td>${da.demandeur?.nom || '—'}</td>
            <td>${cat.split(' / ')[0]}</td>
            <td>${cat.split(' / ')[1] || '—'}</td>
            <td>${statusBadge(da.statut)}</td>
            <td>
                <button class="btn btn-modifier" onclick="event.stopPropagation(); openModal(${JSON.stringify(da).replace(/"/g,'&quot;')})">
                     View
                </button>
            </td>
        </tr>`;
    }).join('');
    updateKPIs(list, filtered);
}

function filterByRole(list, role) {
    const r = (role || '').toUpperCase();
    switch(r) {
        case 'ROLE_DEMANDEUR':
            return list.filter(d => String(d.demandeur?.id_demandeur || d.demandeur?.id) === String(userId));
        case 'ROLE_N1':
            return list.filter(d => d.statut === 'EN_ATTENTE_N1');
        case 'ROLE_TECHNICIEN':
            return list.filter(d => d.statut === 'EN_ATTENTE_TECH');
        case 'ROLE_ACHETEUR':
        case 'ROLE_AMG':
            // The Buyer should see all active requests to anticipate work
            return list.filter(d => !['VALIDEE', 'REJETEE', 'PO_CREE'].includes(d.statut));
        case 'ROLE_DAF':
            return list.filter(d => d.statut === 'EN_ATTENTE_DAF');
        case 'ROLE_DG':
            return list.filter(d => d.statut === 'EN_ATTENTE_DG');
        default:
            return list;
    }
}

function updateKPIs(all, mine) {
    const set = (id, val) => { const e = document.getElementById(id); if (e) e.innerText = val; };
    set('kpiTotal',    all.length);
    set('kpiMine',     mine.length);
    set('kpiApproved', all.filter(d => d.statut === 'PO_CREE').length);
    set('kpiRejected', all.filter(d => d.statut === 'REJETEE').length);
    set('kpiPending',  all.filter(d => !['PO_CREE','REJETEE'].includes(d.statut)).length);
}

function openModal(da) {
    if (typeof da === 'string') da = JSON.parse(da);
    currentDa = da;

    // Standardize ID extraction (Postgres uses oid_da)
    const daId = da.oid_da || da.oidDa || da.id;
    const code = 'DA-' + String(daId).padStart(3, '0');
    
    // Update basic info
    const set = (id, val) => { const e = document.getElementById(id); if (e) e.innerText = val; };
    set('modalDaCode',  code);
    set('modalDaCode2', code);
    set('modalDaDate',  da.dateCreation ? da.dateCreation.substring(0,10) : '');
    set('modalSubject', da.objet || 'No subject');
    set('modalDesc',    da.justification || '');
    
    const total = getDaTotal(da);
    set('modalMontant', total > 0 ? total.toFixed(2) + ' €' : '0.00 €');
    set('modalCategorie', getDaCategory(da));
    
    const statusEl = document.getElementById('modalStatus');
    if (statusEl) statusEl.innerHTML = statusBadge(da.statut);
    
    const stepperEl = document.getElementById('modalStepper');
    if (stepperEl) stepperEl.innerHTML = buildStepper(da.statut);

    // --- Workflow Action Logic ---
    const btnValider = document.getElementById('btnValider');
    const btnRefuser = document.getElementById('btnRefuser');
    const sectionAction = document.getElementById('sectionAction') || document.getElementById('btnValiderSection');

    const role = (userRole || '').toUpperCase();
    const status = da.statut;
    let canAction = false;

    // Show buttons ONLY if it's the right person at the right time
    if (role === 'ROLE_N1'        && status === 'EN_ATTENTE_N1')   canAction = true;
    if (role === 'ROLE_TECHNICIEN' && status === 'EN_ATTENTE_TECH') canAction = true;
    if (role === 'ROLE_ACHETEUR'   && status === 'EN_ATTENTE_ACHAT') canAction = true;
    if (role === 'ROLE_AMG'        && status === 'EN_ATTENTE_AMG')   canAction = true;
    if (role === 'ROLE_DAF'        && status === 'EN_ATTENTE_DAF')   canAction = true;
    if (role === 'ROLE_DG'         && status === 'EN_ATTENTE_DG')    canAction = true;

    // Update button labels based on the guide
    if (canAction) {
        if (role === 'ROLE_N1')         btnValider.innerText = '✔ Accept (Hierarchical)';
        if (role === 'ROLE_TECHNICIEN')  btnValider.innerText = '✔ Validate Technical Conformity';
        if (role === 'ROLE_ACHETEUR')    btnValider.innerText = '✔ Validate Treatment & Budget';
        if (role === 'ROLE_AMG')         btnValider.innerText = '✔ Validate Adjustment Opportunity';
        if (role === 'ROLE_DAF')         btnValider.innerText = '✔ Validate Sub-Family Funds';
        if (role === 'ROLE_DG')          btnValider.innerText = '✔ Final Validation (Arbitrage)';
    }

    if (btnValider) btnValider.style.display = canAction ? 'inline-block' : 'none';
    if (btnRefuser) btnRefuser.style.display = canAction ? 'inline-block' : 'none';
    if (sectionAction) sectionAction.style.display = canAction ? 'block' : 'none';
    
    // Budget insight visibility
    if (['ROLE_ACHETEUR', 'ROLE_AMG', 'ROLE_DAF', 'ROLE_DG'].includes(role)) {
        renderBudgetInsight(da);
    } else {
        const bi = document.getElementById('budgetInsightContainer');
        if (bi) bi.style.display = 'none';
    }

    // Handle special DAF/DG sections (if they exist in the HTML)
    const esc = document.getElementById('btnEscaladeSection');
    const dga = document.getElementById('btnDgApproveSection');
    if (esc) esc.style.display = (role === 'ROLE_DAF' && status === 'EN_ATTENTE_DAF') ? 'block' : 'none';
    if (dga) dga.style.display = (role === 'ROLE_DG'  && status === 'EN_ATTENTE_DG')  ? 'block' : 'none';

    // NEW: Display Adjustment Intent Banner
    const bi = document.getElementById('budgetInsightContainer');
    if (bi && (role === 'ROLE_AMG' || role === 'ROLE_DAF' || role === 'ROLE_DG')) {
        fetch(`/api/da-headers/${daId}`)
            .then(r => r.json())
            .then(daFull => {
                const hasAdj = daFull.budgetTransfers && daFull.budgetTransfers.length > 0;
                if (hasAdj) {
                    const last = daFull.budgetTransfers[daFull.budgetTransfers.length - 1];
                    const type = last.subSource ? 'SUB-FAMILY (DAF Circuit)' : 'FAMILY (DG Circuit)';
                    const banner = document.createElement('div');
                    banner.style = "background:#fff3e0; border-left:5px solid #ff9800; padding:1rem; margin-top:1rem; border-radius:4px; font-size:.9rem;";
                    banner.innerHTML = `<strong>💡 Adjustment request detected :</strong><br>Solicited type: <span style="color:#e65100;font-weight:700">${type}</span>`;
                    bi.prepend(banner);
                }
            });
    }

    document.getElementById('workflowModal').classList.add('show');
}

function renderBudgetInsight(da) {
    const container = document.getElementById('budgetInsightContainer');
    if (!container) return;
    const sf = da.details?.[0]?.subFamily;
    if (!sf || !sf.budget_initial) { container.style.display = 'none'; return; }

    const initial = sf.budget_initial || 0;
    const remaining = sf.budget_restant || 0;
    const consumed = initial - remaining;
    const percent = Math.min(100, Math.max(0, (consumed / initial) * 100));
    const colorClass = percent > 85 ? 'danger' : percent > 70 ? 'warning' : '';

    container.innerHTML = `
        <div class="budget-insight">
            <div class="budget-label-row">
                <span>Budget: ${sf.libelle}</span>
                <span>${percent.toFixed(1)}%</span>
            </div>
            <div class="progress-container"><div class="progress-bar ${colorClass}" style="width: ${percent}%"></div></div>
            <div style="display:flex; justify-content:space-between; font-size:.7rem; margin-top:.3rem; color:var(--text-muted)">
                <span>Spent: ${consumed.toFixed(2)} €</span>
                <span>Left: ${remaining.toFixed(2)} €</span>
            </div>
        </div>`;
    container.style.display = 'block';
}

function closeModal() { document.getElementById('workflowModal').classList.remove('show'); }

function processAction(decision) {
    if (!currentDa) return;
    const motif = document.getElementById('modalComment')?.value || 'Decision taken from dashboard';
    fetch(`/api/workflow/validate?daId=${currentDa.oidDa}&userId=${userId}&decision=${decision}&motif=${encodeURIComponent(motif)}`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            showToast(`Success: ${data.statut}`, 'success');
            closeModal(); loadRequests();
        })
        .catch(() => showToast('Workflow Error', 'error'));
}

// --- LOGOUT & SESSION ---
function logout() {
    localStorage.clear();
    window.location.href = '/';
}

// Global session check
if (!userId && window.location.pathname !== '/' && !window.location.pathname.includes('index.html')) {
    window.location.href = '/';
}

function showToast(msg, type = 'success') {
    const t = document.createElement('div');
    t.className = 'toast toast-' + type;
    t.innerText = msg;
    document.body.appendChild(t);
    setTimeout(() => t.classList.add('show'), 10);
    setTimeout(() => { t.classList.remove('show'); setTimeout(() => t.remove(), 400); }, 3000);
}

