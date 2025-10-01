import React, { useState } from 'react';
import API from '../api';

export default function AddBorrower({ onCreated }){
  const [name,setName] = useState('');
  const [principal,setPrincipal] = useState('');
  const [rate,setRate] = useState('2.0');

  const create = async () => {
    if(!name) return alert('Enter name');
    const res = await API.post('/borrowers', { name });
    const borrower = res.data;
    await API.post(`/borrowers/${borrower.id}/loans`, {
      principal: principal || '1000',
      monthlyInterestRate: rate,
      issuedDate: new Date().toISOString().slice(0,10)
    });
    setName(''); setPrincipal('');
    if(onCreated) onCreated();
  };

  return (
    <div style={{marginBottom:20}}>
      <h3>Add borrower + loan</h3>
      <input placeholder="name" value={name} onChange={e=>setName(e.target.value)} />
      <input placeholder="principal" value={principal} onChange={e=>setPrincipal(e.target.value)} />
      <input placeholder="monthly rate % (e.g., 2)" value={rate} onChange={e=>setRate(e.target.value)} />
      <button onClick={create}>Create</button>
    </div>
  );
}
