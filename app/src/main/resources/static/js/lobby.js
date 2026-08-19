import { initTheme, getCurrentUser, apiFetch, escapeHtml, showError, copyText, initVersionFooter } from './app.js';

initTheme();
initVersionFooter();

const lobbyId = new URLSearchParams(location.search).get('id');
const userSlot = document.getElementById('user-slot');
const pageError = document.getElementById('page-error');
const headerEl = document.getElementById('lobby-header');
const contentEl = document.getElementById('content');

const STATUS_LABEL = { OPEN: 'Сбор участников', VOTING: 'Голосование', CLOSED: 'Завершено' };

let currentUser = null;
let lobby = null;
let genres = [];
let selectedGenreIds = new Set();
let onlyFree = false;
let myPicks = new Set();

async function init() {
    if (!lobbyId) {
        showError(pageError, new Error('Не указан ID лобби'));
        return;
    }

    try {
        currentUser = await getCurrentUser();
        if (!currentUser) {
            location.href = '/index.html';
            return;
        }
        userSlot.innerHTML = `
            <span class="user-chip">
                ${currentUser.avatarUrl ? `<img src="${escapeHtml(currentUser.avatarUrl)}" alt="">` : ''}
                <span>${escapeHtml(currentUser.displayName)}</span>
            </span>
        `;

        genres = await apiFetch('/api/genres');
        await refreshLobby();
        connectEvents();
    } catch (err) {
        showError(pageError, err);
    }
}

async function refreshLobby() {
    lobby = await apiFetch(`/api/lobbies/${lobbyId}`);
    lobby._isHost = lobby.hostUserId === currentUser.id;
    renderHeader();
    await renderContent();
}

function renderHeader() {
    const readyCount = lobby.members.filter(m => m.ready).length;
    const total = lobby.members.length;
    const showReadyPill = lobby.status === 'VOTING';

    headerEl.innerHTML = `
        <div class="lobby-header">
            <div class="invite-code">
                <span>Код приглашения</span>
                <button type="button" id="copy-invite" class="code mono" title="Скопировать код">${escapeHtml(lobby.inviteCode)}</button>
            </div>
            <div class="header-right">
                ${showReadyPill ? `<span class="ready-pill ${readyCount === total ? 'all-ready' : ''}">Готово: ${readyCount}/${total}</span>` : ''}
                <span class="status-pill">${STATUS_LABEL[lobby.status] || lobby.status}</span>
                ${renderExitActionButton()}
            </div>
        </div>
        <div class="members">
            ${lobby.members.map(m => `
                <span class="member-chip">
                    ${m.avatarUrl ? `<img src="${escapeHtml(m.avatarUrl)}" alt="">` : ''}
                    <span>${escapeHtml(m.displayName)}</span>
                    ${lobby.status === 'VOTING' ? `<span class="ready-dot ${m.ready ? 'on' : ''}" title="${m.ready ? 'Готов(а)' : 'Ещё не готов(а)'}"></span>` : ''}
                    ${m.userId === lobby.hostUserId ? '<span class="host-badge">ХОСТ</span>' : ''}
                </span>
            `).join('')}
        </div>
    `;

    document.getElementById('copy-invite').addEventListener('click', async (e) => {
        const ok = await copyText(lobby.inviteCode);
        const original = e.currentTarget.textContent;
        e.currentTarget.textContent = ok ? 'Скопировано' : 'Не удалось скопировать';
        setTimeout(() => { e.currentTarget.textContent = original; }, 1500);
    });

    bindExitAction();
}

function renderExitActionButton() {
    const exitIcon = `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
             <polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>`;
    const trashIcon = `<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline>
             <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>`;

    if (lobby._isHost) {
        if (lobby.status === 'VOTING') return '';
        return `<button type="button" id="exit-action-btn" class="exit-action-btn" title="Удалить лобби">${trashIcon}</button>`;
    }
    if (lobby.status === 'CLOSED') return '';
    return `<button type="button" id="exit-action-btn" class="exit-action-btn" title="Покинуть лобби">${exitIcon}</button>`;
}

function bindExitAction() {
    const btn = document.getElementById('exit-action-btn');
    if (!btn) return;
    const isDelete = lobby._isHost;
    btn.addEventListener('click', async () => {
        const question = isDelete ? 'Удалить это лобби насовсем?' : 'Покинуть это лобби?';
        if (!confirm(question)) return;
        btn.disabled = true;
        try {
            await apiFetch(`/api/lobbies/${lobbyId}${isDelete ? '' : '/leave'}`, { method: isDelete ? 'DELETE' : 'POST' });
            location.href = '/index.html';
        } catch (err) {
            showError(pageError, err);
            btn.disabled = false;
        }
    });
}

async function renderContent() {
    if (lobby.status === 'CLOSED') {
        await renderResults();
        return;
    }
    await renderPool();
}

async function renderPool() {
    if (lobby.status === 'VOTING') {
        try {
            myPicks = new Set(await apiFetch(`/api/lobbies/${lobbyId}/picks/mine`));
        } catch (err) {
            showError(pageError, err);
        }
    }

    const params = new URLSearchParams();
    selectedGenreIds.forEach(id => params.append('genreIds', id));
    if (onlyFree) params.append('isFree', 'true');

    let pool;
    try {
        pool = await apiFetch(`/api/lobbies/${lobbyId}/pool?${params.toString()}`);
    } catch (err) {
        showError(pageError, err);
        return;
    }

    const votingNotStarted = lobby.status === 'OPEN';
    const privacyNotice = pool.membersWithPrivateProfileUserIds.length
        ? `<div class="notice">У ${pool.membersWithPrivateProfileUserIds.length} участник(ов) закрыта библиотека Steam —
           их игры не учтены в общем пуле.</div>`
        : '';

    contentEl.innerHTML = `
        ${votingNotStarted ? '<div class="notice">Голосование ещё не началось — это предпросмотр общего пула игр.</div>' : ''}
        ${privacyNotice}
        <div class="filters" id="filters"></div>
        <div class="pool-grid" id="pool-grid"></div>
        ${renderBottomBar()}
    `;

    renderFilters();
    renderGameCards(pool.games, votingNotStarted);
    bindReadyToggle();
    bindHostActions();
}

function bindReadyToggle() {
    const btn = document.getElementById('ready-toggle-btn');
    if (!btn) return;
    btn.addEventListener('click', async () => {
        const currentlyReady = btn.getAttribute('aria-pressed') === 'true';
        btn.disabled = true;
        try {
            await apiFetch(`/api/lobbies/${lobbyId}/ready?ready=${!currentlyReady}`, { method: 'POST' });
            await refreshLobby();
        } catch (err) {
            showError(pageError, err);
            btn.disabled = false;
        }
    });
}

function renderFilters() {
    const filtersEl = document.getElementById('filters');
    filtersEl.innerHTML = `
        ${genres.map(g => `
            <button type="button" class="chip-toggle" data-genre-id="${g.id}"
                    aria-pressed="${selectedGenreIds.has(g.id)}">${escapeHtml(g.name)}</button>
        `).join('')}
        <button type="button" class="chip-toggle" id="free-toggle" aria-pressed="${onlyFree}">Только бесплатные</button>
    `;

    filtersEl.querySelectorAll('[data-genre-id]').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = Number(btn.dataset.genreId);
            if (selectedGenreIds.has(id)) selectedGenreIds.delete(id); else selectedGenreIds.add(id);
            renderPool();
        });
    });
    document.getElementById('free-toggle').addEventListener('click', () => {
        onlyFree = !onlyFree;
        renderPool();
    });
}

function renderGameCards(games, disabled) {
    const grid = document.getElementById('pool-grid');
    if (!games.length) {
        grid.innerHTML = '<div class="empty-state">Пул пуст. Попробуйте снять часть фильтров.</div>';
        return;
    }

    grid.innerHTML = games.map(game => `
        <div class="game-card">
            <div class="art" style="${game.headerImage ? `background-image:url('${escapeHtml(game.headerImage)}')` : ''}"></div>
            <div class="body">
                <div class="title">${escapeHtml(game.name)}</div>
                <button type="button" data-game-id="${game.id}" aria-pressed="${myPicks.has(game.id)}"
                        ${disabled ? 'disabled' : ''}>
                    ${myPicks.has(game.id) ? 'Пикнуто' : 'Пикнуть'}
                </button>
            </div>
        </div>
    `).join('');

    if (disabled) return;
    grid.querySelectorAll('[data-game-id]').forEach(btn => {
        btn.addEventListener('click', () => togglePick(Number(btn.dataset.gameId), btn));
    });
}

async function togglePick(gameId, button) {
    const alreadyPicked = myPicks.has(gameId);
    button.disabled = true;
    try {
        if (alreadyPicked) {
            await apiFetch(`/api/lobbies/${lobbyId}/picks/${gameId}`, { method: 'DELETE' });
            myPicks.delete(gameId);
        } else {
            await apiFetch(`/api/lobbies/${lobbyId}/picks`, {
                method: 'POST',
                body: JSON.stringify({ gameId }),
            });
            myPicks.add(gameId);
        }
        button.setAttribute('aria-pressed', String(myPicks.has(gameId)));
        button.textContent = myPicks.has(gameId) ? 'Пикнуто' : 'Пикнуть';
    } catch (err) {
        showError(pageError, err);
    } finally {
        button.disabled = false;
    }
}

function renderBottomBar() {
    if (lobby.status === 'OPEN') {
        if (!lobby._isHost) return '';
        return `<div class="action-bar">
                 <span>Все нужные участники в лобби?</span>
                 <button class="primary" id="start-voting-btn">Начать голосование</button>
                 </div>`;
    }
    if (lobby.status === 'VOTING') {
        const me = lobby.members.find(m => m.userId === currentUser.id);
        const myReady = me ? me.ready : false;
        const readyBtn = `<button type="button" id="ready-toggle-btn" class="${myReady ? '' : 'primary'}"
                            aria-pressed="${myReady}">${myReady ? 'Отменить' : 'Готово'}</button>`;

        let rightSide;
        if (lobby._isHost) {
            const readyCount = lobby.members.filter(m => m.ready).length;
            const total = lobby.members.length;
            const allReady = readyCount === total;
            rightSide = `<button class="primary" id="close-voting-btn" ${allReady ? '' : 'disabled'}>Закрыть голосование</button>`;
        } else {
            rightSide = '<span>Ждём остальных</span>';
        }
        return `<div class="action-bar">${readyBtn}${rightSide}</div>`;
    }
    return '';
}

function bindHostActions() {
    const startBtn = document.getElementById('start-voting-btn');
    if (startBtn) {
        startBtn.addEventListener('click', async () => {
            startBtn.disabled = true;
            try {
                await apiFetch(`/api/lobbies/${lobbyId}/voting/start`, { method: 'POST' });
                await refreshLobby();
            } catch (err) {
                showError(pageError, err);
                startBtn.disabled = false;
            }
        });
    }
    const closeBtn = document.getElementById('close-voting-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', async () => {
            closeBtn.disabled = true;
            try {
                await apiFetch(`/api/lobbies/${lobbyId}/close`, { method: 'POST' });
                await refreshLobby();
            } catch (err) {
                showError(pageError, err);
                closeBtn.disabled = false;
            }
        });
    }
}

async function renderResults() {
    let results;
    let nameById = new Map();
    try {
        results = await apiFetch(`/api/lobbies/${lobbyId}/results`);
        const fullPool = await apiFetch(`/api/lobbies/${lobbyId}/pool`);
        fullPool.games.forEach(g => nameById.set(g.id, g.name));
    } catch (err) {
        showError(pageError, err);
        return;
    }

    if (!results.length) {
        contentEl.innerHTML = '<div class="empty-state">Голосование закрыто, но пиков не было — играть не во что.</div>';
        return;
    }

    contentEl.innerHTML = `
        <div class="results-header">
            <h2>Готово. Вот что выбрали.</h2>
            <p>${results.length === 1 ? 'Единогласно.' : `Топ-${results.length} по совпадению пиков.`}</p>
        </div>
        ${results.map(r => `
            <div class="match-row">
                <span class="rank-ghost">${String(r.rankPosition).padStart(2, '0')}</span>
                <div class="info">
                    <div class="title">${escapeHtml(nameById.get(r.gameId) || `Игра #${r.gameId}`)}</div>
                    <div class="bar-track"><div class="bar-fill" data-target="${r.matchPercentage}"></div></div>
                </div>
                <span class="percentage">${r.matchPercentage}%</span>
            </div>
        `).join('')}
    `;

    requestAnimationFrame(() => {
        contentEl.querySelectorAll('.bar-fill').forEach(el => {
            el.style.width = `${el.dataset.target}%`;
        });
    });
}

function connectEvents() {
    const source = new EventSource(`/api/lobbies/${lobbyId}/events`);

    source.addEventListener('MEMBER_JOINED', () => refreshLobby());
    source.addEventListener('MEMBER_LEFT', () => refreshLobby());
    source.addEventListener('READY_CHANGED', () => refreshLobby());
    source.addEventListener('LOBBY_STATUS_CHANGED', () => refreshLobby());
    source.addEventListener('MATCH_COMPUTED', () => refreshLobby());
    source.addEventListener('LOBBY_DELETED', () => {
        if (!lobby || !lobby._isHost) {
            alert('Хост удалил это лобби.');
            location.href = '/index.html';
        }
    });

    source.onerror = () => {
    };
}

init();