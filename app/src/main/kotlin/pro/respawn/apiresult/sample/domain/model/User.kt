package pro.respawn.apiresult.sample.domain.model

import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
)
