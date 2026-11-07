package com.kabuapp.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tab { Topics, Holdings, Track }

/** Which screen the cover display shows; the inner display shows list and detail side by side. */
sealed interface Page {
    data object List : Page
    data class TopicDetail(val code: String) : Page
    data class HoldingDetail(val code: String) : Page
    data object AddHolding : Page
}

data class UiState(
    val data: AppData? = null,
    val refreshing: Boolean = false,
    val tab: Tab = Tab.Topics,
    val page: Page = Page.List,
    /** Selected stock per tab, so unfolding keeps showing the same one. */
    val selectedTopic: String? = null,
    val selectedHolding: String? = null,
    val holdings: List<String> = emptyList(),
    val disclaimerSeen: Boolean = true,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = Repository(app)
    private val prefs = Prefs(app)
    private val _state = MutableStateFlow(UiState(holdings = prefs.holdings, disclaimerSeen = prefs.disclaimerSeen))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(data = repository.load(fetched = false)) }
            refresh()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val fetched = repository.refresh()
            _state.update { it.copy(data = repository.load(fetched), refreshing = false) }
        }
    }

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab, page = Page.List) }

    fun openTopic(code: String) = _state.update { it.copy(selectedTopic = code, page = Page.TopicDetail(code)) }

    fun openHolding(code: String) = _state.update { it.copy(selectedHolding = code, page = Page.HoldingDetail(code)) }

    fun openAddHolding() = _state.update { it.copy(tab = Tab.Holdings, page = Page.AddHolding) }

    /** Back on the cover display: detail → list. Returns false when there is nowhere to go back to. */
    fun back(): Boolean {
        if (_state.value.page == Page.List) return false
        _state.update { it.copy(page = Page.List) }
        return true
    }

    fun addHolding(code: String) = setHoldings(_state.value.holdings.let { if (code in it) it else it + code })

    fun removeHolding(code: String) = setHoldings(_state.value.holdings - code)

    private fun setHoldings(codes: List<String>) {
        prefs.holdings = codes
        _state.update { it.copy(holdings = codes) }
    }

    fun dismissDisclaimer() {
        prefs.disclaimerSeen = true
        _state.update { it.copy(disclaimerSeen = true) }
    }
}
