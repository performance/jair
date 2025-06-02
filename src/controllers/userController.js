const { User } = require('../models/User'); // Import User model to access its static methods like findById

// GET /api/v1/me (Protected)
async function getMe(req, res) {
  // req.user is attached by the authenticateToken middleware
  // and already excludes password_hash thanks to User.toJSON() in the middleware
  if (!req.user || !req.user.id) {
    // This should ideally not happen if authenticateToken is working correctly
    return res.status(401).json({ message: 'Unauthorized: User not available in request' });
  }
  
  // Retrieve the most up-to-date user information, though req.user might be sufficient
  // depending on how stale you allow req.user to be.
  // For this implementation, req.user (which is already a JSON object) is fine.
  res.status(200).json(req.user);
}

// PUT /api/v1/me (Protected)
async function updateMe(req, res) {
  try {
    const { profile_name } = req.body;
    const userId = req.user.id; // from authenticateToken middleware

    if (profile_name === undefined) {
      // Or handle other updatable fields if any. If only profile_name is updatable and it's not provided:
      return res.status(400).json({ message: 'No updateable fields provided (e.g., profile_name)' });
    }
    
    // We need the full User object to call its methods
    const userInstance = User.findById(userId);
    if (!userInstance) {
        // Should not happen if token is valid and user exists
        return res.status(404).json({ message: 'User not found' });
    }

    userInstance.updateProfile(profile_name);
    // User.save(userInstance) is called within userInstance.updateProfile()

    res.status(200).json(userInstance.toJSON());
  } catch (error) {
    console.error('Update Me error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

module.exports = {
  getMe,
  updateMe,
};
