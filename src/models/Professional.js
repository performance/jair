const { v4: uuidv4 } = require('uuid');

// In-memory store for professionals
const professionals = [];

class Professional {
  constructor(email, password_hash, profile_name = null, professional_title = null) {
    this.id = uuidv4();
    this.email = email;
    this.password_hash = password_hash;
    this.profile_name = profile_name;
    this.professional_title = professional_title;
    this.verification_status = 'PENDING'; // Default status
    this.created_at = new Date();
    this.updated_at = new Date();
    this.last_login = null;
  }

  static findByEmail(email) {
    return professionals.find(pro => pro.email === email);
  }

  static findById(id) {
    return professionals.find(pro => pro.id === id);
  }

  static save(professional) {
    const existingProIndex = professionals.findIndex(p => p.id === professional.id);
    if (existingProIndex > -1) {
      professionals[existingProIndex] = professional;
      professional.updated_at = new Date();
    } else {
      professionals.push(professional);
    }
    return professional;
  }

  updateLastLogin() {
    this.last_login = new Date();
    this.updated_at = new Date();
    Professional.save(this);
  }

  updateProfile({ profile_name, professional_title }) {
    if (profile_name !== undefined) {
      this.profile_name = profile_name;
    }
    if (professional_title !== undefined) {
      this.professional_title = professional_title;
    }
    this.updated_at = new Date();
    Professional.save(this);
    return this;
  }

  toJSON() {
    const { password_hash, ...proWithoutPassword } = this;
    return proWithoutPassword;
  }
}

// Export both the class and the professionals array
module.exports = { Professional, professionals };
