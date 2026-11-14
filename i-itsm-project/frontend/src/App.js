import React, { useState, useEffect } from 'react';
import axios from 'axios';
import IncidentForm from './IncidentForm';
import Mailbox from './Mailbox';
import MissionControl from './MissionControl';

const API = 'http://localhost:8080/api';

function App() {
  const [user, setUser] = useState(null);
  const [emailInput, setEmailInput] = useState('');
  const [authMode, setAuthMode] = useState('login');
  const [reg, setReg] = useState({ name: '', email: '', password: '' });
  const [newService, setNewService] = useState({ serviceName: '', businessCriticality: 'MEDIUM', description: '' });
  const [slaEdits, setSlaEdits] = useState({});
  const [passwordInput, setPasswordInput] = useState('');

  const [incidents, setIncidents] = useState([]);
  const [services, setServices] = useState([]);
  const [categories, setCategories] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [showMail, setShowMail] = useState(false);
  const [mailUnread, setMailUnread] = useState(0);
  const [analytics, setAnalytics] = useState(null);
  const [slaViolations, setSlaViolations] = useState([]);
  const [slaRisks, setSlaRisks] = useState([]);
  const [slaHistory, setSlaHistory] = useState([]);

  // Audit Trail State
  const [auditModalOpen, setAuditModalOpen] = useState(false);
  const [currentAuditLogs, setCurrentAuditLogs] = useState([]);
  const [currentAuditIncidentId, setCurrentAuditIncidentId] = useState(null);

  // Admin Management state
  const [adminTab, setAdminTab] = useState('reports');
  const [allUsers, setAllUsers] = useState([]);
  const [slaPolicies, setSlaPolicies] = useState([]);
  const [configList, setConfigList] = useState([]);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newUser, setNewUser] = useState({ name: '', email: '', password: '', role: 'TECHNICIAN', skills: '' });

  // Advanced Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [filterPriority, setFilterPriority] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterCategory, setFilterCategory] = useState('ALL');
  const [filterTechnician, setFilterTechnician] = useState('ALL');
  const [filterService, setFilterService] = useState('ALL');
  const [filterDateFrom, setFilterDateFrom] = useState('');
  const [filterDateTo, setFilterDateTo] = useState('');
  const [filterSla, setFilterSla] = useState('ALL');

  const handleLogout = () => {
    delete axios.defaults.headers.common['Authorization'];
    localStorage.removeItem('token');     
    localStorage.removeItem('user');
    setUser(null);
    setEmailInput('');
    setPasswordInput('');
    setIncidents([]);
    setNotifications([]);
    setAnalytics(null);
  };

  useEffect(() => {
    const id = axios.interceptors.response.use(
      (r) => r,
      (err) => {
        const isLogin = err.config?.url?.includes('/auth/login');
                if (err.response?.status === 401 && !isLogin && err.config?.headers?.Authorization) {
          if (!localStorage.getItem('token')) return Promise.reject(err);  // already logged out by an earlier failed request
          handleLogout();
          alert('Your session expired. Please sign in again.');
        }
        return Promise.reject(err);
      }
    );
    return () => axios.interceptors.response.eject(id);
  }, []);

      useEffect(() => {
    const token = localStorage.getItem('token');
    const saved = localStorage.getItem('user');
    if (token && saved) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      const u = JSON.parse(saved);
      setUser(u);
      loadDashboardData(u);
    }
  }, []);
  
  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(`${API}/auth/login`, {
        email: emailInput,
        password: passwordInput
      });

      const { token, ...u } = res.data;
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      localStorage.setItem('token', token);              
      localStorage.setItem('user', JSON.stringify(u));
      setUser(u);
      loadDashboardData(u);
    } catch (err) {
      alert(err.response?.data?.error || "Login failed. Please check credentials or approval status.");
      setEmailInput('');
      setPasswordInput('');
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(`${API}/auth/register`, reg);
      alert(res.data.message || 'Registration successful. Wait for administrator approval.');
      setReg({ name: '', email: '', password: '' });
      setAuthMode('login');
    } catch (err) {
      alert(err.response?.data?.error || 'Registration failed.');
    }
  };

  const handleAddService = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API}/admin/services`, newService);
      setNewService({ serviceName: '', businessCriticality: 'MEDIUM', description: '' });
      loadDashboardData(user);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to add service (the name may already exist).');
    }
  };

  const handleDeleteService = async (id) => {
    if (!window.confirm('Remove this service?')) return;
    try {
      await axios.delete(`${API}/admin/services/${id}`);
      loadDashboardData(user);
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to remove service.');
    }
  };

  const handleSaveSla = async (p) => {
    const edit = slaEdits[p.id] || {};
    const body = {
      ...p,
      responseMinutes: Number(edit.responseMinutes ?? p.responseMinutes),
      resolutionMinutes: Number(edit.resolutionMinutes ?? p.resolutionMinutes)
    };
    try {
      await axios.post(`${API}/admin/sla`, body);
      setSlaEdits(prev => { const n = { ...prev }; delete n[p.id]; return n; });
      loadDashboardData(user);
      alert('SLA policy saved. It applies to new incidents.');
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to save SLA policy.');
    }
  };

  const loadDashboardData = async (u, light = false) => {
    const isAdmin = u.role === 'ADMINISTRATOR';
    const calls = [
      axios.get(`${API}/incidents`),
      axios.get(`${API}/notifications`),
      axios.get(`${API}/emails`)
    ];
    if (!light) {
      calls.push(axios.get(`${API}/services`), axios.get(`${API}/categories`));
      if (isAdmin) {
        calls.push(
          axios.get(`${API}/analytics`),
          axios.get(`${API}/admin/sla-violations`),
          axios.get(`${API}/admin/sla-risks`),
          axios.get(`${API}/admin/users`),
          axios.get(`${API}/admin/sla`),
                    axios.get(`${API}/admin/config`),
          axios.get(`${API}/admin/sla-violation-history`)
        );
      }
    }
    const r = await Promise.allSettled(calls);
    const data = (i) => (r[i] && r[i].status === 'fulfilled' ? r[i].value.data : null);

    if (data(0)) setIncidents(data(0));
    if (data(1)) setNotifications([...data(1)].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt) || b.id - a.id));
    if (data(2)) setMailUnread(data(2).filter(m => !m.opened).length);
    if (light) return;

    if (data(3)) setServices(data(3));
    if (data(4)) setCategories(data(4));
    if (isAdmin) {
      if (data(5)) setAnalytics(data(5));
      if (data(6)) setSlaViolations(data(6));
      if (data(7)) setSlaRisks(data(7));
      if (data(8)) setAllUsers(data(8));
      if (data(9)) setSlaPolicies(data(9));
      if (data(10)) setConfigList(data(10));
      if (data(11)) setSlaHistory(data(11));
    }
  };

  useEffect(() => {
    if (!user) return;
    let tick = 0;
    const interval = setInterval(() => {
      if (document.hidden) return;                 // tab not visible: skip
      tick += 1;
      loadDashboardData(user, tick % 4 !== 0);     // light refresh, full refresh every ~1 minute
    }, 15000);
    return () => clearInterval(interval);
  }, [user]);

  const handleStatusUpdate = async (id, newStatus, extra = {}) => {
    try {
      await axios.put(`${API}/incidents/${id}/status`, { email: user.email, status: newStatus, ...extra });
      loadDashboardData(user);
    } catch (err) {
      alert(err.response?.data?.error || "Failed to update incident state.");
    }
  };

  const fetchAuditLogs = async (incidentId) => {
    try {
      const res = await axios.get(`${API}/audit/${incidentId}`);
      setCurrentAuditLogs(res.data);
      setCurrentAuditIncidentId(incidentId);
      setAuditModalOpen(true);
    } catch (e) {
      alert("Failed to fetch audit trail");
    }
  };

  const handleApproveUser = async (id) => {
    await axios.put(`${API}/admin/users/${id}/approve`);
    loadDashboardData(user);
  };

  const handleDeleteUser = async (id) => {
  if (window.confirm("Are you sure you want to delete this user?")) {
    try {
      await axios.delete(`${API}/admin/users/${id}`);
      loadDashboardData(user);
    } catch (err) {
      alert(err.response?.data?.error || "Failed to delete user.");
    }
  }
};

const handleCreateUser = async (e) => {
  e.preventDefault();
  try {
    await axios.post(`${API}/admin/users`, newUser);
    setNewUser({ name: '', email: '', password: '', role: 'TECHNICIAN', skills: '' });
    loadDashboardData(user);
    alert("User created successfully!");
  } catch (err) {
    alert(err.response?.data?.error || "Failed to create user.");
  }
};

const handleChangeRole = async (u, role) => {
  if (role === u.role) return;
  let skills = u.skills || '';
  if (role === 'TECHNICIAN') {
    const input = prompt("Skills for this technician (comma separated categories, e.g. NETWORK,DATABASE):", skills);
    if (input === null) return;
    skills = input;
  }
  try {
    await axios.put(`${API}/admin/users/${u.id}/role`, { role, skills });
    loadDashboardData(user);
  } catch (err) {
    alert(err.response?.data?.error || "Failed to change role.");
  }
};

  const handleAddCategory = async (e) => {
    e.preventDefault();
    if (!newCategoryName.trim()) return;
    try {
      await axios.post(`${API}/admin/categories`, { name: newCategoryName, description: "Custom introduced category" });
      setNewCategoryName('');
      loadDashboardData(user);
      alert("Category added successfully!");
    } catch (err) {
      alert("Failed to add category (may already exist).");
    }
  };

  const handleDeleteCategory = async (id) => {
    if (window.confirm("Remove this category?")) {
      await axios.delete(`${API}/admin/categories/${id}`);
      loadDashboardData(user);
    }
  };

  const markAllNotifRead = async () => {
    await axios.put(`${API}/notifications/read-all`);
    loadDashboardData(user);
  };

  const markNotifRead = async (id) => {
    await axios.put(`${API}/notifications/${id}/read`);
    loadDashboardData(user);
  };

  // Technician workload (the incident list is already limited to this technician)
  const FINISHED_STATES = ['RESOLVED', 'VERIFIED', 'CLOSED'];
  const openMine = incidents.filter(i => !FINISHED_STATES.includes(i.status));
  const nowMs = Date.now();
  const deadlineMs = (i) => (i.slaResolutionDeadline ? new Date(i.slaResolutionDeadline).getTime() : null);
  const myActive = openMine.length;
  const myOverdue = openMine.filter(i => deadlineMs(i) !== null && deadlineMs(i) < nowMs).length;
  const myAtRisk = openMine.filter(i => deadlineMs(i) !== null && deadlineMs(i) >= nowMs && deadlineMs(i) - nowMs < 2 * 60 * 60 * 1000).length;
  const myDone = incidents.length - openMine.length;

  const filteredIncidents = incidents.filter(inc => {
    const matchesSearch = inc.description.toLowerCase().includes(searchQuery.toLowerCase()) || inc.id.toString().includes(searchQuery);
    const matchesPriority = filterPriority === 'ALL' || inc.priority === filterPriority;
    const matchesStatus = filterStatus === 'ALL' || inc.status === filterStatus;
    const matchesCategory = filterCategory === 'ALL' || inc.category === filterCategory;
    const matchesTech = filterTechnician === 'ALL' || (inc.assignedTechnician && inc.assignedTechnician.id.toString() === filterTechnician);
    const matchesService = filterService === 'ALL' || (inc.affectedService && inc.affectedService.id.toString() === filterService);

    const created = new Date(inc.createdAt);
    const matchesDateFrom = !filterDateFrom || created >= new Date(filterDateFrom + 'T00:00:00');
    const matchesDateTo = !filterDateTo || created <= new Date(filterDateTo + 'T23:59:59');

    const finished = ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(inc.status);
    const overdue = inc.slaResolutionDeadline && new Date(inc.slaResolutionDeadline) < new Date();
    let slaState = 'ON_TRACK';
    if (!finished && overdue) slaState = 'BREACHED';
    const matchesSla = filterSla === 'ALL' || slaState === filterSla;

    return matchesSearch && matchesPriority && matchesStatus && matchesCategory && matchesTech && matchesService && matchesDateFrom && matchesDateTo && matchesSla;
  });

  return (
    <div className="app-shell">
      {/* Top Header */}
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">I-ITSM</span>
          <span className="brand-name">IT Service Desk</span>
        </div>
        {user && (
          <div className="topbar-right">
            <div className="user-chip">
              <strong>{user.name}</strong>
              <span className="user-role">{user.role.toLowerCase()}</span>
            </div>
            <button onClick={() => setShowMail(true)} className="btn btn-ghost">Mail{mailUnread > 0 ? ` (${mailUnread})` : ''}</button>
            <button onClick={handleLogout} className="btn btn-ghost">Sign out</button>
          </div>
        )}
      </header>

      {!user ? (
        <div className="auth-wrap">
          <div className="auth-card">
            <h2>{authMode === 'login' ? 'Sign in' : 'Create an account'}</h2>
            <p className="auth-sub">
              {authMode === 'login'
                ? 'Use your organisation email to reach the service desk.'
                : 'New accounts must be approved by an administrator before the first sign-in.'}
            </p>

            {authMode === 'login' ? (
              <form onSubmit={handleLogin} autoComplete="off" className="stack">
                <div>
                  <label className="field-label">Email</label>
                  <input type="email" required className="input-field" value={emailInput} onChange={e => setEmailInput(e.target.value)} maxLength={150} autoComplete="off" name="itsm-login-email" />
                </div>
                <div>
                  <label className="field-label">Password</label>
                  <input type="password" required className="input-field" value={passwordInput} onChange={e => setPasswordInput(e.target.value)} maxLength={100} autoComplete="new-password" name="itsm-login-password" />
                </div>
                <button type="submit" className="btn btn-primary" style={{ padding: '10px' }}>Sign in</button>
              </form>
            ) : (
              <form onSubmit={handleRegister} autoComplete="off" className="stack">
                <div>
                  <label className="field-label">Full name</label>
                  <input type="text" required minLength={2} maxLength={100} className="input-field" value={reg.name} onChange={e => setReg({ ...reg, name: e.target.value })} />
                </div>
                <div>
                  <label className="field-label">Email</label>
                  <input type="email" required maxLength={150} className="input-field" value={reg.email} onChange={e => setReg({ ...reg, email: e.target.value })} />
                </div>
                <div>
                  <label className="field-label">Password (6 to 100 characters)</label>
                  <input type="password" required minLength={6} maxLength={100} className="input-field" autoComplete="new-password" value={reg.password} onChange={e => setReg({ ...reg, password: e.target.value })} />
                </div>
                <button type="submit" className="btn btn-primary" style={{ padding: '10px' }}>Request account</button>
              </form>
            )}

            <div className="auth-switch">
              {authMode === 'login' ? 'No account yet? ' : 'Already registered? '}
              <button type="button" className="link-btn" onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}>
                {authMode === 'login' ? 'Create one' : 'Sign in'}
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div style={{ maxWidth: '1400px', margin: '32px auto', padding: '0 24px', display: 'grid', gridTemplateColumns: '1fr 340px', gap: '32px' }}>
          <div>
            {/* EMPLOYEE PORTAL */}
            {user.role === 'EMPLOYEE' && (
              <div>
                <div className="dashboard-card">
                  <h2 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '20px', color: '#0f172a', borderBottom: '1px solid #e2e8f0', paddingBottom: '12px' }}>Report New IT Incident</h2>
                  <IncidentForm user={user} categories={categories} services={services} onSubmitted={() => loadDashboardData(user)} />
                </div>

                <div className="dashboard-card" style={{ display: 'flex', gap: '12px', alignItems: 'center', background: '#fff', flexWrap: 'wrap' }}>
                  <span style={{ fontWeight: '600', color: '#64748b', fontSize: '14px', width: '100%' }}>Filters & Search:</span>
                  <input type="text" className="input-field" placeholder="Search keywords, description, or ID..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} style={{ flex: 1, minWidth: '200px' }} />
                  <select className="input-field" value={filterPriority} onChange={e => setFilterPriority(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Priorities</option>
                    <option value="P1">P1 - Critical</option>
                    <option value="P2">P2 - High</option>
                    <option value="P3">P3 - Medium</option>
                    <option value="P4">P4 - Low</option>
                  </select>
                  <select className="input-field" value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Statuses</option>
                    <option value="NEW">New</option>
                    <option value="CLASSIFIED">Classified</option>
                    <option value="ASSIGNED">Assigned</option>
                    <option value="ACKNOWLEDGED">Acknowledged</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="RESOLVED">Resolved</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                  <select className="input-field" value={filterCategory} onChange={e => setFilterCategory(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Categories</option>
                    {categories.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
                  </select>
                  <select className="input-field" value={filterService} onChange={e => setFilterService(e.target.value)} style={{ width: '150px' }}>
                    <option value="ALL">All Services</option>
                    {services.map(s => <option key={s.id} value={s.id.toString()}>{s.serviceName}</option>)}
                  </select>
                  <select className="input-field" value={filterSla} onChange={e => setFilterSla(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All SLA</option>
                    <option value="ON_TRACK">On Track</option>
                    <option value="BREACHED">Breached</option>
                  </select>
                  <label style={{ fontSize: '12px', color: '#64748b' }}>From
                    <input type="date" className="input-field" value={filterDateFrom} onChange={e => setFilterDateFrom(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                  </label>
                  <label style={{ fontSize: '12px', color: '#64748b' }}>To
                    <input type="date" className="input-field" value={filterDateTo} onChange={e => setFilterDateTo(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                  </label>
                </div>

                <div className="dashboard-card">
                  <h2 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '20px', color: '#0f172a' }}>My Incident History</h2>
                  <div style={{ overflowX: 'auto' }}>
                    <table className="table-custom">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Description</th>
                          <th>Priority</th>
                          <th>Status</th>
                          <th>Resolution Details</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredIncidents.map(inc => (
                          <tr key={inc.id}>
                            <td style={{ fontWeight: '600' }}>#{inc.id} {inc.duplicate && <span className="pill" style={{ background: '#fee2e2', color: '#dc2626', marginLeft: '6px' }}>Duplicate of #{inc.duplicateOfIncidentId}</span>}</td>
                            <td>{inc.description}</td>
                            <td><span className={`pill pill-${inc.priority}`}>{inc.priority}</span></td>
                            <td><span className="pill pill-status">{inc.status}</span></td>
                            <td style={{ fontSize: '13px' }}>
                              {inc.resolutionDetails && <div style={{ background: '#f0fdf4', color: '#166534', padding: '6px 10px', borderRadius: '6px', border: '1px solid #bbf7d0' }}><strong>Resolved:</strong> {inc.resolutionDetails}</div>}
                              {inc.techRequestInfo && <div style={{ background: '#fffbeb', color: '#b45309', padding: '6px 10px', borderRadius: '6px', marginTop: '6px', border: '1px solid #fde68a' }}><strong>Info Requested:</strong> {inc.techRequestInfo}</div>}
                            </td>
                            <td>
                              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                {inc.techRequestInfo && !inc.additionalInfo && (
                                  <button onClick={() => {
                                    const info = prompt("Provide additional information requested by technician:");
                                    if (info) handleStatusUpdate(inc.id, inc.status, { additionalInfo: info });
                                  }} className="btn btn-primary">Respond</button>
                                )}
                                {inc.status === 'RESOLVED' && !inc.feedbackRating && (
                                  <button onClick={() => {
                                    const rating = prompt("Rate resolution quality (1 to 5 stars):");
                                    const comments = prompt("Optional feedback comments:");
                                    if (rating) handleStatusUpdate(inc.id, 'VERIFIED', { feedbackRating: rating, feedbackComments: comments });
                                  }} className="btn btn-success">Verify & Rate</button>
                                )}
                                <button onClick={() => fetchAuditLogs(inc.id)} className="btn btn-secondary">History</button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {filteredIncidents.length === 0 && (
                          <tr><td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#94a3b8' }}>No incidents match your criteria.</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* TECHNICIAN PORTAL */}
            {user.role === 'TECHNICIAN' && (
              <div className="dashboard-card">
                <h2 className="card-title">Assigned incidents</h2>
                <div className="workload-strip">
                  <div><span>{myActive}</span>Active</div>
                  <div><span>{myAtRisk}</span>Due within 2 hours</div>
                  <div><span>{myOverdue}</span>Overdue</div>
                  <div><span>{myDone}</span>Resolved</div>
                </div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center', background: '#fff', flexWrap: 'wrap', marginBottom: '20px', borderBottom: '1px solid #e2e8f0', paddingBottom: '16px' }}>
                  <span style={{ fontWeight: '600', color: '#64748b', fontSize: '14px', width: '100%' }}>Filters & Search:</span>
                  <input type="text" className="input-field" placeholder="Search keywords, description, or ID..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} style={{ flex: 1, minWidth: '200px' }} />
                  <select className="input-field" value={filterPriority} onChange={e => setFilterPriority(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Priorities</option>
                    <option value="P1">P1 - Critical</option>
                    <option value="P2">P2 - High</option>
                    <option value="P3">P3 - Medium</option>
                    <option value="P4">P4 - Low</option>
                  </select>
                  <select className="input-field" value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Statuses</option>
                    <option value="ASSIGNED">Assigned</option>
                    <option value="ACKNOWLEDGED">Acknowledged</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="RESOLVED">Resolved</option>
                  </select>
                  <select className="input-field" value={filterCategory} onChange={e => setFilterCategory(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All Categories</option>
                    {categories.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
                  </select>
                  <select className="input-field" value={filterService} onChange={e => setFilterService(e.target.value)} style={{ width: '150px' }}>
                    <option value="ALL">All Services</option>
                    {services.map(s => <option key={s.id} value={s.id.toString()}>{s.serviceName}</option>)}
                  </select>
                  <select className="input-field" value={filterSla} onChange={e => setFilterSla(e.target.value)} style={{ width: '140px' }}>
                    <option value="ALL">All SLA</option>
                    <option value="ON_TRACK">On Track</option>
                    <option value="BREACHED">Breached</option>
                  </select>
                  <label style={{ fontSize: '12px', color: '#64748b' }}>From
                    <input type="date" className="input-field" value={filterDateFrom} onChange={e => setFilterDateFrom(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                  </label>
                  <label style={{ fontSize: '12px', color: '#64748b' }}>To
                    <input type="date" className="input-field" value={filterDateTo} onChange={e => setFilterDateTo(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                  </label>
                </div>
                <div style={{ overflowX: 'auto' }}>
                  <table className="table-custom">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Description</th>
                        <th>Priority</th>
                        <th>Status</th>
                        <th>Actions & Investigation</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredIncidents.filter(inc => inc.status !== 'CLOSED').map(inc => (
                        <tr key={inc.id}>
                          <td style={{ fontWeight: '600' }}>#{inc.id} {inc.duplicate && <span className="pill" style={{ background: '#fee2e2', color: '#dc2626', marginLeft: '6px' }}>Duplicate of #{inc.duplicateOfIncidentId}</span>}</td>
                          <td>
                            {inc.description}
                            {inc.additionalInfo && <div style={{ background: '#f0fdf4', color: '#166534', padding: '6px 10px', borderRadius: '6px', marginTop: '8px', fontSize: '12px', border: '1px solid #bbf7d0' }}><strong>Employee Reply:</strong> {inc.additionalInfo}</div>}
                          </td>
                          <td><span className={`pill pill-${inc.priority}`}>{inc.priority}</span></td>
                          <td><span className="pill pill-status">{inc.status}</span></td>
                          <td>
                            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                              {inc.status === 'ASSIGNED' && (
                                <button onClick={() => handleStatusUpdate(inc.id, 'ACKNOWLEDGED')} className="btn btn-primary">Acknowledge</button>
                              )}
                              {inc.status === 'ACKNOWLEDGED' && (
                                <button onClick={() => handleStatusUpdate(inc.id, 'IN_PROGRESS')} className="btn btn-warning">Start Progress</button>
                              )}
                              <button onClick={() => {
                                const notes = prompt("Enter investigation notes:", inc.investigationNotes || "");
                                if (notes) handleStatusUpdate(inc.id, inc.status, { investigationNotes: notes });
                              }} className="btn btn-secondary">Notes</button>
                              
                              {(inc.status === 'ASSIGNED' || inc.status === 'ACKNOWLEDGED' || inc.status === 'IN_PROGRESS') && (
                                <>
                                  <button onClick={() => {
                                    const req = prompt("Request additional info from employee:");
                                    if (req) handleStatusUpdate(inc.id, inc.status, { techRequestInfo: req });
                                  }} className="btn" style={{ background: '#f59e0b', color: 'white' }}>Req Info</button>
                                  <button onClick={() => {
                                    const res = prompt("Enter resolution details:");
                                    if (res) handleStatusUpdate(inc.id, 'RESOLVED', { resolutionDetails: res });
                                  }} className="btn btn-success">Resolve</button>
                                </>
                              )}
                              <button onClick={() => fetchAuditLogs(inc.id)} className="btn btn-secondary">History</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                      {filteredIncidents.filter(inc => inc.status !== 'CLOSED').length === 0 && (
                        <tr><td colSpan="5" style={{ textAlign: 'center', padding: '32px', color: '#94a3b8' }}>No active assignments match your criteria.</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* ADMINISTRATOR PORTAL */}
            {user.role === 'ADMINISTRATOR' && (
              <div>
                <div className="dashboard-card" style={{ padding: '0', overflow: 'hidden' }}>
                  <div style={{ display: 'flex', borderBottom: '1px solid #e2e8f0', background: '#f8fafc' }}>
                    <button onClick={() => setAdminTab('reports')} className={`nav-tab ${adminTab === 'reports' ? 'active' : ''}`}>Reports & Dashboards</button>
                    <button onClick={() => setAdminTab('mission')} className={`nav-tab ${adminTab === 'mission' ? 'active' : ''}`}>Mission Control</button>
                    <button onClick={() => setAdminTab('tickets')} className={`nav-tab ${adminTab === 'tickets' ? 'active' : ''}`}>Ticket Governance</button>
                    <button onClick={() => setAdminTab('users')} className={`nav-tab ${adminTab === 'users' ? 'active' : ''}`}>Users</button>
                    <button onClick={() => setAdminTab('categories')} className={`nav-tab ${adminTab === 'categories' ? 'active' : ''}`}>Categories</button>
                    <button onClick={() => setAdminTab('services')} className={`nav-tab ${adminTab === 'services' ? 'active' : ''}`}>IT Services</button>
                    <button onClick={() => setAdminTab('settings')} className={`nav-tab ${adminTab === 'settings' ? 'active' : ''}`}>Settings</button>
                    <button onClick={() => setAdminTab('slapolicies')} className={`nav-tab ${adminTab === 'slapolicies' ? 'active' : ''}`}>SLA Policies</button>
                    <button onClick={() => setAdminTab('violations')} className={`nav-tab ${adminTab === 'violations' ? 'active' : ''}`}>SLA Violations</button>
                    <button onClick={() => setAdminTab('risks')} className={`nav-tab ${adminTab === 'risks' ? 'active' : ''}`}>SLA Risks</button>
                  </div>

                  <div style={{ padding: '24px' }}>
                    {adminTab === 'mission' && <MissionControl />}
                    {adminTab === 'reports' && analytics && (
                      <div>
                         <h3 style={{ fontSize: '18px', fontWeight: '700', marginBottom: '24px', color: '#0f172a' }}>Global System Analytics</h3>
                         
                         <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '16px', marginBottom: '32px' }}>
                          <div className="stat-card" style={{ borderBottomColor: '#4f46e5' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase' }}>Total Tickets</div>
                            <div style={{ fontSize: '28px', fontWeight: '700', color: '#1e293b', marginTop: '8px' }}>{analytics.totalIncidents}</div>
                          </div>
                          <div className="stat-card" style={{ borderBottomColor: '#f59e0b' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase' }}>Open Tickets</div>
                            <div style={{ fontSize: '28px', fontWeight: '700', color: '#1e293b', marginTop: '8px' }}>{analytics.openIncidents}</div>
                          </div>
                          <div className="stat-card" style={{ borderBottomColor: '#ef4444' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase' }}>Critical (P1)</div>
                            <div style={{ fontSize: '28px', fontWeight: '700', color: '#ef4444', marginTop: '8px' }}>{analytics.criticalIncidents}</div>
                          </div>
                          <div className="stat-card" style={{ borderBottomColor: '#10b981' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase' }}>Resolved</div>
                            <div style={{ fontSize: '28px', fontWeight: '700', color: '#10b981', marginTop: '8px' }}>{analytics.resolvedIncidents}</div>
                          </div>
                          <div className="stat-card" style={{ borderBottomColor: '#8b5cf6' }}>
                            <div style={{ fontSize: '13px', color: '#64748b', fontWeight: '600', textTransform: 'uppercase' }}>SLA Breaches</div>
                            <div style={{ fontSize: '28px', fontWeight: '700', color: '#8b5cf6', marginTop: '8px' }}>{analytics.slaViolations}</div>
                          </div>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
                          <div className="report-section">
                             <div className="report-header">Incident Overview</div>
                             <div className="report-row"><span>Total Incidents</span><span className="report-val">{analytics.totalIncidents}</span></div>
                             <div className="report-row"><span>Open Incidents</span><span className="report-val">{analytics.openIncidents}</span></div>
                             <div className="report-row"><span>Critical Incidents</span><span className="report-val">{analytics.criticalIncidents}</span></div>
                             <div className="report-row"><span>Resolved Incidents</span><span className="report-val">{analytics.resolvedIncidents}</span></div>
                             <div className="report-row"><span>SLA Violations</span><span className="report-val">{analytics.slaViolations}</span></div>
                          </div>

                          <div className="report-section">
                             <div className="report-header">Technician Workload</div>
                             <div className="report-row" style={{ color: '#64748b', fontWeight: '600', textTransform: 'uppercase', fontSize: '12px' }}>
                               <span>Technician</span><span>Active Incidents</span>
                             </div>
                             {analytics.technicianWorkload?.map((t, idx) => (
                               <div key={idx} className="report-row"><span>{t.name}</span><span className="report-val">{t.active}</span></div>
                             ))}
                          </div>

                          <div className="report-section" style={{ gridColumn: 'span 2' }}>
                             <div className="report-header">Service Analysis</div>
                             <div className="report-row" style={{ color: '#64748b', fontWeight: '600', textTransform: 'uppercase', fontSize: '12px' }}>
                               <span>Service</span><span>Incidents</span>
                             </div>
                             {analytics.serviceAnalysis?.map((s, idx) => (
                               <div key={idx} className="report-row"><span>{s.serviceName}</span><span className="report-val">{s.count}</span></div>
                             ))}
                          </div>
                        </div>
                      </div>
                    )}

                    {adminTab === 'tickets' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px' }}>Global Ticket Governance</h3>
                        <div style={{ display: 'flex', gap: '12px', alignItems: 'center', background: '#f8fafc', flexWrap: 'wrap', marginBottom: '20px', border: '1px solid #e2e8f0', padding: '16px', borderRadius: '8px' }}>
                          <span style={{ fontWeight: '600', color: '#64748b', fontSize: '14px', width: '100%' }}>Filters & Search:</span>
                          <input type="text" className="input-field" placeholder="Search ID, description..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} style={{ flex: 1, minWidth: '150px' }} />
                          <select className="input-field" value={filterPriority} onChange={e => setFilterPriority(e.target.value)} style={{ width: '130px' }}>
                            <option value="ALL">All Priorities</option>
                            <option value="P1">P1</option>
                            <option value="P2">P2</option>
                            <option value="P3">P3</option>
                            <option value="P4">P4</option>
                          </select>
                          <select className="input-field" value={filterStatus} onChange={e => setFilterStatus(e.target.value)} style={{ width: '130px' }}>
                            <option value="ALL">All Statuses</option>
                            <option value="NEW">New</option>
                            <option value="CLASSIFIED">Classified</option>
                            <option value="ASSIGNED">Assigned</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="RESOLVED">Resolved</option>
                            <option value="CLOSED">Closed</option>
                          </select>
                          <select className="input-field" value={filterCategory} onChange={e => setFilterCategory(e.target.value)} style={{ width: '130px' }}>
                            <option value="ALL">All Categories</option>
                            {categories.map(c => <option key={c.id} value={c.name}>{c.name}</option>)}
                          </select>
                          <select className="input-field" value={filterTechnician} onChange={e => setFilterTechnician(e.target.value)} style={{ width: '130px' }}>
                            <option value="ALL">All Techs</option>
                            {allUsers.filter(u => u.role === 'TECHNICIAN').map(u => <option key={u.id} value={u.id.toString()}>{u.name}</option>)}
                          </select>
                          <select className="input-field" value={filterService} onChange={e => setFilterService(e.target.value)} style={{ width: '150px' }}>
                            <option value="ALL">All Services</option>
                            {services.map(s => <option key={s.id} value={s.id.toString()}>{s.serviceName}</option>)}
                          </select>
                          <select className="input-field" value={filterSla} onChange={e => setFilterSla(e.target.value)} style={{ width: '140px' }}>
                            <option value="ALL">All SLA</option>
                            <option value="ON_TRACK">On Track</option>
                            <option value="BREACHED">Breached</option>
                          </select>
                          <label style={{ fontSize: '12px', color: '#64748b' }}>From
                            <input type="date" className="input-field" value={filterDateFrom} onChange={e => setFilterDateFrom(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                          </label>
                          <label style={{ fontSize: '12px', color: '#64748b' }}>To
                            <input type="date" className="input-field" value={filterDateTo} onChange={e => setFilterDateTo(e.target.value)} style={{ width: '150px', marginLeft: '6px' }} />
                          </label>
                        </div>
                        
                        <table className="table-custom">
                          <thead>
                            <tr><th>ID</th><th>Description</th><th>Priority</th><th>Status</th><th>Tech</th><th>Action</th></tr>
                          </thead>
                          <tbody>
                            {filteredIncidents.map(inc => (
                              <tr key={inc.id}>
                                <td style={{ fontWeight: '600' }}>#{inc.id} {inc.duplicate && <span className="pill" style={{ background: '#fee2e2', color: '#dc2626', marginLeft: '6px' }}>Duplicate of #{inc.duplicateOfIncidentId}</span>}</td>
                                <td>{inc.description}</td>
                                <td><span className={`pill pill-${inc.priority}`}>{inc.priority}</span></td>
                                <td><span className="pill pill-status">{inc.status}</span></td>
                                <td style={{ fontSize: '13px' }}>{inc.assignedTechnician ? inc.assignedTechnician.name : 'Unassigned'}</td>
                                <td>
                                  <div style={{ display: 'flex', gap: '8px' }}>
                                    {(inc.status === 'RESOLVED' || inc.status === 'VERIFIED') && <button onClick={() => handleStatusUpdate(inc.id, 'CLOSED')} className="btn btn-success">Close</button>}
                                    <button onClick={() => fetchAuditLogs(inc.id)} className="btn btn-secondary">History</button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                            {filteredIncidents.length === 0 && (
                              <tr><td colSpan="6" style={{ textAlign: 'center', padding: '32px', color: '#94a3b8' }}>No incidents match governance criteria.</td></tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {adminTab === 'users' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px' }}>Enterprise Directory</h3>
                        <form onSubmit={handleCreateUser} style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr) auto', gap: '12px', marginBottom: '24px', alignItems: 'center' }}>
  <input type="text" className="input-field" placeholder="Name" maxLength={100} required value={newUser.name} onChange={e => setNewUser({ ...newUser, name: e.target.value })} />
  <input type="email" className="input-field" 	placeholder="Email" maxLength={150} required value={newUser.email} onChange={e => setNewUser({ ...newUser, email: e.target.value })} />
  <input type="password" className="input-field" placeholder="Password (min 6)" maxLength={100} required minLength={6} value={newUser.password} onChange={e => setNewUser({ ...newUser, password: e.target.value })} />
  <select className="input-field" value={newUser.role} onChange={e => setNewUser({ ...newUser, role: e.target.value })}>
    <option value="TECHNICIAN">Technician</option>
    <option value="EMPLOYEE">Employee</option>
    <option value="ADMINISTRATOR">Administrator</option>
  </select>
  <input type="text" className="input-field" placeholder="New category name (e.g. Cloud)..." maxLength={50} disabled={newUser.role !== 'TECHNICIAN'} value={newUser.skills} onChange={e => setNewUser({ ...newUser, skills: e.target.value })} />
  <button type="submit" className="btn btn-primary">Add User</button>
</form>
                        <table className="table-custom">
                          <thead>
                            <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Skills</th><th>Status</th><th>Actions</th></tr>
                          </thead>
                          <tbody>
                            {allUsers.map(u => (
                              <tr key={u.id}>
                                <td>#{u.id}</td>
                                <td style={{ fontWeight: '600' }}>{u.name}</td>
                                <td>{u.email}</td>
                                <td>
                                  <select className="input-field" value={u.role} disabled={u.id === user.id} onChange={e => handleChangeRole(u, e.target.value)} style={{ width: '150px' }}>
                                    <option value="EMPLOYEE">Employee</option>
                                    <option value="TECHNICIAN">Technician</option>
                                    <option value="ADMINISTRATOR">Administrator</option>
                                  </select>
                                </td>
                                <td style={{ fontSize: '13px', color: '#64748b' }}>{u.skills || '-'}</td>
                                <td>
                                  {u.approved ? <span className="pill" style={{ background: '#dcfce7', color: '#166534' }}>Approved</span> : <span className="pill" style={{ background: '#fef3c7', color: '#b45309' }}>Pending</span>}
                                </td>
                                <td>
                                  <div style={{ display: 'flex', gap: '8px' }}>
                                    {!u.approved && <button onClick={() => handleApproveUser(u.id)} className="btn btn-success">Approve</button>}
                                    <button onClick={() => handleDeleteUser(u.id)} className="btn btn-danger">Delete</button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {adminTab === 'categories' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px' }}>Category Management</h3>
                        <form onSubmit={handleAddCategory} style={{ display: 'flex', gap: '12px', marginBottom: '24px' }}>
                          <input type="text" className="input-field" placeholder="New category name (e.g. Cloud)..." value={newCategoryName} onChange={e => setNewCategoryName(e.target.value)} style={{ maxWidth: '300px' }} />
                          <button type="submit" className="btn btn-primary">Add Category</button>
                        </form>
                        <table className="table-custom">
                          <thead>
                            <tr><th>ID</th><th>Category</th><th>Description</th><th>Action</th></tr>
                          </thead>
                          <tbody>
                            {categories.map(c => (
                              <tr key={c.id}>
                                <td>#{c.id}</td>
                                <td style={{ fontWeight: '600' }}>{c.name}</td>
                                <td style={{ color: '#64748b' }}>{c.description}</td>
                                <td><button onClick={() => handleDeleteCategory(c.id)} className="btn btn-danger">Remove</button></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {adminTab === 'services' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px' }}>Managed Services</h3>
                        <form onSubmit={handleAddService} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 3fr auto', gap: '12px', marginBottom: '24px' }}>
                          <input className="input-field" placeholder="Service name" maxLength={100} required value={newService.serviceName} onChange={e => setNewService({ ...newService, serviceName: e.target.value })} />
                          <select className="input-field" value={newService.businessCriticality} onChange={e => setNewService({ ...newService, businessCriticality: e.target.value })}>
                            <option value="HIGH">High</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="LOW">Low</option>
                          </select>
                          <input className="input-field" placeholder="Description" maxLength={255} value={newService.description} onChange={e => setNewService({ ...newService, description: e.target.value })} />
                          <button type="submit" className="btn btn-primary">Add service</button>
                        </form>
                        <table className="table-custom">
                          <thead>
                            <tr><th>ID</th><th>Service Name</th><th>Criticality</th><th>Description</th><th>Action</th></tr>
                          </thead>
                          <tbody>
                            {services.map(s => (
                              <tr key={s.id}>
                                <td>#{s.id}</td>
                                <td style={{ fontWeight: '600' }}>{s.serviceName}</td>
                                <td><span className="pill pill-status">{s.businessCriticality}</span></td>
                                <td style={{ color: '#64748b' }}>{s.description}</td>
                                <td><button onClick={() => handleDeleteService(s.id)} className="btn btn-danger">Remove</button></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {adminTab === 'settings' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px' }}>System Configuration</h3>
                        <div style={{ background: '#f8fafc', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
                          <label style={{ display: 'block', fontSize: '14px', fontWeight: '600', marginBottom: '12px', color: '#1e293b' }}>Technician Assignment Strategy</label>
                          <select 
                            className="input-field"
                            value={configList.find(c => c.configKey === 'ASSIGNMENT_STRATEGY')?.configValue || 'SKILL_BASED'}
                            onChange={async (e) => {
                              await axios.post(`${API}/admin/config`, { configKey: 'ASSIGNMENT_STRATEGY', configValue: e.target.value });
                              loadDashboardData(user);
                              alert("Assignment Strategy Updated Successfully!");
                            }}
                            style={{ maxWidth: '300px' }}
                          >
                            <option value="SKILL_BASED">Skill-Based Assignment</option>
                            <option value="LOAD_BASED">Load-Based Assignment</option>
                          </select>
                          <p style={{ fontSize: '13px', color: '#64748b', marginTop: '12px', lineHeight: '1.5' }}>
                            Dynamically alters the algorithm used to route new incidents without system downtime. Active immediately.
                          </p>
                        </div>
                      </div>
                    )}
      
                    {adminTab === 'slapolicies' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '8px' }}>SLA policies</h3>
                        <p style={{ fontSize: '13px', color: '#667085', marginBottom: '16px' }}>Changes apply to incidents created after you save.</p>
                        <table className="table-custom">
                          <thead><tr><th>Priority</th><th>Response (min)</th><th>Resolution (min)</th><th></th></tr></thead>
                          <tbody>
                            {[...slaPolicies].sort((a, b) => a.priority.localeCompare(b.priority)).map(p => (
                              <tr key={p.id}>
                                <td><span className={`pill pill-${p.priority}`}>{p.priority}</span></td>
                                <td><input type="number" min="1" className="input-field" style={{ width: '120px' }}
                                  value={slaEdits[p.id]?.responseMinutes ?? p.responseMinutes}
                                  onChange={e => setSlaEdits(s => ({ ...s, [p.id]: { ...s[p.id], responseMinutes: e.target.value } }))} /></td>
                                <td><input type="number" min="1" className="input-field" style={{ width: '120px' }}
                                  value={slaEdits[p.id]?.resolutionMinutes ?? p.resolutionMinutes}
                                  onChange={e => setSlaEdits(s => ({ ...s, [p.id]: { ...s[p.id], resolutionMinutes: e.target.value } }))} /></td>
                                <td><button className="btn btn-primary" disabled={!slaEdits[p.id]} onClick={() => handleSaveSla(p)}>Save</button></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}

                    {adminTab === 'risks' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px', color: '#d97706' }}>SLA risk detection</h3>
                        <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '16px' }}>
                          Identifies open incidents approaching or at risk of breaching their SLA resolution deadlines (&lt; 2 hours remaining).
                        </p>
                        {slaRisks.length === 0 ? (
                          <div style={{ background: '#f0fdf4', color: '#166534', padding: '16px', borderRadius: '8px', border: '1px solid #bbf7d0', textAlign: 'center', fontWeight: '500' }}>
                            All open incidents are on track within their SLA targets.
                          </div>
                        ) : (
                          <table className="table-custom">
                            <thead>
                              <tr><th>ID</th><th>Priority</th><th>Description</th><th>Risk Level</th><th>Time Remaining</th><th>Deadline</th></tr>
                            </thead>
                            <tbody>
                              {slaRisks.map(risk => (
                                <tr key={risk.id}>
                                  <td style={{ fontWeight: '600' }}>#{risk.id}</td>
                                  <td><span className={`pill pill-${risk.priority}`}>{risk.priority}</span></td>
                                  <td>{risk.description}</td>
                                  <td>
                                    <span className="pill" style={{ background: risk.riskLevel === 'VIOLATED' ? '#fee2e2' : '#fef3c7', color: risk.riskLevel === 'VIOLATED' ? '#dc2626' : '#d97706' }}>
                                      {risk.riskLevel}
                                    </span>
                                  </td>
                                  <td style={{ fontWeight: '600', color: risk.minutesRemaining < 0 ? '#dc2626' : '#d97706' }}>
                                    {risk.minutesRemaining < 0 ? `Overdue by ${Math.abs(risk.minutesRemaining)} mins` : `${risk.minutesRemaining} mins left`}
                                  </td>
                                  <td style={{ color: '#64748b' }}>{new Date(risk.deadline).toLocaleString()}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </div>
                    )}

                    {adminTab === 'violations' && (
                      <div>
                        <h3 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '16px', color: '#ef4444' }}>Active SLA Violations</h3>
                        {slaViolations.length === 0 ? (
                          <div style={{ background: '#f0fdf4', color: '#166534', padding: '16px', borderRadius: '8px', border: '1px solid #bbf7d0', textAlign: 'center', fontWeight: '500' }}>
                            No active SLA violations.
                          </div>
                        ) : (
                          <table className="table-custom">
                            <thead>
                              <tr><th>ID</th><th>Priority</th><th>Description</th><th>Resolution Deadline</th></tr>
                            </thead>
                            <tbody>
                              {slaViolations.map(inc => (
                                <tr key={inc.id}>
                                  <td style={{ fontWeight: '600' }}>#{inc.id}</td>
                                  <td><span className="pill" style={{ background: '#ef4444', color: 'white' }}>{inc.priority}</span></td>
                                  <td>{inc.description}</td>
                                  <td style={{ color: '#ef4444', fontWeight: '600' }}>{new Date(inc.slaResolutionDeadline).toLocaleString()}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                                                <h3 style={{ fontSize: '16px', fontWeight: '700', margin: '32px 0 16px 0' }}>Violation History (saved)</h3>
                        {slaHistory.length === 0 ? (
                          <div style={{ color: '#94a3b8', textAlign: 'center', padding: '16px' }}>No violations recorded yet.</div>
                        ) : (
                          <table className="table-custom">
                            <thead>
                              <tr><th>Incident</th><th>Type</th><th>Priority</th><th>Deadline</th><th>Detected</th></tr>
                            </thead>
                            <tbody>
                              {slaHistory.map(v => (
                                <tr key={v.id}>
                                  <td style={{ fontWeight: '600' }}>#{v.incidentId}</td>
                                  <td><span className="pill pill-status">{v.type}</span></td>
                                  <td><span className={`pill pill-${v.priority}`}>{v.priority}</span></td>
                                  <td>{new Date(v.deadline).toLocaleString()}</td>
                                  <td style={{ color: '#ef4444', fontWeight: '600' }}>{new Date(v.detectedAt).toLocaleString()}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Right Column: Notifications */}
          <div>
            <div className="dashboard-card" style={{ position: 'sticky', top: '32px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #e2e8f0', paddingBottom: '12px' }}>
                <h3 style={{ fontSize: '16px', fontWeight: '700', margin: 0 }}>🔔 Notifications</h3>
                {notifications.filter(n => !n.read).length > 0 && (
                  <span style={{ background: '#ef4444', color: 'white', fontSize: '12px', fontWeight: '700', padding: '2px 8px', borderRadius: '999px' }}>
                    {notifications.filter(n => !n.read).length} New
                  </span>
                )}
              </div>
              
              {notifications.filter(n => !n.read).length > 0 && (
                <button onClick={markAllNotifRead} className="btn btn-secondary" style={{ width: '100%', marginBottom: '12px' }}>Mark all as read</button>
              )}

              {notifications.filter(n => !n.read).length === 0 ? (
                <div style={{ textAlign: 'center', color: '#94a3b8', padding: '24px 0', fontSize: '14px' }}>
                  You're all caught up!
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {notifications.filter(n => !n.read).map(n => (
                    <div key={n.id} className="notif">
                      <p style={{ fontSize: '13px', margin: '0 0 10px 0', color: '#1e293b', lineHeight: '1.4', fontWeight: '500' }}>{n.message}</p>
                      <button onClick={() => markNotifRead(n.id)} className="link-btn">Mark as read</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
      
      {user && showMail && <Mailbox user={user} onClose={() => setShowMail(false)} onChanged={() => loadDashboardData(user)} />}
      {/* Audit Trail Modal */}
      {auditModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
          <div className="dashboard-card" style={{ width: '600px', maxHeight: '80vh', overflowY: 'auto', margin: 0, display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #e2e8f0', paddingBottom: '12px' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '700', color: '#0f172a' }}>Incident #{currentAuditIncidentId} Audit Trail</h2>
              <button onClick={() => setAuditModalOpen(false)} style={{ background: 'none', border: 'none', fontSize: '24px', cursor: 'pointer', color: '#64748b', lineHeight: 1 }}>&times;</button>
            </div>
            {currentAuditLogs.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '14px', textAlign: 'center' }}>No audit history found.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {currentAuditLogs.map((log, idx) => (
                  <div key={idx} style={{ padding: '14px', background: '#f8fafc', borderRadius: '8px', borderLeft: '4px solid #4f46e5' }}>
                    <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '6px' }}>
                      {new Date(log.timestamp).toLocaleString()} - <strong>{log.userEmail}</strong>
                    </div>
                    <div style={{ fontSize: '14px', color: '#1e293b', fontWeight: '700', marginBottom: '4px' }}>
                      {log.actionPerformed}
                    </div>
                    <div style={{ fontSize: '13px', color: '#334155' }}>
                      {log.description}
                    </div>
                    {log.previousState && log.newState && log.previousState !== log.newState && (
                      <div style={{ fontSize: '12px', marginTop: '8px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        State: <span className="pill pill-status">{log.previousState}</span> &rarr; <span className="pill pill-status">{log.newState}</span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default App;