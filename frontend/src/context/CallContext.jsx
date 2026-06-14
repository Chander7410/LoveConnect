import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import api from '../services/api.js';
import { appUidFromToken, createPeer, createSignalClient, publishSignal } from '../services/webrtc.js';

const CallContext = createContext(null);
const logCall = (message, detail = '') => {
  console.log(`[LoveConnect Call] ${message}`, detail);
};
const wait = (ms) => new Promise((resolve) => window.setTimeout(resolve, ms));
const emitIncomingCall = (call) => {
  logCall('incoming call event emitted', call?.callId);
  if (call) {
    sessionStorage.setItem('loveconnect_incoming_call', JSON.stringify({ ...call, cachedAt: Date.now() }));
  }
  window.dispatchEvent(new CustomEvent('loveconnect:incomingCall', { detail: call }));
};

const mergeIncomingSignal = (current, signal, pendingOffer = null) => {
  const base = current?.callId === signal.callId ? current : null;
  return {
    ...(base || signal),
    ...signal,
    senderName: base?.senderName || signal.senderName || signal.payload?.callerName,
    payload: {
      ...(signal.payload || {}),
      ...(base?.payload || {}),
      ...(pendingOffer?.payload || {})
    }
  };
};

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
  const incomingCallRef = useRef(null);
  const handledIncomingRef = useRef(new Set());
  const pendingOffersRef = useRef(new Map());
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
    pendingOffersRef.current.clear();
    pendingRemoteIceRef.current = [];
    sessionStorage.removeItem('loveconnect_incoming_call');
  }, []);

  const clearCallRequestRetry = useCallback(() => {
    if (callRequestRetryRef.current) {
      window.clearInterval(callRequestRetryRef.current);
      callRequestRetryRef.current = null;
    }
  }, []);

  const ensurePeer = useCallback(async (receiverId) => {
    if (peerRef.current) return peerRef.current;
    const peer = await createPeer();
    peer.onicecandidate = (event) => {
      if (event.candidate && activeRef.current?.peerId) {
        logCall('ICE candidate sent', event.candidate);
        sendSignal('ice-candidate', {
          callId: activeRef.current.id,
          receiverId: activeRef.current.peerId,
          receiverUid: activeRef.current.peerUid || undefined,
          payload: { candidate: event.candidate }
        });
      }
    };
    peer.ontrack = (event) => {
      const stream = event.streams?.[0] || new MediaStream([event.track]);
      const videoTracks = stream.getVideoTracks();
      const audioTracks = stream.getAudioTracks();
      logCall('remote track received', {
        kind: event.track?.kind,
        id: event.track?.id,
        readyState: event.track?.readyState,
        streamId: stream.id
      });
      logCall('remote stream tracks count', {
        streamId: stream.id,
        videoTracks: videoTracks.length,
        audioTracks: audioTracks.length,
        videoTrackStates: videoTracks.map((track) => `${track.id}:${track.readyState}:${track.enabled}`),
        audioTrackStates: audioTracks.map((track) => `${track.id}:${track.readyState}:${track.enabled}`)
      });
      logCall('remote stream received', stream);
      setRemoteStream((currentStream) => {
        if (currentStream?.id === stream.id) return currentStream;
        return stream;
      });
    };
    peer.onconnectionstatechange = () => {
      logCall('connection state change', peer.connectionState);
      logCall('connectionState', peer.connectionState);
      if (peer.connectionState === 'connected') {
        const receivers = peer.getReceivers().map((receiver) => ({
          kind: receiver.track?.kind,
          id: receiver.track?.id,
          readyState: receiver.track?.readyState,
          enabled: receiver.track?.enabled
        }));
        logCall('connection connected remote receivers', receivers);
      }
      if (['failed', 'disconnected', 'closed'].includes(peer.connectionState)) {
        setError(peer.connectionState === 'failed' ? 'Call connection failed.' : '');
      }
    };
    peer.oniceconnectionstatechange = () => {
      logCall('iceConnectionState', peer.iceConnectionState);
    };
    peer.onsignalingstatechange = () => {
      logCall('signalingState', peer.signalingState);
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
    logCall('local stream ready', stream);
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
      await peer.addIceCandidate(candidate).then(() => logCall('ICE candidate received', candidate)).catch(() => {});
    }
    pendingRemoteIceRef.current = [];
  };

  const answerIncomingOffer = useCallback(async (signal) => {
    const current = activeRef.current;
    logCall('answerIncomingOffer entered', signal?.callId);
    if (!signal?.payload?.description || !current || current.id !== signal.callId) {
      logCall('answerIncomingOffer skipped', {
        hasDescription: Boolean(signal?.payload?.description),
        activeCallId: current?.id,
        signalCallId: signal?.callId
      });
      return false;
    }
    const peer = await ensurePeer(signal.senderId || current.peerId);
    if (peer.signalingState !== 'stable') {
      logCall('offer received while signaling not stable', peer.signalingState);
      return false;
    }
    logCall('offer received', signal.payload.description);
    await peer.setRemoteDescription(signal.payload.description);
    logCall('setRemoteDescription success', signal.callId);
    await applyPendingIce(peer);
    const answer = await peer.createAnswer();
    logCall('createAnswer success', answer);
    await peer.setLocalDescription(answer);
    logCall('setLocalDescription success', answer);
    const answerDestination = signal.senderUid || current.peerUid || signal.senderId || current.peerId;
    logCall('receiver answer publish destination', {
      callId: signal.callId,
      receiverId: signal.senderId || current.peerId,
      receiverUid: signal.senderUid || current.peerUid || undefined,
      destination: answerDestination,
      sdpType: answer.type,
      hasSdp: Boolean(answer.sdp)
    });
    const sent = await sendSignal('answer', {
      callId: signal.callId,
      receiverId: signal.senderId || current.peerId,
      receiverUid: signal.senderUid || current.peerUid || undefined,
      callType: signal.callType || current.callType,
      payload: { description: answer }
    });
    if (!sent) {
      logCall('answer send failed', signal.callId);
      return false;
    }
    logCall('answer sent', answer);
    pendingOffersRef.current.delete(signal.callId);
    return true;
  }, [ensurePeer, sendSignal]);

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
    const receiverUid = receiver.firebaseUid || receiver.uid || receiver.userId || '';
    const { data } = await api.post('/calls/start', { receiverId: receiver.id, type: callType });
    const nextCall = { ...data, callType, peerId: receiver.id, peerUid: receiverUid, peer: receiver, direction: 'outgoing', status: 'RINGING' };
    activeRef.current = nextCall;
    setActiveCall(nextCall);
    const stream = await openMedia(callType);
    const peer = await ensurePeer(receiver.id);
    attachLocalTracks(peer, stream);
    const offer = await peer.createOffer();
    await peer.setLocalDescription(offer);
    logCall('offer sent', offer);
    const requestSignal = {
      callId: data.id,
      receiverId: receiver.id,
      receiverUid: receiverUid || undefined,
      callType,
      payload: { callerName: 'Incoming call', description: offer }
    };
    const offerSignal = {
      callId: data.id,
      receiverId: receiver.id,
      receiverUid: receiverUid || undefined,
      callType,
      payload: { description: offer }
    };
    await sendSignal('call-request', requestSignal);
    await sendSignal('offer', offerSignal);
    clearCallRequestRetry();
    callRequestRetryRef.current = window.setInterval(() => {
      const current = activeRef.current;
      if (!current || current.id !== data.id || current.status !== 'RINGING' || peer.remoteDescription) {
        clearCallRequestRetry();
        return;
      }
      logCall('offer sent', offer);
      sendSignal('call-request', requestSignal);
      sendSignal('offer', offerSignal);
    }, 2000);
  }, [clearCallRequestRetry, ensurePeer, openMedia, sendSignal]);

  const acceptCall = useCallback(async (callOverride = null) => {
    const callToAccept = callOverride || incomingCall;
    logCall('acceptCall() entered', {
      callId: callToAccept?.callId,
      hasOverride: Boolean(callOverride),
      hasPayloadOffer: Boolean(callToAccept?.payload?.description),
      hasPendingOffer: Boolean(callToAccept?.callId && pendingOffersRef.current.has(callToAccept.callId))
    });
    if (!callToAccept) return;
    setError('');
    sessionStorage.removeItem('loveconnect_incoming_call');
    const nextCall = {
      id: callToAccept.callId,
      callType: callToAccept.callType || 'AUDIO',
      peerId: callToAccept.senderId,
      peerUid: callToAccept.senderUid || '',
      peer: { id: callToAccept.senderId, name: callToAccept.senderName },
      direction: 'incoming',
      status: 'ACTIVE'
    };
    activeRef.current = nextCall;
    setActiveCall(nextCall);
    setIncomingCall(null);
    incomingCallRef.current = null;
    clearCallRequestRetry();
    const stream = await openMedia(nextCall.callType);
    const peer = await ensurePeer(callToAccept.senderId);
    attachLocalTracks(peer, stream);
    api.post(`/calls/${callToAccept.callId}/accept`).catch(() => {});
    await sendSignal('call-accept', {
      callId: callToAccept.callId,
      receiverId: callToAccept.senderId,
      receiverUid: callToAccept.senderUid || undefined,
      callType: nextCall.callType
    });
    const pendingOffer = pendingOffersRef.current.get(callToAccept.callId);
    const offerSignal = callToAccept.payload?.description ? callToAccept : pendingOffer;
    if (offerSignal) {
      const answered = await answerIncomingOffer(offerSignal);
      if (!answered) logCall('acceptCall() could not answer offer yet', callToAccept.callId);
    } else {
      logCall('call accepted; waiting for offer', callToAccept.callId);
    }
  }, [answerIncomingOffer, clearCallRequestRetry, ensurePeer, incomingCall, openMedia, sendSignal]);

  const rejectCall = useCallback(async (callOverride = null) => {
    const callToReject = callOverride || incomingCall;
    if (!callToReject) return;
    await api.post(`/calls/${callToReject.callId}/reject`);
    await sendSignal('call-reject', {
      callId: callToReject.callId,
      receiverId: callToReject.senderId,
      receiverUid: callToReject.senderUid || undefined,
      callType: callToReject.callType
    });
    setIncomingCall(null);
    incomingCallRef.current = null;
    sessionStorage.removeItem('loveconnect_incoming_call');
  }, [incomingCall, sendSignal]);

  const endCall = useCallback(async (status = 'COMPLETED') => {
    const call = activeRef.current;
    if (!call) return;
    clearCallRequestRetry();
    await sendSignal('call-end', {
      callId: call.id,
      receiverId: call.peerId,
      receiverUid: call.peerUid || undefined,
      callType: call.callType
    });
    await api.post(`/calls/${call.id}/end`, null, { params: { status } }).catch(() => {});
    activeRef.current = null;
    setActiveCall(null);
    sessionStorage.removeItem('loveconnect_incoming_call');
    stopMedia();
  }, [clearCallRequestRetry, sendSignal, stopMedia]);

  const handleSignal = useCallback(async (signal) => {
    if (signal.type === 'answer') {
      logCall('caller answer event received', {
        callId: signal.callId,
        senderId: signal.senderId,
        senderUid: signal.senderUid,
        receiverId: signal.receiverId,
        receiverUid: signal.receiverUid,
        sdpType: signal.payload?.description?.type,
        hasSdp: Boolean(signal.payload?.description?.sdp)
      });
    }
    if (signal.type === 'call-request') {
      if (activeRef.current?.id === signal.callId) {
        logCall('duplicate call request ignored for active call', signal.callId);
        return;
      }
      handledIncomingRef.current.add(signal.callId);
      const pendingOffer = pendingOffersRef.current.get(signal.callId);
      logCall('incoming call request received', signal.callId);
      const nextIncoming = mergeIncomingSignal(incomingCallRef.current, signal, pendingOffer);
      incomingCallRef.current = nextIncoming;
      setIncomingCall(nextIncoming);
      emitIncomingCall(nextIncoming);
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
      const callWithUid = { ...call, peerUid: signal.senderUid || call.peerUid };
      activeRef.current = callWithUid;
      clearCallRequestRetry();
      const peer = await ensurePeer(call.peerId);
      if (peer.localDescription) {
        logCall('offer sent', peer.localDescription);
        await sendSignal('offer', {
          callId: callWithUid.id,
          receiverId: callWithUid.peerId,
          receiverUid: callWithUid.peerUid || undefined,
          callType: callWithUid.callType,
          payload: { description: peer.localDescription }
        });
        const active = { ...callWithUid, status: 'ACTIVE' };
        activeRef.current = active;
        setActiveCall(active);
        return;
      }
      const offer = await peer.createOffer();
      await peer.setLocalDescription(offer);
      logCall('offer sent', offer);
      await sendSignal('offer', {
        callId: callWithUid.id,
        receiverId: callWithUid.peerId,
        receiverUid: callWithUid.peerUid || undefined,
        callType: callWithUid.callType,
        payload: { description: offer }
      });
      const active = { ...callWithUid, status: 'ACTIVE' };
      activeRef.current = active;
      setActiveCall(active);
      return;
    }
    if (signal.type === 'offer') {
      logCall('offer received', signal.payload?.description);
      pendingOffersRef.current.set(signal.callId, signal);
      if (!activeRef.current) {
        logCall('incoming offer queued', signal.callId);
        const nextIncoming = mergeIncomingSignal(incomingCallRef.current, signal, signal);
        incomingCallRef.current = nextIncoming;
        setIncomingCall(nextIncoming);
        emitIncomingCall(nextIncoming);
        return;
      }
      activeRef.current = { ...activeRef.current, peerUid: signal.senderUid || activeRef.current.peerUid };
      if (activeRef.current.direction === 'incoming') {
        await answerIncomingOffer(signal);
      }
    } else if (signal.type === 'answer') {
      const peer = await ensurePeer(signal.senderId);
      logCall('answer received', signal.payload?.description);
      if (peer.signalingState !== 'have-local-offer') return;
      await peer.setRemoteDescription(signal.payload.description);
      logCall('caller setRemoteDescription(answer) success', signal.callId);
      await applyPendingIce(peer);
      clearCallRequestRetry();
      if (activeRef.current) {
        const active = { ...activeRef.current, peerUid: signal.senderUid || activeRef.current.peerUid, status: 'ACTIVE' };
        activeRef.current = active;
        setActiveCall(active);
      }
    } else if (signal.type === 'ice-candidate' && signal.payload?.candidate) {
      const peer = await ensurePeer(signal.senderId);
      logCall('ICE candidate received', signal.payload.candidate);
      if (peer.remoteDescription) {
        await peer.addIceCandidate(signal.payload.candidate).then(() => logCall('ICE candidate received', signal.payload.candidate)).catch(() => {});
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
        const nextIncoming = {
          type: 'call-request',
          callId: call.id,
          callType: call.callType || call.type || 'AUDIO',
          senderId: call.caller?.id || call.callerId,
          senderName: call.caller?.name || 'LoveConnect member',
          payload: {}
        };
        incomingCallRef.current = nextIncoming;
        setIncomingCall(nextIncoming);
        emitIncomingCall(nextIncoming);
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
