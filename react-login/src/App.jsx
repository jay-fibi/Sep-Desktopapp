import { useCallback, useEffect, useState } from "react";
import { LoginForm } from "./components/LoginForm.jsx";
import { SessionPanel } from "./components/SessionPanel.jsx";
import { appConfig, authConfig } from "./config.js";
import { readJson, readStored, removeStored, writeStored } from "./storage.js";

/* Light/dark switch. The palette itself comes from the CSS variables in
 * styles.css and is picked by the data-theme attribute on <html>. */
function useTheme() {
  const [theme, setTheme] = useState(() => readStored(authConfig.themeKey) || "dark");

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    writeStored(authConfig.themeKey, theme);
  }, [theme]);

  return {
    theme,
    toggle: useCallback(() => setTheme((prev) => (prev === "dark" ? "light" : "dark")), [])
  };
}

export default function App() {
  const [user, setUser] = useState(() => readJson(authConfig.sessionKey));
  const theme = useTheme();

  const handleSignOut = useCallback(() => {
    removeStored(authConfig.sessionKey);
    setUser(null);
  }, []);

  return (
    <main className="login-layout">
      <header className="login-header">
        <p className="meta">
          {appConfig.name} {appConfig.version} &mdash; React login screen
        </p>
        <button type="button" className="btn" onClick={theme.toggle}>
          {theme.theme === "dark" ? "Light theme" : "Dark theme"}
        </button>
      </header>

      {user ? (
        <SessionPanel user={user} onSignOut={handleSignOut} />
      ) : (
        <LoginForm onSignedIn={setUser} />
      )}

      <footer className="page-footer login-footer">
        <p>
          Nothing is uploaded: the demo user lives in <code>src/config.js</code> and
          is matched in the browser. Source for the standalone page in{" "}
          <code>login.html</code>.
        </p>
      </footer>
    </main>
  );
}
