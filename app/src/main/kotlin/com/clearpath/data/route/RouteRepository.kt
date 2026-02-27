package com.clearpath.data.route

import kotlinx.coroutines.flow.Flow

class RouteRepository(private val dao: RouteDao) {

    fun observeAll(): Flow<List<SavedRoute>> = dao.observeAll()

    fun observeByAlias(alias: String): Flow<List<SavedRoute>> = dao.observeByAlias(alias)

    fun observeNoAlias(): Flow<List<SavedRoute>> = dao.observeNoAlias()

    fun observeAliases(): Flow<List<String>> = dao.observeAliases()

    suspend fun save(route: SavedRoute) = dao.insert(route)

    suspend fun getById(id: String): SavedRoute? = dao.getById(id)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getMostSurveilled(): SavedRoute? = dao.getMostSurveilled()
}
