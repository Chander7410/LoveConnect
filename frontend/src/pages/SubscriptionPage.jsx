import React, { useEffect, useState } from 'react';
import api from '../services/api.js';

export default function SubscriptionPage() {
  const [history, setHistory] = useState([]);
  const load = async () => setHistory((await api.get('/subscriptions')).data);
  useEffect(() => { load(); }, []);
  const subscribe = async (planType) => {
    await api.post('/subscriptions', { planType, amount: planType === 'PREMIUM' ? 19.99 : 0, paymentReference: `demo-${Date.now()}` });
    load();
  };

  return (
    <div className="container py-4 page-transition">
      <p className="eyebrow">Premium membership</p>
      <h2>Subscription</h2>
      <div className="row g-4">
        {['FREE', 'PREMIUM'].map((plan) => (
          <div className="col-md-6" key={plan}>
            <div className={`premium-plan h-100 ${plan === 'PREMIUM' ? 'featured' : ''}`}>
              <h4>{plan === 'FREE' ? 'Free Plan' : 'Premium Plan'}</h4>
              <p className="text-muted">{plan === 'FREE' ? 'Browse, search, receive likes.' : 'Priority recommendations, unlimited likes, premium visibility.'}</p>
              <div className="plan-price">{plan === 'FREE' ? '$0' : '$19.99'}<span className="small"> / month</span></div>
              <ul className="plan-feature-list">
                <li>✓ Smart profile discovery</li>
                <li>✓ Match score visibility</li>
                <li>{plan === 'PREMIUM' ? '✓ Priority visibility' : '✓ Basic messaging'}</li>
              </ul>
              <button className="btn btn-rose premium-upgrade-button mt-3" onClick={() => subscribe(plan)}>Choose {plan}</button>
            </div>
          </div>
        ))}
      </div>
      <h5 className="mt-5">History</h5>
      <div className="surface p-3">
        {history.map((item) => <div className="border-bottom py-2" key={item.id}>{item.planType} · {item.status} · {item.startDate} to {item.endDate}</div>)}
      </div>
    </div>
  );
}
