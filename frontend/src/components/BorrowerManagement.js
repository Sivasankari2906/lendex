import React, { useState, useEffect } from 'react';
import API from '../api';

export default function BorrowerManagement() {
  const [borrowers, setBorrowers] = useState([]);
  const [selectedBorrower, setSelectedBorrower] = useState(null);
  const [editForm, setEditForm] = useState({ name: '', phone: '' });

  useEffect(() => {
    loadBorrowers();
  }, []);

  const loadBorrowers = async () => {
    try {
      const { data } = await API.get('/borrowers');
      setBorrowers(data);
    } catch (err) {
      console.error('Failed to load borrowers');
    }
  };

  const handleEdit = (borrower) => {
    setSelectedBorrower(borrower);
    setEditForm({ name: borrower.name, phone: borrower.phone });
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      await API.put(`/borrowers/${selectedBorrower.id}`, editForm);
      loadBorrowers();
      setSelectedBorrower(null);
      alert('Borrower updated successfully');
    } catch (err) {
      alert('Failed to update borrower');
    }
  };

  const handleDelete = async (borrowerId) => {
    if (window.confirm('Are you sure you want to delete this borrower and all their loans?')) {
      try {
        await API.delete(`/borrowers/${borrowerId}`);
        loadBorrowers();
        alert('Borrower deleted successfully');
      } catch (err) {
        alert('Failed to delete borrower');
      }
    }
  };

  return (
    <div>
      <h2>Borrower Management</h2>
      <div className="loans-grid">
        {borrowers.map(borrower => (
          <div key={borrower.id} className="loan-card">
            <h4>{borrower.name}</h4>
            <div className="loan-info">
              <div>Phone: {borrower.phone}</div>
              <div>Loans: {borrower.loanCount || 0}</div>
            </div>
            <div className="loan-actions">
              <button className="btn-small btn-edit" onClick={() => handleEdit(borrower)}>
                Edit
              </button>
              <button className="btn-small btn-delete" onClick={() => handleDelete(borrower.id)}>
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>

      {selectedBorrower && (
        <div className="modal-overlay" onClick={() => setSelectedBorrower(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Edit Borrower</h3>
            <form onSubmit={handleUpdate}>
              <div className="form-group">
                <label className="form-label">Name</label>
                <input
                  type="text"
                  className="form-input"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label">Phone</label>
                <input
                  type="tel"
                  className="form-input"
                  value={editForm.phone}
                  onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                  required
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setSelectedBorrower(null)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Update
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}