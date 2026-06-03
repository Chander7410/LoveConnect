import { useState } from 'react';
import { Save } from 'lucide-react';
import { endpoints } from '../services/api.js';

export default function ProfilePage({ currentUser, onSaved }) {
  const [form, setForm] = useState({
    displayName: currentUser.displayName || '',
    bio: currentUser.bio || '',
    photoUrl: currentUser.photoUrl || ''
  });
  const [message, setMessage] = useState('');

  function updateField(field, value) {
    setForm((previous) => ({ ...previous, [field]: value }));
  }

  async function save(event) {
    event.preventDefault();
    const response = await endpoints.updateProfile(form);
    onSaved(response.data);
    setMessage('Profile saved.');
  }

  return (
    <section className="profileEditor">
      <div className="profilePreview">
        <img src={form.photoUrl || `https://api.dicebear.com/9.x/lorelei/svg?seed=${currentUser.firebaseUid}`} alt="" />
        <h2>{form.displayName || 'Your name'}</h2>
        <p>{form.bio || 'Add a short intro for matches.'}</p>
      </div>
      <form className="panel form" onSubmit={save}>
        <h2>Profile management</h2>
        <label>
          Display name
          <input value={form.displayName} onChange={(event) => updateField('displayName', event.target.value)} />
        </label>
        <label>
          Photo URL
          <input value={form.photoUrl} onChange={(event) => updateField('photoUrl', event.target.value)} />
        </label>
        <label>
          Bio
          <textarea value={form.bio} onChange={(event) => updateField('bio', event.target.value)} />
        </label>
        <button className="primary" type="submit"><Save size={18} /> Save profile</button>
        {message && <p className="notice">{message}</p>}
      </form>
    </section>
  );
}
