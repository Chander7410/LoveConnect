export const defaultCallSettings = {
  ringingEnabled: true,
  ringMuted: false,
  ringtone: 'classic'
};

const ringtonePatterns = {
  classic: [[0, 660, 0.2], [0.28, 880, 0.2], [0.7, 660, 0.22]],
  soft: [[0, 523, 0.25], [0.35, 659, 0.25], [0.7, 784, 0.25]],
  digital: [[0, 988, 0.12], [0.18, 740, 0.12], [0.36, 988, 0.12], [0.7, 587, 0.18]]
};

const videoRingtonePattern = [[0, 440, 0.16], [0.18, 660, 0.16], [0.36, 880, 0.16], [0.72, 660, 0.2]];

export const getCallSettings = () => {
  try {
    return { ...defaultCallSettings, ...JSON.parse(localStorage.getItem('loveconnect.callSettings') || '{}') };
  } catch {
    return defaultCallSettings;
  }
};

export const createRingtone = () => {
  let context = null;
  let gain = null;
  let interval = null;
  let oscillators = [];

  const stop = () => {
    if (interval) {
      window.clearInterval(interval);
      interval = null;
    }
    oscillators.forEach((oscillator) => {
      try {
        oscillator.stop();
        oscillator.disconnect();
      } catch {
        // Oscillators may already be stopped by their scheduled end time.
      }
    });
    oscillators = [];
    if (gain) {
      try {
        gain.gain.cancelScheduledValues(0);
        gain.disconnect();
      } catch {
        // Audio nodes can be disconnected more than once during cleanup.
      }
      gain = null;
    }
    if (context) {
      context.close().catch(() => {});
      context = null;
    }
  };

  const play = (callType = 'AUDIO') => {
    stop();
    const settings = getCallSettings();
    if (!settings.ringingEnabled || settings.ringMuted) return;
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;

    context = new AudioContext();
    gain = context.createGain();
    gain.gain.value = 0;
    gain.connect(context.destination);

    const playTone = (delay, frequency, duration = 0.22) => {
      const start = context.currentTime + delay;
      const oscillator = context.createOscillator();
      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(frequency, start);
      oscillator.connect(gain);
      gain.gain.setValueAtTime(0.0001, start);
      gain.gain.exponentialRampToValueAtTime(0.08, start + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
      oscillator.start(start);
      oscillator.stop(start + duration + 0.04);
      oscillators.push(oscillator);
      oscillator.onended = () => {
        oscillators = oscillators.filter((item) => item !== oscillator);
      };
    };

    const playPattern = () => {
      const pattern = callType === 'VIDEO'
        ? videoRingtonePattern
        : (ringtonePatterns[settings.ringtone] || ringtonePatterns.classic);
      pattern.forEach(([delay, frequency, duration]) => playTone(delay, frequency, duration));
    };

    context.resume?.().catch(() => {});
    playPattern();
    interval = window.setInterval(playPattern, 1400);
  };

  return { play, stop };
};
