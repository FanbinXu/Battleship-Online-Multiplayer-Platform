/**
 * Performance Optimization Utilities
 * 
 * This module provides utility functions for optimizing React component rendering
 * performance through deep comparison of props.
 */

interface Coord {
  r: number;
  c: number;
}

/**
 * Deep compare two arrays of coordinates
 * @param prev - Previous array of coordinates
 * @param next - Next array of coordinates
 * @returns true if arrays are equal, false otherwise
 */
export const areCoordArraysEqual = (prev: Coord[], next: Coord[]): boolean => {
  if (prev.length !== next.length) return false;
  
  for (let i = 0; i < prev.length; i++) {
    if (prev[i].r !== next[i].r || prev[i].c !== next[i].c) {
      return false;
    }
  }
  
  return true;
};

/**
 * Deep compare two objects by their keys and values
 * @param prev - Previous object
 * @param next - Next object
 * @returns true if objects are equal, false otherwise
 */
export const shallowEqual = (prev: any, next: any): boolean => {
  if (prev === next) return true;
  if (!prev || !next) return false;
  
  const prevKeys = Object.keys(prev);
  const nextKeys = Object.keys(next);
  
  if (prevKeys.length !== nextKeys.length) return false;
  
  for (const key of prevKeys) {
    if (prev[key] !== next[key]) return false;
  }
  
  return true;
};

/**
 * Performance monitoring utility for React components
 * Logs render time in development mode
 */
export const withPerformanceMonitoring = (componentName: string) => {
  if (import.meta.env.DEV) {  // Vite environment variable
    const start = performance.now();
    
    return () => {
      const end = performance.now();
      const renderTime = end - start;
      
      if (renderTime > 16) { // More than one frame (60fps)
        console.warn(
          `[Performance] ${componentName} took ${renderTime.toFixed(2)}ms to render`
        );
      }
    };
  }
  
  return () => {}; // No-op in production
};

/**
 * Debounce function for performance optimization
 * @param func - Function to debounce
 * @param wait - Wait time in milliseconds
 * @returns Debounced function
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeoutId: ReturnType<typeof setTimeout> | null = null;
  
  return (...args: Parameters<T>) => {
    if (timeoutId !== null) {
      clearTimeout(timeoutId);
    }
    
    timeoutId = setTimeout(() => {
      func(...args);
    }, wait);
  };
};

/**
 * Throttle function for performance optimization
 * @param func - Function to throttle
 * @param limit - Time limit in milliseconds
 * @returns Throttled function
 */
export const throttle = <T extends (...args: any[]) => any>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle = false;
  
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      
      setTimeout(() => {
        inThrottle = false;
      }, limit);
    }
  };
};

/**
 * Memoize function results for expensive computations
 * @param fn - Function to memoize
 * @returns Memoized function
 */
export const memoize = <T extends (...args: any[]) => any>(fn: T): T => {
  const cache = new Map<string, ReturnType<T>>();
  
  return ((...args: Parameters<T>): ReturnType<T> => {
    const key = JSON.stringify(args);
    
    if (cache.has(key)) {
      return cache.get(key)!;
    }
    
    const result = fn(...args);
    cache.set(key, result);
    
    return result;
  }) as T;
};

