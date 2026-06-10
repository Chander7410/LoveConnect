import React from 'react';
import { Link } from 'react-router-dom';
import { Heart, LockKeyhole, MapPin, MessageCircle, Sparkles } from 'lucide-react';

export default function HomePage() {
  return (
    <section className="home-hero maratha-hero">
      <div className="container">
        <div className="row align-items-center min-vh-75">
          <div className="col-lg-7 slide-up">
            <p className="eyebrow">Maratha matchmaking for the USA</p>
            <h1>LoveConnect</h1>
            <p className="lead">Meet Marathi and Maratha singles across the United States through verified profiles, shared values, city-based discovery, and private chat.</p>
            <div className="d-flex gap-3">
              <Link className="btn btn-rose btn-lg" to="/register"><Heart size={18} fill="currentColor" /> Create profile</Link>
              <Link className="btn btn-outline-dark btn-lg" to="/login">Login</Link>
            </div>
            <div className="hero-stats mt-5">
              <div className="stat-card"><Sparkles size={22} /><strong>92%</strong><span>Value match</span></div>
              <div className="stat-card"><MapPin size={22} /><strong>USA</strong><span>City discovery</span></div>
              <div className="stat-card"><MessageCircle size={22} /><strong>Live</strong><span>Private chat</span></div>
              <div className="stat-card"><LockKeyhole size={22} /><strong>Safe</strong><span>Protected accounts</span></div>
            </div>
          </div>
          <div className="col-lg-5 d-none d-lg-block">
            <div className="hero-phone glass-card float">
              <div className="hero-phone-top"></div>
              <div className="hero-match-photo"></div>
              <div className="hero-match-info">
                <span className="interest-chip">Marathi</span>
                <span className="interest-chip">Family values</span>
                <h4>98% Match</h4>
                <p>New Jersey · Pune roots</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
