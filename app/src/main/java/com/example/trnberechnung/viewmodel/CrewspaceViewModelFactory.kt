package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trnberechnung.repository.TideRepository
import com.example.trnberechnung.repository.ChatRepository

import com.example.trnberechnung.model.AuthRepository

class CrewspaceViewModelFactory(
    private val repository: TideRepository,
    private val chatRepository: ChatRepository,
    private val authRepo: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrewspaceViewModel::class.java)) {
            return CrewspaceViewModel(repository, chatRepository, authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
