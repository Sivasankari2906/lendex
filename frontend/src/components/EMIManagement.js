import React, { useState, useEffect } from 'react';
import EMIForm from './EMIForm';
import EMICalculator from './EMICalculator';
import EMIPaymentsPage from './EMIPaymentsPage';
import API from '../api';
import '../styles/Dashboard.css';

export default function EMIManagement() {
  const [emis, setEmis] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [showCalculator, setShowCalculator] = useState(false);
  const [selectedEmi, setSelectedEmi] = useState(null);
  const [editingEmi, setEditingEmi] = useState(null);
  const [activeTab, setActiveTab] = useState('active');

  useEffect(() => {
    loadEmis();
  }, []);

  if (selectedEmi) {
    return (
      <EMIPaymentsPage 
        emi={selectedEmi} 
        onBack={() => setSelectedEmi(null)}
      />
    );
  }

  const loadEmis = async () => {
    try {
      const { data } = await API.get('/emis');
      setEmis(data);
    } catch (err) {
      console.error('Failed to load EMIs:', err);
    }
  };

  const handleEmiAdded = () => {
    loadEmis();
  };

  const handlePayments = (emi) => {
    setSelectedEmi(emi);
  };

  const handleEdit = (emi) => {
    setEditingEmi(emi);
    setShowForm(true);
  };

  const handleDelete = async (emiId) => {
    if (window.confirm('Are you sure you want to delete this EMI?')) {
      try {
        await API.delete(`/emis/${emiId}`);
        loadEmis();
      } catch (err) {
        alert('Failed to delete EMI');
      }
    }
  };

  const activeEmis = emis.filter(e => !e.completed);
  const completedEmis = emis.filter(e => e.completed);
  const displayEmis = activeTab === 'active' ? activeEmis : completedEmis;

  return (
    <div>
      <h2>EMI Management</h2>
      
      <div className="loans-tabs">
        <button 
          className={`tab-button ${activeTab === 'active' ? 'active' : ''}`}
          onClick={() => setActiveTab('active')}
        >
          Active EMIs ({activeEmis.length})
        </button>
        <button 
          className={`tab-button ${activeTab === 'completed' ? 'active' : ''}`}
          onClick={() => setActiveTab('completed')}
        >
          Completed EMIs ({completedEmis.length})
        </button>
      </div>

      <div className="loans-grid">
        {displayEmis.map(emi => (
          <div key={emi.id} className="loan-card">
            <h4>{emi.borrowerName}</h4>
            <div className="loan-info">
              <div>Total Amount: ₹{emi.totalAmount}</div>
              <div>Given in Cash: ₹{emi.givenInCash}</div>
              <div>Given Date: {new Date(emi.givenDate).toLocaleDateString()}</div>
              <div>Tenure: {emi.tenure} months</div>
              <div>EMI: ₹{emi.emiAmount}</div>
              <div>Start Date: {new Date(emi.startDate).toLocaleDateString()}</div>
            </div>
            <div className="loan-actions">
              <button 
                className="btn-small btn-edit" 
                onClick={() => handleEdit(emi)}
              >
                Edit
              </button>
              <button 
                className="btn-small btn-primary" 
                onClick={() => handlePayments(emi)}
              >
                Payments
              </button>
              <button 
                className="btn-small btn-delete" 
                onClick={() => handleDelete(emi.id)}
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
      
      {displayEmis.length === 0 && <p>No {activeTab} EMIs found.</p>}

      <button 
        className="add-loan-btn" 
        onClick={() => setShowForm(true)}
        title="Add New EMI"
      >
        +
      </button>

      <button 
        className="notification-bell"
        onClick={() => setShowCalculator(true)}
        title="EMI Calculator"
        style={{ right: '80px' }}
      >
        🧮
      </button>

      {showForm && (
        <EMIForm 
          emi={editingEmi}
          onClose={() => {
            setShowForm(false);
            setEditingEmi(null);
          }}
          onEmiAdded={handleEmiAdded}
        />
      )}

      {showCalculator && (
        <EMICalculator 
          onClose={() => setShowCalculator(false)}
        />
      )}
    </div>
  );
}