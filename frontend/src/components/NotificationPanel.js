import React, { useState, useEffect } from 'react';
import API from '../api';

export default function NotificationPanel({ show, onClose, reminders, emiReminders = [] }) {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    if (show) {
      loadNotifications();
    }
  }, [show]);

  const loadNotifications = async () => {
    try {
      const { data } = await API.get('/notifications');
      setNotifications(data);
    } catch (err) {
      console.error('Failed to load notifications');
      setNotifications([]);
    }
  };

  if (!show) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Notifications</h3>
        <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
          {notifications.length > 0 && (
            <div>
              <h4>Recent Notifications ({notifications.length})</h4>
              {notifications.map(notification => (
                <div key={notification.id} style={{
                  padding: '12px',
                  border: '1px solid #007bff',
                  borderRadius: '6px',
                  marginBottom: '10px',
                  backgroundColor: '#f8f9ff'
                }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                    {notification.loan ? `Loan: ${notification.loan.borrower?.name}` : 
                     notification.emi ? `EMI: ${notification.emi.borrowerName}` : 'System'}
                  </div>
                  <div style={{ marginBottom: '8px' }}>{notification.message}</div>
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    Sent: {new Date(notification.sentDate).toLocaleDateString()}
                    {notification.daysPastDue > 0 && ` • ${notification.daysPastDue} days overdue`}
                  </div>
                </div>
              ))}
            </div>
          )}
          
          <h4>Overdue Loan Payments ({reminders?.length || 0})</h4>
          {reminders && reminders.length === 0 ? (
            <p>No overdue loan payments</p>
          ) : (
            reminders && reminders.map(loan => {
              // This will be updated by backend to show actual unpaid months
              const trackingStart = new Date(loan.trackingStartDate || loan.issuedDate);
              const today = new Date();
              const monthsFromStart = Math.floor((today - trackingStart) / (1000 * 60 * 60 * 24 * 30));
              const monthlyInterest = (loan.principal * loan.monthlyInterestRate / 100).toFixed(2);
              
              return (
                <div key={loan.id} style={{ 
                  padding: '15px', 
                  border: '1px solid #dc3545', 
                  borderRadius: '4px', 
                  marginBottom: '10px',
                  backgroundColor: '#f8d7da'
                }}>
                  <div style={{ fontWeight: 'bold', fontSize: '16px', marginBottom: '8px' }}>
                    {loan.borrower?.name}
                  </div>
                  <div style={{ marginBottom: '5px' }}>Phone: {loan.borrower?.phone}</div>
                  <div style={{ marginBottom: '5px' }}>Overdue: {monthsFromStart} month(s)</div>
                  <div style={{ marginBottom: '5px' }}>Monthly Interest: ₹{monthlyInterest}</div>
                  <div style={{ fontWeight: 'bold', color: '#dc3545' }}>
                    Total Overdue: ₹{(monthsFromStart * monthlyInterest).toFixed(2)}
                  </div>
                  <small style={{ color: '#666' }}>
                    Since: {trackingStart.toLocaleDateString()}
                  </small>
                </div>
              );
            })
          )}
          
          <h4>Overdue EMI Payments ({emiReminders.length})</h4>
          {emiReminders.length === 0 ? (
            <p>No overdue EMI payments</p>
          ) : (
            emiReminders.map(emi => (
              <div key={emi.id} style={{ 
                padding: '15px', 
                border: '1px solid #fd7e14', 
                borderRadius: '4px', 
                marginBottom: '10px',
                backgroundColor: '#fff3cd'
              }}>
                <div style={{ fontWeight: 'bold', fontSize: '16px', marginBottom: '8px' }}>
                  {emi.borrowerName}
                </div>
                <div style={{ marginBottom: '5px' }}>Total Amount: ₹{emi.totalAmount}</div>
                <div style={{ marginBottom: '5px' }}>Monthly EMI: ₹{emi.emiAmount}</div>
                <div style={{ marginBottom: '5px' }}>Tenure: {emi.tenure} months</div>
                <div style={{ fontWeight: 'bold', color: '#fd7e14' }}>
                  EMI Collection Overdue
                </div>
                <small style={{ color: '#666' }}>
                  Start Date: {new Date(emi.startDate).toLocaleDateString()}
                </small>
              </div>
            ))
          )}
        </div>
        <div className="form-actions">
          <button className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}