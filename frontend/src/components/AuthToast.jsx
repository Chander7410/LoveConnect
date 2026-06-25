import React from 'react';

export default function AuthToast({ message, type = 'info' }) {
  if (!message) return null;
  const alertType = type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info';
  return <div className={`alert alert-${alertType}`}>{message}</div>;
}
