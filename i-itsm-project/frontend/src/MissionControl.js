import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const API = 'http://localhost:8080/api';
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

// ---------- small helpers ----------

// Numbers that count up smoothly when they change
function useCountUp(target) {
  const [val, setVal] = useState(0);
  const prev = useRef(0);
  useEffect(() => {
    const from = prev.current;
    const to = Number(target) || 0;
    if (from === to) { setVal(to); return undefined; }
    const start = performance.now();
    let raf;
    const step = (now) => {
      const t = Math.min(1, (now - start) / 700);
      const eased = 1 - Math.pow(1 - t, 3);
      setVal(Math.round(from + (to - from) * eased));
      if (t < 1) { raf = requestAnimationFrame(step); } else { prev.current = to; }
    };
    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [target]);
  return val;
}

function last7Keys() {
  const keys = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`);
  }
  return keys;
}

// How many items fall on each of the last 7 days (real data, no fake trends)
function dailyCounts(items, getTime) {
  const keys = last7Keys();
  const counts = keys.map(() => 0);
  items.forEach((it) => {
    const t = getTime(it);
    if (!t) return;
    const i = keys.indexOf(String(t).slice(0, 10));
    if (i >= 0) counts[i] += 1;
  });
  return counts;
}

function Spark({ points, color }) {
  const max = Math.max(1, ...points);
  const step = 200 / (points.length - 1);
  const pts = points
    .map((p, i) => `${(i * step).toFixed(1)},${(30 - (p / max) * 26).toFixed(1)}`)
    .join(' ');
  return (
    <svg width="100%" height="34" viewBox="0 0 200 34" preserveAspectRatio="none">
      <polyline fill="none" stroke={color} strokeWidth="2.5" vectorEffect="non-scaling-stroke"
        strokeLinejoin="round" strokeLinecap="round" points={pts} />
    </svg>
  );
}

function heatColor(v, max, dark = true) {
  if (!v) return dark ? 'rgba(255,255,255,0.05)' : 'rgba(15,23,42,0.05)';
  const t = Math.min(1, v / Math.max(1, max));
  const stops = dark
    ? [[49, 46, 129], [99, 102, 241], [199, 210, 254]]
    : [[224, 231, 255], [129, 140, 248], [67, 56, 202]];
  const lo = t < 0.5 ? stops[0] : stops[1];
  const hi = t < 0.5 ? stops[1] : stops[2];
  const f = t < 0.5 ? t * 2 : (t - 0.5) * 2;
  return `rgb(${lo.map((x, i) => Math.round(x + (hi[i] - x) * f)).join(',')})`;
}

const fmtLeft = (m) => (m < 0 ? 'LATE' : `${Math.floor(m / 60)}:${String(m % 60).padStart(2, '0')}`);
const ringColor = (m) => (m < 30 ? '#f87171' : m < 75 ? '#fbbf24' : '#818cf8');
const prioClass = (p) => (p === 'P1' ? 'mc-p1' : p === 'P2' ? 'mc-p2' : 'mc-p3');

function eventColor(action) {
  const a = String(action || '').toUpperCase();
  if (a.includes('BREACH') || a.includes('ESCALAT')) return '#f87171';
  if (a.includes('WARN')) return '#fbbf24';
  if (a.includes('RESOLV') || a.includes('CLOSE') || a.includes('VERIF')) return '#4ade80';
  return '#818cf8';
}

const CSS = `
.mc-root{background:#0f172a;color:#e2e8f0;border-radius:16px;padding:22px 24px;margin:-8px;min-height:640px;font-family:inherit}
.mc-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;gap:12px;flex-wrap:wrap}
.mc-brand{display:flex;align-items:center;gap:12px}
.mc-logo{background:#1d4ed8;border-radius:10px;padding:7px 11px;font-weight:800;letter-spacing:.5px;color:#fff}
.mc-title{font-size:20px;font-weight:700;color:#f8fafc}
.mc-sub{font-size:12px;color:#94a3b8;margin-top:2px}
.mc-live{display:flex;align-items:center;gap:8px;font-size:13px;color:#94a3b8}
.mc-dot{width:9px;height:9px;border-radius:50%;background:#22c55e;animation:mcPulse 1.6s infinite}
@keyframes mcPulse{0%{box-shadow:0 0 0 0 rgba(34,197,94,.7)}70%{box-shadow:0 0 0 10px rgba(34,197,94,0)}100%{box-shadow:0 0 0 0 rgba(34,197,94,0)}}
@keyframes mcGlow{0%,100%{box-shadow:0 0 0 1px rgba(239,68,68,.55),0 0 14px rgba(239,68,68,.15) inset,0 0 14px rgba(239,68,68,.15)}50%{box-shadow:0 0 0 1px rgba(239,68,68,.9),0 0 30px rgba(239,68,68,.3) inset,0 0 30px rgba(239,68,68,.35)}}
@keyframes mcSlide{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:none}}
.mc-cards{display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin-bottom:14px}
.mc-card{background:rgba(255,255,255,.045);border:1px solid rgba(255,255,255,.08);border-radius:16px;padding:16px 18px;min-width:0}
.mc-crit{border-color:rgba(239,68,68,.6)}
.mc-crit .mc-num{color:#fca5a5}
.mc-lbl{font-size:11px;text-transform:uppercase;letter-spacing:1.2px;color:#94a3b8}
.mc-num{font-size:40px;font-weight:800;margin:4px 0 2px;color:#f8fafc;line-height:1.15}
.mc-delta{font-size:12px;color:#94a3b8;margin-bottom:6px}
.mc-main{display:grid;grid-template-columns:1.25fr .8fr 1fr;gap:14px}
.mc-h{font-size:13px;text-transform:uppercase;letter-spacing:1.2px;color:#94a3b8;margin-bottom:12px;font-weight:600}
.mc-heat{display:grid;grid-template-columns:34px repeat(24,1fr);gap:3px;font-size:10px;color:#94a3b8}
.mc-heat div{height:30px;border-radius:5px;display:flex;align-items:center;justify-content:center}
.mc-heat .mc-hh{height:14px;background:none;font-size:10px}
.mc-heat .mc-dd{background:none;justify-content:flex-start}
.mc-leg{display:flex;align-items:center;gap:8px;font-size:11px;color:#94a3b8;margin-top:12px;flex-wrap:wrap}
.mc-leg i{display:inline-block;width:70px;height:8px;border-radius:4px;background:linear-gradient(90deg,#1e293b,#6366f1,#c7d2fe)}
.mc-bar{display:flex;align-items:center;gap:10px;font-size:12px;margin:7px 0;color:#cbd5e1}
.mc-bar .mc-bn{width:84px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.mc-bar .mc-bt{flex:1;height:9px;border-radius:5px;background:#1e293b;overflow:hidden}
.mc-bar .mc-bt i{display:block;height:100%;border-radius:5px;transition:width .8s ease}
.mc-ring{display:flex;align-items:center;gap:14px;background:rgba(255,255,255,.04);border-radius:12px;padding:10px 12px;margin-bottom:10px}
.mc-ring b{font-size:14px;color:#f8fafc}
.mc-ring small{display:block;color:#94a3b8;font-size:11px;margin-top:2px;max-width:170px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.mc-ring circle.mc-arc{transition:stroke-dashoffset .8s ease}
.mc-tag{display:inline-block;font-size:10px;font-weight:700;padding:2px 7px;border-radius:6px;margin-right:6px}
.mc-p1{background:#7f1d1d;color:#fecaca}.mc-p2{background:#78350f;color:#fde68a}.mc-p3{background:#1e3a8a;color:#bfdbfe}
.mc-ev{display:flex;gap:10px;align-items:flex-start;padding:9px 10px;border-radius:10px;background:rgba(255,255,255,.04);margin-bottom:8px;font-size:13px}
.mc-ev.mc-new{background:rgba(99,102,241,.18);border:1px solid rgba(129,140,248,.5);animation:mcSlide .5s ease}
.mc-ev time{color:#64748b;font-size:11px;min-width:40px;padding-top:2px}
.mc-ev .mc-ic{width:8px;height:8px;border-radius:50%;margin-top:5px;flex:none}
.mc-ev .mc-et{min-width:0}
.mc-ev .mc-ed{color:#94a3b8;font-size:12px;margin-top:1px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.mc-empty{color:#64748b;font-size:13px;padding:14px 4px}
@media (max-width:1150px){.mc-cards{grid-template-columns:repeat(2,1fr)}.mc-main{grid-template-columns:1fr}}
`;

// Watches the <html data-theme> attribute so Mission Control follows the app theme
function useIsDark() {
  const read = () => document.documentElement.getAttribute('data-theme') !== 'light';
  const [dark, setDark] = useState(read);
  useEffect(() => {
    const obs = new MutationObserver(() => setDark(read()));
    obs.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    return () => obs.disconnect();
  }, []);
  return dark;
}

// Animations, skeletons, and the light theme (dark is the base CSS above)
const CSS_EXTRA = `
@keyframes mcRise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
@keyframes mcShimmer{0%{background-position:-800px 0}100%{background-position:800px 0}}
@keyframes mcBar{0%{transform:translateX(-100%)}100%{transform:translateX(260%)}}
.mc-root{position:relative;overflow:hidden;transition:background .25s,color .25s}
.mc-card{animation:mcRise .55s ease both;transition:background .25s,border-color .25s}
.mc-cards .mc-card:nth-child(2){animation-delay:.06s}
.mc-cards .mc-card:nth-child(3){animation-delay:.12s}
.mc-cards .mc-card:nth-child(4){animation-delay:.18s}
.mc-cards .mc-card:nth-child(5){animation-delay:.24s}
.mc-main .mc-card:nth-child(1){animation-delay:.3s}
.mc-main .mc-card:nth-child(2){animation-delay:.38s}
.mc-main .mc-card:nth-child(3){animation-delay:.46s}
.mc-heat div{transition:background .6s ease}
.mc-progress{position:absolute;top:0;left:0;right:0;height:3px;opacity:0;transition:opacity .3s;overflow:hidden;pointer-events:none}
.mc-progress.on{opacity:1}
.mc-progress i{display:block;width:40%;height:100%;background:linear-gradient(90deg,transparent,#6366f1,transparent)}
.mc-progress.on i{animation:mcBar 1s ease-in-out infinite}
.mc-skel{background:linear-gradient(90deg,rgba(255,255,255,.04) 25%,rgba(255,255,255,.11) 50%,rgba(255,255,255,.04) 75%);background-size:800px 100%;animation:mcShimmer 1.4s linear infinite;border-radius:16px}
.mc-sk-card{height:132px}
.mc-sk-big{height:300px}
.mc-evt{color:#f1f5f9}
.mc-amber{color:#fbbf24}
.mc-green{color:#86efac}

.mc-light{background:#f1f5f9;color:#0f172a;border:1px solid #e2e8f0}
.mc-light .mc-title{color:#0f172a}
.mc-light .mc-sub,.mc-light .mc-live,.mc-light .mc-lbl,.mc-light .mc-delta,.mc-light .mc-h,.mc-light .mc-leg,.mc-light .mc-heat,.mc-light .mc-ev .mc-ed,.mc-light .mc-ring small,.mc-light .mc-empty{color:#64748b}
.mc-light .mc-ev time{color:#94a3b8}
.mc-light .mc-card{background:#ffffff;border-color:#e2e8f0;box-shadow:0 1px 2px rgba(16,24,40,.06)}
.mc-light .mc-crit{border-color:rgba(239,68,68,.55)}
.mc-light .mc-num{color:#0f172a}
.mc-light .mc-crit .mc-num{color:#b91c1c}
.mc-light .mc-amber{color:#b45309}
.mc-light .mc-green{color:#15803d}
.mc-light .mc-bar{color:#334155}
.mc-light .mc-bar .mc-bt{background:#e2e8f0}
.mc-light .mc-ring{background:#f8fafc}
.mc-light .mc-ring b{color:#0f172a}
.mc-light .mc-track{stroke:#e2e8f0}
.mc-light .mc-ringtext{fill:#0f172a}
.mc-light .mc-ev{background:#f8fafc}
.mc-light .mc-ev.mc-new{background:#eef2ff;border-color:#a5b4fc}
.mc-light .mc-evt{color:#0f172a}
.mc-light .mc-leg i{background:linear-gradient(90deg,#e0e7ff,#818cf8,#4338ca)}
.mc-light .mc-p1{background:#fee2e2;color:#b91c1c}
.mc-light .mc-p2{background:#fef3c7;color:#92400e}
.mc-light .mc-p3{background:#dbeafe;color:#1e40af}
.mc-light .mc-skel{background:linear-gradient(90deg,#e2e8f0 25%,#f8fafc 50%,#e2e8f0 75%);background-size:800px 100%}
`;

export default function MissionControl() {
  const [data, setData] = useState({ analytics: null, risks: [], incidents: [], history: [], activity: [] });
  const [updated, setUpdated] = useState(null);
  const baseId = useRef(null); // newest event id when the screen opened
  const [loaded, setLoaded] = useState(false);       // false until the first load finishes (shows skeleton)
  const [refreshing, setRefreshing] = useState(false); // true briefly on every refresh (shows top bar)
  const dark = useIsDark();

  const load = async () => {
    setRefreshing(true);
    const r = await Promise.allSettled([
      axios.get(`${API}/analytics`),
      axios.get(`${API}/admin/sla-risks`),
      axios.get(`${API}/incidents`),
      axios.get(`${API}/admin/sla-violation-history`),
      axios.get(`${API}/admin/activity`)
    ]);
    const pick = (i, fallback) => (r[i].status === 'fulfilled' ? r[i].value.data : fallback);
    if (baseId.current === null && r[4].status === 'fulfilled') {
      const ids = r[4].value.data.map((e) => e.id);
      baseId.current = ids.length ? Math.max(...ids) : 0;
    }
    setData((prev) => ({
      analytics: pick(0, prev.analytics),
      risks: pick(1, prev.risks),
      incidents: pick(2, prev.incidents),
      history: pick(3, prev.history),
      activity: pick(4, prev.activity)
    }));
    setUpdated(new Date());
    setLoaded(true);
    setTimeout(() => setRefreshing(false), 700);
  };

  useEffect(() => {
    load();
    const timer = setInterval(() => { if (!document.hidden) load(); }, 5000);
    return () => clearInterval(timer);
  }, []);

  const { analytics: a, risks, incidents, history, activity } = data;
  const atRisk = risks.filter((x) => x.minutesRemaining >= 0).length;

  const nOpen = useCountUp(a ? a.openIncidents : 0);
  const nP1 = useCountUp(a ? a.criticalIncidents : 0);
  const nRisk = useCountUp(atRisk);
  const nBreach = useCountUp(a ? a.slaViolations : 0);
  const nDone = useCountUp(a ? a.resolvedIncidents : 0);

  // Heatmap: weekday x hour, counted from the real incident list
  const grid = Array.from({ length: 7 }, () => Array(24).fill(0));
  incidents.forEach((i) => {
    if (!i.createdAt) return;
    const d = new Date(i.createdAt);
    if (isNaN(d)) return;
    grid[(d.getDay() + 6) % 7][d.getHours()] += 1;
  });
  let max = 0;
  let best = null;
  grid.forEach((row, di) => row.forEach((v, h) => { if (v > max) { max = v; best = { di, h }; } }));

  const ringList = [...risks].sort((x, y) => x.minutesRemaining - y.minutesRemaining).slice(0, 4);
  const workload = (a && a.technicianWorkload) || [];
  const maxLoad = Math.max(5, ...workload.map((t) => t.active));
  const total = a ? a.totalIncidents : 0;
  const donePct = total ? Math.round(((a ? a.resolvedIncidents : 0) / total) * 100) : 0;

  if (!loaded) {
    return (
      <div className={`mc-root ${dark ? '' : 'mc-light'}`}>
        <style>{CSS + CSS_EXTRA}</style>
        <div className="mc-progress on"><i /></div>
        <div className="mc-top">
          <div className="mc-brand">
            <div className="mc-logo">I-ITSM</div>
            <div>
              <div className="mc-title">Mission Control</div>
              <div className="mc-sub">Loading live data…</div>
            </div>
          </div>
        </div>
        <div className="mc-cards">
          {[0, 1, 2, 3, 4].map(i => <div key={i} className="mc-skel mc-sk-card" />)}
        </div>
        <div className="mc-main">
          {[0, 1, 2].map(i => <div key={i} className="mc-skel mc-sk-big" />)}
        </div>
      </div>
    );
  }

  return (
    <div className={`mc-root ${dark ? '' : 'mc-light'}`}>
      <style>{CSS + CSS_EXTRA}</style>
      <div className={`mc-progress ${refreshing ? 'on' : ''}`}><i /></div>

      <div className="mc-top">
        <div className="mc-brand">
          <div className="mc-logo">I-ITSM</div>
          <div>
            <div className="mc-title">Mission Control</div>
            <div className="mc-sub">Live operational overview · refreshes every 5 seconds</div>
          </div>
        </div>
        <div className="mc-live"><span className="mc-dot" /> LIVE{updated ? ` · updated ${updated.toLocaleTimeString()}` : ''}</div>
      </div>

      <div className="mc-cards">
        <div className="mc-card">
          <div className="mc-lbl">Open incidents</div>
          <div className="mc-num">{nOpen}</div>
          <div className="mc-delta">of {total} total · created per day</div>
          <Spark points={dailyCounts(incidents, (i) => i.createdAt)} color="#818cf8" />
        </div>
        <div className={`mc-card ${(a ? a.criticalIncidents : 0) > 0 ? 'mc-crit' : ''}`}>
          <div className="mc-lbl">Critical (P1)</div>
          <div className="mc-num">{nP1}</div>
          <div className="mc-delta">P1 created per day</div>
          <Spark points={dailyCounts(incidents.filter((i) => i.priority === 'P1'), (i) => i.createdAt)} color="#f87171" />
        </div>
        <div className="mc-card">
          <div className="mc-lbl">SLA at risk</div>
          <div className="mc-num mc-amber">{nRisk}</div>
          <div className="mc-delta mc-amber">under 2 hours left</div>
          <div style={{ height: 34 }} />
        </div>
        <div className="mc-card">
          <div className="mc-lbl">SLA breached</div>
          <div className="mc-num">{nBreach}</div>
          <div className="mc-delta">breaches detected per day</div>
          <Spark points={dailyCounts(history, (v) => v.detectedAt)} color="#4ade80" />
        </div>
        <div className="mc-card">
          <div className="mc-lbl">Resolved</div>
          <div className="mc-num mc-green">{nDone}</div>
          <div className="mc-delta mc-green">{donePct}% of all tickets</div>
          <div style={{ height: 34 }} />
        </div>
      </div>

      <div className="mc-main">
        <div className="mc-card">
          <div className="mc-h">When do incidents happen?</div>
          <div className="mc-heat">
            <div className="mc-hh" />
            {Array.from({ length: 24 }, (_, h) => (
              <div key={h} className="mc-hh">{h % 3 === 0 ? h : ''}</div>
            ))}
            {grid.map((row, di) => (
              <React.Fragment key={di}>
                <div className="mc-dd">{DAYS[di]}</div>
                {row.map((v, h) => (
                  <div key={h} title={`${DAYS[di]} ${h}:00 · ${v} incident(s)`}
                    style={{ background: heatColor(v, max, dark), color: dark ? (v / Math.max(1, max) > 0.6 ? '#1e1b4b' : '#e0e7ff') : (v / Math.max(1, max) > 0.5 ? '#ffffff' : '#312e81') }}>
                    {v || ''}
                  </div>
                ))}
              </React.Fragment>
            ))}
          </div>
          <div className="mc-leg">
            fewer <i /> more
            <span>·</span>
            {best ? `Busiest: ${DAYS[best.di]} ${best.h}:00 (${max} incident${max === 1 ? '' : 's'})` : 'No incidents yet'}
          </div>

          <div className="mc-h" style={{ marginTop: 22 }}>Technician workload</div>
          {workload.length === 0 && <div className="mc-empty">No technicians yet.</div>}
          {workload.map((t, i) => {
            const ratio = t.active / maxLoad;
            const color = ratio > 0.75 ? '#f87171' : ratio > 0.5 ? '#fbbf24' : '#818cf8';
            return (
              <div className="mc-bar" key={i}>
                <span className="mc-bn">{t.name}</span>
                <div className="mc-bt"><i style={{ width: `${Math.max(3, ratio * 100)}%`, background: color }} /></div>
                <span>{t.active} open</span>
              </div>
            );
          })}
        </div>

        <div className="mc-card">
          <div className="mc-h">Closest to SLA deadline</div>
          {ringList.length === 0 && <div className="mc-empty">All incidents are comfortably within SLA.</div>}
          {ringList.map((r) => {
            const C = 2 * Math.PI * 22;
            const pct = r.minutesRemaining < 0 ? 1 : Math.min(1, r.minutesRemaining / 120);
            const color = r.minutesRemaining < 0 ? '#f87171' : ringColor(r.minutesRemaining);
            return (
              <div className="mc-ring" key={r.id}>
                <svg width="60" height="60" viewBox="0 0 60 60" style={{ flex: 'none' }}>
                  <circle className="mc-track" cx="30" cy="30" r="22" fill="none" stroke="#1e293b" strokeWidth="7" />
                  <circle className="mc-arc" cx="30" cy="30" r="22" fill="none" stroke={color} strokeWidth="7"
                    strokeLinecap="round" strokeDasharray={C} strokeDashoffset={C * (1 - pct)} transform="rotate(-90 30 30)" />
                  <text className="mc-ringtext" x="30" y="34" textAnchor="middle" fontSize="12" fontWeight="700" fill="#e2e8f0">{fmtLeft(r.minutesRemaining)}</text>
                </svg>
                <div style={{ minWidth: 0 }}>
                  <b><span className={`mc-tag ${prioClass(r.priority)}`}>{r.priority}</span>#{r.id}</b>
                  <small>{r.description}</small>
                </div>
              </div>
            );
          })}
        </div>

        <div className="mc-card">
          <div className="mc-h">Live activity</div>
          {activity.length === 0 && <div className="mc-empty">No activity yet.</div>}
          {activity.slice(0, 8).map((e) => (
            <div key={e.id} className={`mc-ev ${baseId.current !== null && e.id > baseId.current ? 'mc-new' : ''}`}>
              <time>{String(e.timestamp || '').slice(11, 16)}</time>
              <span className="mc-ic" style={{ background: eventColor(e.actionPerformed) }} />
              <div className="mc-et">
                <b className="mc-evt">{String(e.actionPerformed || '').replace(/_/g, ' ')}</b>
                {e.incidentId ? ` · #${e.incidentId}` : ''}
                <div className="mc-ed">{e.description}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}