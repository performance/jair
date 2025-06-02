const { User } = require('../models/User');
const { hashPassword, comparePassword } = require('../services/passwordService');
const { generateAccessToken, generateRefreshToken, verifyRefreshToken } = require('../services/tokenService');

// Basic email validation
const isValidEmail = (email) => {
  // Simple regex for email validation
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// Basic password strength (example: at least 6 characters)
const isValidPassword = (password) => {
  return password && password.length >= 6;
};

async function register(req, res) {
  try {
    const { email, password, profile_name } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }

    if (!isValidEmail(email)) {
      return res.status(400).json({ message: 'Invalid email format' });
    }

    if (!isValidPassword(password)) {
      return res.status(400).json({ message: 'Password must be at least 6 characters long' });
    }

    if (User.findByEmail(email)) {
      return res.status(400).json({ message: 'Email already exists' });
    }

    const hashedPassword = await hashPassword(password);
    const newUser = new User(email, hashedPassword, profile_name);
    User.save(newUser);

    res.status(201).json(newUser.toJSON());
  } catch (error) {
    console.error('Registration error:', error);
    // Check if the error is a known one (e.g., from hashPassword)
    if (error.message === 'Error hashing password') {
        return res.status(500).json({ message: 'Server error during password processing' });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

async function login(req, res) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }

    const user = User.findByEmail(email);
    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    const isMatch = await comparePassword(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    const accessToken = generateAccessToken(user, 'user'); // Explicitly set role for user
    const newRefreshToken = generateRefreshToken(user, 'user'); // Explicitly set role for user

    user.updateLastLogin(); // Update last_login timestamp

    res.status(200).json({
      accessToken,
      refreshToken: newRefreshToken, // Use the new variable name
      user: user.toJSON(),
    });
  } catch (error) {
    console.error('Login error:', error);
    if (error.message === 'Error comparing password') {
        // This specific error message is from our passwordService
        return res.status(500).json({ message: 'Server error during authentication' });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

async function refreshToken(req, res) {
  try {
    const { refreshToken: providedRefreshToken } = req.body;

    if (!providedRefreshToken) {
      return res.status(400).json({ message: 'Refresh token is required' });
    }

    const decoded = verifyRefreshToken(providedRefreshToken);
    if (!decoded) {
      return res.status(401).json({ message: 'Invalid or expired refresh token' });
    }

    const user = User.findById(decoded.id);
    if (!user) {
      // This could happen if the user is deleted after token issuance
      return res.status(401).json({ message: 'User not found for refresh token' });
    }

    // Generate new tokens
    const newAccessToken = generateAccessToken(user, 'user'); // Explicitly set role for user
    const newRefreshToken = generateRefreshToken(user, 'user'); // Explicitly set role for user

    res.status(200).json({
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    });
  } catch (error) {
    console.error('Refresh token error:', error);
    // Check for specific jwt errors if needed, e.g., TokenExpiredError, JsonWebTokenError
    if (error.name === 'TokenExpiredError') {
        return res.status(401).json({ message: 'Refresh token expired' });
    }
    if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({ message: 'Invalid refresh token' });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

module.exports = {
  register,
  login,
  refreshToken,
};
