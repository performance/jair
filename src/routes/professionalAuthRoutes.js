const express = require('express');
const router = express.Router();
const professionalController = require('../controllers/professionalController');

// POST /api/v1/professionals/auth/register
router.post('/register', professionalController.register);

// POST /api/v1/professionals/auth/login
router.post('/login', professionalController.login);

// POST /api/v1/professionals/auth/refresh-token
router.post('/refresh-token', professionalController.refreshToken);

module.exports = router;
