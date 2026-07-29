package com.example.trnberechnung.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.trnberechnung.database.MaritimeNoticeSyncEntity
import com.example.trnberechnung.database.SeafarerMessageEntity
import com.example.trnberechnung.repository.MaritimeNoticeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MaritimeNoticeUiState(
    val isRefreshing: Boolean = false,
    val detailLoading: Boolean = false,
    val selectedSummary: SeafarerMessageEntity? = null,
    val selectedDetail: SeafarerMessageEntity? = null,
    val error: String? = null,
)

class MaritimeNoticeViewModel(
    private val repository: MaritimeNoticeRepository,
) : ViewModel() {
    val current =
        repository.observeCurrent.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val unread =
        repository.observeUnread.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val archive =
        repository.observeArchive.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val all =
        repository.observeAll.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )
    val unreadCount =
        repository.unreadCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0,
        )
    val syncMetadata: StateFlow<MaritimeNoticeSyncEntity?> =
        repository.syncMetadata.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val searchResults =
        _query
            .combine(all) { query, notices ->
                if (query.isBlank()) {
                    notices
                } else {
                    val term = query.trim()
                    notices.filter {
                        it.title.contains(term, ignoreCase = true) ||
                            it.location.orEmpty().contains(term, ignoreCase = true) ||
                            it.publisher.contains(term, ignoreCase = true) ||
                            it.bfsNumber.contains(term, ignoreCase = true) ||
                            it.regionPath.contains(term, ignoreCase = true)
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )

    private val _uiState = MutableStateFlow(MaritimeNoticeUiState())
    val uiState: StateFlow<MaritimeNoticeUiState> = _uiState.asStateFlow()

    init {
        refresh(force = false)
    }

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun refresh(force: Boolean = true) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                repository.refresh(force)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(error = error.message ?: "Seefahrer-Nachrichten konnten nicht geladen werden.")
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    fun markRead(noticeId: String) {
        viewModelScope.launch { repository.markRead(noticeId) }
    }

    fun openDetail(summary: SeafarerMessageEntity) {
        _uiState.update {
            it.copy(
                selectedSummary = summary,
                selectedDetail = if (summary.detailRevision >= summary.revision) summary else null,
                detailLoading = summary.detailRevision < summary.revision,
                error = null,
            )
        }
        viewModelScope.launch {
            repository.markRead(summary.id)
            try {
                val detail = repository.detail(summary.id, summary.revision)
                _uiState.update {
                    it.copy(selectedDetail = detail, detailLoading = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        detailLoading = false,
                        error = error.message ?: "Die Meldung konnte nicht geladen werden.",
                    )
                }
            }
        }
    }

    fun closeDetail() {
        _uiState.update {
            it.copy(
                selectedSummary = null,
                selectedDetail = null,
                detailLoading = false,
                error = null,
            )
        }
    }

    class Factory(
        private val repository: MaritimeNoticeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MaritimeNoticeViewModel::class.java))
            return MaritimeNoticeViewModel(repository) as T
        }
    }
}
