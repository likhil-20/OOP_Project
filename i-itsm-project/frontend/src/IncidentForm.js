import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';

const API = 'http://localhost:8080/api';
const OTHER = 'OTHERS';
const MAX_OTHER_CATEGORY = 50;   // matches backend category limit
const MAX_OTHER_SERVICE = 100;   // matches backend service name limit
const MAX_DESCRIPTION = 1800;    // leaves room under the backend 2000 limit for the "Others" tags

// Defaults: Impact LOW, Urgency LOW, Category "Select", Service "Select"
const EMPTY = {
  category: '',
  otherCategory: '',
  serviceId: '',
  otherService: '',
  impact: 'LOW',
  urgency: 'LOW',
  affectedUsers: '1',
  description: ''
};

// const labelStyle = { display: 'block', fontSize: '13px', fontWeight: '600', marginBottom: '8px', color: '#334155' };
// const counterStyle = { fontSize: '11px', color: '#94a3b8', textAlign: 'right', marginTop: '4px' };

// Converts a recording into a small 16 kHz mono WAV file (base64) that Gemini accepts
async function blobToWavBase64(blob) {
  const ctx = new (window.AudioContext || window.webkitAudioContext)();
  const decoded = await ctx.decodeAudioData(await blob.arrayBuffer());
  ctx.close();
  const rate = 16000;
  const offline = new OfflineAudioContext(1, Math.ceil(decoded.duration * rate), rate);
  const src = offline.createBufferSource();
  src.buffer = decoded;
  src.connect(offline.destination);
  src.start();
  const pcm = (await offline.startRendering()).getChannelData(0);

  const view = new DataView(new ArrayBuffer(44 + pcm.length * 2));
  const text = (o, s) => { for (let i = 0; i < s.length; i++) view.setUint8(o + i, s.charCodeAt(i)); };
  text(0, 'RIFF'); view.setUint32(4, 36 + pcm.length * 2, true); text(8, 'WAVE'); text(12, 'fmt ');
  view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true);
  view.setUint32(24, rate, true); view.setUint32(28, rate * 2, true);
  view.setUint16(32, 2, true); view.setUint16(34, 16, true);
  text(36, 'data'); view.setUint32(40, pcm.length * 2, true);
  for (let i = 0; i < pcm.length; i++) {
    const s = Math.max(-1, Math.min(1, pcm[i]));
    view.setInt16(44 + i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }

  const bytes = new Uint8Array(view.buffer);
  let bin = '';
  for (let i = 0; i < bytes.length; i += 0x8000) {
    bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  }
  return btoa(bin);
}

export default function IncidentForm({ user, categories, services, onSubmitted }) {
  const [form, setForm] = useState(EMPTY);
  const [suggested, setSuggested] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const lock = useRef(false);
  const debounce = useRef(null);

  // AI triage + voice
  const [triage, setTriage] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [listening, setListening] = useState(false);
  const [transcribing, setTranscribing] = useState(false);
  const recorder = useRef(null);
  const chunks = useRef([]);
  const cancelled = useRef(false);
  const stopTimer = useRef(null);
  const descRef = useRef('');
  descRef.current = form.description;

  useEffect(() => () => {
    clearTimeout(stopTimer.current);
    if (recorder.current) {
      cancelled.current = true;
      if (recorder.current.state === 'recording') recorder.current.stop();
    }
  }, []);

  const setField = (field, value) => setForm(f => ({ ...f, [field]: value }));
  const isDirty = JSON.stringify(form) !== JSON.stringify(EMPTY);

  const resetForm = () => {
    cancelListening();
    setTriage(null);
    setForm(EMPTY);
    setSuggested([]);
  };

  const handleClear = () => resetForm();

  const handleDiscard = () => {
    if (isDirty && !window.confirm('Discard this ticket? Everything you typed will be lost.')) return;
    resetForm();
  };

  const runTriage = async (text) => {
    const desc = (text ?? form.description).trim();
    if (desc.length < 5) return alert('Describe the problem in a few words first.');
    setAnalyzing(true);
    try {
      const res = await axios.post(`${API}/incidents/triage`, { description: desc });
      const t = res.data;
      const match = categories.find(c => c.name.toLowerCase() === String(t.category).toLowerCase());
      const svc = services.find(s => s.id === t.serviceId);
      setForm(f => ({
        ...f,
        category: match ? match.name : OTHER,
        serviceId: svc ? String(svc.id) : f.serviceId,
        impact: t.impact || f.impact,
        urgency: t.urgency || f.urgency
      }));
      setTriage(t);
    } catch (err) {
      alert(err.response?.data?.error || 'AI triage is unavailable right now. Please fill the form manually.');
    } finally {
      setAnalyzing(false);
    }
  };

    const transcribe = async (blob) => {
    setTranscribing(true);
    try {
      const audio = await blobToWavBase64(blob);
      const res = await axios.post(`${API}/incidents/transcribe`, { audio, mimeType: 'audio/wav' });
      const spoken = String(res.data.text || '').trim();
      if (!spoken) return alert('No speech was heard. Please try again.');
      const base = descRef.current ? descRef.current.trim() + ' ' : '';
      const full = (base + spoken).slice(0, MAX_DESCRIPTION);
      setField('description', full);
      await runTriage(full);
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.error || ('Recording error: ' + (err.name || '') + ' ' + (err.message || '')));
    } finally {
      setTranscribing(false);
    }
  };

  const startListening = async () => {
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      return alert('This browser cannot record audio.');
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      chunks.current = [];
      const rec = new MediaRecorder(stream);
      rec.ondataavailable = (ev) => { if (ev.data.size > 0) chunks.current.push(ev.data); };
      rec.onstop = async () => {
        clearTimeout(stopTimer.current);
        stream.getTracks().forEach(t => t.stop());
        setListening(false);
        recorder.current = null;
        if (cancelled.current) { cancelled.current = false; return; }
        const blob = new Blob(chunks.current, { type: rec.mimeType });
        if (blob.size < 1000) return alert('Nothing was recorded. Check your microphone.');
        await transcribe(blob);
      };
      recorder.current = rec;
      rec.start();
      setListening(true);
      stopTimer.current = setTimeout(() => { if (rec.state === 'recording') rec.stop(); }, 60000); // 1 minute max
    } catch (e) {
      alert('Microphone error: ' + e.name + '. Allow the microphone for this site and try again.');
    }
  };

  const stopListening = () => {
    if (recorder.current && recorder.current.state === 'recording') recorder.current.stop();
  };

  const cancelListening = () => {
    if (recorder.current) {
      cancelled.current = true;
      if (recorder.current.state === 'recording') recorder.current.stop();
    }
    setListening(false);
  };

  const fetchSuggestions = async (cat, desc) => {
    if (!desc || desc.length < 3) {
      setSuggested([]);
      return;
    }
    try {
      const res = await axios.get(`${API}/incidents/suggested-resolution`, {
        params: { category: cat === OTHER ? '' : cat, description: desc }
      });
      setSuggested(res.data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (lock.current) return;

    if (!form.category) return alert('Please select a category.');
    if (form.category === OTHER && !form.otherCategory.trim()) {
      return alert('You chose "Others" for category. Please specify the category.');
    }
    if (!form.serviceId) return alert('Please select an affected IT service.');
    if (form.serviceId === OTHER && !form.otherService.trim()) {
      return alert('You chose "Others" for service. Please specify the service.');
    }
    const users = Number(form.affectedUsers);
    if (!users || users < 1 || users > 10000) {
      return alert('Affected users must be between 1 and 10000.');
    }
    if (form.description.trim().length < 5) {
      return alert('Description must be at least 5 characters.');
    }

    // The backend has no field for "other" text, so it is added to the start of the description
    const tags = [];
    if (form.category === OTHER) tags.push(`[Other category: ${form.otherCategory.trim()}]`);
    if (form.serviceId === OTHER) tags.push(`[Other service: ${form.otherService.trim()}]`);
    const fullDescription = tags.length ? `${tags.join(' ')} ${form.description.trim()}` : form.description.trim();

    lock.current = true;
    setSubmitting(true);
    try {
      await axios.post(`${API}/incidents`, {
        email: user.email,
        category: form.category === OTHER ? 'Others' : form.category,
        serviceId: form.serviceId === OTHER ? null : Number(form.serviceId),
        impact: form.impact,
        urgency: form.urgency,
        description: fullDescription,
        affectedUsers: users
      });
      resetForm();                     // clear EVERYTHING after a successful submit
      if (onSubmitted) onSubmitted();
      alert('Incident submitted successfully!');
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to create incident.');
    } finally {
      lock.current = false;
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
      <div>
        <label className="form-label">Category</label>
        <select className="input-field" required value={form.category} onChange={e => setField('category', e.target.value)}>
          <option value="" disabled>Select category</option>
          {categories.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
          <option value={OTHER}>Others</option>
        </select>
        {form.category === OTHER && (
          <div style={{ marginTop: '8px' }}>
            <input type="text" className="input-field" required placeholder="Specify the category..."
              maxLength={MAX_OTHER_CATEGORY} value={form.otherCategory}
              onChange={e => setField('otherCategory', e.target.value)} />
            <div className="char-count">{form.otherCategory.length}/{MAX_OTHER_CATEGORY}</div>
          </div>
        )}
      </div>

      <div>
        <label className="form-label">Affected Service</label>
        <select className="input-field" required value={form.serviceId} onChange={e => setField('serviceId', e.target.value)}>
          <option value="" disabled>Select service</option>
          {services.map(s => <option key={s.id} value={s.id}>{s.serviceName} ({s.businessCriticality})</option>)}
          <option value={OTHER}>Others</option>
        </select>
        {form.serviceId === OTHER && (
          <div style={{ marginTop: '8px' }}>
            <input type="text" className="input-field" required placeholder="Specify the service..."
              maxLength={MAX_OTHER_SERVICE} value={form.otherService}
              onChange={e => setField('otherService', e.target.value)} />
            <div className="char-count">{form.otherService.length}/{MAX_OTHER_SERVICE}</div>
          </div>
        )}
      </div>

      <div>
        <label className="form-label">Impact</label>
        <select className="input-field" value={form.impact} onChange={e => setField('impact', e.target.value)}>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      <div>
        <label className="form-label">Urgency</label>
        <select className="input-field" value={form.urgency} onChange={e => setField('urgency', e.target.value)}>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      <div>
        <label className="form-label">Number of Affected Users</label>
        <input type="number" min="1" max="10000" className="input-field" value={form.affectedUsers}
          onChange={e => setField('affectedUsers', e.target.value)} />
      </div>

      <div style={{ gridColumn: 'span 2' }}>
        <label className="form-label">Description</label>
        <textarea className="input-field" required rows="3" maxLength={MAX_DESCRIPTION} value={form.description}
          placeholder="Provide clear details regarding the disruption..."
          onChange={e => {
            const v = e.target.value;
            setField('description', v);
            clearTimeout(debounce.current);
            debounce.current = setTimeout(() => fetchSuggestions(form.category, v), 600);
          }}></textarea>
        <div className="char-count">{form.description.length}/{MAX_DESCRIPTION}</div>

        <div className="ai-bar">
          <button type="button" className="btn btn-secondary" onClick={() => runTriage()} disabled={analyzing || submitting}>
            {analyzing ? 'Analysing…' : 'Suggest category and priority with AI'}
          </button>
          <button type="button" className={`btn ${listening ? 'btn-danger' : 'btn-secondary'}`}
            onClick={listening ? stopListening : startListening} disabled={submitting || transcribing || analyzing}>
            {transcribing ? 'Transcribing…' : listening ? 'Stop recording' : 'Speak the problem'}
          </button>
        </div>
        {triage && (
          <div className="ai-panel">
            <div className="ai-head">
              <strong>Suggested: {triage.category} · impact {String(triage.impact).toLowerCase()} · urgency {String(triage.urgency).toLowerCase()}</strong>
              <span className="ai-source">{triage.source === 'GEMINI' ? 'Gemini' : 'Rule-based fallback'}</span>
            </div>
            <div className="conf"><i style={{ width: `${triage.confidence}%` }} /></div>
            <div className="ai-note">{triage.confidence}% confidence. {triage.reason} The fields above were filled in for you; please review them before submitting.</div>
          </div>
        )}
                {suggested.length > 0 && (
          <div className="suggest-box">
            <div className="suggest-title">Suggested resolutions from similar past incidents.</div>
            {suggested.map((s, idx) => (
              <div key={idx} className="suggest-item">
                <div><strong>Past Issue #{s.matchedIncidentId}:</strong> {s.pastDescription}</div>
                <div className="suggest-res"><strong>Resolution:</strong> {s.resolutionDetails}{s.feedbackRating ? ` (rated ${s.feedbackRating}/5 by the employee)` : ''}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div style={{ gridColumn: 'span 2', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
        <button type="button" className="btn btn-danger" style={{ padding: '10px 24px' }} onClick={handleDiscard} disabled={submitting}>Discard Ticket</button>
        <button type="button" className="btn btn-secondary" style={{ padding: '10px 24px' }} onClick={handleClear} disabled={submitting}>Clear Fields</button>
        <button type="submit" className="btn btn-primary" style={{ padding: '10px 24px' }} disabled={submitting}>{submitting ? 'Submitting...' : 'Submit Ticket'}</button>
      </div>
    </form>
  );
}