import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import Login from './components/Login';
import Register from './components/Register';
import ForgotPassword from './components/ForgotPassword';
import ResetPassword from './components/ResetPassword';
import Header from './components/Header';
import './styles/App.css';

function AuthPages() {
  const [showRegister, setShowRegister] = useState(false);
  const [showForgotPassword, setShowForgotPassword] = useState(false);

  const handleAuth = (newToken) => {
    localStorage.setItem('token', newToken);
    window.location.href = '/';
  };

  return (
    <div className="app-container">
      {showForgotPassword ? (
        <ForgotPassword onBack={() => setShowForgotPassword(false)} />
      ) : showRegister ? (
        <div>
          <Register onRegister={handleAuth} />
          <div className="auth-switch">
            Already have an account?{' '}
            <button onClick={() => setShowRegister(false)} className="auth-link">
              Login
            </button>
          </div>
        </div>
      ) : (
        <div>
          <Login 
            onLogin={handleAuth} 
            onForgotPassword={() => setShowForgotPassword(true)}
          />
          <div className="auth-switch">
            Don't have an account?{' '}
            <button onClick={() => setShowRegister(true)} className="auth-link">
              Register
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function MainApp() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [currentView, setCurrentView] = useState('dashboard');
  const [currentUser, setCurrentUser] = useState('User');

  const handleAuth = (newToken) => {
    setToken(newToken);
    try {
      const payload = JSON.parse(atob(newToken.split('.')[1]));
      setCurrentUser(payload.sub || 'User');
    } catch (e) {
      setCurrentUser('User');
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setCurrentUser('User');
    setCurrentView('dashboard');
  };

  const handleNavigate = (view) => {
    setCurrentView(view);
  };

  useEffect(() => {
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setCurrentUser(payload.sub || 'User');
      } catch (e) {
        setCurrentUser('User');
      }
    }
  }, [token]);

  if (!token) {
    return <Navigate to="/login" />;
  }

  return (
    <div className="app-container">
      <Header 
        currentUser={currentUser} 
        onLogout={logout} 
        onNavigate={handleNavigate}
      />
      <Dashboard currentView={currentView} />
    </div>
  );
}

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<AuthPages />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/" element={<MainApp />} />
      </Routes>
    </Router>
  );
}
