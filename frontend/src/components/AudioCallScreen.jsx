import React from 'react';
import { Mic, MicOff, PhoneOff } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

export default function AudioCallScreen() {
  const { activeCall, muted, toggleMute, endCall } = useCall();
  if (!activeCall || activeCall.callType === 'VIDEO') return null;
  return (
    <div className="webrtc-call-screen audio surface">
      <span>Audio call</span>
      <strong>{activeCall.peer?.name || 'LoveConnect member'}</strong>
      <div className="webrtc-call-bar inline">
        <button className="btn btn-light" type="button" onClick={toggleMute}>
          {muted ? <MicOff /> : <Mic />} {muted ? 'Unmute' : 'Mute'}
        </button>
        <button className="btn btn-danger" type="button" onClick={() => endCall()}>
          <PhoneOff /> End
        </button>
      </div>
    </div>
  );
}
