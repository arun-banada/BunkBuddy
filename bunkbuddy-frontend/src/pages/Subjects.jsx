import React, { useEffect, useState } from 'react';
import api from '../services/api';
import GlassCard from '../components/GlassCard';
import { motion } from 'framer-motion';
import { Check, X, Plus } from 'lucide-react';

const Subjects = () => {
  const [subjects, setSubjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newSubject, setNewSubject] = useState('');
  const [error, setError] = useState('');

  const fetchSubjects = async () => {
    try {
      const res = await api.get('/subjects');
      setSubjects(res.data);
    } catch (err) {
      setError('Error fetching subjects');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubjects();
  }, []);

  const handleAddSubject = async (e) => {
    e.preventDefault();
    if (!newSubject.trim()) return;
    try {
      await api.post('/subjects', { name: newSubject });
      setNewSubject('');
      fetchSubjects();
    } catch (err) {
      setError('Error adding subject');
    }
  };

  // markAttendance function removed as it is now exclusively handled by Dashboard

  if (loading) return <div className="p-8 text-center text-white">Loading Subjects...</div>;

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-3xl font-bold mb-2">Subject Management</h1>
        <p className="text-gray-400">Track your attendance and safe bunks for each subject.</p>
      </motion.div>

      {error && <p className="text-red-400">{error}</p>}

      <GlassCard className="mb-8">
        <form onSubmit={handleAddSubject} className="flex gap-4 items-end">
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-300 mb-1">New Subject Name</label>
            <input 
              type="text" 
              className="w-full rounded-md bg-gray-800 border-gray-700 text-white shadow-sm focus:border-purple-500 focus:ring focus:ring-purple-500 focus:ring-opacity-50 p-2" 
              value={newSubject} 
              onChange={(e) => setNewSubject(e.target.value)} 
              placeholder="e.g. Data Structures"
            />
          </div>
          <button type="submit" className="bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-md flex items-center gap-2 transition-colors">
            <Plus size={20} /> Add
          </button>
        </form>
      </GlassCard>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {subjects.map((subject) => (
          <GlassCard key={subject.subjectId} className="flex flex-col h-full relative overflow-hidden">
             <div className={`absolute top-0 left-0 w-full h-1 ${subject.currentPercentage >= 75 ? 'bg-green-500' : 'bg-red-500'}`}></div>
             <h3 className="text-xl font-semibold mb-4 mt-2">{subject.subjectName}</h3>
             
             <div className="flex justify-between items-center mb-4">
                <div className="text-center">
                    <p className="text-gray-400 text-xs">Attendance</p>
                    <p className={`text-2xl font-bold ${subject.currentPercentage >= 75 ? 'text-green-400' : 'text-red-400'}`}>
                        {subject.currentPercentage.toFixed(1)}%
                    </p>
                </div>
                <div className="text-center">
                    <p className="text-gray-400 text-xs">Attended/Total</p>
                    <p className="text-xl font-bold text-gray-200">{subject.attendedClasses}/{subject.totalClasses}</p>
                </div>
             </div>

             <div className="bg-gray-800/50 rounded-lg p-3 mb-6 flex-grow">
                 <p className="text-sm font-medium text-purple-400 mb-1">AI Prediction:</p>
                 <p className="text-sm text-gray-300">{subject.recommendation}</p>
             </div>

             {/* Attendance marking buttons have been removed from here. 
                 Users should only mark attendance for today's scheduled classes on the Dashboard. */}
          </GlassCard>
        ))}
      </div>
      
      {subjects.length === 0 && (
         <div className="text-center text-gray-500 mt-12">
            No subjects added yet. Add a subject above to start tracking.
         </div>
      )}
    </div>
  );
};

export default Subjects;
