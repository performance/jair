const express = require('express');
const app = express();

const authRoutes = require('./routes/authRoutes');
const userRoutes = require('./routes/userRoutes');
const professionalAuthRoutes = require('./routes/professionalAuthRoutes');
const professionalProfileRoutes = require('./routes/professionalProfileRoutes');
const recommendationRoutes = require('./routes/recommendationRoutes');

// Middleware to parse JSON bodies
app.use(express.json());

// Mount user authentication routes
app.use('/api/v1/auth', authRoutes);

// Mount user profile routes
// The routes within userRoutes are /me, so this makes them /api/v1/me
app.use('/api/v1', userRoutes);

// Mount professional authentication routes
app.use('/api/v1/professionals/auth', professionalAuthRoutes);

// Mount professional profile routes
// The routes within professionalProfileRoutes are /me, so this makes them /api/v1/professionals/me
app.use('/api/v1/professionals', professionalProfileRoutes); // Existing: /api/v1/professionals/me

// Mount recommendation routes for professionals
// This will be /api/v1/professionals/me/recommendations
app.use('/api/v1/professionals/me/recommendations', recommendationRoutes);

// Handle 404 for undefined routes
app.use((req, res, next) => {
  res.status(404).json({ message: 'Not Found' });
});

// Basic error handler (can be expanded)
// This will catch errors passed by next(error) or uncaught synchronous errors in route handlers
// For async errors not wrapped in try-catch and passed to next(), you might need more robust setup or use a library.
app.use((err, req, res, next) => {
  console.error(err.stack); // Log error stack for debugging
  
  // Check if the error is one of our known types or has a status code
  if (err.status) {
    return res.status(err.status).json({ message: err.message });
  }
  
  // Default to 500 Internal Server Error
  res.status(500).json({ message: 'Internal Server Error' });
});

module.exports = app;
