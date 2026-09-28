import React from 'react';
import { NavLink } from 'react-router-dom';
import './Navbar.css';

const Navbar: React.FC = () => (
  <nav className="navbar">
    <NavLink to="/" end className={({ isActive }) => (isActive ? 'active' : undefined)}>
      Dashboard
    </NavLink>
    <NavLink to="/analysis" className={({ isActive }) => (isActive ? 'active' : undefined)}>
      Analyze Incident
    </NavLink>
    <NavLink to="/comparison" className={({ isActive }) => (isActive ? 'active' : undefined)}>
      Memory Comparison
    </NavLink>
    <NavLink to="/postmortem" className={({ isActive }) => (isActive ? 'active' : undefined)}>
      Postmortem
    </NavLink>
    <NavLink to="/demo" className={({ isActive }) => (isActive ? 'active' : undefined)}>
      Demo
    </NavLink>
  </nav>
);

export default Navbar;
