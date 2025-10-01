import React from 'react';

export default function DashboardStats({ loans, reminders }) {
  const totalLoans = loans.length;
  const totalPrincipal = loans.reduce((sum, loan) => sum + parseFloat(loan.principal), 0);
  const monthlyInterest = loans
    .filter(loan => !loan.repaid)
    .reduce((sum, loan) => sum + (parseFloat(loan.remainingPrincipal) * parseFloat(loan.monthlyInterestRate) / 100), 0);
  const overduePayments = reminders.length;

  return (
    <div className="stats-grid">
      <div className="stat-card">
        <div className="stat-value stat-total">{totalLoans}</div>
        <div className="stat-label">Total Loans</div>
      </div>
      <div className="stat-card">
        <div className="stat-value stat-principal">₹{totalPrincipal.toLocaleString()}</div>
        <div className="stat-label">Principal Given</div>
      </div>
      <div className="stat-card">
        <div className="stat-value stat-interest">₹{monthlyInterest.toLocaleString()}</div>
        <div className="stat-label">Monthly Interest</div>
      </div>
      <div className="stat-card">
        <div className="stat-value stat-overdue">{overduePayments}</div>
        <div className="stat-label">Overdue Payments</div>
      </div>
    </div>
  );
}