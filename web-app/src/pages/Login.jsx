import { useState } from "react";
import { login } from "../services/authService";
import HomieMascot from "../components/HomieMascot";

function Login({ onSuccess, onSwitchToRegister }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage("");

    if (!email.trim() || !password) {
      setErrorMessage("Lütfen e-posta ve şifrenizi girin.");
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await login(email.trim(), password);
      onSuccess(user);
    } catch (error) {
      console.error("Giriş yapılamadı:", error);
      setErrorMessage(
        error.message || "Giriş yapılırken bir sorun oluştu."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <span className="auth-brand-logo" aria-hidden="true">
            <HomieMascot size={40} />
          </span>

          <h1>
            <span className="brand-letter-box">
              <span>P</span>
            </span>
            ower
            <span className="brand-letter-box">
              <span>P</span>
            </span>
            ulse
          </h1>
        </div>

        <p className="auth-subtitle">Hesabına giriş yap</p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="auth-field">
            <span>E-posta</span>

            <input
              type="email"
              name="pp-login-email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ornek@eposta.com"
              autoComplete="off"
            />
          </label>

          <label className="auth-field">
            <span>Şifre</span>

            <input
              type="password"
              name="pp-login-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="••••••••"
              autoComplete="off"
            />
          </label>

          {errorMessage && <p className="auth-error">{errorMessage}</p>}

          <button
            type="submit"
            className="auth-submit-button"
            disabled={isSubmitting}
          >
            {isSubmitting ? "Giriş yapılıyor..." : "Giriş Yap"}
          </button>
        </form>

        <p className="auth-switch">
          Hesabın yok mu?{" "}
          <button
            type="button"
            className="auth-switch-button"
            onClick={onSwitchToRegister}
          >
            Kayıt ol
          </button>
        </p>
      </div>
    </div>
  );
}

export default Login;
