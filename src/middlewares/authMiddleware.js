const { verifyAccessToken } = require('../services/tokenService');
const { User } = require('../models/User');

function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (token == null) {
    return res.status(401).json({ message: 'Unauthorized: No token provided' });
  }

  const decoded = verifyAccessToken(token);

  if (!decoded) {
    return res.status(401).json({ message: 'Unauthorized: Invalid or expired token' });
  }

  // Attach user to request object
  const user = User.findById(decoded.id);
  if (!user) {
      // This case might happen if the user was deleted after the token was issued
      return res.status(401).json({ message: 'Unauthorized: User not found for token' });
  }
  
  // Exclude password_hash from the user object attached to req
  req.user = user.toJSON(); 
  next();
}

module.exports = { authenticateToken };
