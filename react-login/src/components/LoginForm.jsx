import { useLoginForm } from "../hooks/useLoginForm.js";

/* Small presentational input with label, hint and error message wired together
 * through aria attributes so screen readers announce the message too. */
function TextField({ id, label, type, hint, error, autoComplete, placeholder, value, onChange, onBlur, children }) {
  const errorId = `${id}-error`;
  const hintId = hint ? `${id}-hint` : undefined;
  const describedBy = [error && errorId, hintId].filter(Boolean).join(" ") || undefined;

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="input-group">
        <input
          id={id}
          className="text-input"
          name={id}
          type={type}
          value={value}
          autoComplete={autoComplete}
          placeholder={placeholder}
          aria-invalid={error ? "true" : undefined}
          aria-describedby={describedBy}
          onChange={(event) => onChange(event.target.value)}
          onBlur={onBlur}
          required
        />
        {children}
      </div>
      {hint ? (
        <p className="hint" id={hintId}>
          {hint}
        </p>
      ) : null}
      <p className="field-error" id={errorId} role={error ? "alert" : undefined}>
        {error}
      </p>
    </div>
  );
}

/* The sign-in card. It receives `onSignedIn` and delegates every other detail
 * to the useLoginForm hook. */
export function LoginForm({ onSignedIn }) {
  const form = useLoginForm({ onSignedIn });
  const { fields, status } = form;

  /* Enter submits (native form behaviour); Escape clears the fields. */
  function handleKeyDown(event) {
    if (event.key === "Escape") {
      event.preventDefault();
      form.clear();
    }
  }

  return (
    <section className="card login-card" aria-labelledby="login-heading">
      <h1 id="login-heading">Sign in</h1>
      <p className="subtitle">
        Demo account only &mdash; the credentials are checked in the browser.
      </p>

      <form id="login-form" onSubmit={form.submit} onKeyDown={handleKeyDown} noValidate>
        <TextField
          id="email"
          label="Email"
          type="email"
          autoComplete="username"
          placeholder="demo@example.com"
          value={fields.email.value}
          error={fields.email.error}
          onChange={fields.email.onChange}
          onBlur={fields.email.onBlur}
        />

        <TextField
          id="password"
          label="Password"
          type={fields.showPassword.value ? "text" : "password"}
          autoComplete="current-password"
          placeholder="Your password"
          hint={`At least ${form.minPasswordLength} characters.`}
          value={fields.password.value}
          error={fields.password.error}
          onChange={fields.password.onChange}
          onBlur={fields.password.onBlur}
        >
          <button
            type="button"
            className="btn password-toggle"
            aria-controls="password"
            aria-pressed={fields.showPassword.value}
            onClick={fields.showPassword.toggle}
          >
            {fields.showPassword.value ? "Hide" : "Show"}
          </button>
        </TextField>

        <div className="checkbox-row">
          <label className="checkbox" htmlFor="remember">
            <input
              id="remember"
              name="remember"
              type="checkbox"
              checked={fields.remember.value}
              onChange={(event) => fields.remember.onChange(event.target.checked)}
            />
            <span>Remember my email</span>
          </label>
          <button
            type="button"
            className="link-button"
            onClick={() => window.alert("This is a demo: password reset is not wired up.")}
          >
            Forgot password?
          </button>
        </div>

        <div className="login-actions">
          <button
            type="submit"
            id="sign-in"
            className="btn btn-primary btn-block"
            disabled={form.submitting || form.locked}
          >
            {form.submitting ? "Signing in..." : "Sign in"}
          </button>
          <div className="actions">
            <button type="button" className="btn" onClick={form.fillDemo} disabled={form.locked}>
              Use demo credentials
            </button>
            <button type="button" className="btn" onClick={form.clear}>
              Clear
            </button>
          </div>
        </div>

        <p
          className={`message message-${status.kind}`}
          role="status"
          aria-live="polite"
        >
          {status.text}
        </p>

        <p className="demo-line">
          Demo account:{" "}
          <code>
            {form.demoUser.email} / {form.demoUser.password}
          </code>
        </p>
      </form>
    </section>
  );
}
