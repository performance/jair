const { verifyAccessToken } = require('../services/tokenService');
const { Professional } = require('../models/Professional');

function authenticateProfessionalToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (token == null) {
    return res.status(401).json({ message: 'Unauthorized: No token provided' });
  }

  const decoded = verifyAccessToken(token);

  if (!decoded) {
    return res.status(401).json({ message: 'Unauthorized: Invalid or expired token' });
  }

  // Check for professional role
  if (decoded.role !== 'professional') {
    return res.status(403).json({ message: 'Forbidden: Access restricted to professionals' });
  }

  const professional = Professional.findById(decoded.id);
  if (!professional) {
    return res.status(401).json({ message: 'Unauthorized: Professional not found for token' });
  }
  
  req.professional = professional.toJSON(); // Attach professional to request, excluding password
  next();
}

module.exports = { authenticateProfessionalToken };
