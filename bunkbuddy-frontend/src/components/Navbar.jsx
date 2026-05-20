import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { LogOut, Home, BookOpen, Clock, Settings } from 'lucide-react';

const Navbar = () => {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-gray-900 border-b border-gray-800 px-4 py-3 flex items-center justify-between shadow-lg sticky top-0 z-50">
      <div className="flex items-center space-x-4">
        <Link to="/dashboard" className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-500 flex items-center gap-2">
          <BookOpen className="text-purple-500" /> BunkBuddy
        </Link>
        <div className="hidden md:flex space-x-2 ml-6">
          <Link to="/dashboard" className="text-gray-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium flex items-center gap-2"><Home size={16}/> Dashboard</Link>
          <Link to="/subjects" className="text-gray-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium flex items-center gap-2"><BookOpen size={16}/> Subjects</Link>
          <Link to="/timetable" className="text-gray-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium flex items-center gap-2"><Clock size={16}/> Timetable</Link>
        </div>
      </div>
      <div className="flex items-center">
        <button onClick={handleLogout} className="text-gray-300 hover:text-white p-2 rounded-full hover:bg-gray-800 transition-colors">
          <LogOut size={20} />
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
