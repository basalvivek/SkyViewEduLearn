const API_BASE = '/api/v1';

async function apiFetch(path, options = {}) {
    const token = getToken();
    const headers = options.noJson ? {} : { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const { noJson, ...fetchOptions } = options;

    const res = await fetch(API_BASE + path, { ...fetchOptions, headers });

    if (res.status === 401) {
        sessionStorage.clear();
        window.location.href = '/login.html';
        return null;
    }

    const data = await res.json();
    return data;
}

async function apiGet(path) { return apiFetch(path, { method: 'GET' }); }
async function apiPost(path, body) {
    return apiFetch(path, { method: 'POST', body: JSON.stringify(body) });
}
async function apiPut(path, body) {
    return apiFetch(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined });
}
async function apiDelete(path) { return apiFetch(path, { method: 'DELETE' }); }
