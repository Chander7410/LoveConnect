import React from 'react';
import { Link } from 'react-router-dom';
import { Heart, LockKeyhole, MessageCircle, Sparkles } from 'lucide-react';

export default function HomePage() {
  return (
    <section className="home-hero">
      <div className="container">
        <div className="row align-items-center min-vh-75">
          <div className="col-lg-7 slide-up">
            <p className="eyebrow">Modern relationships, serious architecture</p>
            <h1>LoveConnect</h1>
            <p className="lead">Discover compatible people through interest-aware recommendations, secure chat, profile discovery, and premium matching tools.</p>
            <div className="d-flex gap-3">
              <Link className="btn btn-rose btn-lg" to="/register"><Heart size={18} fill="currentColor" /> Create profile</Link>
              <Link className="btn btn-outline-dark btn-lg" to="/login">Login</Link>
            </div>
            <div className="hero-stats mt-5">
              <div className="stat-card"><Sparkles size={22} /><strong>92%</strong><span>Compatibility precision</span></div>
              <div className="stat-card"><MessageCircle size={22} /><strong>Live</strong><span>Secure chat</span></div>
              <div className="stat-card"><LockKeyhole size={22} /><strong>JWT</strong><span>Protected accounts</span></div>
            </div>
          </div>
          <div className="col-lg-5 d-none d-lg-block">
            <div className="hero-phone glass-card float">
              <div className="hero-phone-top"></div>
              <div className="hero-match-photo"></div>
              <div className="hero-match-info">
                <span className="interest-chip">Travel</span>
                <span className="interest-chip">Music</span>
                <h4>98% Match</h4>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
