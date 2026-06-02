import React from 'react';
import { Heart, Instagram, ShieldCheck, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="container footer-grid">
        <div>
          <Link className="footer-brand" to="/">
            <Heart size={24} fill="currentColor" /> LoveConnect
          </Link>
          <p className="mt-3 mb-0">
            Premium matching, secure conversations, and elegant profile discovery for modern relationships.
          </p>
        </div>
        <div className="footer-links">
          <Link to="/search"><Sparkles size={16} /> Discover</Link>
          <Link to="/subscription"><ShieldCheck size={16} /> Premium</Link>
          <Link to="/profile"><Instagram size={16} /> Profile</Link>
        </div>
      </div>
    </footer>
  );
}
