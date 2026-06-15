package com.rashodi.app.ui.funds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rashodi.app.data.db.FundEntity
import com.rashodi.app.data.db.FundTxnEntity
import com.rashodi.app.data.repo.FinanceRepository
import com.rashodi.app.ui.categories.CategoriesViewModel
import com.rashodi.core.money.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FundUi(
    val fund: FundEntity,
    val currentKop: Long,
    val progress: Double?,
    val txns: List<FundTxnEntity>,
)

data class FundsUiState(
    val funds: List<FundUi> = emptyList(),
    val totalCurrentKop: Long = 0,
    val totalTargetKop: Long = 0,
)

data class FundDraft(val id: Long = 0, val name: String = "", val target: String = "", val colorArgb: Int = CategoriesViewModel.PALETTE.first()) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()
}

data class TxnDraft(val fundId: Long, val fundName: String, val amount: String = "", val deposit: Boolean = true, val note: String = "") {
    val canSave: Boolean get() = Money.parseToKop(amount)?.let { it != 0L } == true
}

class FundsViewModel(private val repo: FinanceRepository) : ViewModel() {

    private val _fundDraft = MutableStateFlow<FundDraft?>(null)
    val fundDraft: StateFlow<FundDraft?> = _fundDraft
    private val _txnDraft = MutableStateFlow<TxnDraft?>(null)
    val txnDraft: StateFlow<TxnDraft?> = _txnDraft

    val state: StateFlow<FundsUiState> = combine(repo.funds, repo.fundTxns) { funds, txns ->
        val items = funds.map { f ->
            val ftx = txns.filter { it.fundId == f.id }
            val current = ftx.sumOf { it.amountKop }
            FundUi(f, current, Money.progress(current, f.targetKop), ftx.take(4))
        }
        FundsUiState(
            funds = items,
            totalCurrentKop = items.sumOf { it.currentKop },
            totalTargetKop = funds.sumOf { it.targetKop },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FundsUiState())

    // фонд
    fun openNewFund() { _fundDraft.value = FundDraft() }
    fun openEditFund(f: FundEntity) {
        _fundDraft.value = FundDraft(f.id, f.name, if (f.targetKop > 0) Money.toDecimalString(f.targetKop) else "", f.colorArgb)
    }
    fun setFundName(v: String) { _fundDraft.value = _fundDraft.value?.copy(name = v) }
    fun setFundTarget(v: String) { _fundDraft.value = _fundDraft.value?.copy(target = v) }
    fun setFundColor(v: Int) { _fundDraft.value = _fundDraft.value?.copy(colorArgb = v) }
    fun closeFund() { _fundDraft.value = null }
    fun saveFund() {
        val d = _fundDraft.value ?: return
        if (!d.canSave) return
        val target = Money.parseToKop(d.target)?.let { Math.abs(it) } ?: 0L
        viewModelScope.launch {
            val e = FundEntity(id = d.id, name = d.name.trim(), targetKop = target, colorArgb = d.colorArgb, sortOrder = 100)
            if (d.isNew) repo.addFund(e) else repo.updateFund(e)
            _fundDraft.value = null
        }
    }
    fun deleteFund(f: FundEntity) { viewModelScope.launch { repo.deleteFund(f) } }

    // операция фонда
    fun openTxn(f: FundEntity, deposit: Boolean = true) { _txnDraft.value = TxnDraft(f.id, f.name, deposit = deposit) }
    fun setTxnAmount(v: String) { _txnDraft.value = _txnDraft.value?.copy(amount = v) }
    fun setTxnDeposit(v: Boolean) { _txnDraft.value = _txnDraft.value?.copy(deposit = v) }
    fun setTxnNote(v: String) { _txnDraft.value = _txnDraft.value?.copy(note = v) }
    fun closeTxn() { _txnDraft.value = null }
    fun saveTxn() {
        val d = _txnDraft.value ?: return
        val raw = Money.parseToKop(d.amount)?.let { Math.abs(it) } ?: return
        val signed = if (d.deposit) raw else -raw
        viewModelScope.launch {
            repo.addFundTxn(FundTxnEntity(fundId = d.fundId, epochDay = LocalDate.now().toEpochDay(), amountKop = signed, note = d.note.trim()))
            _txnDraft.value = null
        }
    }
    fun deleteTxn(t: FundTxnEntity) { viewModelScope.launch { repo.deleteFundTxn(t) } }
}
