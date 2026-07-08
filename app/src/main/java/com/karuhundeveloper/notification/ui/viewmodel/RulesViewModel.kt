package com.karuhundeveloper.notification.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.karuhundeveloper.notification.data.local.AppDatabase
import com.karuhundeveloper.notification.data.local.entity.InterceptRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the main rules list screen.
 *
 * Exposes [rules] as a [StateFlow] so the Compose UI recomposes automatically whenever
 * the database table changes. All write operations run on [viewModelScope] (IO dispatcher
 * is used inside the Room DAO suspend functions).
 *
 * Uses [AndroidViewModel] to safely hold an application [Context] without leaking an Activity.
 */
class RulesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).interceptRuleDao()

    /** Live list of all saved [InterceptRule]s, ordered newest-first. */
    val rules: StateFlow<List<InterceptRule>> = dao.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun insertRule(rule: InterceptRule) {
        viewModelScope.launch { dao.insertRule(rule) }
    }

    fun deleteRule(rule: InterceptRule) {
        viewModelScope.launch { dao.deleteRule(rule) }
    }
}
