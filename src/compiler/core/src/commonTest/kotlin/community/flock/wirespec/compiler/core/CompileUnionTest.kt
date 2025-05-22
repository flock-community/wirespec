package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileUnionTest {

    private val compiler = """
        |type UserAccount = UserAccountPassword | UserAccountToken
        |type UserAccountPassword {
        |  username: String,
        |  password: String
        |}
        |type UserAccountToken {
        |  token: String
        |}
        |type User {
        |   username: String,
        |   account: UserAccount
        |}
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val expected = """
            |package community.flock.wirespec.generated.model
            |
            |sealed interface UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class UserAccountPassword(
            |  val username: String,
            |  val password: String
            |) : UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class UserAccountToken(
            |  val token: String
            |) : UserAccount
            |
            |package community.flock.wirespec.generated.model
            |
            |data class User(
            |  val username: String,
            |  val account: UserAccount
            |)
            |
        """.trimMargin()

        compiler { KotlinEmitter() } shouldBeRight expected
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |public sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record UserAccountPassword (
            |  String username,
            |  String password
            |) extends UserAccountimplements UserAccount {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record UserAccountToken (
            |  String token
            |) extends UserAccountimplements UserAccount {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record User (
            |  String username,
            |  UserAccount account
            |) {
            |};
            |
        """.trimMargin()

        compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun typeScript() {
        val ts = """
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |
            |export type UserAccount = UserAccountPassword | UserAccountToken
            |
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |
            |
            |export type UserAccountPassword = {
            |  "username": string,
            |  "password": string
            |}
            |
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |
            |
            |export type UserAccountToken = {
            |  "token": string
            |}
            |
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |
            |import {UserAccount} from '../model/UserAccount'
            |export type User = {
            |  "username": string,
            |  "account": UserAccount
            |}
            |
            |export {UserAccount} from './UserAccount'
            |export {UserAccountPassword} from './UserAccountPassword'
            |export {UserAccountToken} from './UserAccountToken'
            |export {User} from './User'
        """.trimMargin()

        compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun wireSpec() {
        val wirespec = """
            |type UserAccount = UserAccountPassword | UserAccountToken
            |
            |type UserAccountPassword {
            |  username: String,
            |  password: String
            |}
            |
            |type UserAccountToken {
            |  token: String
            |}
            |
            |type User {
            |  username: String,
            |  account: UserAccount
            |}
            |
        """.trimMargin()

        compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}
