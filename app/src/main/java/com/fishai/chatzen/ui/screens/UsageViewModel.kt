package com.fishai.chatzen.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fishai.chatzen.data.database.UsageEntity
import com.fishai.chatzen.data.repository.SettingsRepository
import com.fishai.chatzen.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar

enum class TimeFilter {
    DAY, WEEK, MONTH, YEAR
}

enum class StatsMode {
    REQUESTS, TOKENS
}

data class UsageUiState(
    val usageData: List<UsageEntity> = emptyList(),
    val allModelsUsageData: List<UsageEntity> = emptyList(),
    val totalTokens: Int = 0,
    val totalRequests: Int = 0,
    val selectedFilter: TimeFilter = TimeFilter.DAY,
    val statsMode: StatsMode = StatsMode.REQUESTS,
    val currentModelId: String? = null
)

class UsageViewModel(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TimeFilter.DAY)
    private val _statsMode = MutableStateFlow(StatsMode.REQUESTS)
    
    val uiState: StateFlow<UsageUiState> = combine(
        _filter,
        _statsMode,
        settingsRepository.getSelectedModel()
    ) { filter, statsMode, selectedModelPair ->
        Triple(filter, statsMode, selectedModelPair)
    }.flatMapLatest { (filter, statsMode, selectedModelPair) ->
        val (start, end) = getTimeRange(filter)
        
        // 1. Get filtered data (for bar chart and current model summary)
        val filteredFlow = if (selectedModelPair != null) {
            usageRepository.getUsageInRangeForModel(start, end, selectedModelPair.second)
        } else {
            usageRepository.getUsageInRange(start, end)
        }

        // 2. Get ALL data for the time range (for donut chart distribution)
        val allFlow = usageRepository.getUsageInRange(start, end)
        
        combine(filteredFlow, allFlow, _filter, _statsMode) { filteredData, allData, f, sm ->
            UsageDataPackage(filteredData, allData, f, sm, selectedModelPair?.second)
        }
    }.map { pkg ->
        UsageUiState(
            usageData = pkg.filteredData,
            allModelsUsageData = pkg.allData,
            totalTokens = pkg.filteredData.sumOf { it.inputTokens + it.outputTokens },
            totalRequests = pkg.filteredData.size,
            selectedFilter = pkg.filter,
            statsMode = pkg.statsMode,
            currentModelId = pkg.modelId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageUiState()
    )

    fun setFilter(filter: TimeFilter) {
        _filter.value = filter
    }

    fun toggleStatsMode(isTokens: Boolean) {
        _statsMode.value = if (isTokens) StatsMode.TOKENS else StatsMode.REQUESTS
    }

    fun checkAndAutoUpgradeFilter() {
        val currentFilter = _filter.value
        val nextFilter = when(currentFilter) {
            TimeFilter.DAY -> TimeFilter.WEEK
            TimeFilter.WEEK -> TimeFilter.MONTH
            TimeFilter.MONTH -> TimeFilter.YEAR
            TimeFilter.YEAR -> null // No more upgrades
        }
        
        if (nextFilter != null) {
            _filter.value = nextFilter
        }
    }

    private data class UsageDataPackage(
        val filteredData: List<UsageEntity>,
        val allData: List<UsageEntity>,
        val filter: TimeFilter,
        val statsMode: StatsMode,
        val modelId: String?
    )

    private fun getTimeRange(filter: TimeFilter): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        when (filter) {
            TimeFilter.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeFilter.WEEK -> {
                // Ensure we start from Monday
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                val daysToSubtract = if (currentDay == Calendar.SUNDAY) 6 else currentDay - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            TimeFilter.YEAR -> {
                calendar.set(Calendar.MONTH, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }
        return Pair(calendar.timeInMillis, endTime)
    }
}

class UsageViewModelFactory(
    private val usageRepository: UsageRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsageViewModel(usageRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
