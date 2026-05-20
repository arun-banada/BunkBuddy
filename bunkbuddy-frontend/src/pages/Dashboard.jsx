import React, { useEffect, useState } from 'react';
import api from '../services/api';
import GlassCard from '../components/GlassCard';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { motion } from 'framer-motion';
import { Check, X } from 'lucide-react';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [todayClasses, setTodayClasses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [markedClasses, setMarkedClasses] = useState({});

  const fetchData = async () => {
    try {
      const [resStats, resClasses] = await Promise.all([
         api.get('/dashboard'),
         api.get('/timetable/today').catch(() => ({ data: [] }))
      ]);
      setStats(resStats.data);
      setTodayClasses(resClasses.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleMarkAttendance = async (scheduleId, subjectId, status) => {
    try {
      await api.post('/attendance/mark', { subjectId, status });
      setMarkedClasses(prev => ({ ...prev, [scheduleId]: status }));
      fetchData(); // Refresh stats after marking
    } catch (error) {
      console.error("Error marking attendance", error);
    }
  };

  if (loading) return <div className="p-8 text-center text-white">Loading Dashboard...</div>;
  if (!stats) return <div className="p-8 text-center text-white">Error loading stats.</div>;

  const data = [
    { name: 'Attended', value: stats.totalAttended },
    { name: 'Missed', value: stats.totalClasses - stats.totalAttended },
  ];
  const COLORS = ['#10B981', '#EF4444']; // Green, Red

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="text-3xl font-bold mb-2">Welcome back, {stats.username}!</h1>
        <p className="text-gray-400">Here's your schedule and attendance overview.</p>
      </motion.div>

      {/* Today's Classes Section */}
      <GlassCard>
        <h2 className="text-2xl font-bold text-purple-400 mb-4">Today's Classes</h2>
        {todayClasses.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {todayClasses.map((schedule) => (
              <div key={schedule.id} className="bg-gray-800 border border-gray-700 p-4 rounded-xl flex flex-col justify-between">
                <div>
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-bold text-xl text-gray-100">{schedule.subject.name}</h3>
                    <span className="text-xs font-semibold bg-gray-700 text-purple-300 px-2 py-1 rounded">
                      {schedule.startTime} - {schedule.endTime}
                    </span>
                  </div>
                  <div className="text-sm text-gray-400 mb-4">
                    Current Attendance: <span className="font-medium text-gray-300">
                      {schedule.subject.totalClasses > 0 
                        ? ((schedule.subject.attendedClasses / schedule.subject.totalClasses) * 100).toFixed(0) + "%" 
                        : "N/A"}
                    </span>
                  </div>
                </div>
                
                {markedClasses[schedule.id] ? (
                   <div className={`py-2 text-center rounded-lg font-medium text-sm border ${markedClasses[schedule.id] === 'PRESENT' ? 'bg-green-900/30 border-green-500/50 text-green-400' : 'bg-red-900/30 border-red-500/50 text-red-400'}`}>
                      Marked {markedClasses[schedule.id]}
                   </div>
                ) : (
                  <div className="flex gap-2">
                    <button 
                      onClick={() => handleMarkAttendance(schedule.id, schedule.subject.id, 'PRESENT')}
                      className="flex-1 flex justify-center items-center gap-1 bg-green-600/20 hover:bg-green-600/40 border border-green-600/50 text-green-400 py-2 rounded-lg transition-colors font-medium text-sm"
                    >
                      <Check size={16} /> Present
                    </button>
                    <button 
                      onClick={() => handleMarkAttendance(schedule.id, schedule.subject.id, 'ABSENT')}
                      className="flex-1 flex justify-center items-center gap-1 bg-red-600/20 hover:bg-red-600/40 border border-red-600/50 text-red-400 py-2 rounded-lg transition-colors font-medium text-sm"
                    >
                      <X size={16} /> Absent
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-6 bg-gray-800/30 rounded-lg border border-gray-700 border-dashed">
            <p className="text-gray-400">You don't have any classes scheduled for today.</p>
            <p className="text-sm text-gray-500 mt-2">Make sure you've uploaded your timetable.</p>
          </div>
        )}
      </GlassCard>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <GlassCard className="flex flex-col items-center justify-center text-center">
          <h3 className="text-gray-400 text-sm mb-1">Overall Attendance</h3>
          <p className={`text-4xl font-bold ${stats.overallPercentage >= 75 ? 'text-green-400' : 'text-red-400'}`}>
            {stats.overallPercentage.toFixed(1)}%
          </p>
        </GlassCard>
        
        <GlassCard className="flex flex-col items-center justify-center text-center">
          <h3 className="text-gray-400 text-sm mb-1">Total Classes</h3>
          <p className="text-4xl font-bold text-blue-400">{stats.totalClasses}</p>
          <p className="text-xs text-gray-500 mt-2">Attended: {stats.totalAttended}</p>
        </GlassCard>

        <GlassCard className="flex flex-col items-center justify-center text-center">
          <h3 className="text-gray-400 text-sm mb-1">Risky Subjects</h3>
          <p className={`text-4xl font-bold ${stats.riskySubjects > 0 ? 'text-red-400' : 'text-green-400'}`}>
            {stats.riskySubjects}
          </p>
          <p className="text-xs text-gray-500 mt-2">Below 75% threshold</p>
        </GlassCard>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <GlassCard className="h-80">
          <h3 className="text-xl font-semibold mb-4">Attendance Distribution</h3>
          {stats.totalClasses > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                  {data.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px', color: '#fff' }} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
             <p className="text-gray-500 text-center mt-20">No data available. Add subjects and mark attendance.</p>
          )}
        </GlassCard>
        
        <GlassCard>
          <h3 className="text-xl font-semibold mb-4">AI Recommendations</h3>
          <div className="space-y-4">
            {stats.riskySubjects > 0 ? (
              <div className="p-4 bg-red-900/30 border border-red-500/50 rounded-lg">
                <p className="text-red-400 font-semibold">⚠️ Action Required</p>
                <p className="text-sm text-gray-300 mt-1">You have subjects below the 75% threshold. Focus on attending the next classes for these subjects.</p>
              </div>
            ) : (
              <div className="p-4 bg-green-900/30 border border-green-500/50 rounded-lg">
                <p className="text-green-400 font-semibold">✅ All Good!</p>
                <p className="text-sm text-gray-300 mt-1">Your attendance is healthy. Keep it up!</p>
              </div>
            )}
            
            <div className="p-4 bg-blue-900/30 border border-blue-500/50 rounded-lg">
              <p className="text-blue-400 font-semibold">💡 Tip</p>
              <p className="text-sm text-gray-300 mt-1">Check individual subjects for specific safe bunk predictions.</p>
            </div>
          </div>
        </GlassCard>
      </div>
    </div>
  );
};

export default Dashboard;
