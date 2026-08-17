const THEME_KEY = 'gp-theme';

export function initTheme() {
    const saved = localStorage.getItem(THEME_KEY);
    const preferred = saved || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    applyTheme(preferred);

    const toggle = document.querySelector('[data-theme-toggle]');
    if (toggle) {
        updateToggleLabel(toggle, preferred);
        toggle.addEventListener('click', () => {
            const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            applyTheme(next);
            localStorage.setItem(THEME_KEY, next);
            updateToggleLabel(toggle, next);
        });
    }
}

function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
}

function updateToggleLabel(toggle, theme) {
    toggle.textContent = theme === 'dark' ? '☀' : '☾';
    toggle.setAttribute('aria-label', theme === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему');
}

class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

export async function apiFetch(path, options = {}) {
    const response = await fetch(path, {
        credentials: 'same-origin',
        headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
        ...options,
    });

    if (response.status === 204) {
        return null;
    }

    let body = null;
    const text = await response.text();
    if (text) {
        try {
            body = JSON.parse(text);
        } catch {
            body = null;
        }
    }

    if (!response.ok) {
        const message = body && body.message ? body.message : `Ошибка запроса (${response.status})`;
        throw new ApiError(message, response.status);
    }

    return body;
}

export async function getCurrentUser() {
    try {
        return await apiFetch('/api/auth/me');
    } catch (e) {
        if (e.status === 401) return null;
        throw e;
    }
}

export function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value ?? '';
    return div.innerHTML;
}

export async function copyText(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch {
        return false;
    }
}

export function showError(container, error) {
    container.innerHTML = '';
    if (!error) return;
    const banner = document.createElement('div');
    banner.className = 'error-banner';
    banner.setAttribute('role', 'alert');
    banner.textContent = error.message || String(error);
    container.appendChild(banner);
}

export async function initVersionFooter() {
    const el = document.getElementById('app-version');
    if (!el) return;
    try {
        const { version } = await apiFetch('/api/version');
        el.textContent = `v${version}`;
    } catch {
        el.textContent = '';
    }
}

export function bindLogout(button) {
    if (!button) return;
    button.addEventListener('click', async () => {
        if (!confirm('Выйти из аккаунта Steam?')) return;
        button.disabled = true;
        try {
            await apiFetch('/api/auth/logout', { method: 'POST' });
        } catch {
        }
        location.href = '/index.html';
    });
}