import React, { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Footer from './components/Footer.jsx';
import AudioCallScreen from './components/AudioCallScreen.jsx';
import IncomingCallModal from './components/IncomingCallModal.jsx';
import VideoCallScreen from './components/VideoCallScreen.jsx';
import { CallProvider } from './context/CallContext.jsx';
import HomePage from './pages/HomePage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import SearchPage from './pages/SearchPage.jsx';
import MatchesPage from './pages/MatchesPage.jsx';
import ChatPage from './pages/ChatPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import SubscriptionPage from './pages/SubscriptionPage.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import api, { currentUser } from './services/api.js';

function PrivateRoute({ children, user }) {
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  const [user, setUser] = useState(currentUser);

  useEffect(() => {
    const syncUser = () => setUser(currentUser());
    window.addEventListener('loveconnect:sessionChanged', syncUser);
    window.addEventListener('storage', syncUser);
    return () => {
      window.removeEventListener('loveconnect:sessionChanged', syncUser);
      window.removeEventListener('storage', syncUser);
    };
  }, []);

  useEffect(() => {
    if (!user) return undefined;
    const markOnline = () => {
      if (document.visibilityState === 'visible') {
        api.post('/auth/presence').catch(() => {});
      }
    };
    markOnline();
    const interval = window.setInterval(markOnline, 30000);
    document.addEventListener('visibilitychange', markOnline);
    return () => {
      window.clearInterval(interval);
      document.removeEventListener('visibilitychange', markOnline);
    };
  }, [user]);

  return (
    <BrowserRouter>
      <CallProvider>
        <Navbar />
        <IncomingCallModal />
        <AudioCallScreen />
        <VideoCallScreen />
        <main className="app-shell">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/profile" element={<PrivateRoute user={user}><ProfilePage /></PrivateRoute>} />
            <Route path="/search" element={<PrivateRoute user={user}><SearchPage /></PrivateRoute>} />
            <Route path="/matches" element={<PrivateRoute user={user}><MatchesPage /></PrivateRoute>} />
            <Route path="/chat" element={<PrivateRoute user={user}><ChatPage /></PrivateRoute>} />
            <Route path="/notifications" element={<PrivateRoute user={user}><NotificationsPage /></PrivateRoute>} />
            <Route path="/subscription" element={<PrivateRoute user={user}><SubscriptionPage /></PrivateRoute>} />
            <Route path="/admin" element={<PrivateRoute user={user}><AdminDashboard /></PrivateRoute>} />
          </Routes>
        </main>
        <Footer />
      </CallProvider>
    </BrowserRouter>
  );
}
