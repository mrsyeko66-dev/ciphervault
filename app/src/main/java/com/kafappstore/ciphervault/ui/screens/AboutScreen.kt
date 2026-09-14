package com.kafappstore.ciphervault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafappstore.ciphervault.R
import com.kafappstore.ciphervault.ui.theme.CyberAmber
import com.kafappstore.ciphervault.ui.theme.CyberCyan
import com.kafappstore.ciphervault.ui.theme.MatrixBorderNeon
import com.kafappstore.ciphervault.ui.theme.MatrixGreenPrimary

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Emblem
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, MatrixGreenPrimary), CircleShape)
                .background(Color(0xFF0A140E)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ciphervault_logo),
                contentDescription = "CipherVault Logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "CipherVault",
            style = MaterialTheme.typography.displayMedium.copy(
                color = MatrixGreenPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = "Ultra-Secure 3-Layer Cascade Encryption System for Text & Documents",
            style = MaterialTheme.typography.bodySmall.copy(
                color = CyberCyan,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cryptography Architecture Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cryptographic Algorithms (Cascaded)",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ArchitectureStepItem(
                    step = "1",
                    title = "AES-256-GCM",
                    description = "Authenticated Encryption with Associated Data (AEAD) and key derived from user password",
                    color = MatrixGreenPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                ArchitectureStepItem(
                    step = "2",
                    title = "ChaCha20-Poly1305 (BouncyCastle)",
                    description = "Modern stream cipher with Poly1305 MAC based on combined key (Password + Pepper)",
                    color = CyberCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                ArchitectureStepItem(
                    step = "3",
                    title = "XOR Stream Transformation with SHA-256",
                    description = "Bit restructuring for maximum entropy and diffusion of cipher output",
                    color = CyberAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Key Derivation Specifications Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Technical Specifications & Key Standards",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SecuritySpecRow(label = "Key Derivation Function", value = "PBKDF2 with SHA-256")
                SecuritySpecRow(label = "Iteration Count", value = "600,000 iterations (Brute-force resistant)")
                SecuritySpecRow(label = "Random Salt Length", value = "16 bytes (Cryptographically secure per operation)")
                SecuritySpecRow(label = "Random IV Length", value = "12 bytes (AEAD cipher Nonce)")
                SecuritySpecRow(label = "App Secret Pepper", value = "32 characters customizable in Settings")
                SecuritySpecRow(label = "Output Encoding", value = "Base64 packaging Salt + IV + Ciphertext")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Memory & Privacy Guarantees
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MatrixGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Memory Security Principles (Zeroization)",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• No passwords or master secrets are written to persistent storage.\n" +
                            "• Byte arrays holding keys and sensitive buffers are immediately zeroed out after operations (Memory Zeroization).\n" +
                            "• File access is strictly managed via Android's official Storage Access Framework.\n" +
                            "• Network interactions are restricted to secure HTTPS endpoints for text/raw downloads only.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9EC4A7),
                        lineHeight = 20.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Files Streaming & Supported Formats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MatrixBorderNeon), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1610)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Large File Processing (1GB+) & Formats",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Chunked Streaming: Files are processed in 128KB chunks without filling device RAM, handling multi-gigabyte files effortlessly.\n" +
                            "• Supported Inputs: All file types (PDF, video, audio, images, archives, databases, and binaries) are supported.\n" +
                            "• Output Extensions: Choose between proprietary container format .cvault (retaining original name and extension for auto-restoration), .cenc, .enc, or any custom extension.\n" +
                            "• Project Management: Integrated with Room local database to draft confidential text over multiple sessions and encrypt directly.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF9EC4A7),
                        lineHeight = 20.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ArchitectureStepItem(
    step: String,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060D08))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, color), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium.copy(color = color, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF88A391))
            )
        }
    }
}

@Composable
private fun SecuritySpecRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF88A391))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MatrixGreenPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
