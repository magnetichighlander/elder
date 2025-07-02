package com.eldercare.assistant.ui.settings

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.Fragment
import com.eldercare.assistant.databinding.FragmentSettingsBinding
import com.eldercare.assistant.utils.BackupManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment(), TextToSpeech.OnInitListener {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var backupManager: BackupManager
    private var tts: TextToSpeech? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        backupManager = BackupManager(requireContext())
        tts = TextToSpeech(requireContext(), this)
        
        setupClickListeners()
        updateLastBackupInfo()
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(0.8f) // Slower speech for elderly users
        }
    }
    
    private fun setupClickListeners() {
        binding.btnManualBackup.setOnClickListener {
            performBackup()
        }
        
        binding.btnRestoreBackup.setOnClickListener {
            performRestore()
        }
        
        // Add haptic feedback for better accessibility
        binding.btnManualBackup.setOnTouchListener { v, event ->
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            false
        }
        
        binding.btnRestoreBackup.setOnTouchListener { v, event ->
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            false
        }
    }
    
    private fun performBackup() {
        binding.btnManualBackup.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        // Perform backup in background
        Thread {
            val success = backupManager.createBackup()
            
            activity?.runOnUiThread {
                binding.btnManualBackup.isEnabled = true
                binding.progressBar.visibility = View.GONE
                updateBackupStatus(success)
                updateLastBackupInfo()
            }
        }.start()
    }
    
    private fun performRestore() {
        binding.btnRestoreBackup.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        // Perform restore in background
        Thread {
            val success = backupManager.restoreBackup()
            
            activity?.runOnUiThread {
                binding.btnRestoreBackup.isEnabled = true
                binding.progressBar.visibility = View.GONE
                updateBackupStatus(success, isRestore = true)
                updateLastBackupInfo()
            }
        }.start()
    }
    
    private fun updateBackupStatus(success: Boolean, isRestore: Boolean = false) {
        val message = if (success) {
            if (isRestore) "Data restored successfully!" else "Backup created successfully!"
        } else {
            if (isRestore) "Failed to restore data" else "Failed to create backup"
        }
        
        binding.tvBackupStatus.text = message
        binding.tvBackupStatus.contentDescription = message
        
        // Announce to screen reader and TTS
        speakText(message)
    }
    
    private fun updateLastBackupInfo() {
        val backupFile = File(backupManager.getBackupDir(), "eldercare_backup.json")
        val message = if (backupFile.exists()) {
            val date = Date(backupFile.lastModified())
            val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            "Last backup: ${formatter.format(date)}"
        } else {
            "No backups available"
        }
        
        binding.tvLastBackup.text = message
        binding.tvLastBackup.contentDescription = message
    }
    
    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "backup_status")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
        tts = null
        _binding = null
    }
}
