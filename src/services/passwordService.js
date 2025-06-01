const bcrypt = require('bcrypt');
const saltRounds = 10; // Or a configurable value

async function hashPassword(password) {
  try {
    const salt = await bcrypt.genSalt(saltRounds);
    const hash = await bcrypt.hash(password, salt);
    return hash;
  } catch (error) {
    console.error('Error hashing password:', error);
    throw new Error('Error hashing password');
  }
}

async function comparePassword(password, hash) {
  try {
    const isMatch = await bcrypt.compare(password, hash);
    return isMatch;
  } catch (error) {
    console.error('Error comparing password:', error);
    // It's important not to reveal whether the error was due to a bad hash or other issues
    // for security reasons, but for server-side logging, it's fine.
    return false; // Treat comparison errors as a mismatch
  }
}

module.exports = {
  hashPassword,
  comparePassword,
};
