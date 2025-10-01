import React, { useState } from 'react';
import API from '../api';
import '../styles/Register.css';

export default function Register({ onRegister }) {
  const [form, setForm] = useState({ username: '', password: '', fullName: '', email: '', phone: '' });
  const [error, setError] = useState('');
  const [otp, setOtp] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [loading, setLoading] = useState(false);

  const sendOtp = async () => {
    if (!form.email || !form.email.includes('@')) {
      setError('Please enter a valid email address first');
      return;
    }
    
    if (!form.phone || form.phone.length < 10) {
      setError('Please enter a valid phone number');
      return;
    }
    
    setLoading(true);
    try {
      await API.post('/auth/send-otp', { email: form.email });
      setOtpSent(true);
      setError('');
    } catch (err) {
      setError('Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async () => {
    if (!otp || otp.length !== 6) {
      setError('Please enter a valid 6-digit OTP');
      return;
    }
    
    setLoading(true);
    try {
      const { data } = await API.post('/auth/verify-otp', { email: form.email, otp });
      if (data.valid) {
        setPhoneVerified(true);
        setError('');
      } else {
        setError('Invalid OTP');
      }
    } catch (err) {
      setError('OTP verification failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (form.phone && !phoneVerified) {
      setError('Please verify your phone number first');
      return;
    }
    
    try {
      const { data } = await API.post('/auth/register', form);
      localStorage.setItem('token', data.token);
      onRegister(data.token);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    }
  };

  return (
    <div className="register-container">
      <div className="register-form">
        <h2>Register</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <input
              type="text"
              placeholder="Username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="form-input"
              required
            />
          </div>
          <div className="form-group">
            <input
              type="password"
              placeholder="Password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="form-input"
              required
            />
          </div>
          <div className="form-group">
            <input
              type="text"
              placeholder="Full Name (optional)"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              className="form-input"
            />
          </div>
          <div className="form-group">
            <input
              type="email"
              placeholder="Email (optional)"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="form-input"
            />
          </div>
          <div className="form-group">
            <div className="phone-input-group">
              <input
                type="tel"
                placeholder="Phone (optional)"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                className="form-input"
                disabled={phoneVerified}
              />
              {form.phone && form.email && !phoneVerified && (
                <button
                  type="button"
                  onClick={sendOtp}
                  className="btn btn-sm btn-secondary"
                  disabled={loading}
                >
                  {loading ? 'Sending...' : 'Verify Phone'}
                </button>
              )}
              {phoneVerified && (
                <span className="verification-badge">✓ Verified</span>
              )}
            </div>
          </div>
          {otpSent && !phoneVerified && (
            <div className="form-group">
              <div className="otp-input-group">
                <input
                  type="text"
                  placeholder="Enter 6-digit OTP sent to your email"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  className="form-input"
                  maxLength="6"
                />
                <button
                  type="button"
                  onClick={verifyOtp}
                  className="btn btn-sm btn-success"
                  disabled={loading}
                >
                  {loading ? 'Verifying...' : 'Verify'}
                </button>
              </div>
              <small className="help-text">Check your email for the verification code</small>
            </div>
          )}
          {error && <div className="register-error">{error}</div>}
          <button type="submit" className="register-button">
            Register
          </button>
        </form>
      </div>
    </div>
  );
}