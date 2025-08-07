package com.chromachaos.game.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chromachaos.game.data.model.GameSettings
import com.chromachaos.game.domain.usecase.GameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val gameUseCase: GameUseCase
) : ViewModel() {
    
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep
    
    private val _settings = MutableStateFlow(GameSettings())
    val settings: StateFlow<GameSettings> = _settings
    
    fun nextStep() {
        _currentStep.value = _currentStep.value + 1
    }
    
    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value = _currentStep.value - 1
        }
    }
    
    fun updateDifficulty(difficulty: com.chromachaos.game.data.model.Difficulty) {
        _settings.value = _settings.value.copy(difficulty = difficulty)
    }
    
    fun updateSoundEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableSound = enabled)
    }
    
    fun updateVibrationEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableVibration = enabled)
    }
    
    fun updateSpecialBlocksEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(enableSpecialBlocks = enabled)
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            gameUseCase.updateGameSettings(_settings.value)
        }
    }
} 