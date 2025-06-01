const { v4: uuidv4 } = require('uuid');

// In-memory store for users
const users = [];

class User {
  constructor(email, password_hash, profile_name = null) {
    this.id = uuidv4();
    this.email = email;
    this.password_hash = password_hash;
    this.profile_name = profile_name;
    this.created_at = new Date();
    this.updated_at = new Date();
    this.last_login = null;
  }

  static findByEmail(email) {
    return users.find(user => user.email === email);
  }

  static findById(id) {
    return users.find(user => user.id === id);
  }

  static save(user) {
    // For updates, find existing user and update their properties
    const existingUserIndex = users.findIndex(u => u.id === user.id);
    if (existingUserIndex > -1) {
      users[existingUserIndex] = user;
      user.updated_at = new Date();
    } else {
      // For new users, just add to the array
      users.push(user);
    }
    return user;
  }

  // Method to update last_login timestamp
  updateLastLogin() {
    this.last_login = new Date();
    this.updated_at = new Date(); // Also update updated_at
    User.save(this);
  }

  // Method to update profile
  updateProfile(profile_name) {
    if (profile_name !== undefined) {
      this.profile_name = profile_name;
    }
    this.updated_at = new Date();
    User.save(this);
    return this;
  }

  // Helper to exclude password_hash from user object
  toJSON() {
    const { password_hash, ...userWithoutPassword } = this;
    return userWithoutPassword;
  }
}

// Export both the class and the users array for direct manipulation if needed (e.g. for clearing in tests)
module.exports = { User, users };
