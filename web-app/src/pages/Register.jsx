import { useState } from "react";
import { registerAccount } from "../services/authService";
import HomieMascot from "../components/HomieMascot";

function Register({ onSuccess, onSwitchToLogin }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage("");

    if (!name.trim() || !email.trim() || !password) {
      setErrorMessage("Lütfen tüm alanları doldurun.");
      return;
    }

    if (password.length < 6) {
      setErrorMessage("Şifre en az 6 karakter olmalıdır.");
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage("Şifreler birbiriyle uyuşmuyor.");
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await registerAccount(
        name.trim(),
        email.trim(),
        password
      );
      onSuccess(user);
    } catch (error) {
      console.error("Kayıt olunamadı:", error);
      setErrorMessage(
        error.message || "Kayıt olurken bir sorun oluştu."
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

        <p className="auth-subtitle">Yeni bir hesap oluştur</p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="auth-field">
            <span>Ad Soyad</span>

            <input
              type="text"
              name="pp-register-name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Adın Soyadın"
              autoComplete="off"
            />
          </label>

          <label className="auth-field">
            <span>E-posta</span>

            <input
              type="email"
              name="pp-register-email"
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
              name="pp-register-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="En az 6 karakter"
              autoComplete="off"
            />
          </label>

          <label className="auth-field">
            <span>Şifre (tekrar)</span>

            <input
              type="password"
              name="pp-register-password-confirm"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
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
            {isSubmitting ? "Kayıt olunuyor..." : "Kayıt Ol"}
          </button>
        </form>

        <p className="auth-switch">
          Zaten bir hesabın var mı?{" "}
          <button
            type="button"
            className="auth-switch-button"
            onClick={onSwitchToLogin}
          >
            Giriş yap
          </button>
        </p>
      </div>
    </div>
  );
}

export default Register;
