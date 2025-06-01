const app = require('./app');

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  // Log routes for clarity during development/testing
  console.log('Available routes:');
  console.log('POST /api/v1/auth/register');
  console.log('POST /api/v1/auth/login');
  console.log('POST /api/v1/auth/refresh-token');
  console.log('GET  /api/v1/me (Protected)');
  console.log('PUT  /api/v1/me (Protected)');
  console.log('');
  console.log('Professional routes:');
  console.log('POST /api/v1/professionals/auth/register');
  console.log('POST /api/v1/professionals/auth/login');
  console.log('POST /api/v1/professionals/auth/refresh-token');
  console.log('GET  /api/v1/professionals/me (Protected)');
  console.log('PUT  /api/v1/professionals/me (Protected)');
  console.log('');
  console.log('Professional Recommendation Management routes:');
  console.log('POST /api/v1/professionals/me/recommendations (Protected)');
  console.log('GET  /api/v1/professionals/me/recommendations (Protected)');
  console.log('GET  /api/v1/professionals/me/recommendations/{recommendationId} (Protected)');
  console.log('PUT  /api/v1/professionals/me/recommendations/{recommendationId} (Protected)');
  console.log('DELETE /api/v1/professionals/me/recommendations/{recommendationId} (Protected)');
});
