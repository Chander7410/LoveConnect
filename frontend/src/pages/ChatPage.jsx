import React, { useEffect, useRef, useState } from 'react';
import {
  BadgeCheck,
  History,
  MessageCircle,
  Phone,
  PhoneOff,
  Search,
  Send,
  Video,
  VideoOff
} from 'lucide-react';
import api, { currentUser, mediaUrl } from '../services/api.js';

const defaultCallSettings = {
  audioCallsEnabled: true,
  videoCallsEnabled: true,
  ringingEnabled: true,
  ringMuted: false,
  ringtone: 'classic',
  cameraEnabled: true,
  microphoneEnabled: true
};

const ringtonePatterns = {
  classic: [[0, 740, 0.22], [0.28, 980, 0.22], [0.56, 740, 0.18]],
  soft: [[0, 520, 0.28], [0.34, 660, 0.28]],
  digital: [[0, 880, 0.12], [0.18, 1175, 0.12], [0.36, 880, 0.12], [0.54, 1320, 0.12]]
};

const videoRingtonePattern = [
  [0, 1046, 0.1],
  [0.14, 1318, 0.1],
  [0.28, 1568, 0.12],
  [0.58, 1318, 0.1],
  [0.72, 1568, 0.16]
];

export default function ChatPage() {
  const me = currentUser();
  const [myProfile, setMyProfile] = useState(null);
  const [people, setPeople] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [query, setQuery] = useState('');
  const [content, setContent] = useState('');
  const [messages, setMessages] = useState([]);
  const [callHistory, setCallHistory] = useState([]);
  const [activeCall, setActiveCall] = useState(null);
  const [error, setError] = useState('');
  const [loadingPeople, setLoadingPeople] = useState(false);
  const [callSettings, setCallSettings] = useState(() => {
    try {
      return { ...defaultCallSettings, ...JSON.parse(localStorage.getItem('loveconnect.callSettings') || '{}') };
    } catch {
      return defaultCallSettings;
    }
  });
  const ringContextRef = useRef(null);
  const ringGainRef = useRef(null);
  const ringIntervalRef = useRef(null);
  const ringOscillatorsRef = useRef([]);
  const localVideoRef = useRef(null);
  const cameraStreamRef = useRef(null);
  const [cameraStatus, setCameraStatus] = useState('');

  const visiblePeople = people.filter(({ user, commonInterests = [] }) => {
    const needle = query.trim().toLowerCase();
    if (!needle) return true;
    return [user.name, user.location, user.gender, ...(commonInterests || [])]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle));
  });

  const loadConversation = async (user) => {
    if (!user) return;
    setError('');
    try {
      const { data } = await api.get(`/chat/conversation/${user.id}`);
      setMessages(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load conversation.');
    }
  };

  const loadCallHistory = async () => {
    try {
      const { data } = await api.get('/calls/history');
      setCallHistory(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load call history.');
    }
  };

  const selectUser = async (user) => {
    setSelectedUser(user);
    await loadConversation(user);
  };

  const loadPeople = async () => {
    setLoadingPeople(true);
    setError('');
    try {
      const { data } = await api.get('/search');
      setPeople(data);
      if (!selectedUser && data.length > 0) {
        await selectUser(data[0].user);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load contacts.');
    } finally {
      setLoadingPeople(false);
    }
  };

  const send = async (event) => {
    event.preventDefault();
    if (!selectedUser) {
      setError('Please select a contact before sending a message.');
      return;
    }
    try {
      await api.post('/chat/messages', { receiverId: selectedUser.id, content });
      setContent('');
      await loadConversation(selectedUser);
    } catch (err) {
      setError(err.response?.data?.message || 'Message send failed.');
    }
  };

  const stopRinging = () => {
    if (ringIntervalRef.current) {
      window.clearInterval(ringIntervalRef.current);
      ringIntervalRef.current = null;
    }
    ringOscillatorsRef.current.forEach((oscillator) => {
      try {
        oscillator.stop();
      } catch {
        // Oscillator may already be stopped by its scheduled envelope.
      }
    });
    ringOscillatorsRef.current = [];
    if (ringGainRef.current) {
      try {
        ringGainRef.current.gain.cancelScheduledValues(0);
        ringGainRef.current.disconnect();
      } catch {
        // Audio nodes can be disconnected more than once during cleanup.
      }
      ringGainRef.current = null;
    }
    if (ringContextRef.current) {
      ringContextRef.current.close().catch(() => {});
      ringContextRef.current = null;
    }
  };

  const playRingingTone = (callType) => {
    stopRinging();
    if (!callSettings.ringingEnabled || callSettings.ringMuted) return;
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;

    const context = new AudioContext();
    const gain = context.createGain();
    gain.gain.value = 0;
    gain.connect(context.destination);
    ringContextRef.current = context;
    ringGainRef.current = gain;

    const playTone = (delay, frequency, duration = 0.22) => {
      const start = context.currentTime + delay;
      const oscillator = context.createOscillator();
      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(frequency, start);
      oscillator.connect(gain);
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(0.08, start + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
      oscillator.start(start);
      oscillator.stop(start + duration + 0.04);
      ringOscillatorsRef.current.push(oscillator);
      oscillator.onended = () => {
        ringOscillatorsRef.current = ringOscillatorsRef.current.filter((item) => item !== oscillator);
      };
    };

    const playPattern = () => {
      const pattern = callType === 'VIDEO'
        ? videoRingtonePattern
        : (ringtonePatterns[callSettings.ringtone] || ringtonePatterns.classic);
      pattern.forEach(([delay, frequency, duration]) => {
        playTone(delay, frequency, duration);
      });
    };

    playPattern();
    ringIntervalRef.current = window.setInterval(playPattern, 1400);
  };

  const stopCameraPreview = () => {
    if (cameraStreamRef.current) {
      cameraStreamRef.current.getTracks().forEach((track) => track.stop());
      cameraStreamRef.current = null;
    }
    if (localVideoRef.current) {
      localVideoRef.current.srcObject = null;
    }
    setCameraStatus('');
  };

  const startCallMedia = async (type) => {
    const needsVideo = type === 'VIDEO' && callSettings.cameraEnabled;
    const needsAudio = callSettings.microphoneEnabled;
    if (!needsVideo && !needsAudio) {
      setCameraStatus('Camera off · Mic muted');
      return true;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraStatus('Media blocked · Call started without camera/mic');
      return true;
    }
    stopCameraPreview();
    setCameraStatus(needsVideo ? 'Opening camera...' : 'Opening microphone...');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: needsVideo ? { width: { ideal: 640 }, height: { ideal: 360 }, facingMode: 'user' } : false,
        audio: needsAudio
      });
      cameraStreamRef.current = stream;
      if (needsVideo && localVideoRef.current) {
        localVideoRef.current.srcObject = stream;
        await localVideoRef.current.play().catch(() => {});
      }
      setCameraStatus(`${needsVideo ? 'Camera active' : 'Camera off'} · ${needsAudio ? 'Mic active' : 'Mic muted'}`);
      return true;
    } catch (err) {
      const isPermissionIssue = err.name === 'NotAllowedError' || err.name === 'SecurityError';
      setCameraStatus(isPermissionIssue ? 'Camera/mic blocked · Call continues muted' : 'Media unavailable · Call continues muted');
      return true;
    }
  };

  const startCall = async (type) => {
    if (!selectedUser) {
      setError('Please select a contact before starting a call.');
      return;
    }
    if (type === 'AUDIO' && !callSettings.audioCallsEnabled) {
      setError('Audio calls are disabled in Call Settings.');
      return;
    }
    if (type === 'VIDEO' && !callSettings.videoCallsEnabled) {
      setError('Video calls are disabled in Call Settings.');
      return;
    }
    if (type === 'VIDEO' || callSettings.microphoneEnabled) {
      await startCallMedia(type);
    } else {
      stopCameraPreview();
    }
    try {
      const { data } = await api.post('/calls/start', { receiverId: selectedUser.id, type });
      setActiveCall(data);
      playRingingTone(type);
      await loadCallHistory();
    } catch (err) {
      stopCameraPreview();
      setError(err.response?.data?.message || 'Call could not be started.');
    }
  };

  const endCall = async () => {
    if (!activeCall) return;
    stopRinging();
    stopCameraPreview();
    try {
      const { data } = await api.post(`/calls/${activeCall.id}/end`);
      setActiveCall(null);
      setCallHistory((items) => [data, ...items.filter((item) => item.id !== data.id)]);
    } catch (err) {
      setError(err.response?.data?.message || 'Call could not be ended.');
    }
  };

  const callPartner = (call) => call.caller?.id === me?.id ? call.receiver : call.caller;
  const audioCalls = callHistory.filter((call) => call.type === 'AUDIO');
  const videoCalls = callHistory.filter((call) => call.type === 'VIDEO');
  const formatDuration = (seconds) => {
    if (!seconds) return '0s';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return mins ? `${mins}m ${secs}s` : `${secs}s`;
  };

  const messageUser = (senderId) => senderId === me?.id ? myProfile?.user : selectedUser;

  useEffect(() => {
    loadPeople();
    loadCallHistory();
    api.get('/profile/me').then(({ data }) => setMyProfile(data)).catch(() => {});
    const syncCallSettings = (event) => {
      if (event.detail) {
        setCallSettings({ ...defaultCallSettings, ...event.detail });
        return;
      }
      try {
        setCallSettings({ ...defaultCallSettings, ...JSON.parse(localStorage.getItem('loveconnect.callSettings') || '{}') });
      } catch {
        setCallSettings(defaultCallSettings);
      }
    };
    window.addEventListener('loveconnect:callSettingsChanged', syncCallSettings);
    window.addEventListener('storage', syncCallSettings);
    return () => {
      window.removeEventListener('loveconnect:callSettingsChanged', syncCallSettings);
      window.removeEventListener('storage', syncCallSettings);
      stopRinging();
      stopCameraPreview();
    };
  }, []);

  useEffect(() => {
    if (activeCall?.type === 'VIDEO' && cameraStreamRef.current && localVideoRef.current) {
      localVideoRef.current.srcObject = cameraStreamRef.current;
      localVideoRef.current.play().catch(() => {});
    }
  }, [activeCall]);

  return (
    <div className="container py-4 page-transition">
      <p className="eyebrow">Real-time connection</p>
      <div className="chat-page-title">
        <div>
          <h2>Messages</h2>
          <p className="text-muted mb-0">Choose a contact and start chatting.</p>
        </div>
        <button className="btn btn-outline-dark" type="button" onClick={loadPeople}>Refresh</button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="row g-4">
        <div className="col-lg-4">
          <div className="surface p-3 chat-sidebar">
            <div className="chat-sidebar-header">
              <strong>Contacts</strong>
              <span>{visiblePeople.length}</span>
            </div>
            <div className="chat-search mb-3">
              <Search size={18} />
              <input
                className="form-control"
                placeholder="Search name, city, interest"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <div className="chat-user-list">
              {visiblePeople.map(({ user, matchScore, commonInterests = [] }) => (
                <button
                  className={`chat-user-item ${selectedUser?.id === user.id ? 'active' : ''}`}
                  key={user.id}
                  type="button"
                  onClick={() => selectUser(user)}
                >
                  <span className="chat-user-avatar" style={{ backgroundImage: `url(${mediaUrl(user.profilePictureUrl, '/placeholder.svg')})` }}></span>
                  <span>
                    <strong>{user.name} {user.verified && <BadgeCheck size={15} className="verified-badge" />}</strong>
                    <small>{user.location} · {matchScore}% match</small>
                    {commonInterests.length > 0 && <em>{commonInterests.slice(0, 2).join(', ')}</em>}
                  </span>
                </button>
              ))}
              {!loadingPeople && visiblePeople.length === 0 && <div className="empty-state p-3">No contacts found. Try clearing the search.</div>}
              {loadingPeople && <div className="empty-state p-3">Loading contacts...</div>}
            </div>
            <div className="call-history">
              <div className="chat-sidebar-header mt-4">
                <strong><History size={16} /> Audio Call History</strong>
                <span>{audioCalls.length}</span>
              </div>
              {audioCalls.slice(0, 4).map((call) => {
                const partner = callPartner(call);
                return (
                  <button className="call-history-item" type="button" key={call.id} onClick={() => partner && selectUser(partner)}>
                    <span className="call-history-icon"><Phone size={16} /></span>
                    <span>
                      <strong>{partner?.name || 'Contact'}</strong>
                      <small>{call.status} · {formatDuration(call.durationSeconds)}</small>
                    </span>
                  </button>
                );
              })}
              {audioCalls.length === 0 && <div className="empty-state p-3">No audio calls yet.</div>}
            </div>
            <div className="call-history video-history">
              <div className="chat-sidebar-header mt-4">
                <strong><Video size={16} /> Video Call History</strong>
                <span>{videoCalls.length}</span>
              </div>
              {videoCalls.slice(0, 4).map((call) => {
                const partner = callPartner(call);
                return (
                  <button className="call-history-item" type="button" key={call.id} onClick={() => partner && selectUser(partner)}>
                    <span className="call-history-icon video"><Video size={16} /></span>
                    <span>
                      <strong>{partner?.name || 'Contact'}</strong>
                      <small>{call.status} · {formatDuration(call.durationSeconds)}</small>
                    </span>
                  </button>
                );
              })}
              {videoCalls.length === 0 && <div className="empty-state p-3">No video calls yet.</div>}
            </div>
          </div>
        </div>

        <div className="col-lg-8">
          <div className="surface p-3 mb-3 d-flex justify-content-between align-items-center chat-conversation-header">
            <div className="chat-selected-user">
              <span className="chat-user-avatar lg" style={{ backgroundImage: `url(${mediaUrl(selectedUser?.profilePictureUrl, '/placeholder.svg')})` }}></span>
              <span>
                <strong>{selectedUser ? selectedUser.name : 'Select a contact'} {selectedUser?.verified && <BadgeCheck size={16} className="verified-badge" />}</strong>
                {selectedUser && <div className="small text-muted">{selectedUser.location} · Contact #{selectedUser.id}</div>}
              </span>
            </div>
            <div className="call-actions">
              <button className="btn btn-outline-dark" type="button" disabled={!selectedUser || activeCall || !callSettings.audioCallsEnabled} onClick={() => startCall('AUDIO')} title="Audio Call"><Phone size={18} /> Audio Call</button>
              <button className="btn btn-outline-dark" type="button" disabled={!selectedUser || activeCall || !callSettings.videoCallsEnabled} onClick={() => startCall('VIDEO')} title="Video Call"><Video size={18} /> Video Call</button>
            </div>
          </div>

          {activeCall && (
            <div className={`active-call-panel ${activeCall.type === 'VIDEO' ? 'video' : ''}`}>
              <div className="call-avatar" style={{ backgroundImage: `url(${mediaUrl(selectedUser?.profilePictureUrl, '/placeholder.svg')})` }}></div>
              <div>
                <span>Active {activeCall.type === 'VIDEO' ? 'Video Call' : 'Audio Call'}</span>
                <strong>{selectedUser?.name}</strong>
                <small>{activeCall.status === 'RINGING' ? (callSettings.ringMuted || !callSettings.ringingEnabled ? 'Ringing muted' : 'Ringing sound playing') : activeCall.status}</small>
              </div>
              {activeCall.type === 'VIDEO' && (
                <div className={`video-preview ${cameraStreamRef.current ? 'live' : ''}`}>
                  <video ref={localVideoRef} autoPlay muted playsInline aria-label="Local camera preview"></video>
                  {!cameraStreamRef.current && (callSettings.cameraEnabled ? <Video size={34} /> : <VideoOff size={34} />)}
                  <span>{cameraStatus || 'Camera preview'}</span>
                </div>
              )}
              <button className="btn btn-danger" type="button" onClick={endCall}><PhoneOff size={18} /> End</button>
            </div>
          )}

          <div className="surface chat-window p-3 mb-3">
            {messages.map((message) => {
              const sender = messageUser(message.senderId);
              return (
                <div className={`message-row ${message.senderId === me?.id ? 'outgoing' : ''}`} key={message.id}>
                  <span className="message-avatar" style={{ backgroundImage: `url(${mediaUrl(sender?.profilePictureUrl, '/placeholder.svg')})` }}></span>
                  <span className="message-body">
                    <strong>{sender?.name || `Contact #${message.senderId}`}</strong>
                    <span>{message.content}</span>
                    {message.readAt && <span className="read-receipt">Read</span>}
                  </span>
                </div>
              );
            })}
            {messages.length === 0 && (
              <div className="chat-empty-state">
                <MessageCircle size={34} />
                <strong>{selectedUser ? `Say hello to ${selectedUser.name}` : 'Choose a contact'}</strong>
                <span>{selectedUser ? 'No messages yet. Send the first one.' : 'Select someone from Contacts to open chat.'}</span>
              </div>
            )}
          </div>

          <form className="chat-composer" onSubmit={send}>
            <input
              className="form-control"
              required
              disabled={!selectedUser}
              placeholder={selectedUser ? `Message ${selectedUser.name}` : 'Select a contact first'}
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
            <button className="btn btn-rose" disabled={!selectedUser} title="Send message"><Send size={18} /></button>
          </form>
        </div>
      </div>
    </div>
  );
}
