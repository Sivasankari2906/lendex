import React, { useState, useEffect } from 'react';
import Dashboard from './components/Dashboard';
import Login from './components/Login';
import Register from './components/Register';
import Header from './components/Header';
import './styles/App.css';

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [showRegister, setShowRegister] = useState(false);
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
    return (
      <div className="app-container">
        {showRegister ? (
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
            <Login onLogin={handleAuth} />
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
