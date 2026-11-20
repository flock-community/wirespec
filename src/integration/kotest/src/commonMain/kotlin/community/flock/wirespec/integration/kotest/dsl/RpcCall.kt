package community.flock.wirespec.integration.kotest.dsl

import community.flock.wirespec.integration.kotest.extension.currentRpcContext
import community.flock.wirespec.integration.kotest.runtime.currentRandomSource
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.arbitrary
import kotlin.reflect.KClass

public suspend fun <S : Any, Req, Res> rpcCall(
    serviceClass: KClass<S>,
    requestGen: Gen<Req>,
    invoke: suspend (S, Req) -> Res,
): Res = invoke(currentRpcContext().service(serviceClass), requestGen.draw(currentRandomSource()))

public fun <Req> modifiedRpcRequest(requestGen: Gen<Req>, modification: Req.() -> Req): Arb<Req> = arbitrary { rs -> requestGen.draw(rs).modification() }
