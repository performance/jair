const express = require('express');
const router = express.Router();
const professionalController = require('../controllers/professionalController');
const { authenticateProfessionalToken } = require('../middlewares/authProfessionalMiddleware');

// All routes in this file are protected and require a valid accessToken for a 'professional'

// GET /api/v1/professionals/me
router.get('/me', authenticateProfessionalToken, professionalController.getMe);

// PUT /api/v1/professionals/me
router.put('/me', authenticateProfessionalToken, professionalController.updateMe);

module.exports = router;
