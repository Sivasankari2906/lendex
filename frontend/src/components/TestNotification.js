import React from 'react';

export default function TestNotification() {
  const testNotification = () => {
    if ('Notification' in window) {
      if (Notification.permission === 'granted') {
        new Notification('Test Notification', {
          body: 'This is a test notification from Lendex',
          icon: '/favicon.ico'
        });
      } else if (Notification.permission === 'default') {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            new Notification('Test Notification', {
              body: 'Permission granted! Notifications are working.',
              icon: '/favicon.ico'
            });
          }
        });
      } else {
        alert('Notifications are blocked. Please enable them in browser settings.');
      }
    } else {
      alert('This browser does not support notifications.');
    }
  };

  return (
    <div style={{ 
      position: 'fixed', 
      bottom: '20px', 
      left: '20px', 
      background: 'white', 
      padding: '10px', 
      border: '1px solid #ccc',
      borderRadius: '4px',
      zIndex: 2000
    }}>
      <button onClick={testNotification} style={{ padding: '8px 16px' }}>
        Test Notification
      </button>
      <div style={{ fontSize: '12px', marginTop: '5px' }}>
        Permission: {typeof Notification !== 'undefined' ? Notification.permission : 'Not supported'}
      </div>
    </div>
  );
}