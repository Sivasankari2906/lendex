import React, { useState } from 'react';
import API from '../api';

export default function EMIForm({ emi, onClose, onEmiAdded }) {
  const [form, setForm] = useState({
    borrowerName: emi?.borrowerName || '',
    totalAmount: emi?.totalAmount || '',
    givenInCash: emi?.givenInCash || '',
    givenDate: emi?.givenDate || '',
    tenure: emi?.tenure || '',
    emiAmount: emi?.emiAmount || '',
    startDate: emi?.startDate || ''
  });

  const handleSave = async () => {
    if (!form.borrowerName || !form.totalAmount || !form.givenInCash || !form.tenure || !form.emiAmount) {
      alert('Please fill all required fields');
      return;
    }

    try {
      if (emi) {
        await API.put(`/emis/${emi.id}`, {
          borrowerName: form.borrowerName,
          totalAmount: form.totalAmount,
          givenInCash: form.givenInCash,
          givenDate: form.givenDate,
          tenure: form.tenure,
          emiAmount: form.emiAmount,
          startDate: form.startDate
        });
      } else {
        await API.post('/emis', {
          borrowerName: form.borrowerName,
          totalAmount: form.totalAmount,
          givenInCash: form.givenInCash,
          givenDate: form.givenDate,
          tenure: form.tenure,
          emiAmount: form.emiAmount,
          startDate: form.startDate
        });
      }

      onEmiAdded();
      onClose();
    } catch (err) {
      alert(`Failed to ${emi ? 'update' : 'create'} EMI`);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>{emi ? 'Edit EMI' : 'Add EMI'}</h3>
        
        <div className="form-group">
          <label>Borrower Name</label>
          <input
            type="text"
            value={form.borrowerName}
            onChange={(e) => setForm({ ...form, borrowerName: e.target.value })}
            placeholder="e.g., Vinoth Axis"
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>Total Amount (₹)</label>
          <input
            type="number"
            value={form.totalAmount}
            onChange={(e) => setForm({ ...form, totalAmount: e.target.value })}
            placeholder="Enter total amount"
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>Given in Cash (₹)</label>
          <input
            type="number"
            value={form.givenInCash}
            onChange={(e) => setForm({ ...form, givenInCash: e.target.value })}
            placeholder="Enter cash given"
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>Given Date</label>
          <input
            type="date"
            value={form.givenDate}
            onChange={(e) => setForm({ ...form, givenDate: e.target.value })}
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>Tenure (months)</label>
          <input
            type="number"
            value={form.tenure}
            onChange={(e) => setForm({ ...form, tenure: e.target.value })}
            placeholder="Enter tenure in months"
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>EMI Amount (₹)</label>
          <input
            type="number"
            value={form.emiAmount}
            onChange={(e) => setForm({ ...form, emiAmount: e.target.value })}
            placeholder="Enter EMI amount"
            className="form-input"
            required
          />
        </div>

        <div className="form-group">
          <label>Start Date</label>
          <input
            type="date"
            value={form.startDate}
            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
            className="form-input"
            required
          />
        </div>

        <div className="form-actions">
          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" onClick={handleSave}>
            {emi ? 'Update EMI' : 'Save EMI'}
          </button>
        </div>
      </div>
    </div>
  );
}