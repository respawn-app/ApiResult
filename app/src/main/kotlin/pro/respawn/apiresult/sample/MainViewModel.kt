package pro.respawn.apiresult.sample

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.respawn.apiresult.asResult
import pro.respawn.apiresult.chain
import pro.respawn.apiresult.fold
import pro.respawn.apiresult.map
import pro.respawn.apiresult.recover
import pro.respawn.apiresult.requireNotNull
import pro.respawn.apiresult.sample.data.MockSecurityRepository
import pro.respawn.apiresult.sample.data.MockTransactionRepository
import pro.respawn.apiresult.sample.data.MockUserRepository
import pro.respawn.apiresult.sample.domain.SecurityRepository
import pro.respawn.apiresult.sample.domain.TransactionRepository
import pro.respawn.apiresult.sample.domain.errors.DeviceIntegrityException
import pro.respawn.apiresult.sample.domain.errors.TransactionError
import pro.respawn.apiresult.sample.domain.errors.TransactionError.NotEnoughFunds
import pro.respawn.apiresult.sample.util.log
import pro.respawn.apiresult.then
import pro.respawn.apiresult.tryChain
import pro.respawn.apiresult.tryRecover
import java.io.IOException
import java.util.UUID

enum class TransactionResult {
    NotEnoughFunds, UnsupportedDevice, Offline, UnknownError, Success
}

@Immutable
data class UiState(
    val userId: UUID? = null,
    val isLoading: Boolean = false,
    val result: TransactionResult? = null,
)

class MainViewModel(
    private val transactionRepository: TransactionRepository = MockTransactionRepository(),
    private val securityRepository: SecurityRepository = MockSecurityRepository(),
    private val userRepository: UserRepository = MockUserRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun onClickPurchase() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, result = null) }

        state.value.userId
            .asResult
            .requireNotNull()
            .then { userRepository.getUser(it) }
            .recover { userRepository.getAnonymousUser() }
            .tryChain { user -> _state.update { it.copy(userId = user.id, result = null) } }
            .chain { securityRepository.verifyDeviceIntegrity() }
            .then { user -> transactionRepository.performTransaction(user) }
            .chain { transaction -> transactionRepository.persistTransaction(transaction) }
            .log()
            .map { TransactionResult.Success }
            .tryRecover<DeviceIntegrityException, _> { TransactionResult.UnsupportedDevice }
            .tryRecover<IOException, _> { TransactionResult.Offline }
            .tryRecover<TransactionError, _> {
                when (it) {
                    is NotEnoughFunds -> TransactionResult.NotEnoughFunds
                }
            }
            .fold(
                onSuccess = { state.value.copy(result = it, isLoading = false) },
                onError = { state.value.copy(result = TransactionResult.UnknownError, isLoading = false) }
            )
            .also { _state.value = it }
    }
}
