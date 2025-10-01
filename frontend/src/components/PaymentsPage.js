import React, { useState, useEffect } from 'react';
import API from '../api';
import '../styles/Payments.css';

export default function PaymentsPage({ loan, onBack }) {
  const [payments, setPayments] = useState([]);
  const [monthlySchedule, setMonthlySchedule] = useState([]);
  const [showPartialModal, setShowPartialModal] = useState(false);
  const [selectedMonth, setSelectedMonth] = useState(null);
  const [partialAmount, setPartialAmount] = useState('');

  useEffect(() => {
    loadPayments();
    generateMonthlySchedule();
  }, []);

  const loadPayments = async () => {
    try {
      const { data } = await API.get(`/loans/${loan.id}/payments`);
      setPayments(data);
    } catch (err) {
      console.error('Failed to load payments');
    }
  };

  const generateMonthlySchedule = async () => {
    const schedule = [];
    const startDate = new Date(loan.trackingStartDate || loan.issuedDate);
    const today = new Date();
    const monthlyInterest = (parseFloat(loan.principal) * parseFloat(loan.monthlyInterestRate)) / 100;
    
    // Load existing payments to check which months are paid
    let existingPayments = [];
    try {
      const { data } = await API.get(`/loans/${loan.id}/payments`);
      existingPayments = data;
    } catch (err) {
      console.error('Failed to load payments');
    }
    
    let currentDate = new Date(startDate);
    let monthIndex = 0;
    
    while (currentDate <= today || monthIndex < 3) {
      const dueDate = new Date(currentDate);
      const monthKey = dueDate.toISOString().slice(0, 7); // YYYY-MM format
      
      // Check if this month has payments
      const monthPayments = existingPayments.filter(p => p.date.startsWith(monthKey));
      const totalPaid = monthPayments.reduce((sum, p) => sum + parseFloat(p.amount), 0);
      const isFullyPaid = totalPaid >= (monthlyInterest - 0.01); // Small tolerance for rounding
      
      schedule.push({
        month: dueDate.toLocaleDateString('en-US', { month: 'short', year: 'numeric' }),
        dueDate: dueDate.toISOString().split('T')[0],
        amount: monthlyInterest,
        paid: isFullyPaid,
        partialPaid: totalPaid > 0 && !isFullyPaid ? totalPaid : 0,
        remainingAmount: totalPaid > 0 && !isFullyPaid ? monthlyInterest - totalPaid : 0,
        paidDate: monthPayments.length > 0 ? monthPayments[0].date : null,
        isPastDue: dueDate < today && !isFullyPaid
      });
      
      currentDate.setMonth(currentDate.getMonth() + 1);
      monthIndex++;
      
      if (monthIndex > 24) break; // Safety limit
    }
    setMonthlySchedule(schedule);
  };

  const handlePaymentToggle = async (monthData, index) => {
    if (monthData.paid) return;

    try {
      const today = new Date().toISOString().split('T')[0];
      const response = await API.post(`/loans/${loan.id}/payments`, {
        amount: monthData.amount.toString(),
        date: today,
        note: `Full interest payment for ${monthData.month}`
      });

      if (response.status === 200) {
        // Immediately update UI state
        const updatedSchedule = [...monthlySchedule];
        updatedSchedule[index] = {
          ...updatedSchedule[index],
          paid: true,
          paidDate: today,
          partialPaid: 0,
          remainingAmount: 0
        };
        setMonthlySchedule(updatedSchedule);
        
        // Also reload data in background
        generateMonthlySchedule();
        alert('Payment recorded successfully!');
      }
    } catch (err) {
      console.error('Payment error:', err);
      alert('Failed to record payment');
    }
  };

  const handlePartialPayment = (monthData, index) => {
    setSelectedMonth({ ...monthData, index });
    setPartialAmount('');
    setShowPartialModal(true);
  };

  const submitPartialPayment = async () => {
    if (!partialAmount || parseFloat(partialAmount) <= 0) {
      alert('Please enter a valid amount');
      return;
    }

    const amount = parseFloat(partialAmount);
    const fullAmount = selectedMonth.amount;
    
    if (amount >= fullAmount) {
      alert('Use "Mark as Paid" for full payments');
      return;
    }

    try {
      const today = new Date().toISOString().split('T')[0];
      await API.post(`/loans/${loan.id}/payments`, {
        amount: amount.toString(),
        date: today,
        note: `Partial payment for ${selectedMonth.month} - ₹${amount} of ₹${fullAmount.toFixed(2)}`
      });

      // Immediately update UI
      const updatedSchedule = [...monthlySchedule];
      const currentPaid = updatedSchedule[selectedMonth.index].partialPaid || 0;
      const newTotalPaid = currentPaid + amount;
      const isNowFullyPaid = newTotalPaid >= (fullAmount - 0.01);
      
      updatedSchedule[selectedMonth.index] = {
        ...updatedSchedule[selectedMonth.index],
        partialPaid: isNowFullyPaid ? 0 : newTotalPaid,
        remainingAmount: isNowFullyPaid ? 0 : fullAmount - newTotalPaid,
        paid: isNowFullyPaid,
        paidDate: today
      };
      setMonthlySchedule(updatedSchedule);
      
      setShowPartialModal(false);
      
      // Reload data in background
      generateMonthlySchedule();
      
      alert('Partial payment recorded successfully!');
    } catch (err) {
      alert('Failed to record partial payment');
    }
  };

  return (
    <div className="payments-page">
      <div className="payments-header">
        <h2>Payments - {loan.borrower?.name}</h2>
        <button className="back-button" onClick={onBack}>
          ← Back
        </button>
      </div>

      <div className="loan-summary">
        <h3>Loan Details</h3>
        <p>Principal: ₹{loan.principal}</p>
        <p>Monthly Interest: {loan.monthlyInterestRate}%</p>
        <p>Monthly Amount: ₹{((parseFloat(loan.principal) * parseFloat(loan.monthlyInterestRate)) / 100).toFixed(2)}</p>
        <p>Issue Date: {new Date(loan.issuedDate).toLocaleDateString()}</p>
        <p>Tracking From: {new Date(loan.trackingStartDate || loan.issuedDate).toLocaleDateString()}</p>
      </div>

      <div className="payments-list">
        <h3 style={{ padding: '20px', margin: 0, borderBottom: '1px solid #eee' }}>Payment Schedule</h3>
        {monthlySchedule.map((monthData, index) => (
          <div 
            key={index} 
            className={`payment-row ${monthData.paid ? 'paid' : ''} ${monthData.isPastDue && !monthData.paid ? 'overdue' : ''}`}
          >
            <div className="payment-month">{monthData.month}</div>
            <div className="payment-amount">₹{monthData.amount.toFixed(2)}</div>
            <div className="payment-status">
              <input
                type="checkbox"
                className="payment-checkbox"
                checked={monthData.paid}
                onChange={() => handlePaymentToggle(monthData, index)}
                disabled={monthData.paid}
              />
              <label 
                className="payment-label"
                onClick={() => !monthData.paid && !monthData.partialPaid && handlePaymentToggle(monthData, index)}
              >
                {monthData.paid ? `Paid on ${new Date(monthData.paidDate).toLocaleDateString()}` : 
                 monthData.partialPaid ? `Partial: ₹${monthData.partialPaid} (Remaining: ₹${monthData.remainingAmount.toFixed(2)})` : 
                 'Mark as Paid'}
              </label>
              {!monthData.paid && (
                <button 
                  className="btn-small btn-secondary"
                  onClick={() => handlePartialPayment(monthData, index)}
                  style={{ marginLeft: '10px', fontSize: '12px', padding: '4px 8px' }}
                >
                  Partial
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {showPartialModal && (
        <div className="modal-overlay" onClick={() => setShowPartialModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Partial Payment - {selectedMonth?.month}</h3>
            <p>Full Amount: ₹{selectedMonth?.amount.toFixed(2)}</p>
            <div className="form-group">
              <label>Amount Received:</label>
              <input
                type="number"
                value={partialAmount}
                onChange={(e) => setPartialAmount(e.target.value)}
                placeholder="Enter amount received"
                className="form-input"
                step="0.01"
                min="0.01"
                max={selectedMonth?.amount}
              />
            </div>
            <div className="form-actions">
              <button 
                className="btn btn-secondary" 
                onClick={() => setShowPartialModal(false)}
              >
                Cancel
              </button>
              <button 
                className="btn btn-primary" 
                onClick={submitPartialPayment}
              >
                Record Payment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}