import React, { useState } from 'react';
import API from '../api';

export default function LoanForm({ onClose, onLoanAdded }) {
  const [form, setForm] = useState({
    borrowerName: '',
    borrowerPhone: '',
    principal: '',
    monthlyInterestRate: '',
    issuedDate: new Date().toISOString().split('T')[0],
    trackingStartDate: new Date().toISOString().split('T')[0],
    remarks: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // First create borrower
      const borrowerData = {
        name: form.borrowerName,
        phone: form.borrowerPhone
      };
      const { data: borrower } = await API.post('/borrowers', borrowerData);

      // Then create loan for the borrower
      const loanData = {
        principal: form.principal,
        monthlyInterestRate: form.monthlyInterestRate,
        issuedDate: form.issuedDate,
        trackingStartDate: form.trackingStartDate,
        remarks: form.remarks
      };
      await API.post(`/borrowers/${borrower.id}/loans`, loanData);

      onLoanAdded();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create loan');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Add New Loan</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Borrower Name</label>
              <input
                type="text"
                className="form-input"
                value={form.borrowerName}
                onChange={(e) => setForm({ ...form, borrowerName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Phone</label>
              <input
                type="tel"
                className="form-input"
                value={form.borrowerPhone}
                onChange={(e) => setForm({ ...form, borrowerPhone: e.target.value })}
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Principal Amount (₹)</label>
              <input
                type="number"
                className="form-input"
                value={form.principal}
                onChange={(e) => setForm({ ...form, principal: e.target.value })}
                required
                min="1"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Monthly Interest Rate (%)</label>
              <input
                type="number"
                className="form-input"
                value={form.monthlyInterestRate}
                onChange={(e) => setForm({ ...form, monthlyInterestRate: e.target.value })}
                required
                min="0"
                step="0.1"
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Issue Date</label>
              <input
                type="date"
                className="form-input"
                value={form.issuedDate}
                onChange={(e) => setForm({ ...form, issuedDate: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Start Tracking From</label>
              <input
                type="date"
                className="form-input"
                value={form.trackingStartDate}
                onChange={(e) => setForm({ ...form, trackingStartDate: e.target.value })}
                required
              />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Remarks</label>
            <textarea
              className="form-input"
              value={form.remarks}
              onChange={(e) => setForm({ ...form, remarks: e.target.value })}
              placeholder="Optional notes or remarks about this loan"
              rows="3"
            />
          </div>
          {error && <div className="error-message">{error}</div>}
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Creating...' : 'Create Loan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}