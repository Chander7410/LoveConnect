import React, { useEffect, useState } from 'react';
import api from '../services/api.js';

export default function AdminDashboard() {
  const [dashboard, setDashboard] = useState({});
  const [users, setUsers] = useState([]);
  const [reports, setReports] = useState([]);
  const load = async () => {
    const [summary, people, reportQueue] = await Promise.all([api.get('/admin/dashboard'), api.get('/admin/users'), api.get('/admin/reports')]);
    setDashboard(summary.data);
    setUsers(people.data);
    setReports(reportQueue.data);
  };
  useEffect(() => { load(); }, []);
  const block = async (id, blocked) => {
    await api.patch(`/admin/users/${id}/block`, null, { params: { blocked } });
    load();
  };

  return (
    <div className="container py-4 page-transition">
      <p className="eyebrow">Enterprise operations</p>
      <h2>Admin dashboard</h2>
      <div className="row g-3 mb-4">
        {Object.entries(dashboard).map(([key, value]) => <div className="col-md-4" key={key}><div className="metric"><span>{key}</span><strong>{value}</strong></div></div>)}
      </div>
      <div className="chart-section mb-4">
        <div className="chart-card">
          <h5>Revenue analytics</h5>
          <div className="chart-placeholder">
            {[45, 62, 38, 74, 58, 82, 67, 91, 73, 88, 96, 78].map((height, index) => <div className={`chart-bar lc-chart-bar-${height}`} key={index}></div>)}
          </div>
        </div>
        <div className="notification-panel">
          <h5>Moderation queue</h5>
          <div className="notification-item"><span className="notification-icon">!</span><span>Profile reports ready for review</span><strong>{reports.length}</strong></div>
          {reports.slice(0, 3).map((report) => (
            <div className="notification-item" key={report.id}>
              <span className="notification-icon">{report.riskScore}</span>
              <span>{report.reportedUser?.name}: {report.reason}</span>
              <strong>{report.status}</strong>
            </div>
          ))}
        </div>
      </div>
      <div className="surface table-responsive">
        <table className="table align-middle mb-0">
          <thead><tr><th>Name</th><th>Email</th><th>Location</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.name}</td><td>{user.email}</td><td>{user.location}</td><td>{user.blocked ? 'Blocked' : 'Active'}</td>
                <td className="text-end"><button className="btn btn-sm btn-outline-danger" onClick={() => block(user.id, !user.blocked)}>{user.blocked ? 'Unblock' : 'Block'}</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
