import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { authConfig } from "../config.js";
import {
  credentialsMatch,
  maskEmail,
  minPasswordLength,
  normalizeEmail,
  validateCredentials
} from "../auth/credentials.js";
import { readStored, removeStored, writeStored } from "../storage.js";

/* All of the sign-in behaviour lives here so the components stay presentational.
 *
 * The demo "server call" is simulated with a short timeout - there is no
 * network traffic and no real authentication anywhere in this app.
 */

const AUTH_DELAY_MS = 600;

function wait(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function useLoginForm({ onSignedIn } = {}) {
  const [email, setEmail] = useState(() => readStored(authConfig.rememberEmailKey) || "");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(
    () => Boolean(readStored(authConfig.rememberEmailKey))
  );
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({ email: false, password: false });
  const [attempts, setAttempts] = useState(0);
  const [status, setStatus] = useState({ kind: "idle", text: "" });
  const [submitting, setSubmitting] = useState(false);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const limit = minPasswordLength(authConfig.minPasswordLength);
  const locked = attempts >= authConfig.maxAttempts;

  /* Validation is derived, never stored: it always matches what is typed. */
  const validation = useMemo(
    () => validateCredentials(email, password, limit),
    [email, password, limit]
  );
  const errors = validation.errors;
  /* A field only shows its message once it has been touched, so the form does
   * not shout at the user while they are still typing. */
  const visibleErrors = {
    email: touched.email ? errors.email : "",
    password: touched.password ? errors.password : ""
  };

  const submit = useCallback(
    async (event) => {
      event?.preventDefault?.();
      setTouched({ email: true, password: true });

      if (locked) {
        setStatus({ kind: "error", text: "Too many attempts - reload the page to try again" });
        return;
      }
      if (!validation.valid) {
        setStatus({
          kind: "error",
          text: errors.email || errors.password || "Check the highlighted fields"
        });
        return;
      }

      setSubmitting(true);
      setStatus({ kind: "pending", text: "Checking your details..." });
      await wait(AUTH_DELAY_MS);
      if (!mounted.current) {
        return;
      }
      setSubmitting(false);

      const user = credentialsMatch(validation.email, password, authConfig.demoUser)
        ? {
            email: normalizeEmail(authConfig.demoUser.email),
            displayName: authConfig.demoUser.displayName
          }
        : null;

      if (!user) {
        const next = attempts + 1;
        setAttempts(next);
        setStatus({
          kind: "error",
          text:
            next >= authConfig.maxAttempts
              ? `Wrong email or password - ${authConfig.maxAttempts} attempts used`
              : `Wrong email or password (${next}/${authConfig.maxAttempts})`
        });
        setPassword("");
        return;
      }

      if (remember) {
        writeStored(authConfig.rememberEmailKey, user.email);
      } else {
        removeStored(authConfig.rememberEmailKey);
      }
      writeStored(authConfig.sessionKey, JSON.stringify(user));
      setAttempts(0);
      setPassword("");
      setStatus({ kind: "ok", text: `Signed in as ${maskEmail(user.email)}` });
      onSignedIn?.(user);
    },
    [attempts, errors.email, errors.password, limit, locked, onSignedIn, password, remember, validation.email, validation.valid]
  );

  const clear = useCallback(() => {
    setEmail("");
    setPassword("");
    setTouched({ email: false, password: false });
    setStatus({ kind: "idle", text: "" });
  }, []);

  const fillDemo = useCallback(() => {
    setEmail(authConfig.demoUser.email);
    setPassword(authConfig.demoUser.password);
    setTouched({ email: false, password: false });
    setStatus({ kind: "idle", text: "" });
  }, []);

  return {
    fields: {
      email: {
        value: email,
        error: visibleErrors.email,
        onChange: (value) => {
          setEmail(value);
          setStatus({ kind: "idle", text: "" });
        },
        onBlur: () => setTouched((prev) => ({ ...prev, email: true }))
      },
      password: {
        value: password,
        error: visibleErrors.password,
        onChange: (value) => {
          setPassword(value);
          setStatus({ kind: "idle", text: "" });
        },
        onBlur: () => setTouched((prev) => ({ ...prev, password: true }))
      },
      remember: {
        value: remember,
        onChange: setRemember
      },
      showPassword: {
        value: showPassword,
        toggle: () => setShowPassword((prev) => !prev)
      }
    },
    minPasswordLength: limit,
    attempts,
    maxAttempts: authConfig.maxAttempts,
    locked,
    submitting,
    status,
    submit,
    clear,
    fillDemo,
    demoUser: authConfig.demoUser
  };
}
