const { Professional } = require('../models/Professional');
const { hashPassword, comparePassword } = require('../services/passwordService');
const { generateAccessToken, generateRefreshToken, verifyRefreshToken } = require('../services/tokenService');

// Basic email validation (can be moved to a shared utility if used in many places)
const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
// Basic password strength (example: at least 6 characters)
const isValidPassword = (password) => password && password.length >= 6;

async function register(req, res) {
  try {
    const { email, password, profile_name, professional_title } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }
    if (!isValidEmail(email)) {
      return res.status(400).json({ message: 'Invalid email format' });
    }
    if (!isValidPassword(password)) {
      return res.status(400).json({ message: 'Password must be at least 6 characters long' });
    }

    if (Professional.findByEmail(email)) {
      return res.status(400).json({ message: 'Email already exists for a professional account' });
    }

    const hashedPassword = await hashPassword(password);
    const newProfessional = new Professional(email, hashedPassword, profile_name, professional_title);
    // verification_status is set to 'PENDING' by default in the constructor
    Professional.save(newProfessional);

    res.status(201).json(newProfessional.toJSON());
  } catch (error) {
    console.error('Professional Registration error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

async function login(req, res) {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password are required' });
    }

    const professional = Professional.findByEmail(email);
    if (!professional) {
      return res.status(404).json({ message: 'Professional not found' });
    }

    // For this task, PENDING professionals can log in.
    // If verification_status needed to be 'VERIFIED' for login:
    // if (professional.verification_status !== 'VERIFIED') {
    //   return res.status(403).json({ message: 'Account not verified. Please check your email or contact support.' });
    // }

    const isMatch = await comparePassword(password, professional.password_hash);
    if (!isMatch) {
      return res.status(401).json({ message: 'Invalid credentials' });
    }

    const accessToken = generateAccessToken(professional, 'professional');
    const newRefreshToken = generateRefreshToken(professional, 'professional');

    professional.updateLastLogin();

    res.status(200).json({
      accessToken,
      refreshToken: newRefreshToken,
      professional: professional.toJSON(),
    });
  } catch (error) {
    console.error('Professional Login error:', error);
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
    // Ensure the refresh token was intended for a professional
    if (!decoded || decoded.role !== 'professional') {
      return res.status(401).json({ message: 'Invalid or expired refresh token for professional' });
    }

    const professional = Professional.findById(decoded.id);
    if (!professional) {
      return res.status(401).json({ message: 'Professional not found for refresh token' });
    }

    const newAccessToken = generateAccessToken(professional, 'professional');
    const newRefreshToken = generateRefreshToken(professional, 'professional'); // Rotate refresh token

    res.status(200).json({
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    });
  } catch (error) {
    console.error('Professional Refresh token error:', error);
    if (error.name === 'TokenExpiredError') {
        return res.status(401).json({ message: 'Refresh token expired' });
    }
    if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({ message: 'Invalid refresh token' });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

// Corresponds to GET /api/v1/professionals/me
async function getMe(req, res) {
  // req.professional is attached by the authenticateProfessionalToken middleware
  if (!req.professional || !req.professional.id) {
    return res.status(401).json({ message: 'Unauthorized: Professional data not available in request' });
  }
  res.status(200).json(req.professional);
}

// Corresponds to PUT /api/v1/professionals/me
async function updateMe(req, res) {
  try {
    const { profile_name, professional_title } = req.body;
    const professionalId = req.professional.id; // from middleware

    if (profile_name === undefined && professional_title === undefined) {
      return res.status(400).json({ message: 'No updateable fields provided (e.g., profile_name, professional_title)' });
    }

    const professionalInstance = Professional.findById(professionalId);
    if (!professionalInstance) {
        return res.status(404).json({ message: 'Professional not found' });
    }

    professionalInstance.updateProfile({ profile_name, professional_title });

    res.status(200).json(professionalInstance.toJSON());
  } catch (error) {
    console.error('Professional Update Me error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

module.exports = {
  register,
  login,
  refreshToken,
  getMe,
  updateMe,
};
