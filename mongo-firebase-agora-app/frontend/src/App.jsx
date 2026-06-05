import { useEffect, useMemo, useState } from 'react';
import { Heart, LogOut, MessageCircle, Phone, Search, UserRound, Video } from 'lucide-react';
import LoginPage from './pages/LoginPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import ChatPage from './pages/ChatPage.jsx';
import { auth } from './services/firebase.js';
import { clearToken, endpoints, saveToken, setTokenProvider } from './services/api.js';

const tabs = [
  { id: 'discover', label: 'Discover', icon: Search },
  { id: 'chat', label: 'Chat', icon: MessageCircle },
  { id: 'calls', label: 'Calls', icon: Phone },
  { id: 'profile', label: 'Profile', icon: UserRound }
];

export default function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [profiles, setProfiles] = useState([]);
  const [selected, setSelected] = useState(null);
  const [activeTab, setActiveTab] = useState('discover');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setTokenProvider(async () => {
      if (auth?.currentUser) {
        const token = await auth.currentUser.getIdToken();
        saveToken(token);
        return token;
      }
      return localStorage.getItem('loveconnect_token');
    });
    bootstrap();
  }, []);

  async function bootstrap() {
    try {
      setLoading(true);
      const me = await endpoints.me();
      setCurrentUser(me.data);
      const list = await endpoints.profiles();
      setProfiles(list.data);
      setSelected(list.data[0] || null);
      setError('');
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(token) {
    saveToken(token);
    setTokenProvider(async () => token);
    setError('');
    await bootstrap();
  }

  async function handleLogout() {
    await auth?.signOut();
    clearToken();
    setCurrentUser(null);
    setProfiles([]);
    setSelected(null);
  }

  const selectedProfile = useMemo(() => selected || profiles[0] || null, [selected, profiles]);

  if (!currentUser) {
    return <LoginPage onLogin={handleLogin} initialError={error} />;
  }

  return (
    <main className="shell">
      <nav className="topbar">
        <div className="brand">
          <Heart fill="currentColor" />
          <span>LoveConnect</span>
        </div>
        <div className="tabs">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button className={activeTab === tab.id ? 'tab active' : 'tab'} key={tab.id} onClick={() => setActiveTab(tab.id)}>
                <Icon size={18} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
        <button className="iconButton" onClick={handleLogout} aria-label="Logout" title="Logout">
          <LogOut />
        </button>
      </nav>

      {loading && <p className="notice">Loading your dating workspace...</p>}
      {error && <p className="error">{error}</p>}

      {activeTab === 'discover' && (
        <section className="grid">
          <aside className="panel">
            <h2>People</h2>
            <p className="muted">Profiles are stored in MongoDB Atlas and secured by Firebase phone auth.</p>
            <div className="profileList">
              {profiles.map((profile) => (
                <button key={profile.firebaseUid} className={selectedProfile?.firebaseUid === profile.firebaseUid ? 'person selected' : 'person'} onClick={() => setSelected(profile)}>
                  <img src={profile.photoUrl || `https://api.dicebear.com/9.x/lorelei/svg?seed=${profile.firebaseUid}`} alt="" />
                  <span>
                    <strong>{profile.displayName || 'New Member'}</strong>
                    <small>{profile.phoneNumber}</small>
                  </span>
                </button>
              ))}
            </div>
          </aside>
          <section className="heroProfile">
            {selectedProfile ? (
              <>
                <img src={selectedProfile.photoUrl || `https://api.dicebear.com/9.x/lorelei/svg?seed=${selectedProfile.firebaseUid}`} alt="" />
                <div>
                  <p className="eyebrow">Verified member</p>
                  <h1>{selectedProfile.displayName || 'New Member'}</h1>
                  <p>{selectedProfile.bio || 'No bio yet. Start a chat and say hello.'}</p>
                  <button className="primary" onClick={() => setActiveTab('chat')}>
                    <MessageCircle size={18} /> Start chat
                  </button>
                </div>
              </>
            ) : (
              <p className="notice">No other users yet. Login from another phone number to create a test match.</p>
            )}
          </section>
        </section>
      )}

      {activeTab === 'chat' && <ChatPage currentUser={currentUser} profiles={profiles} selected={selectedProfile} onSelect={setSelected} />}
      {activeTab === 'calls' && <ChatPage currentUser={currentUser} profiles={profiles} selected={selectedProfile} onSelect={setSelected} callsOnly />}
      {activeTab === 'profile' && <ProfilePage currentUser={currentUser} onSaved={setCurrentUser} />}

      <div className="floatingHint">
        <Video size={16} /> Firebase OTP, MongoDB Atlas, WebSocket chat, and Agora-ready calls
      </div>
    </main>
  );
}
