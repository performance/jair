package com.hairhealth.platform.service

import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class ClientEncryptionService {

    fun generateMasterKey(): ClientMasterKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()
        
        return ClientMasterKey(
            keyId = UUID.randomUUID(),
            algorithm = "AES-256-GCM",
            keyBytes = secretKey.encoded,
            createdAt = Date()
        )
    }
    
    fun encryptPhotoForStorage(
        photoData: ByteArray,
        masterKey: ClientMasterKey
    ): EncryptedPhotoPackage {
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(masterKey.keyBytes, "AES")
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(photoData)
        
        return EncryptedPhotoPackage(
            encryptedData = encryptedData,
            iv = iv,
            keyInfo = EncryptionKeyInfo(
                keyId = masterKey.keyId,
                algorithm = masterKey.algorithm,
                keyVersion = "v1"
            ),
            originalSize = photoData.size
        )
    }
    
    fun generateMedicalSharingKey(
        masterKey: ClientMasterKey,
        sessionId: UUID,
        photoId: UUID,
        professionalId: UUID,
        validityMinutes: Int
    ): MedicalSharingKey {
        
        // Derive a time-limited key for medical sharing
        val sessionSeed = "$sessionId-$photoId-$professionalId-${System.currentTimeMillis()}"
        val derivedKey = deriveKey(masterKey.keyBytes, sessionSeed.toByteArray())
        
        return MedicalSharingKey(
            keyId = UUID.randomUUID(),
            sessionId = sessionId,
            derivedKeyBytes = derivedKey,
            validUntil = Date(System.currentTimeMillis() + validityMinutes * 60 * 1000),
            allowedPhotoId = photoId,
            allowedProfessionalId = professionalId,
            maxDecryptions = 1, // Single use
            decryptionCount = 0
        )
    }
    
    fun createSecureMedicalPackage(
        photoPackage: EncryptedPhotoPackage,
        masterKey: ClientMasterKey,
        medicalSharingKey: MedicalSharingKey,
        notes: String?
    ): SecureMedicalPackage {
        
        // Re-encrypt the photo data with the medical sharing key
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(medicalSharingKey.derivedKeyBytes, "AES")
        
        // First decrypt with master key
        val masterSecretKey = SecretKeySpec(masterKey.keyBytes, "AES")
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, photoPackage.iv)
        decryptCipher.init(Cipher.DECRYPT_MODE, masterSecretKey, spec)
        val decryptedData = decryptCipher.doFinal(photoPackage.encryptedData)
        
        // Re-encrypt with medical sharing key
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val medicalIv = cipher.iv
        val medicalEncryptedData = cipher.doFinal(decryptedData)
        
        // Encrypt notes if provided
        val encryptedNotes = notes?.let { 
            cipher.doFinal(it.toByteArray())
        }
        
        return SecureMedicalPackage(
            sessionId = medicalSharingKey.sessionId,
            encryptedPhotoData = medicalEncryptedData,
            encryptedNotes = encryptedNotes,
            iv = medicalIv,
            keyInfo = MedicalKeyInfo(
                keyId = medicalSharingKey.keyId,
                expiresAt = medicalSharingKey.validUntil,
                maxDecryptions = medicalSharingKey.maxDecryptions
            ),
            accessRestrictions = AccessRestrictions(
                allowScreenshots = false,
                allowDownload = false,
                allowPrint = false,
                requireContinuousAuth = true,
                maxViewTimeMinutes = 5
            )
        )
    }
    
    private fun deriveKey(masterKey: ByteArray, seed: ByteArray): ByteArray {
        // Simple key derivation - in production use PBKDF2 or similar
        val combined = masterKey + seed
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(combined)
    }
}

data class ClientMasterKey(
    val keyId: UUID,
    val algorithm: String,
    val keyBytes: ByteArray,
    val createdAt: Date
)

data class EncryptedPhotoPackage(
    val encryptedData: ByteArray,
    val iv: ByteArray,
    val keyInfo: EncryptionKeyInfo,
    val originalSize: Int
)

data class EncryptionKeyInfo(
    val keyId: UUID,
    val algorithm: String,
    val keyVersion: String
)

data class MedicalSharingKey(
    val keyId: UUID,
    val sessionId: UUID,
    val derivedKeyBytes: ByteArray,
    val validUntil: Date,
    val allowedPhotoId: UUID,
    val allowedProfessionalId: UUID,
    val maxDecryptions: Int,
    val decryptionCount: Int
)

data class SecureMedicalPackage(
    val sessionId: UUID,
    val encryptedPhotoData: ByteArray,
    val encryptedNotes: ByteArray?,
    val iv: ByteArray,
    val keyInfo: MedicalKeyInfo,
    val accessRestrictions: AccessRestrictions
)

data class MedicalKeyInfo(
    val keyId: UUID,
    val expiresAt: Date,
    val maxDecryptions: Int
)
