import React from 'react';

export const Skeleton: React.FC<{ width?: string; height?: string; count?: number }> = ({ 
  width = '100%', 
  height = '20px', 
  count = 1 
}) => {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className="skeleton"
          style={{ width, height, marginBottom: i < count - 1 ? '12px' : 0 }}
        />
      ))}
    </>
  );
};

export const LoadingSpinner: React.FC<{ size?: 'small' | 'medium' | 'large' }> = ({ size = 'medium' }) => {
  const sizeMap = { small: '20px', medium: '40px', large: '60px' };
  return (
    <div className={`spinner spinner-${size}`} style={{ width: sizeMap[size], height: sizeMap[size] }} />
  );
};
