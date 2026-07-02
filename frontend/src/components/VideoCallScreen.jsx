import React, { useEffect, useRef, useState } from 'react';
import { Clock, Maximize2, Mic, MicOff, Minimize2, PhoneOff, Video, VideoOff } from 'lucide-react';
import { useCall } from '../context/CallContext.jsx';

const logCall = (message, detail = '') => {
  console.log(`[LoveConnect Call] ${message}`, detail);
};

const formatElapsed = (seconds) => {
  const safeSeconds = Math.max(0, seconds);
  const minutes = Math.floor(safeSeconds / 60).toString().padStart(2, '0');
  const remainingSeconds = (safeSeconds % 60).toString().padStart(2, '0');
  return `${minutes}:${remainingSeconds}`;
};

const useCallTimer = (activeCall) => {
  const [startedAt, setStartedAt] = useState(null);
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (!activeCall || activeCall.status !== 'ACTIVE') {
      setStartedAt(null);
      setElapsed(0);
      return undefined;
    }
    const connectedAt = Date.now();
    setStartedAt(connectedAt);
    return undefined;
  }, [activeCall?.id, activeCall?.status]);

  useEffect(() => {
    if (!startedAt) return undefined;
    const tick = () => setElapsed(Math.floor((Date.now() - startedAt) / 1000));
    tick();
    const interval = window.setInterval(tick, 1000);
    return () => window.clearInterval(interval);
  }, [startedAt]);

  return formatElapsed(elapsed);
};

export default function VideoCallScreen() {
  const { activeCall, localStream, remoteStream, muted, cameraOff, toggleMute, toggleCamera, endCall } = useCall();
  const localRef = useRef(null);
  const remoteRef = useRef(null);
  const remoteAudioRef = useRef(null);
  const renderedFrameRef = useRef(false);
  const [zoomed, setZoomed] = useState(false);
  const elapsed = useCallTimer(activeCall);
  const statusText = activeCall?.status === 'ACTIVE'
    ? elapsed
    : activeCall?.status === 'CONNECTING'
      ? 'Connecting'
      : 'Ringing';

  useEffect(() => {
    if (localRef.current && localRef.current.srcObject !== localStream) {
      localRef.current.srcObject = localStream || null;
    }
  }, [localStream]);

  useEffect(() => {
    const remoteVideo = remoteRef.current;
    if (!remoteVideo) return;
    if (remoteVideo.srcObject !== remoteStream) {
      remoteVideo.srcObject = remoteStream || null;
      renderedFrameRef.current = false;
      logCall('remote video srcObject assigned', {
        hasStream: Boolean(remoteStream),
        streamId: remoteStream?.id,
        videoTracks: remoteStream?.getVideoTracks().length || 0,
        audioTracks: remoteStream?.getAudioTracks().length || 0
      });
    }
    if (!remoteStream) return;
    const logVideoState = (eventName) => {
      logCall(`remote video ${eventName}`, {
        readyState: remoteVideo.readyState,
        videoWidth: remoteVideo.videoWidth,
        videoHeight: remoteVideo.videoHeight,
        paused: remoteVideo.paused,
        muted: remoteVideo.muted,
        srcObject: Boolean(remoteVideo.srcObject),
        videoTracks: remoteStream.getVideoTracks().map((track) => `${track.id}:${track.readyState}:${track.enabled}`),
        audioTracks: remoteStream.getAudioTracks().map((track) => `${track.id}:${track.readyState}:${track.enabled}`)
      });
    };
    const onLoadedMetadata = () => logVideoState('loadedmetadata');
    const onPlaying = () => {
      logVideoState('playing');
      if (!renderedFrameRef.current && remoteVideo.videoWidth > 0 && remoteVideo.videoHeight > 0) {
        renderedFrameRef.current = true;
        logCall('first remote frame rendered', {
          readyState: remoteVideo.readyState,
          videoWidth: remoteVideo.videoWidth,
          videoHeight: remoteVideo.videoHeight
        });
      }
    };
    const onTimeUpdate = () => {
      if (!renderedFrameRef.current && remoteVideo.videoWidth > 0 && remoteVideo.videoHeight > 0) {
        renderedFrameRef.current = true;
        logCall('first remote frame rendered', {
          readyState: remoteVideo.readyState,
          videoWidth: remoteVideo.videoWidth,
          videoHeight: remoteVideo.videoHeight
        });
      }
    };
    remoteVideo.addEventListener('loadedmetadata', onLoadedMetadata);
    remoteVideo.addEventListener('playing', onPlaying);
    remoteVideo.addEventListener('timeupdate', onTimeUpdate);
    remoteVideo.play().catch((error) => logCall('remote video play blocked', error.message));
    logVideoState('state after srcObject');
    return () => {
      remoteVideo.removeEventListener('loadedmetadata', onLoadedMetadata);
      remoteVideo.removeEventListener('playing', onPlaying);
      remoteVideo.removeEventListener('timeupdate', onTimeUpdate);
    };
  }, [remoteStream]);

  useEffect(() => {
    const remoteAudio = remoteAudioRef.current;
    if (!remoteAudio) return;
    if (remoteAudio.srcObject !== remoteStream) {
      remoteAudio.srcObject = remoteStream || null;
      logCall('remote video-call audio srcObject assigned', {
        hasStream: Boolean(remoteStream),
        streamId: remoteStream?.id,
        audioTracks: remoteStream?.getAudioTracks().length || 0
      });
    }
    remoteAudio.muted = false;
    remoteAudio.volume = 1;
    if (!remoteStream) return;
    remoteAudio.play().catch((error) => logCall('remote video-call audio play blocked', error.message));
  }, [remoteStream]);

  if (!activeCall || activeCall.callType !== 'VIDEO') return null;
  return (
    <div className={`webrtc-call-screen video ${zoomed ? 'zoomed' : ''}`}>
      <video ref={remoteRef} autoPlay playsInline muted className="remote-video" />
      <audio ref={remoteAudioRef} autoPlay playsInline />
      <video ref={localRef} autoPlay muted playsInline className="local-video" />
      <div className="webrtc-call-bar">
        <strong>{activeCall.peer?.name || 'Video call'}</strong>
        <span className="call-timer"><Clock size={15} /> {statusText}</span>
        <button className="btn btn-light" type="button" onClick={() => setZoomed((value) => !value)}>
          {zoomed ? <Minimize2 /> : <Maximize2 />} {zoomed ? 'Normal' : 'Zoom'}
        </button>
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
