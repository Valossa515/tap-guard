package io.github.valossa515.tapguard.core.data.repository

import io.github.valossa515.tapguard.core.data.database.dao.UserDecisionDao
import io.github.valossa515.tapguard.core.domain.risk.TrustedAppRepository
import java.util.Collections
import javax.inject.Inject

/**
 * [TrustedAppRepository] sobre o Room: os pacotes de `user_decision` com `decision` igual a
 * `TRUSTED` (comparação exata), num conjunto que não pode ser alterado.
 */
class RoomTrustedAppRepository
    @Inject
    constructor(
        private val userDecisionDao: UserDecisionDao,
    ) : TrustedAppRepository {
        override suspend fun trustedPackages(): Set<String> =
            Collections.unmodifiableSet(LinkedHashSet(userDecisionDao.getTrustedPackageNames()))
    }
