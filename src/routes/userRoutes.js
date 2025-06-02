const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { authenticateToken } = require('../middlewares/authMiddleware');

// All routes in this file are protected and require a valid accessToken

// GET /api/v1/me
router.get('/me', authenticateToken, userController.getMe);

// PUT /api/v1/me
router.put('/me', authenticateToken, userController.updateMe);

module.exports = router;
