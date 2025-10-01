import React, { useState } from 'react';
import EditLoanForm from './EditLoanForm';
import API from '../api';

export default function LoanCard({ loan, onUpdate, onPayments }) {
  const [showEditForm, setShowEditForm] = useState(false);
  const [showPostponeInput, setShowPostponeInput] = useState(false);
  const [postponeDays, setPostponeDays] = useState('');
  const isOverdue = new Date(loan.nextDueDate) < new Date() && !loan.repaid;

  const handleDelete = async () => {
    if (window.confirm('Are you sure you want to delete this loan?')) {
      try {
        await API.delete(`/loans/${loan.id}`);
        onUpdate();
      } catch (err) {
        alert('Failed to delete loan');
      }
    }
  };

  const handleClose = async () => {
    if (window.confirm('Are you sure you want to close this loan?')) {
      try {
        await API.post(`/loans/${loan.id}/close`);
        onUpdate();
      } catch (err) {
        alert('Failed to close loan');
      }
    }
  };

  const handlePostpone = () => {
    setShowPostponeInput(true);
  };

  const submitPostpone = async () => {
    if (postponeDays && !isNaN(postponeDays) && parseInt(postponeDays) > 0) {
      try {
        await API.post(`/loans/${loan.id}/postpone`, {
          days: parseInt(postponeDays)
        });
        onUpdate();
        setShowPostponeInput(false);
        setPostponeDays('');
        alert(`Notification postponed for ${postponeDays} days`);
      } catch (err) {
        alert('Failed to postpone notification');
      }
    }
  };

  const cancelPostpone = () => {
    setShowPostponeInput(false);
    setPostponeDays('');
  };
  
  return (
    <div className={`loan-card ${loan.repaid ? 'closed-loan-card' : ''}`}>
      {!loan.repaid && (
        <button className="close-button" onClick={handleClose} title="Close Loan">
          × Close
        </button>
      )}
      <h4>{loan.borrower?.name || 'Unknown Borrower'}</h4>
      <div className="loan-info">
        <div className="loan-amount">₹{loan.principal}</div>
        <div>Interest: {loan.monthlyInterestRate}% monthly</div>
        <div>Issued: {new Date(loan.issuedDate).toLocaleDateString()}</div>
        <div>Next Due: {new Date(loan.nextDueDate).toLocaleDateString()}</div>
        <div>Remaining: ₹{loan.remainingPrincipal}</div>
        <div>Phone: {loan.borrower?.phone || 'N/A'}</div>
      </div>
      <div className={`loan-status ${loan.repaid ? 'status-active' : isOverdue ? 'status-overdue' : 'status-active'}`}>
        {loan.repaid ? 'CLOSED' : isOverdue ? 'OVERDUE' : 'ACTIVE'}
      </div>
      {!loan.repaid && (
        <div className="loan-actions">
          <button className="btn-small btn-payments" onClick={() => onPayments && onPayments(loan)}>
            Payments
          </button>
          <button className="btn-small btn-postpone" onClick={handlePostpone}>
            Postpone
          </button>
          <button className="btn-small btn-edit" onClick={() => setShowEditForm(true)}>
            Edit
          </button>
          <button className="btn-small btn-delete" onClick={handleDelete}>
            Delete
          </button>
        </div>
      )}
      {showPostponeInput && (
        <div className="postpone-input">
          <span>Days:</span>
          <input
            type="number"
            value={postponeDays}
            onChange={(e) => setPostponeDays(e.target.value)}
            placeholder="7"
            min="1"
          />
          <button className="btn-small btn-primary" onClick={submitPostpone}>
            Confirm
          </button>
          <button className="btn-small btn-secondary" onClick={cancelPostpone}>
            Cancel
          </button>
        </div>
      )}
      {showEditForm && (
        <EditLoanForm 
          loan={loan}
          onClose={() => setShowEditForm(false)}
          onLoanUpdated={onUpdate}
        />
      )}
    </div>
  );
}