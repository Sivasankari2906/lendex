import React, { useState, useEffect } from 'react';
import API from '../api';

export default function SettingsPage() {
  const [profile, setProfile] = useState({ username: '', fullName: '', email: '', phone: '' });
  const [preferences, setPreferences] = useState({
    notificationsEnabled: true,
    defaultInterestRate: 2.0,
    currency: 'INR'
  });
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(false);
  const [activeSection, setActiveSection] = useState('profile');

  useEffect(() => {
    loadProfile();
    loadStats();
    loadPreferences();
  }, []);

  const loadProfile = async () => {
    try {
      const { data } = await API.get('/user/profile');
      setProfile(data);
    } catch (err) {
      console.error('Failed to load profile');
    }
  };

  const loadStats = async () => {
    try {
      const { data: loans } = await API.get('/loans');
      const { data: borrowers } = await API.get('/borrowers');
      const { data: reminders } = await API.get('/reminders');
      
      const totalPrincipal = loans.reduce((sum, loan) => sum + parseFloat(loan.principal), 0);
      const activeLoans = loans.filter(l => !l.repaid);
      const closedLoans = loans.filter(l => l.repaid);
      
      setStats({
        totalLoans: loans.length,
        activeLoans: activeLoans.length,
        closedLoans: closedLoans.length,
        totalBorrowers: borrowers.length,
        totalPrincipal: totalPrincipal,
        overduePayments: reminders.length
      });
    } catch (err) {
      console.error('Failed to load stats');
    }
  };

  const loadPreferences = () => {
    const saved = localStorage.getItem('lendex-preferences');
    if (saved) {
      setPreferences(JSON.parse(saved));
    }
  };

  const saveProfile = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await API.put('/user/profile', profile);
      alert('Profile updated successfully');
    } catch (err) {
      alert('Failed to update profile');
    } finally {
      setLoading(false);
    }
  };

  const savePreferences = () => {
    localStorage.setItem('lendex-preferences', JSON.stringify(preferences));
    alert('Preferences saved');
  };

  const exportData = async () => {
    try {
      const { data: loans } = await API.get('/loans');
      const { data: borrowers } = await API.get('/borrowers');
      
      const exportData = {
        loans,
        borrowers,
        exportDate: new Date().toISOString(),
        user: profile.username
      };
      
      const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `lendex-data-${new Date().toISOString().split('T')[0]}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      alert('Failed to export data');
    }
  };

  const clearNotificationPermission = () => {
    if ('Notification' in window) {
      alert('Please manually reset notification permissions in your browser settings');
    }
  };

  return (
    <div className="settings-page">
      <h2>Settings</h2>
      
      <div className="settings-nav">
        <button 
          className={`settings-nav-btn ${activeSection === 'profile' ? 'active' : ''}`}
          onClick={() => setActiveSection('profile')}
        >
          Profile
        </button>
        <button 
          className={`settings-nav-btn ${activeSection === 'preferences' ? 'active' : ''}`}
          onClick={() => setActiveSection('preferences')}
        >
          Preferences
        </button>
        <button 
          className={`settings-nav-btn ${activeSection === 'data' ? 'active' : ''}`}
          onClick={() => setActiveSection('data')}
        >
          Data & Privacy
        </button>
        <button 
          className={`settings-nav-btn ${activeSection === 'about' ? 'active' : ''}`}
          onClick={() => setActiveSection('about')}
        >
          About
        </button>
      </div>

      <div className="settings-content">
        {activeSection === 'profile' && (
          <div className="settings-section">
            <h3>Profile Information</h3>
            <form onSubmit={saveProfile} className="profile-form">
              <div className="form-group">
                <label>Username</label>
                <input
                  type="text"
                  value={profile.username}
                  disabled
                  className="form-input disabled"
                />
                <small>Username cannot be changed</small>
              </div>
              
              <div className="form-group">
                <label>Full Name</label>
                <input
                  type="text"
                  value={profile.fullName || ''}
                  onChange={(e) => setProfile({...profile, fullName: e.target.value})}
                  className="form-input"
                  placeholder="Enter your full name"
                />
              </div>
              
              <div className="form-group">
                <label>Email</label>
                <input
                  type="email"
                  value={profile.email || ''}
                  onChange={(e) => setProfile({...profile, email: e.target.value})}
                  className="form-input"
                  placeholder="Enter your email"
                />
              </div>
              
              <div className="form-group">
                <label>Phone</label>
                <input
                  type="tel"
                  value={profile.phone || ''}
                  onChange={(e) => setProfile({...profile, phone: e.target.value})}
                  className="form-input"
                  placeholder="Enter your phone number"
                />
              </div>
              
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Updating...' : 'Update Profile'}
              </button>
            </form>
          </div>
        )}

        {activeSection === 'preferences' && (
          <div className="settings-section">
            <h3>App Preferences</h3>
            
            <div className="preference-group">
              <h4>Notifications</h4>
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={preferences.notificationsEnabled}
                  onChange={(e) => setPreferences({...preferences, notificationsEnabled: e.target.checked})}
                />
                Enable browser notifications
              </label>
              <button className="btn btn-secondary btn-small" onClick={clearNotificationPermission}>
                Reset Notification Permissions
              </button>
            </div>
            
            <div className="preference-group">
              <h4>Default Settings</h4>
              <div className="form-group">
                <label>Default Interest Rate (%)</label>
                <input
                  type="number"
                  step="0.1"
                  value={preferences.defaultInterestRate}
                  onChange={(e) => setPreferences({...preferences, defaultInterestRate: parseFloat(e.target.value)})}
                  className="form-input"
                />
              </div>
              
              <div className="form-group">
                <label>Currency</label>
                <select
                  value={preferences.currency}
                  onChange={(e) => setPreferences({...preferences, currency: e.target.value})}
                  className="form-input"
                >
                  <option value="INR">Indian Rupee (₹)</option>
                  <option value="USD">US Dollar ($)</option>
                  <option value="EUR">Euro (€)</option>
                </select>
              </div>
            </div>
            
            <button className="btn btn-primary" onClick={savePreferences}>
              Save Preferences
            </button>
          </div>
        )}

        {activeSection === 'data' && (
          <div className="settings-section">
            <h3>Data & Privacy</h3>
            
            <div className="stats-grid">
              <div className="stat-card">
                <h4>Your Data Summary</h4>
                <div className="stat-item">Total Loans: {stats.totalLoans}</div>
                <div className="stat-item">Active Loans: {stats.activeLoans}</div>
                <div className="stat-item">Closed Loans: {stats.closedLoans}</div>
                <div className="stat-item">Total Borrowers: {stats.totalBorrowers}</div>
                <div className="stat-item">Total Principal: ₹{stats.totalPrincipal?.toFixed(2)}</div>
                <div className="stat-item">Overdue Payments: {stats.overduePayments}</div>
              </div>
            </div>
            
            <div className="data-actions">
              <h4>Data Management</h4>
              <button className="btn btn-secondary" onClick={exportData}>
                Export All Data (JSON)
              </button>
              <p className="help-text">
                Export your loans, borrowers, and payment data for backup or migration purposes.
              </p>
            </div>
            
            <div className="privacy-info">
              <h4>Privacy Information</h4>
              <p>• Your data is stored locally and securely</p>
              <p>• No data is shared with third parties</p>
              <p>• You can export or delete your data anytime</p>
              <p>• All communications are encrypted</p>
            </div>
          </div>
        )}

        {activeSection === 'about' && (
          <div className="settings-section">
            <h3>About Lendex</h3>
            
            <div className="about-info">
              <div className="app-info">
                <h4>Lendex v1.0</h4>
                <p>Personal Loan Management System</p>
                <p>Built with React & Spring Boot</p>
              </div>
              
              <div className="features-list">
                <h4>Features</h4>
                <ul>
                  <li>✓ Loan tracking and management</li>
                  <li>✓ Payment scheduling and recording</li>
                  <li>✓ Borrower contact management</li>
                  <li>✓ Overdue payment notifications</li>
                  <li>✓ Interest calculations</li>
                  <li>✓ Data export and backup</li>
                </ul>
              </div>
              
              <div className="support-info">
                <h4>Support</h4>
                <p>For technical support or feature requests:</p>
                <p>Email: support@lendex.app</p>
                <p>Version: 1.0.0</p>
                <p>Last Updated: December 2024</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}