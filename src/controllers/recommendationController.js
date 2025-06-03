const { Recommendation, ALLOWED_TYPES } = require('../models/Recommendation');
const { User, users } = require('../models/User'); // Used for validating user_id conceptually
const { Professional } = require('../models/Professional'); // Just in case, though professional_id comes from token

// POST /api/v1/professionals/me/recommendations
async function createRecommendation(req, res) {
  try {
    const professional_id = req.professional.id; // From authenticateProfessionalToken middleware
    const { user_id, consultation_id, title, description, type, recommendation_details } = req.body;

    // --- Input Validation ---
    if (!user_id || !title || !description || !type || recommendation_details === undefined) {
      return res.status(400).json({ message: 'Missing required fields: user_id, title, description, type, recommendation_details' });
    }

    if (!ALLOWED_TYPES.includes(type)) {
      return res.status(400).json({ message: `Invalid recommendation type. Allowed types: ${ALLOWED_TYPES.join(', ')}` });
    }

    // Conceptual check for user_id existence (replace with actual DB query if using a real DB)
    const patientExists = User.findById(user_id);
    if (!patientExists) {
      return res.status(404).json({ message: `User (patient) with ID ${user_id} not found.` });
    }
    // --- End Validation ---

    const newRecommendation = Recommendation.create({
      professional_id,
      user_id,
      consultation_id,
      title,
      description,
      type,
      recommendation_details
    });

    res.status(201).json(newRecommendation);
  } catch (error) {
    console.error('Create Recommendation Error:', error);
    if (error.message.startsWith('Invalid recommendation type') || error.message.startsWith('User with id')) {
        return res.status(400).json({ message: error.message });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

// GET /api/v1/professionals/me/recommendations
async function getRecommendations(req, res) {
  try {
    const professional_id = req.professional.id;
    const { user_id, status } = req.query; // Optional filters

    const filters = {};
    if (user_id) filters.user_id = user_id;
    if (status) filters.status = status;

    const professionalRecommendations = Recommendation.findAllByProfessional(professional_id, filters);
    res.status(200).json(professionalRecommendations);
  } catch (error) {
    console.error('Get Recommendations Error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

// GET /api/v1/professionals/me/recommendations/:recommendationId
async function getRecommendationById(req, res) {
  try {
    const professional_id = req.professional.id;
    const { recommendationId } = req.params;

    const recommendation = Recommendation.findById(recommendationId);

    if (!recommendation) {
      return res.status(404).json({ message: 'Recommendation not found or has been deleted.' });
    }

    if (recommendation.professional_id !== professional_id) {
      // This check ensures the recommendation belongs to the authenticated professional.
      // findById already filters by non-DELETED status.
      return res.status(403).json({ message: 'Forbidden: You do not have access to this recommendation.' });
    }

    res.status(200).json(recommendation);
  } catch (error) {
    console.error('Get Recommendation By ID Error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

// PUT /api/v1/professionals/me/recommendations/:recommendationId
async function updateRecommendation(req, res) {
  try {
    const professional_id = req.professional.id;
    const { recommendationId } = req.params;
    const updates = req.body;

    // --- Input Validation ---
    if (Object.keys(updates).length === 0) {
        return res.status(400).json({ message: 'No update fields provided.' });
    }
    if (updates.type && !ALLOWED_TYPES.includes(updates.type)) {
      return res.status(400).json({ message: `Invalid recommendation type. Allowed types: ${ALLOWED_TYPES.join(', ')}` });
    }
    if (updates.id || updates.professional_id || updates.user_id || updates.created_at) {
        return res.status(400).json({ message: 'Cannot update id, professional_id, user_id, or created_at fields.'});
    }
    // --- End Validation ---

    // First, check if the recommendation exists and belongs to the professional (even if deleted, to allow reactivation)
    const existingRec = Recommendation.findByIdIncludeDeleted(recommendationId);
    if (!existingRec) {
        return res.status(404).json({ message: 'Recommendation not found.' });
    }
    if (existingRec.professional_id !== professional_id) {
        return res.status(403).json({ message: 'Forbidden: You do not have permission to update this recommendation.' });
    }

    const updatedRecommendation = Recommendation.update(recommendationId, professional_id, updates);

    if (!updatedRecommendation) {
      // This case should be rare now due to the checks above, but kept for safety.
      return res.status(404).json({ message: 'Recommendation not found or does not belong to you.' });
    }

    res.status(200).json(updatedRecommendation);
  } catch (error) {
    console.error('Update Recommendation Error:', error);
     if (error.message.startsWith('Invalid recommendation type') || error.message.startsWith('Invalid status')) {
        return res.status(400).json({ message: error.message });
    }
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

// DELETE /api/v1/professionals/me/recommendations/:recommendationId
async function deleteRecommendation(req, res) {
  try {
    const professional_id = req.professional.id;
    const { recommendationId } = req.params;

    const recommendation = Recommendation.findById(recommendationId); // Check active recommendations first

    if (!recommendation) {
        const deletedRec = Recommendation.findByIdIncludeDeleted(recommendationId);
        if (deletedRec && deletedRec.status === 'DELETED' && deletedRec.professional_id === professional_id) {
             return res.status(200).json({ message: 'Recommendation already deleted.', recommendation: deletedRec});
        }
        return res.status(404).json({ message: 'Recommendation not found.' });
    }

    if (recommendation.professional_id !== professional_id) {
      return res.status(403).json({ message: 'Forbidden: You do not have permission to delete this recommendation.' });
    }

    const softDeletedRecommendation = Recommendation.softDelete(recommendationId, professional_id);

    // res.status(204).send(); // 204 No Content is common for DELETE
    res.status(200).json({ message: 'Recommendation successfully deleted (soft delete).', recommendation: softDeletedRecommendation });


  } catch (error) {
    console.error('Delete Recommendation Error:', error);
    res.status(500).json({ message: 'Internal Server Error' });
  }
}

module.exports = {
  createRecommendation,
  getRecommendations,
  getRecommendationById,
  updateRecommendation,
  deleteRecommendation,
};
