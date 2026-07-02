import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import api from '../services/api.js';
import { appUidFromToken, createPeer, createSignalClient, publishSignal } from '../services/webrtc.js';
import { createRingtone } from '../utils/callRingtone.js';

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
  const remoteStreamRef = useRef(null);
  const callRequestRetryRef = useRef(null);
  const ringtoneRef = useRef(null);
  const ringingCallIdRef = useRef(null);

  const token = localStorage.getItem('loveconnect_token') || sessionStorage.getItem('loveconnect_token');
  const uid = appUidFromToken(token);

  const waitForSignalConnection = useCallback(async () => {
    const deadline = Date.now() + 15000;
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
    remoteStreamRef.current?.getTracks().forEach((track) => track.stop());
    remoteStreamRef.current = null;
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

  const stopRinging = useCallback(() => {
    ringtoneRef.current?.stop();
    ringingCallIdRef.current = null;
  }, []);

  const playRinging = useCallback((callType, callId = '') => {
    if (callId && ringingCallIdRef.current === callId) return;
    if (!ringtoneRef.current) ringtoneRef.current = createRingtone();
    ringingCallIdRef.current = callId || 'active-call';
    ringtoneRef.current.play(callType);
  }, []);

  const publishRemoteStream = useCallback(() => {
    const stream = remoteStreamRef.current;
    if (!stream) {
      setRemoteStream(null);
      return;
    }
    const snapshot = new MediaStream(stream.getTracks());
    logCall('remote stream snapshot published', {
      streamId: snapshot.id,
      videoTracks: snapshot.getVideoTracks().map((track) => `${track.id}:${track.readyState}:${track.enabled}`),
      audioTracks: snapshot.getAudioTracks().map((track) => `${track.id}:${track.readyState}:${track.enabled}`)
    });
    setRemoteStream(snapshot);
  }, []);

  const markCallConnected = useCallback((reason = '') => {
    const current = activeRef.current;
    if (!current || current.status === 'ACTIVE') return;
    const active = { ...current, status: 'ACTIVE' };
    activeRef.current = active;
    setActiveCall(active);
    stopRinging();
    clearCallRequestRetry();
    logCall('call marked active', reason);
  }, [clearCallRequestRetry, stopRinging]);

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
      if (!remoteStreamRef.current) remoteStreamRef.current = new MediaStream();
      const remoteStreamTarget = remoteStreamRef.current;
      const incomingTracks = new Map();
      if (event.track) incomingTracks.set(event.track.id, event.track);
      (event.streams || []).forEach((stream) => {
        stream.getTracks().forEach((track) => incomingTracks.set(track.id, track));
      });
      incomingTracks.forEach((track) => {
        const exists = remoteStreamTarget.getTracks().some((currentTrack) => currentTrack.id === track.id);
        if (!exists) {
          remoteStreamTarget.addTrack(track);
          track.onunmute = () => {
            logCall('remote track unmuted', { kind: track.kind, id: track.id });
            publishRemoteStream();
            markCallConnected('remote-track-unmuted');
          };
          track.onended = () => {
            logCall('remote track ended', { kind: track.kind, id: track.id });
            publishRemoteStream();
          };
        }
      });
      const videoTracks = remoteStreamTarget.getVideoTracks();
      const audioTracks = remoteStreamTarget.getAudioTracks();
      logCall('remote track received', {
        kind: event.track?.kind,
        id: event.track?.id,
        readyState: event.track?.readyState,
        incomingStreamIds: (event.streams || []).map((stream) => stream.id),
        remoteStreamId: remoteStreamTarget.id
      });
      logCall('remote stream tracks count', {
        streamId: remoteStreamTarget.id,
        videoTracks: videoTracks.length,
        audioTracks: audioTracks.length,
        videoTrackStates: videoTracks.map((track) => `${track.id}:${track.readyState}:${track.enabled}`),
        audioTrackStates: audioTracks.map((track) => `${track.id}:${track.readyState}:${track.enabled}`)
      });
      logCall('remote stream received', remoteStreamTarget);
      publishRemoteStream();
      markCallConnected('remote-track');
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
        markCallConnected('peer-connected');
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
  }, [markCallConnected, publishRemoteStream, sendSignal]);

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
    const signalReady = await waitForSignalConnection();
    if (!signalReady) {
      setError('Call server is not connected. Backend/WebSocket is offline or starting. Please wait and try again.');
      logCall('call start blocked; WebSocket not connected', receiver.id);
      return;
    }
    const receiverUid = receiver.firebaseUid || receiver.uid || receiver.userId || '';
    const { data } = await api.post('/calls/start', { receiverId: receiver.id, type: callType });
    const nextCall = { ...data, callType, peerId: receiver.id, peerUid: receiverUid, peer: receiver, direction: 'outgoing', status: 'RINGING' };
    activeRef.current = nextCall;
    setActiveCall(nextCall);
    playRinging(callType, data.id);
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
    const requestSent = await sendSignal('call-request', requestSignal);
    const offerSent = requestSent ? await sendSignal('offer', offerSignal) : false;
    if (!requestSent || !offerSent) {
      logCall('call start cancelled; initial signaling failed', {
        callId: data.id,
        requestSent,
        offerSent
      });
      stopRinging();
      await api.post(`/calls/${data.id}/end`, null, { params: { status: 'FAILED' } }).catch(() => {});
      activeRef.current = null;
      setActiveCall(null);
      stopMedia();
      setError('Call server is not connected. Please refresh after backend is running and try again.');
      return;
    }
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
  }, [clearCallRequestRetry, ensurePeer, openMedia, sendSignal, stopMedia, stopRinging, waitForSignalConnection]);

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
    stopRinging();
    sessionStorage.removeItem('loveconnect_incoming_call');
    const nextCall = {
      id: callToAccept.callId,
      callType: callToAccept.callType || 'AUDIO',
      peerId: callToAccept.senderId,
      peerUid: callToAccept.senderUid || '',
      peer: { id: callToAccept.senderId, name: callToAccept.senderName },
      direction: 'incoming',
      status: 'CONNECTING'
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
    stopRinging();
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
    stopRinging();
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
      playRinging(nextIncoming.callType || signal.callType || 'AUDIO', signal.callId);
      return;
    }
    if (signal.type === 'call-reject') {
      setError('Call rejected.');
      stopRinging();
      clearCallRequestRetry();
      activeRef.current = null;
      setActiveCall(null);
      stopMedia();
      return;
    }
    if (signal.type === 'call-end') {
      activeRef.current = null;
      stopRinging();
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
      stopRinging();
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
        const active = { ...callWithUid, status: 'CONNECTING' };
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
      const active = { ...callWithUid, status: 'CONNECTING' };
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
        playRinging(nextIncoming.callType || signal.callType || 'AUDIO', signal.callId);
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
        stopRinging();
        const active = {
          ...activeRef.current,
          peerUid: signal.senderUid || activeRef.current.peerUid,
          status: activeRef.current.status === 'ACTIVE' ? 'ACTIVE' : 'CONNECTING'
        };
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
  }, [clearCallRequestRetry, ensurePeer, playRinging, sendSignal, stopMedia, stopRinging]);

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
        playRinging(nextIncoming.callType || 'AUDIO', call.id);
      } catch {
        // WebSocket is primary; polling is a quiet mobile fallback.
      }
    };
    const interval = window.setInterval(pollIncoming, 2500);
    pollIncoming();
    return () => window.clearInterval(interval);
  }, [incomingCall, playRinging, token, uid]);

  useEffect(() => {
    ringtoneRef.current = createRingtone();
    return () => ringtoneRef.current?.stop();
  }, []);

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
