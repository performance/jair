const { v4: uuidv4 } = require('uuid');
const { User } = require('./User'); // To conceptually check if user_id exists

// In-memory store for recommendations
const recommendations = [];

// Allowed recommendation types (as per story I.1)
const ALLOWED_TYPES = ['TREATMENT_ADJUSTMENT', 'NEW_INTERVENTION', 'LIFESTYLE_CHANGE'];
// Allowed status values
const ALLOWED_STATUSES = ['ACTIVE', 'SUPERSEDED', 'DELETED'];


class Recommendation {
  constructor(professional_id, user_id, title, description, type, recommendation_details, consultation_id = null) {
    if (!ALLOWED_TYPES.includes(type)) {
      throw new Error(`Invalid recommendation type: ${type}. Allowed types are: ${ALLOWED_TYPES.join(', ')}`);
    }

    this.id = uuidv4();
    this.professional_id = professional_id; // ID of the professional who created it
    this.user_id = user_id; // ID of the user (patient/client) it's for
    this.consultation_id = consultation_id;
    this.title = title;
    this.description = description;
    this.type = type;
    this.recommendation_details = recommendation_details; // Should be a JSON object
    this.status = 'ACTIVE'; // Default status
    this.created_at = new Date();
    this.updated_at = new Date();
  }

  static create(data) {
    // Conceptual check for user_id existence
    // In a real DB, this might be a foreign key constraint.
    // For in-memory, we can check against the User model's store if needed for stricter validation.
    // if (!User.findById(data.user_id)) {
    //   throw new Error(`User with id ${data.user_id} not found.`);
    // }
    const newRecommendation = new Recommendation(
      data.professional_id,
      data.user_id,
      data.title,
      data.description,
      data.type,
      data.recommendation_details,
      data.consultation_id
    );
    recommendations.push(newRecommendation);
    return newRecommendation;
  }

  static findById(id) {
    return recommendations.find(rec => rec.id === id && rec.status !== 'DELETED');
  }
  
  static findByIdIncludeDeleted(id) { // Helper for updates/delete if needed to find a DELETED one
    return recommendations.find(rec => rec.id === id);
  }

  static findAllByProfessional(professional_id, filters = {}) {
    let results = recommendations.filter(rec => rec.professional_id === professional_id && rec.status !== 'DELETED');

    if (filters.user_id) {
      results = results.filter(rec => rec.user_id === filters.user_id);
    }
    if (filters.status && ALLOWED_STATUSES.includes(filters.status)) {
      // If filtering by status, we look for that specific status, even if it's 'DELETED'
      if (filters.status === 'DELETED') {
         results = recommendations.filter(rec => rec.professional_id === professional_id && rec.status === 'DELETED');
         if (filters.user_id) {
            results = results.filter(rec => rec.user_id === filters.user_id);
         }
      } else {
        results = results.filter(rec => rec.status === filters.status);
      }
    } else if (filters.status) {
        // Invalid status filter, perhaps return empty or ignore
        console.warn(`Invalid status filter: ${filters.status}. Allowed: ${ALLOWED_STATUSES.join(', ')}`);
    }


    return results;
  }

  static update(id, professional_id, updates) {
    const recIndex = recommendations.findIndex(rec => rec.id === id && rec.professional_id === professional_id);
    if (recIndex === -1) {
      return null; // Not found or does not belong to the professional
    }

    const recommendation = recommendations[recIndex];
    
    // Prevent accidental update of DELETED items unless status is explicitly being changed
    if (recommendation.status === 'DELETED' && (!updates.status || updates.status === 'DELETED')) {
        // Or throw an error: throw new Error("Cannot update a DELETED recommendation unless reactivating it.");
        // For now, we allow it, but this might be a business rule.
    }

    Object.keys(updates).forEach(key => {
      if (key === 'id' || key === 'professional_id' || key === 'user_id' || key === 'created_at') return; // Protect certain fields

      if (key === 'type' && !ALLOWED_TYPES.includes(updates[key])) {
        throw new Error(`Invalid recommendation type: ${updates[key]}. Allowed types are: ${ALLOWED_TYPES.join(', ')}`);
      }
      if (key === 'status' && !ALLOWED_STATUSES.includes(updates[key])) {
        throw new Error(`Invalid status: ${updates[key]}. Allowed statuses are: ${ALLOWED_STATUSES.join(', ')}`);
      }
      recommendation[key] = updates[key];
    });

    recommendation.updated_at = new Date();
    recommendations[recIndex] = recommendation;
    return recommendation;
  }

  static softDelete(id, professional_id) {
    const recIndex = recommendations.findIndex(rec => rec.id === id && rec.professional_id === professional_id);
    if (recIndex === -1) {
      return null; // Not found or does not belong to professional
    }
    if (recommendations[recIndex].status === 'DELETED') {
        return recommendations[recIndex]; // Already deleted, return as is
    }

    recommendations[recIndex].status = 'DELETED';
    recommendations[recIndex].updated_at = new Date();
    return recommendations[recIndex];
  }
}

module.exports = { Recommendation, ALLOWED_TYPES, ALLOWED_STATUSES, recommendations };
