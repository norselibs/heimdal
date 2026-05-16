import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import HeimdalApp from './HeimdalApp.jsx';
import './style.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <BrowserRouter>
    <HeimdalApp />
  </BrowserRouter>
);
