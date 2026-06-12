import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import api from '../services/api.js';
import { appUidFromToken, createPeer, createSignalClient, publishSignal } from '../services/webrtc.js';

const CallContext = createContext(null);
const logCall = (message, detail = '') => {
  console.log(`[LoveConnect Call] ${message}`, detail);
};
const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));

export function CallProvider({ children }) {
  const [incomingCall, setIncomingCall] = useState(null);
  const [activeCall, setActiveCall] = useState(null);
  const [localStream, setLocalStream] = useState(null);
  const [remoteStream, setRemoteStream] = useState(null);
  const [muted, setMuted] = useState(false);
  const [cameraOff, setCameraOff] = useState(false);
  const [error, setError] = useState('');
  const peerRef = useRef(null);
  const signalRef = useRef(null);
  const handledIncomingRef = useRef(new Set());
  const pendingRemoteIceRef = useRef([]);
  const activeRef = useRef(null);
  const callRequestRetryRef = useRef(null);

  const token = localStorage.getItem('loveconnect_token') || sessionStorage.getItem('loveconnect_token');
  const uid = appUidFromToken(token);

  const waitForSignalConnection = useCallback(async () => {
    const deadline = Date.now() + 6000;
    while (!signalRef.current?.connected && Date.now() < deadline) {
      await wait(150);
    }
    return Boolean(signalRef.current?.connected);
  }, []);

  const sendSignal = useCallback(async (type, body) => {
    if (publishSignal(signalRef.current, type, body)) return true;
    const connected = await waitForSignalConnection();
    if (connected && publishSignal(signalRef.current, type, body)) return true;
    logCall('signal not connected', type);
    setError('Call connection is still starting. Please try again.');
    return false;
  }, [waitForSignalConnection]);

  const stopMedia = useCallback(() => {
    setLocalStream((stream) => {
      stream?.getTracks().forEach((track) => track.stop());
      return null;
    });
    setRemoteStream(null);
    peerRef.current?.close();
    peerRef.current = null;
    pendingRemoteIceRef.current = [];
  }, []);

  const clearCallRequestRetry = useCallback(() => {
    if (callRequestRetryRef.current) {
      window.clearInterval(callRequestRetryRef.current);
      callRequestRetryRef.current = null;
    }
  }, []);

  const ensurePeer = useCallback((receiverId) => {
    if (peerRef.current) return peerRef.current;
    const peer = createPeer();
    peer.onicecandidate = (event) => {
      if (event.candidate && activeRef.current?.peerId) {
        logCall('ICE candidate sent', event.candidate);
        sendSignal('ice-candidate', {
          callId: activeRef.current.id,
          receiverId: activeRef.current.peerId,
          payload: { candidate: event.candidate }
        });
      }
    };
    peer.ontrack = (event) => {
      logCall('remote track received', event.streams[0]);
      setRemoteStream(event.streams[0]);
    };
    peer.onconnectionstatechange = () => {
      logCall('connection state change', peer.connectionState);
      if (['failed', 'disconnected', 'closed'].includes(peer.connectionState)) {
        setError(peer.connectionState === 'failed' ? 'Call connection failed.' : '');
      }
    };
    peerRef.current = peer;
    return peer;
  }, [sendSignal]);

  const openMedia = useCallback(async (callType) => {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: callType === 'VIDEO'
    });
    setLocalStream(stream);
    setMuted(false);
    setCameraOff(callType !== 'VIDEO');
    return stream;
  }, []);

  const attachLocalTracks = (peer, stream) => {
    const existingTrackIds = new Set(peer.getSenders().map((sender) => sender.track?.id).filter(Boolean));
    stream.getTracks().forEach((track) => {
      if (!existingTrackIds.has(track.id)) peer.addTrack(track, stream);
    });
  };

  const applyPendingIce = async (peer) => {
    for (const candidate of pendingRemoteIceRef.current) {
      await peer.addIceCandidate(candidate).catch(() => {});
    }
    pendingRemoteIceRef.current = [];
  };

  const startCall = useCallback(async (receiver, callType) => {
    setError('');
    if (!receiver?.id) {
      setError('Select a matched contact before calling.');
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      setError('This browser does not support WebRTC media calls.');
      return;
    }
    const { data } = await api.post('/calls/start', { receiverId: receiver.id, type: callType });
    const nextCall = { ...data, callType, peerId: receiver.id, peer: receiver, direction: 'outgoing', status: 'RINGING' };
    activeRef.current = nextCall;
    setActiveCall(nextCall);
    const stream = await openMedia(callType);
    const peer = ensurePeer(receiver.id);
    attachLocalTracks(peer, stream);
    const offer = await peer.createOffer();
    await peer.setLocalDescription(offer);
    logCall('offer sent', offer);
    const requestSignal = {
      callId: data.id,
      receiverId: receiver.id,
      callType,
      payload: { callerName: 'Incoming call', description: offer }
    };
    await sendSignal('call-request', requestSignal);
    clearCallRequestRetry();
    callRequestRetryRef.current = window.setInterval(() => {
      const current = activeRef.current;
      if (!current || current.id !== data.id || current.status !== 'RINGING' || peer.remoteDescription) {
        clearCallRequestRetry();
        return;
      }
      logCall('offer sent', offer);
      sendSignal('call-request', requestSignal);
    }, 2000);
  }, [clearCallRequestRetry, ensurePeer, openMedia, sendSignal]);

  const acceptCall = useCallback(async () => {
    if (!incomingCall) return;
    setError('');
    await api.post(`/calls/${incomingCall.callId}/accept`);
    const nextCall = {
      id: incomingCall.callId,
      callType: incomingCall.callType || 'AUDIO',
      peerId: incomingCall.senderId,
      peer: { id: incomingCall.senderId, name: incomingCall.senderName },
      direction: 'incoming',
      status: 'ACTIVE'
    };
    activeRef.current = nextCall;
    setActiveCall(nextCall);
    setIncomingCall(null);
    clearCallRequestRetry();
    const stream = await openMedia(nextCall.callType);
    const peer = ensurePeer(incomingCall.senderId);
    attachLocalTracks(peer, stream);
    const offer = incomingCall.payload?.description;
    if (offer) {
      logCall('offer received', offer);
      if (peer.signalingState === 'stable') {
        await peer.setRemoteDescription(offer);
        await applyPendingIce(peer);
        const answer = await peer.createAnswer();
        await peer.setLocalDescription(answer);
        logCall('answer sent', answer);
        await sendSignal('answer', {
          callId: incomingCall.callId,
          receiverId: incomingCall.senderId,
          callType: nextCall.callType,
          payload: { description: answer }
        });
      }
    }
    await sendSignal('call-accept', {
      callId: incomingCall.callId,
      receiverId: incomingCall.senderId,
      callType: nextCall.callType
    });
  }, [clearCallRequestRetry, ensurePeer, incomingCall, openMedia, sendSignal]);

  const rejectCall = useCallback(async () => {
    if (!incomingCall) return;
    await api.post(`/calls/${incomingCall.callId}/reject`);
    await sendSignal('call-reject', {
      callId: incomingCall.callId,
      receiverId: incomingCall.senderId,
      callType: incomingCall.callType
    });
    setIncomingCall(null);
  }, [incomingCall, sendSignal]);

  const endCall = useCallback(async (status = 'COMPLETED') => {
    const call = activeRef.current;
    if (!call) return;
    clearCallRequestRetry();
    await sendSignal('call-end', {
      callId: call.id,
      receiverId: call.peerId,
      callType: call.callType
    });
    await api.post(`/calls/${call.id}/end`, null, { params: { status } }).catch(() => {});
    activeRef.current = null;
    setActiveCall(null);
    stopMedia();
  }, [clearCallRequestRetry, sendSignal, stopMedia]);

  const handleSignal = useCallback(async (signal) => {
    if (signal.type === 'call-request') {
      handledIncomingRef.current.add(signal.callId);
      setIncomingCall(signal);
      return;
    }
    if (signal.type === 'call-reject') {
      setError('Call rejected.');
      clearCallRequestRetry();
      activeRef.current = null;
      setActiveCall(null);
      stopMedia();
      return;
    }
    if (signal.type === 'call-end') {
      activeRef.current = null;
      clearCallRequestRetry();
      setActiveCall(null);
      stopMedia();
      return;
    }
    if (signal.type === 'call-accept') {
      const call = activeRef.current;
      if (!call) return;
      clearCallRequestRetry();
      const peer = ensurePeer(call.peerId);
      if (peer.localDescription) {
        logCall('offer sent', peer.localDescription);
        await sendSignal('offer', {
          callId: call.id,
          receiverId: call.peerId,
          callType: call.callType,
          payload: { description: peer.localDescription }
        });
        const active = { ...call, status: 'ACTIVE' };
        activeRef.current = active;
        setActiveCall(active);
        return;
      }
      const offer = await peer.createOffer();
      await peer.setLocalDescription(offer);
      logCall('offer sent', offer);
      await sendSignal('offer', {
        callId: call.id,
        receiverId: call.peerId,
        callType: call.callType,
        payload: { description: offer }
      });
      const active = { ...call, status: 'ACTIVE' };
      activeRef.current = active;
      setActiveCall(active);
      return;
    }
    const peer = ensurePeer(signal.senderId);
    if (signal.type === 'offer') {
      logCall('offer received', signal.payload?.description);
      if (!activeRef.current) {
        setIncomingCall((current) => ({
          ...(current || signal),
          ...signal,
          payload: { ...(current?.payload || {}), ...(signal.payload || {}) }
        }));
        return;
      }
      if (peer.signalingState !== 'stable') return;
      await peer.setRemoteDescription(signal.payload.description);
      await applyPendingIce(peer);
      const answer = await peer.createAnswer();
      await peer.setLocalDescription(answer);
      logCall('answer sent', answer);
      await sendSignal('answer', {
        callId: signal.callId,
        receiverId: signal.senderId,
        callType: signal.callType,
        payload: { description: answer }
      });
    } else if (signal.type === 'answer') {
      logCall('answer received', signal.payload?.description);
      if (peer.signalingState !== 'have-local-offer') return;
      await peer.setRemoteDescription(signal.payload.description);
      await applyPendingIce(peer);
      clearCallRequestRetry();
      if (activeRef.current) {
        const active = { ...activeRef.current, status: 'ACTIVE' };
        activeRef.current = active;
        setActiveCall(active);
      }
    } else if (signal.type === 'ice-candidate' && signal.payload?.candidate) {
      logCall('ICE candidate received', signal.payload.candidate);
      if (peer.remoteDescription) {
        await peer.addIceCandidate(signal.payload.candidate).catch(() => {});
      } else {
        pendingRemoteIceRef.current.push(signal.payload.candidate);
      }
    }
  }, [clearCallRequestRetry, ensurePeer, sendSignal, stopMedia]);

  useEffect(() => {
    if (!token || !uid) return undefined;
    signalRef.current = createSignalClient({ token, uid, onSignal: handleSignal });
    return () => {
      clearCallRequestRetry();
      signalRef.current?.deactivate();
      signalRef.current = null;
    };
  }, [clearCallRequestRetry, handleSignal, token, uid]);

  useEffect(() => {
    if (!token || !uid) return undefined;
    const pollIncoming = async () => {
      if (activeRef.current || incomingCall) return;
      try {
        const { data } = await api.get('/calls/incoming');
        const call = data?.[0];
        if (!call || handledIncomingRef.current.has(call.id)) return;
        handledIncomingRef.current.add(call.id);
        setIncomingCall({
          type: 'call-request',
          callId: call.id,
          callType: call.callType || call.type || 'AUDIO',
          senderId: call.caller?.id || call.callerId,
          senderName: call.caller?.name || 'LoveConnect member',
          payload: {}
        });
      } catch {
        // WebSocket is primary; polling is a quiet mobile fallback.
      }
    };
    const interval = window.setInterval(pollIncoming, 2500);
    pollIncoming();
    return () => window.clearInterval(interval);
  }, [incomingCall, token, uid]);

  const toggleMute = () => {
    localStream?.getAudioTracks().forEach((track) => {
      track.enabled = muted;
    });
    setMuted((value) => !value);
  };

  const toggleCamera = () => {
    localStream?.getVideoTracks().forEach((track) => {
      track.enabled = cameraOff;
    });
    setCameraOff((value) => !value);
  };

  return (
    <CallContext.Provider value={{
      incomingCall,
      activeCall,
      localStream,
      remoteStream,
      muted,
      cameraOff,
      error,
      setError,
      startCall,
      acceptCall,
      rejectCall,
      endCall,
      toggleMute,
      toggleCamera
    }}>
      {children}
    </CallContext.Provider>
  );
}

export const useCall = () => useContext(CallContext);
