package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec

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

class Client(val handler: (Wirespec.Request<*>) -> Wirespec.Response<*> ){
  suspend fun AddPet(body: Pet) = 
     AddPet.Request(body)
       .let{req -> handler(req) as AddPet.Response<*> }
  suspend fun UpdatePet(body: Pet) = 
     UpdatePet.Request(body)
       .let{req -> handler(req) as UpdatePet.Response<*> }
  suspend fun FindPetsByStatus(status: FindPetsByStatusParameterStatus?) = 
     FindPetsByStatus.Request(status)
       .let{req -> handler(req) as FindPetsByStatus.Response<*> }
  suspend fun FindPetsByTags(tags: List<String>?) = 
     FindPetsByTags.Request(tags)
       .let{req -> handler(req) as FindPetsByTags.Response<*> }
  suspend fun GetPetById(petId: Long) = 
     GetPetById.Request(petId)
       .let{req -> handler(req) as GetPetById.Response<*> }
  suspend fun UpdatePetWithForm(petId: Long, name: String?, status: String?) = 
     UpdatePetWithForm.Request(petId, name, status)
       .let{req -> handler(req) as UpdatePetWithForm.Response<*> }
  suspend fun DeletePet(petId: Long, api_key: String?) = 
     DeletePet.Request(petId, api_key)
       .let{req -> handler(req) as DeletePet.Response<*> }
  suspend fun UploadFile(petId: Long, additionalMetadata: String?, body: String) = 
     UploadFile.Request(petId, additionalMetadata, body)
       .let{req -> handler(req) as UploadFile.Response<*> }
  suspend fun GetInventory() = 
     GetInventory.Request
       .let{req -> handler(req) as GetInventory.Response<*> }
  suspend fun PlaceOrder(body: Order) = 
     PlaceOrder.Request(body)
       .let{req -> handler(req) as PlaceOrder.Response<*> }
  suspend fun GetOrderById(orderId: Long) = 
     GetOrderById.Request(orderId)
       .let{req -> handler(req) as GetOrderById.Response<*> }
  suspend fun DeleteOrder(orderId: Long) = 
     DeleteOrder.Request(orderId)
       .let{req -> handler(req) as DeleteOrder.Response<*> }
  suspend fun CreateUser(body: User) = 
     CreateUser.Request(body)
       .let{req -> handler(req) as CreateUser.Response<*> }
  suspend fun CreateUsersWithListInput(body: List<User>) = 
     CreateUsersWithListInput.Request(body)
       .let{req -> handler(req) as CreateUsersWithListInput.Response<*> }
  suspend fun LoginUser(username: String?, password: String?) = 
     LoginUser.Request(username, password)
       .let{req -> handler(req) as LoginUser.Response<*> }
  suspend fun LogoutUser() = 
     LogoutUser.Request
       .let{req -> handler(req) as LogoutUser.Response<*> }
  suspend fun GetUserByName(username: String) = 
     GetUserByName.Request(username)
       .let{req -> handler(req) as GetUserByName.Response<*> }
  suspend fun UpdateUser(username: String, body: User) = 
     UpdateUser.Request(username, body)
       .let{req -> handler(req) as UpdateUser.Response<*> }
  suspend fun DeleteUser(username: String) = 
     DeleteUser.Request(username)
       .let{req -> handler(req) as DeleteUser.Response<*> }
}
