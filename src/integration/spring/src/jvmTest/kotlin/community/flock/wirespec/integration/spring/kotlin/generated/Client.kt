package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec

import community.flock.wirespec.integration.spring.kotlin.generated.client.AddPetClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.UpdatePetClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.FindPetsByStatusClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.FindPetsByTagsClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.GetPetByIdClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.UpdatePetWithFormClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.DeletePetClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.UploadFileClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.GetInventoryClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.PlaceOrderClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.GetOrderByIdClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.DeleteOrderClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.CreateUserClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.CreateUsersWithListInputClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.LoginUserClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.LogoutUserClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.GetUserByNameClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.UpdateUserClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.DeleteUserClient

import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.AddPet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByStatus
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.FindPetsByTags
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetPetById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdatePetWithForm
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeletePet
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UploadFile
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetInventory
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PlaceOrder
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetOrderById
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteOrder
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUser
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.CreateUsersWithListInput
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LoginUser
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.LogoutUser
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetUserByName
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.UpdateUser
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.DeleteUser

import community.flock.wirespec.integration.spring.kotlin.generated.model.Pet
import community.flock.wirespec.integration.spring.kotlin.generated.model.FindPetsByStatusParameterStatus
import community.flock.wirespec.integration.spring.kotlin.generated.model.ApiResponse
import community.flock.wirespec.integration.spring.kotlin.generated.model.Order
import community.flock.wirespec.integration.spring.kotlin.generated.model.User
import community.flock.wirespec.integration.spring.kotlin.generated.model.OrderStatus
import community.flock.wirespec.integration.spring.kotlin.generated.model.Address
import community.flock.wirespec.integration.spring.kotlin.generated.model.Category
import community.flock.wirespec.integration.spring.kotlin.generated.model.Tag
import community.flock.wirespec.integration.spring.kotlin.generated.model.PetStatus

interface C: 
  AddPetClient,
  UpdatePetClient,
  FindPetsByStatusClient,
  FindPetsByTagsClient,
  GetPetByIdClient,
  UpdatePetWithFormClient,
  DeletePetClient,
  UploadFileClient,
  GetInventoryClient,
  PlaceOrderClient,
  GetOrderByIdClient,
  DeleteOrderClient,
  CreateUserClient,
  CreateUsersWithListInputClient,
  LoginUserClient,
  LogoutUserClient,
  GetUserByNameClient,
  UpdateUserClient,
  DeleteUserClient

open class Client(val serialization: Wirespec.Serialization<String>, val handler: (Wirespec.RawRequest) -> Wirespec.RawResponse ): C {
  override suspend fun addPet(body: Pet) = 
     AddPet.Request(body)
       .let { req -> AddPet.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> AddPet.fromResponse(serialization, rawRes) }
  override suspend fun updatePet(body: Pet) = 
     UpdatePet.Request(body)
       .let { req -> UpdatePet.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> UpdatePet.fromResponse(serialization, rawRes) }
  override suspend fun findPetsByStatus(status: FindPetsByStatusParameterStatus?) = 
     FindPetsByStatus.Request(status)
       .let { req -> FindPetsByStatus.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> FindPetsByStatus.fromResponse(serialization, rawRes) }
  override suspend fun findPetsByTags(tags: List<String>?) = 
     FindPetsByTags.Request(tags)
       .let { req -> FindPetsByTags.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> FindPetsByTags.fromResponse(serialization, rawRes) }
  override suspend fun getPetById(petId: Long) = 
     GetPetById.Request(petId)
       .let { req -> GetPetById.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> GetPetById.fromResponse(serialization, rawRes) }
  override suspend fun updatePetWithForm(petId: Long, name: String?, status: String?) = 
     UpdatePetWithForm.Request(petId, name, status)
       .let { req -> UpdatePetWithForm.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> UpdatePetWithForm.fromResponse(serialization, rawRes) }
  override suspend fun deletePet(petId: Long, api_key: String?) = 
     DeletePet.Request(petId, api_key)
       .let { req -> DeletePet.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> DeletePet.fromResponse(serialization, rawRes) }
  override suspend fun uploadFile(petId: Long, additionalMetadata: String?, body: String) = 
     UploadFile.Request(petId, additionalMetadata, body)
       .let { req -> UploadFile.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> UploadFile.fromResponse(serialization, rawRes) }
  override suspend fun getInventory() = 
     GetInventory.Request
       .let { req -> GetInventory.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> GetInventory.fromResponse(serialization, rawRes) }
  override suspend fun placeOrder(body: Order) = 
     PlaceOrder.Request(body)
       .let { req -> PlaceOrder.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> PlaceOrder.fromResponse(serialization, rawRes) }
  override suspend fun getOrderById(orderId: Long) = 
     GetOrderById.Request(orderId)
       .let { req -> GetOrderById.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> GetOrderById.fromResponse(serialization, rawRes) }
  override suspend fun deleteOrder(orderId: Long) = 
     DeleteOrder.Request(orderId)
       .let { req -> DeleteOrder.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> DeleteOrder.fromResponse(serialization, rawRes) }
  override suspend fun createUser(body: User) = 
     CreateUser.Request(body)
       .let { req -> CreateUser.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> CreateUser.fromResponse(serialization, rawRes) }
  override suspend fun createUsersWithListInput(body: List<User>) = 
     CreateUsersWithListInput.Request(body)
       .let { req -> CreateUsersWithListInput.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> CreateUsersWithListInput.fromResponse(serialization, rawRes) }
  override suspend fun loginUser(username: String?, password: String?) = 
     LoginUser.Request(username, password)
       .let { req -> LoginUser.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> LoginUser.fromResponse(serialization, rawRes) }
  override suspend fun logoutUser() = 
     LogoutUser.Request
       .let { req -> LogoutUser.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> LogoutUser.fromResponse(serialization, rawRes) }
  override suspend fun getUserByName(username: String) = 
     GetUserByName.Request(username)
       .let { req -> GetUserByName.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> GetUserByName.fromResponse(serialization, rawRes) }
  override suspend fun updateUser(username: String, body: User) = 
     UpdateUser.Request(username, body)
       .let { req -> UpdateUser.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> UpdateUser.fromResponse(serialization, rawRes) }
  override suspend fun deleteUser(username: String) = 
     DeleteUser.Request(username)
       .let { req -> DeleteUser.toRequest(serialization, req) }
       .let { rawReq -> handler(rawReq) }
       .let { rawRes -> DeleteUser.fromResponse(serialization, rawRes) }
}
