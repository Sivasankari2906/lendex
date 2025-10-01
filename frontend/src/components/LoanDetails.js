import React, { useState, useEffect } from 'react';
import API from '../api';

export default function LoanDetails({ loanId, onClose }) {
  const [loan, setLoan] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadLoanDetails();
  }, [loanId]);

  const loadLoanDetails = async () => {
    try {
      const { data } = await API.get(`/loans/${loanId}`);
      setLoan(data);
    } catch (err) {
      console.error('Failed to load loan details');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="modal-overlay">
        <div className="modal">
          <div>Loading loan details...</div>
        </div>
      </div>
    );
  }

  if (!loan) {
    return (
      <div className="modal-overlay">
        <div className="modal">
          <div>Failed to load loan details</div>
          <button onClick={onClose}>Close</button>
        </div>
      </div>
    );
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Loan Details</h3>
        <div className="loan-details-grid">
          <div className="detail-item">
            <strong>Borrower:</strong> {loan.borrower?.name || 'Unknown'}
          </div>
          <div className="detail-item">
            <strong>Phone:</strong> {loan.borrower?.phone || 'N/A'}
          </div>
          <div className="detail-item">
            <strong>Principal Amount:</strong> ₹{loan.principal}
          </div>
          <div className="detail-item">
            <strong>Remaining Principal:</strong> ₹{loan.remainingPrincipal}
          </div>
          <div className="detail-item">
            <strong>Monthly Interest Rate:</strong> {loan.monthlyInterestRate}%
          </div>
          <div className="detail-item">
            <strong>Monthly Interest Amount:</strong> ₹{(loan.principal * loan.monthlyInterestRate / 100).toFixed(2)}
          </div>
          <div className="detail-item">
            <strong>Issued Date:</strong> {new Date(loan.issuedDate).toLocaleDateString()}
          </div>
          <div className="detail-item">
            <strong>Tracking Start:</strong> {new Date(loan.trackingStartDate).toLocaleDateString()}
          </div>
          <div className="detail-item">
            <strong>Next Due Date:</strong> {new Date(loan.nextDueDate).toLocaleDateString()}
          </div>
          <div className="detail-item">
            <strong>Status:</strong> 
            <span className={`loan-status ${loan.repaid ? 'status-active' : 'status-overdue'}`}>
              {loan.repaid ? 'CLOSED' : 'ACTIVE'}
            </span>
          </div>
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