import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API = 'http://localhost:8080/api';

export default function Mailbox({ user, onClose, onChanged }) {
  const [emails, setEmails] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [showAll, setShowAll] = useState(false);
  const isAdmin = user.role === 'ADMINISTRATOR';

  const load = async () => {
    try {
      const res = await axios.get(`${API}${showAll ? '/admin/emails' : '/emails'}`);
      setEmails(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => {
    load();
    const timer = setInterval(() => { if (!document.hidden) load(); }, 15000);
    return () => clearInterval(timer);
  }, [showAll]);

  const isUnread = (m) => !m.opened && m.recipientEmail === user.email;

  const openMail = async (m) => {
    setSelectedId(m.id);
    if (isUnread(m)) {
      try {
        await axios.put(`${API}/emails/${m.id}/open`);
        load();
        if (onChanged) onChanged();
      } catch (e) {
        console.error(e);
      }
    }
  };

  const selected = emails.find(m => m.id === selectedId);

    return (
    <div className="modal-backdrop">
      <div className="dashboard-card modal-card mail">
        <div className="mail-top">
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 700 }}>Mailbox</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {isAdmin && (
              <>
                <button onClick={() => { setShowAll(false); setSelectedId(null); }} className={`btn ${!showAll ? 'btn-primary' : 'btn-secondary'}`}>My inbox</button>
                <button onClick={() => { setShowAll(true); setSelectedId(null); }} className={`btn ${showAll ? 'btn-primary' : 'btn-secondary'}`}>All sent emails</button>
              </>
            )}
            <button onClick={onClose} className="close-btn" aria-label="Close mailbox" style={{ marginLeft: '8px' }}>&times;</button>
          </div>
        </div>

        <div className="mail-grid">
          <div className="mail-list">
            {emails.length === 0 && <div className="mail-empty">No emails yet.</div>}
            {emails.map(m => (
              <div key={m.id} onClick={() => openMail(m)}
                   className={`mail-item ${isUnread(m) ? 'unread' : ''} ${m.id === selectedId ? 'selected' : ''}`}>
                <div className="mail-item-meta">
                  <span>{showAll ? `To: ${m.recipientEmail}` : 'I-ITSM Service Desk'}</span>
                  <span>{new Date(m.sentAt).toLocaleString()}</span>
                </div>
                <div className="mail-subject">
                  {isUnread(m) && <span className="mail-dot"></span>}
                  {m.subject}
                </div>
              </div>
            ))}
          </div>

          <div className="mail-view">
            {!selected ? (
              <div className="mail-empty" style={{ marginTop: '60px' }}>Select an email to read it.</div>
            ) : (
              <div>
                <h3 style={{ margin: '0 0 16px', fontSize: '20px' }}>{selected.subject}</h3>
                <div className="mail-headers">
                  <div><strong>From:</strong> I-ITSM Service Desk &lt;noreply@iitsm.local&gt;</div>
                  <div><strong>To:</strong> {selected.recipientEmail}</div>
                  <div><strong>Date:</strong> {new Date(selected.sentAt).toLocaleString()}</div>
                </div>
                <div className="mail-body">{selected.body}</div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}