package com.eldercare.assistant.ui.messages

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.MessageTemplate
import com.eldercare.assistant.data.entity.RecentContact
import com.eldercare.assistant.databinding.ActivityMessageEditorBinding
import com.eldercare.assistant.ui.messages.viewmodel.MessageTemplateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MessageEditorActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityMessageEditorBinding
    private val viewModel: MessageTemplateViewModel by viewModels()
    
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    private var selectedContact: RecentContact? = null
    private var selectedTemplate: MessageTemplate? = null
    private var finalMessage = ""
    
    private val SMS_PERMISSION_REQUEST_CODE = 101
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize TTS
        textToSpeech = TextToSpeech(this, this)
        
        // Initialize Speech Recognition
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        }
        
        // Get data from intent
        selectedContact = intent.getParcelableExtra("selected_contact")
        selectedTemplate = intent.getParcelableExtra("selected_template")
        
        setupUI()
        setupListeners()
        
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
        }
    }
    
    private fun setupUI() {
        selectedContact?.let { contact ->
            binding.contactNameText.text = contact.name
            binding.contactPhoneText.text = contact.phoneNumber
        }
        
        selectedTemplate?.let { template ->
            binding.templateTitleText.text = template.title
            binding.messageEditText.setText(template.content)
            finalMessage = template.content
        }
        
        // Set accessibility features
        binding.messageEditText.textSize = 18f
        binding.sendButton.textSize = 20f
        binding.voiceConfirmButton.textSize = 18f
        
        // Enable voice prompts
        Handler(Looper.getMainLooper()).postDelayed({
            speakText("Message editor ready. You can edit your message or use voice confirmation.")
        }, 500)
    }
    
    private fun setupListeners() {
        binding.sendButton.setOnClickListener {
            finalMessage = binding.messageEditText.text.toString().trim()
            if (finalMessage.isNotEmpty()) {
                confirmAndSendMessage()
            } else {
                speakText("Please enter a message first.")
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.voiceConfirmButton.setOnClickListener {
            if (!isListening) {
                startVoiceConfirmation()
            }
        }
        
        binding.backButton.setOnClickListener {
            finish()
        }
        
        binding.clearButton.setOnClickListener {
            binding.messageEditText.setText("")
            speakText("Message cleared.")
        }
    }
    
    private fun confirmAndSendMessage() {
        finalMessage = binding.messageEditText.text.toString().trim()
        selectedContact?.let { contact ->
            val confirmationText = "Ready to send message to ${contact.name}: $finalMessage. Say 'send' to confirm or 'cancel' to abort."
            speakText(confirmationText)
            
            // Auto-start voice confirmation after TTS
            Handler(Looper.getMainLooper()).postDelayed({
                startVoiceConfirmation()
            }, confirmationText.length * 100L) // Approximate TTS duration
        }
    }
    
    private fun startVoiceConfirmation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.RECORD_AUDIO), 102)
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'send' to confirm or 'cancel' to abort")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        isListening = true
        binding.voiceConfirmButton.text = "Listening..."
        binding.voiceConfirmButton.isEnabled = false
        
        speechRecognizer?.startListening(intent)
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("Speech", "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d("Speech", "Beginning of speech")
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                Log.d("Speech", "End of speech")
                isListening = false
                binding.voiceConfirmButton.text = "Voice Confirm"
                binding.voiceConfirmButton.isEnabled = true
            }
            
            override fun onError(error: Int) {
                Log.e("Speech", "Speech recognition error: $error")
                isListening = false
                binding.voiceConfirmButton.text = "Voice Confirm"
                binding.voiceConfirmButton.isEnabled = true
                speakText("Voice recognition error. Please try again.")
            }
            
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                    if (matches.isNotEmpty()) {
                        val spokenText = matches[0].lowercase()
                        Log.d("Speech", "Recognized: $spokenText")
                        
                        when {
                            spokenText.contains("send") || spokenText.contains("yes") -> {
                                speakText("Sending message now.")
                                sendSMS()
                            }
                            spokenText.contains("cancel") || spokenText.contains("no") -> {
                                speakText("Message cancelled.")
                                Toast.makeText(this@MessageEditorActivity, "Message cancelled", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                speakText("Please say 'send' to confirm or 'cancel' to abort.")
                                // Restart listening
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startVoiceConfirmation()
                                }, 2000)
                            }
                        }
                    }
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    
    private fun sendSMS() {
        selectedContact?.let { contact ->
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(contact.phoneNumber, null, finalMessage, null, null)
                
                // Update database
                updateDatabaseAfterSending()
                
                speakText("Message sent successfully to ${contact.name}.")
                Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()
                
                // Return to previous activity
                Handler(Looper.getMainLooper()).postDelayed({
                    setResult(Activity.RESULT_OK)
                    finish()
                }, 2000)
                
            } catch (e: Exception) {
                Log.e("SMS", "Failed to send SMS", e)
                speakText("Failed to send message. Please try again.")
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateDatabaseAfterSending() {
        lifecycleScope.launch {
            try {
                // Update template usage
                selectedTemplate?.let { template ->
                    viewModel.markTemplateAsUsed(template.id)
                }
                
                // Update recent contact
                selectedContact?.let { contact ->
                    viewModel.updateRecentContact(contact)
                }
                
            } catch (e: Exception) {
                Log.e("Database", "Failed to update database", e)
            }
        }
    }
    
    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            Log.d("TTS", "TextToSpeech initialized successfully")
        } else {
            Log.e("TTS", "TextToSpeech initialization failed")
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speakText("SMS permission granted.")
                } else {
                    speakText("SMS permission denied. Cannot send messages.")
                    Toast.makeText(this, "SMS permission required to send messages", Toast.LENGTH_LONG).show()
                }
            }
            102 -> { // RECORD_AUDIO permission
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speakText("Voice permission granted.")
                } else {
                    speakText("Voice permission denied. Voice confirmation not available.")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
