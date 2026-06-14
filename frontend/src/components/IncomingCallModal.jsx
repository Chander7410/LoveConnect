import React, { useEffect, useState } from 'react';
import { Phone, PhoneOff, Video } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

export default function IncomingCallModal() {
  const { incomingCall, acceptCall, rejectCall } = useCall();
  const [fallbackCall, setFallbackCall] = useState(null);

  useEffect(() => {
    const showIncomingCall = (event) => setFallbackCall(event.detail);
    window.addEventListener('loveconnect:incomingCall', showIncomingCall);
    const interval = window.setInterval(() => {
      if (incomingCall || fallbackCall) return;
      const cached = sessionStorage.getItem('loveconnect_incoming_call');
      if (!cached) return;
      try {
        const parsed = JSON.parse(cached);
        if (parsed.cachedAt && Date.now() - parsed.cachedAt > 45000) {
          sessionStorage.removeItem('loveconnect_incoming_call');
          return;
        }
        setFallbackCall(parsed);
      } catch {
        sessionStorage.removeItem('loveconnect_incoming_call');
      }
    }, 500);
    return () => {
      window.removeEventListener('loveconnect:incomingCall', showIncomingCall);
      window.clearInterval(interval);
    };
  }, [fallbackCall, incomingCall]);

  const displayCall = incomingCall || fallbackCall;
  if (!displayCall) return null;
  const isVideo = displayCall.callType === 'VIDEO';
  const accept = () => {
    console.log('[LoveConnect Call] Accept button clicked', displayCall.callId);
    sessionStorage.removeItem('loveconnect_incoming_call');
    setFallbackCall(null);
    acceptCall(displayCall);
  };
  const reject = () => {
    sessionStorage.removeItem('loveconnect_incoming_call');
    setFallbackCall(null);
    rejectCall(displayCall);
  };
  return (
    <div className="call-modal-backdrop">
      <div className="call-modal surface">
        <div className="call-modal-icon">{isVideo ? <Video size={30} /> : <Phone size={30} />}</div>
        <span>Incoming {isVideo ? 'video' : 'audio'} call</span>
        <strong>{displayCall.senderName || 'LoveConnect member'}</strong>
        <div className="call-modal-actions">
          <button className="btn btn-danger" type="button" onClick={reject}><PhoneOff size={18} /> Reject</button>
          <button className="btn btn-rose" type="button" onClick={accept}><Phone size={18} /> Accept</button>
        </div>
      </div>
    </div>
  );
}
