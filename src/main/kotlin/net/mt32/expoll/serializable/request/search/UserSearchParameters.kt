package net.mt32.expoll.serializable.request.search

import kotlinx.serialization.Serializable
import net.mt32.expoll.serializable.request.SortingOrder
import net.mt32.expoll.tPollID
import kotlin.reflect.full.memberProperties

@Serializable
data class UserSearchParameters(
    val sortingOrder: SortingOrder = SortingOrder.DESCENDING,
    val sortingStrategy: SortingStrategy = SortingStrategy.CREATED,
    val specialFilter: SpecialFilter = SpecialFilter.ALL,
    val searchQuery: Query = Query()
) {
    enum class SortingStrategy {
        CREATED,
        USERNAME,
        FIRST_NAME,
        LAST_NAME,
        MAIL,
        DELETED
    }

    enum class SpecialFilter {
        ALL,
        DELETED,
        OIDC,
        ADMIN,
        DEACTIVATED
    }

    @Serializable
    data class Query(
        val username: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val memberInPoll: tPollID? = null,
        val mail: String? = null,
        val any: String? = null
    )

    @Serializable
    data class Descriptor(
        val sortingOrder: List<SortingOrder> = SortingOrder.values().toList(),
        val sortingStrategy: List<SortingStrategy> = SortingStrategy.values().toList(),
        val specialFilter: List<SpecialFilter> = SpecialFilter.values().toList(),
        val searchQuery: List<String> = Query::class.memberProperties.map { it.name }
    )
}