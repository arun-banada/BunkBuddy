import React, { useContext, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { LogOut, Home, BookOpen, Clock, Settings, Menu, X } from 'lucide-react';

const Navbar = () => {
  const { logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navLinks = [
    { name: 'Dashboard', path: '/dashboard', icon: <Home size={16}/> },
    { name: 'Subjects', path: '/subjects', icon: <BookOpen size={16}/> },
    { name: 'Timetable', path: '/timetable', icon: <Clock size={16}/> },
  ];

  return (
    <nav className="bg-gray-900 border-b border-gray-800 shadow-lg sticky top-0 z-50">
      <div className="px-4 py-3 flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link to="/dashboard" className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-500 flex items-center gap-2">
            <BookOpen className="text-purple-500" /> BunkBuddy
          </Link>
          <div className="hidden md:flex space-x-2 ml-6">
            {navLinks.map((link) => (
              <Link key={link.name} to={link.path} className={`px-3 py-2 rounded-md text-sm font-medium flex items-center gap-2 transition-colors ${location.pathname === link.path ? 'bg-gray-800 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-800'}`}>
                {link.icon} {link.name}
              </Link>
            ))}
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <button onClick={handleLogout} className="text-gray-300 hover:text-white p-2 rounded-full hover:bg-gray-800 transition-colors hidden md:block">
            <LogOut size={20} />
          </button>
          
          {/* Mobile menu button */}
          <button 
            className="md:hidden text-gray-300 hover:text-white p-2 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {isMobileMenuOpen && (
        <div className="md:hidden bg-gray-900 border-t border-gray-800">
          <div className="px-2 pt-2 pb-3 space-y-1 sm:px-3">
            {navLinks.map((link) => (
              <Link 
                key={link.name} 
                to={link.path} 
                onClick={() => setIsMobileMenuOpen(false)}
                className={`block px-3 py-2 rounded-md text-base font-medium flex items-center gap-2 ${location.pathname === link.path ? 'bg-gray-800 text-white' : 'text-gray-300 hover:text-white hover:bg-gray-800'}`}
              >
                {link.icon} {link.name}
              </Link>
            ))}
            <button 
              onClick={() => { setIsMobileMenuOpen(false); handleLogout(); }}
              className="w-full text-left mt-4 block px-3 py-2 rounded-md text-base font-medium text-red-400 hover:bg-gray-800 flex items-center gap-2"
            >
              <LogOut size={16} /> Logout
            </button>
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
