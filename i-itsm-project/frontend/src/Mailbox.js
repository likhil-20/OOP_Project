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
    const timer = setInterval(load, 5000);
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
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
      <div className="dashboard-card" style={{ width: '960px', maxWidth: '95vw', height: '80vh', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', borderBottom: '1px solid #e2e8f0' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>✉ Mailbox</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {isAdmin && (
              <>
                <button onClick={() => { setShowAll(false); setSelectedId(null); }} className={`btn ${!showAll ? 'btn-primary' : 'btn-secondary'}`}>My inbox</button>
                <button onClick={() => { setShowAll(true); setSelectedId(null); }} className={`btn ${showAll ? 'btn-primary' : 'btn-secondary'}`}>All sent emails</button>
              </>
            )}
            <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '24px', cursor: 'pointer', color: '#64748b', lineHeight: 1, marginLeft: '8px' }}>&times;</button>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '340px 1fr', flex: 1, minHeight: 0 }}>

          <div style={{ borderRight: '1px solid #e2e8f0', overflowY: 'auto' }}>
            {emails.length === 0 && (
              <div style={{ padding: '32px', textAlign: 'center', color: '#94a3b8', fontSize: '14px' }}>No emails yet.</div>
            )}
            {emails.map(m => (
              <div key={m.id} onClick={() => openMail(m)}
                   style={{ padding: '14px 18px', borderBottom: '1px solid #f1f5f9', cursor: 'pointer', background: m.id === selectedId ? '#eef2ff' : (isUnread(m) ? '#f8fafc' : '#fff') }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#64748b', marginBottom: '4px' }}>
                  <span style={{ fontWeight: isUnread(m) ? '700' : '400', color: isUnread(m) ? '#0f172a' : '#64748b' }}>
                    {showAll ? `To: ${m.recipientEmail}` : 'I-ITSM Service Desk'}
                  </span>
                  <span>{new Date(m.sentAt).toLocaleString()}</span>
                </div>
                <div style={{ fontSize: '14px', fontWeight: isUnread(m) ? '700' : '500', color: '#1e293b' }}>
                  {isUnread(m) && <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', background: '#4f46e5', marginRight: '8px' }}></span>}
                  {m.subject}
                </div>
              </div>
            ))}
          </div>

          <div style={{ overflowY: 'auto', padding: '24px 32px' }}>
            {!selected ? (
              <div style={{ textAlign: 'center', color: '#94a3b8', marginTop: '80px', fontSize: '14px' }}>Select an email to read it.</div>
            ) : (
              <div>
                <h3 style={{ margin: '0 0 16px 0', fontSize: '20px', color: '#0f172a' }}>{selected.subject}</h3>
                <div style={{ fontSize: '13px', color: '#64748b', lineHeight: '1.8', paddingBottom: '16px', marginBottom: '20px', borderBottom: '1px solid #e2e8f0' }}>
                  <div><strong>From:</strong> I-ITSM Service Desk &lt;noreply@iitsm.local&gt;</div>
                  <div><strong>To:</strong> {selected.recipientEmail}</div>
                  <div><strong>Date:</strong> {new Date(selected.sentAt).toLocaleString()}</div>
                </div>
                <div style={{ whiteSpace: 'pre-wrap', fontSize: '14px', lineHeight: '1.7', color: '#1e293b' }}>{selected.body}</div>
              </div>
            )}
          </div>

        </div>
      </div>
    </div>
  );
}