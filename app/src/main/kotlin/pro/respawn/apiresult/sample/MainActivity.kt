package pro.respawn.apiresult.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pro.respawn.apiresult.sample.ui.theme.ApiResultTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            ApiResultTheme {
                Scaffold {
                    PurchaseScreen(
                        modifier = Modifier.padding(it),
                        state = state,
                        onClickPurchase = viewModel::onClickPurchase,
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseScreen(state: UiState, onClickPurchase: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text("UserId: \n${state.userId}", modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
            state.result?.let {
                Text("Transaction Result = ${state.result}", modifier = Modifier.padding(20.dp))
            }
            Button(onClick = onClickPurchase, modifier = Modifier.padding(12.dp)) {
                Text("Purchase")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PurchaseScreenPreview() {
    ApiResultTheme {
        PurchaseScreen(UiState(result = TransactionResult.UnknownError, userId = UUID.randomUUID()), {})
    }
}
