package com.rashodi.app.ui.calculator

import androidx.lifecycle.ViewModel
import com.rashodi.core.analytics.Allocation
import com.rashodi.core.analytics.Distribution
import com.rashodi.core.analytics.DistributionBucket
import com.rashodi.core.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope

data class DistributionUiState(
    val income: String = "",
    val buckets: List<DistributionBucket> = Distribution.DEFAULT,
    val percentSum: Int = 100,
    val valid: Boolean = true,
    val allocations: List<Allocation> = emptyList(),
)

class DistributionViewModel : ViewModel() {

    private val income = MutableStateFlow("")
    private val buckets = MutableStateFlow(Distribution.DEFAULT)

    fun setIncome(v: String) { income.value = v }

    fun setPercent(index: Int, value: String) {
        val p = value.filter { it.isDigit() }.take(3).toIntOrNull() ?: 0
        buckets.value = buckets.value.mapIndexed { i, b -> if (i == index) b.copy(percent = p.coerceIn(0, 100)) else b }
    }

    fun setLabel(index: Int, value: String) {
        buckets.value = buckets.value.mapIndexed { i, b -> if (i == index) b.copy(label = value) else b }
    }

    fun preset(values: List<DistributionBucket>) { buckets.value = values }

    val state: StateFlow<DistributionUiState> = combine(income, buckets) { inc, bks ->
        val incomeKop = Money.parseToKop(inc) ?: 0L
        val sum = Distribution.percentSum(bks)
        val valid = sum == 100
        val allocations = if (valid && incomeKop > 0) Distribution.allocate(incomeKop, bks) else emptyList()
        DistributionUiState(inc, bks, sum, valid, allocations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DistributionUiState())

    companion object {
        val RULE_50_30_20 = listOf(
            DistributionBucket("Необходимое", 50),
            DistributionBucket("Желания", 30),
            DistributionBucket("Сбережения", 20),
        )
    }
}
