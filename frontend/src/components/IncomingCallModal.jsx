import React from 'react';
import { Phone, PhoneOff, Video } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

export default function IncomingCallModal() {
  const { incomingCall, acceptCall, rejectCall } = useCall();
  if (!incomingCall) return null;
  const isVideo = incomingCall.callType === 'VIDEO';
  return (
    <div className="call-modal-backdrop">
      <div className="call-modal surface">
        <div className="call-modal-icon">{isVideo ? <Video size={30} /> : <Phone size={30} />}</div>
        <span>Incoming {isVideo ? 'video' : 'audio'} call</span>
        <strong>{incomingCall.senderName || 'LoveConnect member'}</strong>
        <div className="call-modal-actions">
          <button className="btn btn-danger" type="button" onClick={rejectCall}><PhoneOff size={18} /> Reject</button>
          <button className="btn btn-rose" type="button" onClick={acceptCall}><Phone size={18} /> Accept</button>
        </div>
      </div>
    </div>
  );
}
