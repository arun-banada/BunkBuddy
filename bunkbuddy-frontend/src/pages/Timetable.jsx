import React, { useState } from 'react';
import api from '../services/api';
import GlassCard from '../components/GlassCard';
import { motion } from 'framer-motion';
import { UploadCloud, CheckCircle, X, Plus, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Timetable = () => {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  // matrix: { MONDAY: [{time: "9:00", subject: "DBMS"}], ... }
  const [timetableMatrix, setTimetableMatrix] = useState(null);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    setUploading(true);
    setMessage('');
    setTimetableMatrix(null);
    
    try {
      const res = await api.post('/timetable/upload', formData);
      setMessage(res.data.message);
      setTimetableMatrix(res.data.timetable);
    } catch (err) {
      const data = err.response?.data;
      const errorMsg = typeof data === 'string' ? data : (data?.message || 'Error uploading file.');
      setMessage(errorMsg);
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveSlot = (day, index) => {
    const updated = { ...timetableMatrix };
    updated[day].splice(index, 1);
    setTimetableMatrix(updated);
  };

  const handleSubjectChange = (day, index, field, value) => {
    const updated = { ...timetableMatrix };
    updated[day][index][field] = value;
    setTimetableMatrix(updated);
  };

  const handleAddSlot = (day) => {
    const updated = { ...timetableMatrix };
    if (!updated[day]) updated[day] = [];
    updated[day].push({ time: "00:00-00:00", subject: "New Subject" });
    setTimetableMatrix(updated);
  };

  const handleConfirm = async () => {
    setSaving(true);
    setMessage('');
    try {
      await api.post('/timetable/confirm', timetableMatrix);
      setMessage('Timetable saved successfully! Redirecting...');
      setTimeout(() => navigate('/'), 2000);
    } catch (err) {
      setMessage('Error saving timetable. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-3xl font-bold mb-2">Smart Timetable Parser</h1>
        <p className="text-gray-400">Upload your timetable. We'll extract your daily schedule.</p>
      </motion.div>

      {!timetableMatrix ? (
        <GlassCard>
          <form onSubmit={handleUpload} className="space-y-6">
            <div className="border-2 border-dashed border-gray-600 rounded-lg p-12 text-center hover:border-purple-500 transition-colors bg-gray-800/30">
              <UploadCloud className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <div className="text-sm text-gray-400">
                <label htmlFor="file-upload" className="relative cursor-pointer bg-transparent rounded-md font-medium text-purple-400 hover:text-purple-300 focus-within:outline-none">
                  <span>Upload a file</span>
                  <input id="file-upload" name="file-upload" type="file" className="sr-only" onChange={handleFileChange} accept="image/png, image/jpeg, application/pdf" />
                </label>
                <p className="pl-1 mt-2">or drag and drop</p>
              </div>
              <p className="text-xs text-gray-500 mt-2">PNG, JPG, PDF up to 10MB</p>
              {file && <p className="mt-4 text-green-400 font-semibold">Selected: {file.name}</p>}
            </div>
            
            <button 
              type="submit" 
              disabled={!file || uploading} 
              className="w-full bg-purple-600 hover:bg-purple-700 disabled:bg-gray-600 disabled:cursor-not-allowed text-white py-3 rounded-lg font-medium transition-colors flex items-center justify-center gap-2"
            >
              {uploading ? 'Processing Matrix (OpenCV & Tesseract)...' : 'Upload and Extract'}
            </button>
          </form>
          
          {message && (
              <div className={`mt-6 p-4 rounded-lg ${message.includes('Error') ? 'bg-red-900/30 text-red-400 border border-red-500/50' : 'bg-green-900/30 text-green-400 border border-green-500/50'}`}>
                  {message}
              </div>
          )}
        </GlassCard>
      ) : (
        <GlassCard className="flex flex-col h-full">
          <div className="flex justify-between items-center mb-6 border-b border-gray-700 pb-4">
             <div>
                <h2 className="text-2xl font-bold text-purple-400">Review Timetable</h2>
                <p className="text-sm text-gray-400 mt-1">Review your detected schedule. Edit time slots and subject names, or remove incorrect cells.</p>
             </div>
             <div className="flex gap-4">
                <button onClick={() => setTimetableMatrix(null)} className="bg-gray-700 hover:bg-gray-600 text-white px-4 py-2 rounded-lg font-medium transition-colors">
                  Cancel
                </button>
                <button onClick={handleConfirm} disabled={saving} className="bg-green-600 hover:bg-green-700 disabled:bg-gray-600 text-white px-6 py-2 rounded-lg font-medium transition-colors flex items-center gap-2">
                  <CheckCircle size={20} />
                  {saving ? 'Saving...' : 'Confirm & Save'}
                </button>
             </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {daysOfWeek.map((day) => (
              <div key={day} className="bg-gray-800/50 p-4 rounded-xl border border-gray-700">
                <div className="flex justify-between items-center mb-3">
                  <h3 className="font-semibold text-lg text-gray-200">{day}</h3>
                  <button onClick={() => handleAddSlot(day)} className="text-purple-400 hover:text-purple-300 text-sm flex items-center gap-1 bg-purple-900/30 px-2 py-1 rounded">
                    <Plus size={14} /> Add Slot
                  </button>
                </div>
                
                <div className="space-y-3">
                  {(timetableMatrix[day] || []).map((slot, idx) => (
                    <div key={idx} className="flex flex-col gap-2 bg-gray-900/50 p-3 rounded-lg border border-gray-600 relative group">
                      <button onClick={() => handleRemoveSlot(day, idx)} className="absolute -top-2 -right-2 bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <X size={14} />
                      </button>
                      <div className="flex items-center gap-2 text-sm text-gray-400">
                        <Clock size={14} />
                        <input 
                          type="text" 
                          value={slot.time} 
                          onChange={(e) => handleSubjectChange(day, idx, 'time', e.target.value)}
                          className="bg-transparent border-b border-gray-600 focus:border-purple-500 outline-none w-24 text-xs"
                          placeholder="HH:MM-HH:MM"
                        />
                      </div>
                      <input 
                        type="text" 
                        value={slot.subject} 
                        onChange={(e) => handleSubjectChange(day, idx, 'subject', e.target.value)}
                        className="bg-gray-800 border border-gray-700 rounded p-1.5 text-white text-sm focus:ring-1 focus:ring-purple-500 outline-none w-full font-medium"
                        placeholder="Subject Name"
                      />
                    </div>
                  ))}
                  {(!timetableMatrix[day] || timetableMatrix[day].length === 0) && (
                    <p className="text-sm text-gray-500 italic text-center py-4">No classes</p>
                  )}
                </div>
              </div>
            ))}
          </div>
          {message && (
             <div className="mt-6 p-4 rounded-lg bg-green-900/30 text-green-400 border border-green-500/50 text-center">
                {message}
             </div>
          )}
        </GlassCard>
      )}
    </div>
  );
};

export default Timetable;
