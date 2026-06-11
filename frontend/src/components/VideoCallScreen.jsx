import React, { useEffect, useRef } from 'react';
import { Mic, MicOff, PhoneOff, Video, VideoOff } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

export default function VideoCallScreen() {
  const { activeCall, localStream, remoteStream, muted, cameraOff, toggleMute, toggleCamera, endCall } = useCall();
  const localRef = useRef(null);
  const remoteRef = useRef(null);

  useEffect(() => {
    if (localRef.current) localRef.current.srcObject = localStream;
    if (remoteRef.current) remoteRef.current.srcObject = remoteStream;
  }, [localStream, remoteStream]);

  if (!activeCall || activeCall.callType !== 'VIDEO') return null;
  return (
    <div className="webrtc-call-screen video">
      <video ref={remoteRef} autoPlay playsInline className="remote-video" />
      <video ref={localRef} autoPlay muted playsInline className="local-video" />
      <div className="webrtc-call-bar">
        <strong>{activeCall.peer?.name || 'Video call'}</strong>
        <button className="btn btn-light" type="button" onClick={toggleMute}>
          {muted ? <MicOff /> : <Mic />} {muted ? 'Unmute' : 'Mute'}
        </button>
        <button className="btn btn-light" type="button" onClick={toggleCamera}>
          {cameraOff ? <VideoOff /> : <Video />} {cameraOff ? 'Camera on' : 'Camera off'}
        </button>
        <button className="btn btn-danger" type="button" onClick={() => endCall()}>
          <PhoneOff /> End
        </button>
      </div>
    </div>
  );
}
