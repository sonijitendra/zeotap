import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { rcaApi } from '../api/client';
import { useIncidentStore } from '../store/incidentStore';
import { ArrowLeft, FileText, AlertCircle, CheckCircle } from 'lucide-react';

const CATEGORIES = ['Infrastructure', 'Configuration', 'Code Bug', 'Dependency', 'Network', 'Capacity', 'Security', 'Human Error', 'External', 'Unknown'];

export default function RcaForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { transitionState } = useIncidentStore();
  const [form, setForm] = useState({
    incidentStartTime: '', incidentEndTime: '', rootCauseCategory: '',
    rootCauseDetail: '', fixApplied: '', preventionSteps: '', submittedBy: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const validate = () => {
    const required: (keyof typeof form)[] = ['incidentStartTime', 'incidentEndTime', 'rootCauseCategory', 'rootCauseDetail', 'fixApplied', 'preventionSteps'];
    for (const field of required) {
      if (!form[field].trim()) return `${field} is required`;
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const err = validate();
    if (err) { setError(err); return; }
    setSubmitting(true); setError('');

    try {
      const payload = {
        ...form,
        incidentStartTime: new Date(form.incidentStartTime).toISOString(),
        incidentEndTime: new Date(form.incidentEndTime).toISOString(),
      };
      await rcaApi.submit(id!, payload);
      setSuccess(true);
      // Auto-close incident after RCA
      try { await transitionState(id!, 'CLOSED', form.submittedBy || 'operator'); } catch {}
      setTimeout(() => navigate(`/incidents/${id}`), 1500);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to submit RCA');
    } finally {
      setSubmitting(false);
    }
  };

  if (success) return (
    <div className="glass-card p-12 text-center animate-fade-in">
      <CheckCircle size={48} className="text-green-500 mx-auto mb-4" />
      <h2 className="text-xl font-bold text-green-400">RCA Submitted Successfully</h2>
      <p className="text-sm text-gray-500 mt-2">Incident will be closed. Redirecting...</p>
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto space-y-6 animate-fade-in">
      <button onClick={() => navigate(-1)} className="btn-ghost flex items-center gap-1 text-sm">
        <ArrowLeft size={16} /> Back
      </button>

      <div className="glass-card p-6">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-yellow-500 to-orange-500 flex items-center justify-center">
            <FileText size={20} className="text-white" />
          </div>
          <div>
            <h1 className="text-lg font-bold">Root Cause Analysis</h1>
            <p className="text-xs text-gray-500">All fields required before incident closure</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-sm text-red-400 flex items-center gap-2">
            <AlertCircle size={16} />{error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-gray-500 mb-1 block">Incident Start Time *</label>
              <input type="datetime-local" name="incidentStartTime" value={form.incidentStartTime}
                onChange={handleChange} className="input-field" />
            </div>
            <div>
              <label className="text-xs text-gray-500 mb-1 block">Incident End Time *</label>
              <input type="datetime-local" name="incidentEndTime" value={form.incidentEndTime}
                onChange={handleChange} className="input-field" />
            </div>
          </div>

          <div>
            <label className="text-xs text-gray-500 mb-1 block">Root Cause Category *</label>
            <select name="rootCauseCategory" value={form.rootCauseCategory}
              onChange={handleChange} className="input-field">
              <option value="">Select category...</option>
              {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>

          <div>
            <label className="text-xs text-gray-500 mb-1 block">Root Cause Detail *</label>
            <textarea name="rootCauseDetail" value={form.rootCauseDetail}
              onChange={handleChange} className="input-field min-h-[100px]"
              placeholder="Detailed root cause analysis..." />
          </div>

          <div>
            <label className="text-xs text-gray-500 mb-1 block">Fix Applied *</label>
            <textarea name="fixApplied" value={form.fixApplied}
              onChange={handleChange} className="input-field min-h-[80px]"
              placeholder="What fix was applied to resolve the incident?" />
          </div>

          <div>
            <label className="text-xs text-gray-500 mb-1 block">Prevention Steps *</label>
            <textarea name="preventionSteps" value={form.preventionSteps}
              onChange={handleChange} className="input-field min-h-[80px]"
              placeholder="Steps to prevent recurrence..." />
          </div>

          <div>
            <label className="text-xs text-gray-500 mb-1 block">Submitted By</label>
            <input type="text" name="submittedBy" value={form.submittedBy}
              onChange={handleChange} className="input-field" placeholder="your.email@company.com" />
          </div>

          <button type="submit" disabled={submitting} className="btn-primary w-full">
            {submitting ? 'Submitting...' : 'Submit RCA & Close Incident'}
          </button>
        </form>
      </div>
    </div>
  );
}
