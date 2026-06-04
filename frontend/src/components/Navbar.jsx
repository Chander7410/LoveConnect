import React, { useEffect, useState } from 'react';
import {
  Bell,
  BellOff,
  Crown,
  Heart,
  LogOut,
  MessageCircle,
  Mic,
  MicOff,
  Phone,
  Search,
  Settings,
  Shield,
  UserRound,
  Video,
  VideoOff
} from 'lucide-react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { clearSession, currentUser } from '../services/api.js';

const defaultCallSettings = {
  audioCallsEnabled: true,
  videoCallsEnabled: true,
  ringingEnabled: true,
  ringMuted: false,
  ringtone: 'classic',
  cameraEnabled: true,
  microphoneEnabled: true
};

export default function Navbar() {
  const navigate = useNavigate();
  const [user, setUser] = useState(currentUser);
  const displayName = user?.name || user?.email || 'User';
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [callSettings, setCallSettings] = useState(() => {
    try {
      return { ...defaultCallSettings, ...JSON.parse(localStorage.getItem('loveconnect.callSettings') || '{}') };
    } catch {
      return defaultCallSettings;
    }
  });

  const logout = () => {
    clearSession();
    navigate('/login');
  };

  useEffect(() => {
    const syncUser = () => setUser(currentUser());
    window.addEventListener('loveconnect:sessionChanged', syncUser);
    window.addEventListener('storage', syncUser);
    return () => {
      window.removeEventListener('loveconnect:sessionChanged', syncUser);
      window.removeEventListener('storage', syncUser);
    };
  }, []);

  const updateCallSetting = (key, value) => {
    setCallSettings((current) => {
      const next = { ...current, [key]: value };
      localStorage.setItem('loveconnect.callSettings', JSON.stringify(next));
      window.dispatchEvent(new CustomEvent('loveconnect:callSettingsChanged', { detail: next }));
      return next;
    });
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-light sticky-top floating-navbar">
      <div className="container">
        <Link className="navbar-brand fw-bold text-rose d-flex align-items-center gap-2" to="/">
          <Heart size={24} fill="currentColor" /> LoveConnect
        </Link>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#nav">
          <span className="navbar-toggler-icon"></span>
        </button>
        <div id="nav" className="collapse navbar-collapse">
          <div className="navbar-nav ms-auto align-items-lg-center gap-lg-2">
            {user && (
              <>
                <NavLink className="nav-link" to="/search"><Search size={17} /> Search</NavLink>
                <NavLink className="nav-link" to="/matches"><Heart size={17} /> Matches</NavLink>
                <NavLink className="nav-link" to="/chat"><MessageCircle size={17} /> Chat</NavLink>
                <NavLink className="nav-link" to="/notifications"><Bell size={17} /> Alerts</NavLink>
                <NavLink className="nav-link" to="/profile"><UserRound size={17} /> Profile</NavLink>
                <NavLink className="nav-link" to="/subscription"><Crown size={17} /> Plans</NavLink>
                {user.role === 'ADMIN' && <NavLink className="nav-link" to="/admin"><Shield size={17} /> Admin</NavLink>}
                <button className="nav-link nav-call-settings-button" type="button" onClick={() => setSettingsOpen((open) => !open)}>
                  <Settings size={17} /> Call Settings
                </button>
                <Link className="nav-user-chip" to="/profile" title={displayName}>
                  <span>{displayName.charAt(0).toUpperCase()}</span>
                  <strong>{displayName}</strong>
                </Link>
                <button className="btn btn-glass btn-sm" onClick={logout} title="Log out"><LogOut size={16} /></button>
              </>
            )}
            {!user && (
              <>
                <NavLink className="nav-link" to="/login">Login</NavLink>
                <NavLink className="btn btn-rose" to="/register"><Heart size={17} fill="currentColor" /> Create profile</NavLink>
              </>
            )}
          </div>
          {user && settingsOpen && (
            <div className="nav-call-settings">
              <div className="nav-call-settings-head">
                <strong><Settings size={17} /> Call Settings</strong>
                <span>{callSettings.ringMuted ? 'Ringing muted' : `${callSettings.ringtone} ringtone`}</span>
              </div>
              <div className="nav-call-settings-grid">
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.audioCallsEnabled} onChange={(e) => updateCallSetting('audioCallsEnabled', e.target.checked)} />
                  <span><Phone size={16} /> Audio Call</span>
                </label>
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.videoCallsEnabled} onChange={(e) => updateCallSetting('videoCallsEnabled', e.target.checked)} />
                  <span><Video size={16} /> Video Call</span>
                </label>
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.ringingEnabled} onChange={(e) => updateCallSetting('ringingEnabled', e.target.checked)} />
                  <span><Bell size={16} /> Ringing</span>
                </label>
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.ringMuted} onChange={(e) => updateCallSetting('ringMuted', e.target.checked)} />
                  <span><BellOff size={16} /> Ring mute</span>
                </label>
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.cameraEnabled} onChange={(e) => updateCallSetting('cameraEnabled', e.target.checked)} />
                  <span>{callSettings.cameraEnabled ? <Video size={16} /> : <VideoOff size={16} />} Camera</span>
                </label>
                <label className="call-setting-toggle">
                  <input type="checkbox" checked={callSettings.microphoneEnabled} onChange={(e) => updateCallSetting('microphoneEnabled', e.target.checked)} />
                  <span>{callSettings.microphoneEnabled ? <Mic size={16} /> : <MicOff size={16} />} Microphone</span>
                </label>
                <div className="call-ringtone-picker">
                  <span>Ringtone</span>
                  <div className="ringtone-options" role="group" aria-label="Choose ringtone">
                    {[
                      ['classic', 'Classic dating'],
                      ['soft', 'Soft chime'],
                      ['digital', 'Digital pop']
                    ].map(([value, label]) => (
                      <button
                        className={callSettings.ringtone === value ? 'active' : ''}
                        key={value}
                        type="button"
                        onClick={() => updateCallSetting('ringtone', value)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
