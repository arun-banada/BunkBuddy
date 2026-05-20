import React from 'react';
import { motion } from 'framer-motion';

const GlassCard = ({ children, className, ...props }) => {
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      className={`glass-panel p-6 rounded-2xl ${className}`}
      {...props}
    >
      {children}
    </motion.div>
  );
};

export default GlassCard;
