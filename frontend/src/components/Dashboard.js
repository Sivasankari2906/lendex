import React, { useEffect, useState } from 'react';
import API from '../api';
import LoanCard from './LoanCard';
import LoanForm from './LoanForm';
import DashboardStats from './DashboardStats';
import PaymentsPage from './PaymentsPage';
import NotificationPanel from './NotificationPanel';
import BorrowerManagement from './BorrowerManagement';
import UserProfile from './UserProfile';
import SettingsPage from './SettingsPage';
import '../styles/Dashboard.css';

function Dashboard({ currentView }){
  const [loans, setLoans] = useState([]);
  const [reminders, setReminders] = useState([]);
  const [showLoanForm, setShowLoanForm] = useState(false);
  const [selectedLoan, setSelectedLoan] = useState(null);
  const [activeTab, setActiveTab] = useState('active');
  const [showNotifications, setShowNotifications] = useState(false);
  const [showUserProfile, setShowUserProfile] = useState(false);

  const load = async () => {
    try {
      const { data } = await API.get('/loans');
      setLoans(data);
    } catch (err) {
      console.error('Failed to load loans:', err);
    }
  };

  const loadReminders = async () => {
    try {
      const { data } = await API.get('/reminders');
      setReminders(data);
    } catch (err) {
      console.error('Failed to load reminders:', err);
      setReminders([]);
    }
  };

  useEffect(() => {
    load();
    loadReminders();
    requestNotificationPermission();
    const interval = setInterval(() => {
      loadReminders();
    }, 15000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (reminders.length > 0) {
      checkForNewOverduePayments();
    }
  }, [reminders]);

  const requestNotificationPermission = async () => {
    if ('Notification' in window && Notification.permission === 'default') {
      await Notification.requestPermission();
    }
  };

  const checkForNewOverduePayments = () => {
    if (reminders.length > 0) {
      if ('Notification' in window && Notification.permission === 'granted') {
        reminders.forEach(async (loan, index) => {
          try {
            // Get actual payment data from backend
            const { data: payments } = await API.get(`/loans/${loan.id}/payments`);
            const trackingStart = new Date(loan.trackingStartDate || loan.issuedDate);
            const today = new Date();
            
            // Count unpaid months by checking payment records
            let currentMonth = new Date(trackingStart);
            let unpaidMonths = 0;
            const monthlyInterest = loan.principal * loan.monthlyInterestRate / 100;
            
            while (currentMonth < today) {
              const monthKey = currentMonth.toISOString().slice(0, 7); // YYYY-MM format
              const monthPayments = payments.filter(payment => 
                payment.date.startsWith(monthKey)
              );
              const totalPaid = monthPayments.reduce((sum, p) => sum + parseFloat(p.amount), 0);
              
              if (totalPaid < (monthlyInterest - 0.01)) { // Small tolerance for rounding
                unpaidMonths++;
              }
              
              currentMonth.setMonth(currentMonth.getMonth() + 1);
            }
            
            if (unpaidMonths > 0) {
              // Calculate actual overdue amount considering partial payments
              let totalOverdue = 0;
              let currentMonth2 = new Date(trackingStart);
              
              while (currentMonth2 < today) {
                const monthKey = currentMonth2.toISOString().slice(0, 7);
                const monthPayments = payments.filter(payment => 
                  payment.date.startsWith(monthKey)
                );
                const totalPaid = monthPayments.reduce((sum, p) => sum + parseFloat(p.amount), 0);
                
                if (totalPaid < (monthlyInterest - 0.01)) { // Small tolerance
                  totalOverdue += Math.max(0, monthlyInterest - totalPaid);
                }
                
                currentMonth2.setMonth(currentMonth2.getMonth() + 1);
              }
            
              setTimeout(() => {
                const notification = new Notification(`Lendex - Collect from ${loan.borrower?.name}`, {
                  body: `${unpaidMonths} month(s) overdue - Amount: ₹${totalOverdue.toFixed(0)}\nPhone: ${loan.borrower?.phone}`,
                  icon: '/favicon.ico',
                  tag: `overdue-loan-${loan.id}`,
                  requireInteraction: true
                });
                
                notification.onclick = () => {
                  window.focus();
                  setShowNotifications(true);
                  notification.close();
                };
              }, index * 2000);
            }
          } catch (err) {
            console.error('Failed to load payment data for notifications');
          }
        });
      }
    }
  };

  const handleLoanAdded = () => {
    load();
    loadReminders();
  };

  const handlePayments = (loan) => {
    setSelectedLoan(loan);
  };

  const handlePaymentUpdate = () => {
    load();
    loadReminders();
  };

  if (selectedLoan) {
    return (
      <PaymentsPage 
        loan={selectedLoan} 
        onBack={() => {
          setSelectedLoan(null);
          handlePaymentUpdate();
        }}
      />
    );
  }

  const renderContent = () => {
    switch(currentView) {
      case 'loans':
        const activeLoans = loans.filter(l => !l.repaid);
        const closedLoans = loans.filter(l => l.repaid);
        const displayLoans = activeTab === 'active' ? activeLoans : closedLoans;
        
        return (
          <div>
            <DashboardStats loans={loans} reminders={reminders} />
            <div className="loans-tabs">
              <button 
                className={`tab-button ${activeTab === 'active' ? 'active' : ''}`}
                onClick={() => setActiveTab('active')}
              >
                Active Loans ({activeLoans.length})
              </button>
              <button 
                className={`tab-button ${activeTab === 'closed' ? 'active' : ''}`}
                onClick={() => setActiveTab('closed')}
              >
                Closed Loans ({closedLoans.length})
              </button>
            </div>
            <div className="loans-grid">
              {displayLoans.map(l => <LoanCard key={l.id} loan={l} onUpdate={load} onPayments={handlePayments} />)}
            </div>
            {displayLoans.length === 0 && <p>No {activeTab} loans found.</p>}
          </div>
        );
      case 'borrowers':
        return <BorrowerManagement />;
      case 'settings':
        return <SettingsPage />;
      default:
        return (
          <div>
            <h2>Dashboard</h2>
            <DashboardStats loans={loans} reminders={reminders} />
            <div className="dashboard-section">
              <h3>Reminders ({reminders.length})</h3>
              {reminders.length === 0 ? <p>No overdue payments</p> : reminders.map(l => 
                <div key={l.id} className="reminder-item">
                  <strong>{l.borrower?.name || 'Unknown'}</strong> — due {new Date(l.nextDueDate).toLocaleDateString()} — loan ₹{l.principal}
                </div>
              )}
            </div>
            <div className="dashboard-section">
              <h3>Recent Loans</h3>
              <div className="loans-grid">
                {loans.slice(0, 6).map(l => <LoanCard key={l.id} loan={l} onUpdate={load} onPayments={handlePayments} />)}
              </div>
            </div>
          </div>
        );
    }
  };

  return (
    <div className="content">
      {renderContent()}
      <button 
        className="notification-bell"
        onClick={() => setShowNotifications(!showNotifications)}
        title={`${reminders.length} overdue payments`}
      >
        🔔
        {reminders.length > 0 && (
          <span className="notification-badge">{reminders.length}</span>
        )}
      </button>
      {currentView === 'loans' && activeTab === 'active' && (
        <button 
          className="add-loan-btn" 
          onClick={() => setShowLoanForm(true)}
          title="Add New Loan"
        >
          +
        </button>
      )}
      {showLoanForm && (
        <LoanForm 
          onClose={() => setShowLoanForm(false)} 
          onLoanAdded={handleLoanAdded}
        />
      )}
      <NotificationPanel 
        show={showNotifications}
        onClose={() => setShowNotifications(false)}
        reminders={reminders}
      />
      {showUserProfile && (
        <UserProfile onClose={() => setShowUserProfile(false)} />
      )}
    </div>
  );
}

export default Dashboard;
