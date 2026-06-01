const AUTH_TOKEN_KEY = 'edu_token';
const AUTH_ROLE_KEY = 'edu_role';
const AUTH_NAME_KEY = 'edu_name';

function getToken() { return sessionStorage.getItem(AUTH_TOKEN_KEY); }
function getRole()  { return sessionStorage.getItem(AUTH_ROLE_KEY); }
function getName()  { return sessionStorage.getItem(AUTH_NAME_KEY); }

function isLoggedIn() { return !!getToken(); }

function logout() {
    sessionStorage.clear();
    window.location.href = '/login.html';
}

function requireAuth(expectedRole) {
    if (!isLoggedIn()) { window.location.href = '/login.html'; return false; }
    if (expectedRole && getRole() !== expectedRole) {
        window.location.href = '/login.html';
        return false;
    }
    return true;
}

function initUserDisplay() {
    const name = getName();
    const role = getRole();
    document.querySelectorAll('[data-user-name]').forEach(el => el.textContent = name || '');
    document.querySelectorAll('[data-user-role]').forEach(el => el.textContent = role || '');
    const initials = name ? name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase() : '??';
    document.querySelectorAll('[data-user-initials]').forEach(el => el.textContent = initials);
    const emailEl = document.querySelector('[data-user-email]');
    if (emailEl) {
        // Optionally load email from /api/v1/auth/me
    }
}
