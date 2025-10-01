import React, { useState, useEffect } from 'react';
import API from '../api';

export default function EditLoanForm({ loan, onClose, onLoanUpdated }) {
  const [form, setForm] = useState({
    principal: '',
    monthlyInterestRate: ''
  });
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Load current loan data using the unused API
    loadLoanData();
  }, []);

  const loadLoanData = async () => {
    try {
      const { data } = await API.get(`/loans/${loan.id}`);
      setForm({
        principal: data.principal,
        monthlyInterestRate: data.monthlyInterestRate
      });
    } catch (err) {
      console.error('Failed to load loan data');
      // Fallback to passed loan data
      setForm({
        principal: loan.principal,
        monthlyInterestRate: loan.monthlyInterestRate
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      await API.put(`/loans/${loan.id}`, form);
      onLoanUpdated();
      onClose();
      alert('Loan updated successfully');
    } catch (err) {
      alert('Failed to update loan');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Edit Loan - {loan.borrower?.name}</h3>
        <form onSubmit={handleSubmit}>
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
          <div className="form-actions">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Updating...' : 'Update Loan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}