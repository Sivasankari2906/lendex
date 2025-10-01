import React, { useState } from 'react';

export default function EMICalculator({ onClose }) {
  const [form, setForm] = useState({
    totalAmount: '',
    tenure: '',
    rate: ''
  });
  const [calculatedEmi, setCalculatedEmi] = useState(null);



  const calculateEMI = () => {
    const P = parseFloat(form.totalAmount);
    const r = parseFloat(form.rate) / 12 / 100; // Monthly interest rate
    const n = parseInt(form.tenure);

    if (!P || !r || !n) {
      alert('Please fill total amount, rate, and tenure for calculation');
      return;
    }

    // EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
    const emi = (P * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    const totalAmount = emi * n;
    const totalInterest = totalAmount - P;

    setCalculatedEmi({
      emiAmount: Math.round(emi),
      totalAmount: Math.round(totalAmount),
      totalInterest: Math.round(totalInterest)
    });
  };



  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>EMI Calculator</h3>
        
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
          <label>Interest Rate (% per annum)</label>
          <input
            type="number"
            step="0.01"
            value={form.rate}
            onChange={(e) => setForm({ ...form, rate: e.target.value })}
            placeholder="Enter annual interest rate"
            className="form-input"
            required
          />
        </div>

        <div className="form-actions">
          <button 
            type="button" 
            className="btn btn-primary" 
            onClick={calculateEMI}
          >
            Calculate EMI
          </button>
        </div>

        {calculatedEmi && (
          <div className="emi-result">
            <h4>EMI Calculation Result</h4>
            <div className="result-grid">
              <div className="result-item">
                <strong>Monthly EMI:</strong> ₹{calculatedEmi.emiAmount.toLocaleString()}
              </div>
              <div className="result-item">
                <strong>Total Interest:</strong> ₹{calculatedEmi.totalInterest.toLocaleString()}
              </div>
              <div className="result-item">
                <strong>Total Amount:</strong> ₹{calculatedEmi.totalAmount.toLocaleString()}
              </div>
            </div>
          </div>
        )}

        <div className="form-actions">
          <button className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}