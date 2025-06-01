const express = require('express');
const router = express.Router();
const recommendationController = require('../controllers/recommendationController');
const { authenticateProfessionalToken } = require('../middlewares/authProfessionalMiddleware');

// All routes here are prefixed with /api/v1/professionals/me/recommendations
// and are protected by professional authentication.
router.use(authenticateProfessionalToken);

// POST / (creates a new recommendation)
// Maps to /api/v1/professionals/me/recommendations
router.post('/', recommendationController.createRecommendation);

// GET / (gets all recommendations for the authenticated professional, with optional filters)
// Maps to /api/v1/professionals/me/recommendations
router.get('/', recommendationController.getRecommendations);

// GET /:recommendationId (gets a specific recommendation by ID)
// Maps to /api/v1/professionals/me/recommendations/:recommendationId
router.get('/:recommendationId', recommendationController.getRecommendationById);

// PUT /:recommendationId (updates a specific recommendation by ID)
// Maps to /api/v1/professionals/me/recommendations/:recommendationId
router.put('/:recommendationId', recommendationController.updateRecommendation);

// DELETE /:recommendationId (soft deletes a specific recommendation by ID)
// Maps to /api/v1/professionals/me/recommendations/:recommendationId
router.delete('/:recommendationId', recommendationController.deleteRecommendation);

module.exports = router;
