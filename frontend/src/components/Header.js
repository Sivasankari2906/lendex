import React, { useState } from 'react';
import '../styles/Header.css';

export default function Header({ currentUser, onLogout, onNavigate }) {
  const [showDropdown, setShowDropdown] = useState(false);

  return (
    <header className="header">
      <div className="logo">Lendex</div>
      <div className="nav-container">
        <nav className="nav-links">
          <a href="#" className="nav-link" onClick={() => onNavigate('loans')}>Loans</a>
          <a href="#" className="nav-link" onClick={() => onNavigate('borrowers')}>Borrowers</a>
          <a href="#" className="nav-link" onClick={() => onNavigate('emi')}>EMI</a>
        </nav>
        <div className="user-menu">
          <button 
            className="user-button" 
            onClick={() => setShowDropdown(!showDropdown)}
          >
            {currentUser} ▼
          </button>
          {showDropdown && (
            <div className="dropdown">
              <button className="dropdown-item" onClick={() => onNavigate('settings')}>
                Settings
              </button>
              <button className="dropdown-item" onClick={onLogout}>
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}