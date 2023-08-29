package pro.respawn.apiresult.sample.domain.model

import java.util.UUID

data class Transaction(
    val id: UUID = UUID.randomUUID()
)
