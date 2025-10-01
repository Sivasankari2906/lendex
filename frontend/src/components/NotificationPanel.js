import React, { useState, useEffect } from 'react';
import API from '../api';

export default function NotificationPanel({ show, onClose, reminders }) {
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
    }
  };

  if (!show) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Overdue Payments</h3>
        <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
          {reminders && reminders.length === 0 ? (
            <p>No overdue payments</p>
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