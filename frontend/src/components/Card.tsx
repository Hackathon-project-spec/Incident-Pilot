/* src/components/Card.tsx */
import React from 'react';
import './Card.css';

type CardProps = {
  title?: string;
  children: React.ReactNode;
};

const Card: React.FC<CardProps> = ({ title, children }) => (
  <div className="card">
    {title && <h2>{title}</h2>}
    {children}
  </div>
);

export default Card;
