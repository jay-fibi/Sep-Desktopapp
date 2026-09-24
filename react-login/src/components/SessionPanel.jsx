import { maskEmail } from "../auth/credentials.js";

/* Shown once the demo credentials have been accepted. It is deliberately plain:
 * there is no protected content behind this screen. */
export function SessionPanel({ user, onSignOut }) {
  return (
    <section className="card login-card" aria-labelledby="session-heading">
      <h1 id="session-heading">Welcome, {user.displayName}</h1>
      <p className="subtitle">
        Signed in as <code>{maskEmail(user.email)}</code>. This session lives in{" "}
        <code>localStorage</code> only.
      </p>
      <button type="button" className="btn btn-primary btn-block" onClick={onSignOut}>
        Sign out
      </button>
    </section>
  );
}
