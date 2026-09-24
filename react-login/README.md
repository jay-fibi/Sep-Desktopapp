# React login screen

A simple login screen built with **React 19 + Vite**, added next to the existing
vanilla page (`login.html` / `login.css` / `login.js`). It is a self-contained
Vite app in this folder: install, `npm run dev`, done.

The demo account is matched **in the browser only** — no network calls, no real
authentication, nothing is uploaded. The credentials live in `src/config.js`,
mirroring the `auth` block of the repository's `config.json`:

```
demo@example.com / calculator123
```

## Quick start

```bash
cd react-login
npm install
npm run dev      # http://localhost:5173
```

Other scripts:

| Script            | What it does                                        |
| ----------------- | --------------------------------------------------- |
| `npm run build`   | Production bundle in `dist/`                        |
| `npm run preview` | Serves the built bundle locally                     |
| `npm test`        | Unit + component tests (Vitest, jsdom)              |

## What the screen does

- Email + password fields with inline validation (only after a field is touched).
- Show/Hide password toggle, "Remember my email", "Use demo credentials", Clear.
- Sign-in runs through a short simulated delay, so the button has a pending state.
- Wrong credentials are counted; after 5 attempts the form locks until reload.
- Successful sign-in stores the session in `localStorage` and shows a welcome
  panel with a Sign out button.
- Enter submits, Escape clears the form.
- Light/dark theme toggle, driven by `data-theme` on `<html>` and remembered.
- Accessibility: real `<label>`s, `aria-invalid`, `aria-describedby` pointing at
  the error/hint text, `role="status"` for messages, visible focus outlines.

## Files

```
react-login/
├── index.html                  # Vite entry (mounts #root)
├── vite.config.js              # React plugin + Vitest config
├── src/
│   ├── main.jsx                # createRoot(...).render(<App />)
│   ├── App.jsx                 # theme + session shell
│   ├── config.js               # demo user, limits, storage keys
│   ├── storage.js              # guarded localStorage helpers
│   ├── styles.css              # design tokens + card/form styles
│   ├── auth/credentials.js     # pure validation helpers (no React)
│   ├── auth/credentials.test.js
│   ├── components/LoginForm.jsx
│   ├── components/SessionPanel.jsx
│   ├── hooks/useLoginForm.js   # all form state + submit logic
│   └── App.test.jsx            # types into the form like a user
```

`src/auth/credentials.js` keeps the same rules as the vanilla page's
`src/utils/auth.js` (email pattern, 8 character minimum, `maskEmail`), so both
screens behave identically. The functions are pure and framework-free, which is
what the unit tests exercise.

## Verification

```bash
cd react-login && npm test && npm run build
```

`npm test` covers the validation helpers plus the screen itself in jsdom:
empty-submit errors, an invalid email, a wrong password with the attempt
counter, a successful demo sign-in (session + remembered email), sign-out,
password visibility, Escape-to-clear and the theme toggle.
