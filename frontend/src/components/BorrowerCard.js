import React, { useState } from 'react';
import API from '../api';

export default function BorrowerCard({ loan, onUpdate }){
  const [payment, setPayment] = useState('');
  const [postponeDays, setPostponeDays] = useState(2);

  const pay = async () => {
    await API.post(`/loans/${loan.id}/payments`, { amount: payment || loan.principal, date: new Date().toISOString().slice(0,10), note: 'manual' });
    setPayment('');
    onUpdate();
  };

  const postpone = async () => {
    const newDate = new Date(loan.nextDueDate);
    newDate.setDate(newDate.getDate() + parseInt(postponeDays));
    await API.post(`/loans/${loan.id}/postpone`, { newDueDate: newDate.toISOString().slice(0,10) });
    onUpdate();
  };

  return (
    <div style={{border:'1px solid #ddd', padding:10, borderRadius:6}}>
      <h4>{loan.borrower.name}</h4>
      <div>Principal: ₹{loan.principal}</div>
      <div>Remaining: ₹{loan.remainingPrincipal}</div>
      <div>Monthly interest %: {loan.monthlyInterestRate}</div>
      <div>Next due: {loan.nextDueDate}</div>
      <div style={{marginTop:8}}>
        <input placeholder="payment amount" value={payment} onChange={e=>setPayment(e.target.value)} />
        <button onClick={pay}>Record Payment</button>
      </div>
      <div style={{marginTop:8}}>
        <input type="number" value={postponeDays} onChange={e=>setPostponeDays(e.target.value)} />
        <button onClick={postpone}>Postpone by days</button>
      </div>
      <div style={{marginTop:8}}>{loan.repaid ? <strong style={{color:'green'}}>Repaid</strong> : <span style={{color:'orange'}}>Active</span>}</div>
    </div>
  );
}
