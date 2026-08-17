import { initTheme, getCurrentUser, apiFetch, escapeHtml, showError, initVersionFooter } from './app.js';

initTheme();
initVersionFooter();

const app = document.getElementById('app');
const userSlot = document.getElementById('user-slot');
const pageError = document.getElementById('page-error');

async function init() {
    let user;
    try {
        user = await getCurrentUser();
    } catch (e) {
        showError(pageError, e);
        return;
    }

    if (!user) {
        renderLoggedOut();
        return;
    }

    renderUserSlot(user);

    const params = new URLSearchParams(location.search);
    const joinCode = params.get('join');
    if (joinCode) {
        await joinByCode(joinCode);
        return;
    }

    renderLoggedIn(user);
}

function renderLoggedOut() {
    app.innerHTML = `
        <section class="hero">
            <h1>Во что сегодня играем?</h1>
            <p>
                Собирайте лобби — мы объединим библиотеки участников.
                Вместо воздуханства в стиле «во что сегодня играем?» получите ТОП
                по тем играм, в которые хотят поиграть все!
            </p>
            <a href="/api/auth/steam/login"><button class="primary">Войти через Steam</button></a>
        </section>
    `;
}

function renderUserSlot(user) {
    userSlot.innerHTML = `
        <span class="user-chip">
            ${user.avatarUrl ? `<img src="${escapeHtml(user.avatarUrl)}" alt="">` : ''}
            <span>${escapeHtml(user.displayName)}</span>
        </span>
    `;
}

function renderLoggedIn(user) {
    app.innerHTML = `
        <section class="hero">
            <h1>О, ${escapeHtml(user.displayName)},</h1>
            <h1>ты тоже здесь?</h1>
            <p>
                Прикинь, можно создать новое лобби или
                войти в уже существующее по коду от друга
            </p>
            <p>и залипать по-страшному вместе!</p>
        </section>
        <div class="action-grid">
            <div class="action-card">
                <h3>Создать лобби</h3>
                <p>Вы станете хостом — только вы сможете начать голосование, а затем закрыть его.</p>
                <button class="primary" id="create-lobby-btn">Создать</button>
            </div>
            <div class="action-card">
                <h3>Войти по коду</h3>
                <p>Введите код приглашения, который прислал хост лобби.</p>
                <form class="row" id="join-form">
                    <input type="text" id="join-code-input" placeholder="ABC123" maxlength="8" autocomplete="off">
                    <button class="primary" type="submit">Войти</button>
                </form>
            </div>
        </div>
    `;

    document.getElementById('create-lobby-btn').addEventListener('click', async (e) => {
        const btn = e.currentTarget;
        btn.disabled = true;
        try {
            const lobby = await apiFetch('/api/lobbies', { method: 'POST' });
            location.href = `/lobby.html?id=${lobby.id}`;
        } catch (err) {
            showError(pageError, err);
            btn.disabled = false;
        }
    });

    document.getElementById('join-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const code = document.getElementById('join-code-input').value.trim();
        if (!code) return;
        await joinByCode(code);
    });
}

async function joinByCode(code) {
    showError(pageError, null);
    try {
        const lobby = await apiFetch(`/api/lobbies/join/${encodeURIComponent(code)}`, { method: 'POST' });
        location.href = `/lobby.html?id=${lobby.id}`;
    } catch (err) {
        showError(pageError, err);
        const user = await getCurrentUser();
        if (user) renderLoggedIn(user);
    }
}

init();