import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_ORIGIN } from './api.js';

const turnServer = import.meta.env.VITE_TURN_URL
  ? {
      urls: import.meta.env.VITE_TURN_URL,
      username: import.meta.env.VITE_TURN_USERNAME || undefined,
      credential: import.meta.env.VITE_TURN_CREDENTIAL || undefined
  }
  : null;

const meteredTurnCredential = {
  username: '49d1ee84361e0e0f0b55ec2d',
  credential: 'VW1TsEgIT3CsX3Hb'
};

export const rtcConfig = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:global.stun.twilio.com:3478' },
    { urls: 'stun:stun.relay.metered.ca:80' },
    { urls: 'turn:global.relay.metered.ca:80', ...meteredTurnCredential },
    { urls: 'turn:global.relay.metered.ca:80?transport=tcp', ...meteredTurnCredential },
    { urls: 'turn:global.relay.metered.ca:443', ...meteredTurnCredential },
    { urls: 'turn:global.relay.metered.ca:443?transport=tcp', ...meteredTurnCredential },
    { urls: 'turns:global.relay.metered.ca:443?transport=tcp', ...meteredTurnCredential },
    { urls: 'stun:openrelay.metered.ca:80' },
    { urls: 'turn:openrelay.metered.ca:80', username: 'openrelayproject', credential: 'openrelayproject' },
    { urls: 'turn:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' },
    { urls: 'turn:openrelay.metered.ca:443?transport=tcp', username: 'openrelayproject', credential: 'openrelayproject' },
    { urls: 'turns:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' },
    ...(turnServer ? [turnServer] : [])
  ]
};

const loadIceServers = async () => {
  const credentialsUrl = import.meta.env.VITE_TURN_CREDENTIALS_URL;
  if (!credentialsUrl) return rtcConfig.iceServers;
  try {
    const response = await fetch(credentialsUrl);
    if (!response.ok) throw new Error(`TURN credentials request failed: ${response.status}`);
    const iceServers = await response.json();
    if (!Array.isArray(iceServers) || iceServers.length === 0) throw new Error('TURN credentials response is empty');
    console.log('[LoveConnect Call] TURN/STUN iceServers loaded', iceServers.map((server) => server.urls));
    return iceServers;
  } catch (error) {
    console.warn('[LoveConnect Call] TURN credentials unavailable; falling back to configured STUN/TURN', error);
    return rtcConfig.iceServers;
  }
};

export const createPeer = async () => new RTCPeerConnection({
  iceServers: await loadIceServers()
});

export const createSignalClient = ({ token, uid, onSignal }) => {
  const client = new Client({
    webSocketFactory: () => new SockJS(`${API_ORIGIN}/ws`),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    onConnect: () => {
      console.log('[LoveConnect Call] WebSocket connected', uid);
      client.subscribe(`/topic/signaling/${uid}`, (message) => {
        onSignal(JSON.parse(message.body));
      });
    },
    onStompError: (frame) => console.error('[LoveConnect Call] WebSocket STOMP error', frame.headers?.message || frame.body),
    onWebSocketClose: () => console.warn('[LoveConnect Call] WebSocket closed')
  });
  client.activate();
  return client;
};

export const publishSignal = (client, type, body) => {
  if (!client?.connected) return false;
  client.publish({
    destination: `/app/signal/${type}`,
    body: JSON.stringify(body)
  });
  return true;
};

export const appUidFromToken = (token) => {
  if (!token?.startsWith('app:')) return '';
  const encoded = token.split(':')[1];
  if (!encoded) return '';
  const normalized = encoded.replace(/-/g, '+').replace(/_/g, '/');
  return atob(normalized);
};
