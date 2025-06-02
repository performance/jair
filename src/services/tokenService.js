const jwt = require('jsonwebtoken');

// In a real application, use environment variables for secrets and make them complex
const ACCESS_TOKEN_SECRET = 'your-access-token-secret'; // Replace with a strong secret
const REFRESH_TOKEN_SECRET = 'your-refresh-token-secret'; // Replace with a strong secret

const ACCESS_TOKEN_EXPIRY = '15m'; // Short-lived access token (e.g., 15 minutes)
const REFRESH_TOKEN_EXPIRY = '7d'; // Longer-lived refresh token (e.g., 7 days)

function generateAccessToken(user, role = 'user') { // Added role, default to 'user' for backward compatibility
  const payload = {
    id: user.id,
    email: user.email, // Keep email for easier debugging if needed
    role: role, 
  };
  return jwt.sign(payload, ACCESS_TOKEN_SECRET, { expiresIn: ACCESS_TOKEN_EXPIRY });
}

function generateRefreshToken(user, role = 'user') { // Added role, default to 'user'
  const payload = {
    id: user.id,
    role: role, // Include role in refresh token for consistency
    // You might include a version number or a session ID for more advanced refresh token management
  };
  return jwt.sign(payload, REFRESH_TOKEN_SECRET, { expiresIn: REFRESH_TOKEN_EXPIRY });
}

function verifyAccessToken(token) {
  try {
    return jwt.verify(token, ACCESS_TOKEN_SECRET);
  } catch (error) {
    // console.error('Invalid access token:', error.message);
    return null; // Or throw a specific error
  }
}

function verifyRefreshToken(token) {
  try {
    return jwt.verify(token, REFRESH_TOKEN_SECRET);
  } catch (error) {
    // console.error('Invalid refresh token:', error.message);
    return null; // Or throw a specific error
  }
}

module.exports = {
  generateAccessToken,
  generateRefreshToken,
  verifyAccessToken,
  verifyRefreshToken,
  // Exporting for potential use in authMiddleware or if needed directly
  ACCESS_TOKEN_SECRET, 
  REFRESH_TOKEN_SECRET
};
