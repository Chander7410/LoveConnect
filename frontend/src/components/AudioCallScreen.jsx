import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Clock, Mic, MicOff, PhoneOff, Volume2 } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

const formatElapsed = (seconds) => {
  const safeSeconds = Math.max(0, seconds);
  const minutes = Math.floor(safeSeconds / 60).toString().padStart(2, '0');
  const remainingSeconds = (safeSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${remainingSeconds}`;
};

const useCallTimer = (activeCall) => {
  const startedAt = useMemo(() => {
    const parsed = Date.parse(activeCall?.startTime || '');
    return Number.isNaN(parsed) ? Date.now() : parsed;
  }, [activeCall?.id, activeCall?.startTime]);
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (!activeCall) return undefined;
    const tick = () => setElapsed(Math.floor((Date.now() - startedAt) / 1000));
    tick();
    const interval = window.setInterval(tick, 1000);
    return () => window.clearInterval(interval);
  }, [activeCall, startedAt]);

  return formatElapsed(elapsed);
};

export default function AudioCallScreen() {
  const { activeCall, muted, remoteStream, toggleMute, endCall } = useCall();
  const remoteAudioRef = useRef(null);
  const elapsed = useCallTimer(activeCall);

  useEffect(() => {
    const remoteAudio = remoteAudioRef.current;
    if (!remoteAudio) return;
    if (remoteAudio.srcObject !== remoteStream) {
      remoteAudio.srcObject = remoteStream || null;
      console.log('[LoveConnect Call] remote audio srcObject assigned', {
        hasStream: Boolean(remoteStream),
        streamId: remoteStream?.id,
        audioTracks: remoteStream?.getAudioTracks().length || 0
      });
    }
    if (!remoteStream) return;
    remoteAudio.play().catch((error) => console.log('[LoveConnect Call] remote audio play blocked', error.message));
  }, [remoteStream]);

  if (!activeCall || activeCall.callType === 'VIDEO') return null;
  return (
    <div className="webrtc-call-screen audio surface">
      <audio ref={remoteAudioRef} autoPlay playsInline />
      <span>Audio call</span>
      <strong>{activeCall.peer?.name || 'LoveConnect member'}</strong>
      <div className="call-meta">
        <span><Clock size={15} /> {elapsed}</span>
        <span><Volume2 size={15} /> {remoteStream?.getAudioTracks().length ? 'Audio connected' : 'Connecting audio'}</span>
      </div>
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
