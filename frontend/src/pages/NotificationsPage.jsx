import React, { useEffect, useState } from 'react';
import { Bell, Mail, Smartphone } from 'lucide-react';
import api from '../services/api.js';

const labels = {
  NEW_MATCH: 'Match',
  MESSAGE: 'Message',
  LIKE: 'Like',
  SUPER_LIKE: 'Super Like',
  REPORT: 'Report',
  BLOCK: 'Block',
  VERIFICATION: 'Verification',
  MODERATION: 'Moderation'
};

export default function NotificationsPage() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const { data } = await api.get('/notifications');
      setItems(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load notifications.');
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="container py-4 page-transition">
      <p className="eyebrow">Activity center</p>
      <h2>Notifications</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <div className="notification-panel">
        {items.map((item) => (
          <div className="notification-item" key={item.id}>
            <span className="notification-icon"><Bell size={16} /></span>
            <span><strong>{labels[item.type] || item.type}</strong><br />{item.message}</span>
            <span className="d-flex gap-2">
              {item.pushDelivered && <Smartphone size={16} title="Push delivered" />}
              {item.emailQueued && <Mail size={16} title="Email queued" />}
            </span>
          </div>
        ))}
        {items.length === 0 && <div className="empty-state p-4">No notifications yet.</div>}
      </div>
    </div>
  );
}
