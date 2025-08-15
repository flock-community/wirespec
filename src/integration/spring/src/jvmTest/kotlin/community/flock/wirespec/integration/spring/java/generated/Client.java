package community.flock.wirespec.integration.spring.java.generated;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.endpoint.AddPet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdatePet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.FindPetsByStatus;
import community.flock.wirespec.integration.spring.java.generated.endpoint.FindPetsByTags;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetPetById;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdatePetWithForm;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeletePet;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UploadFile;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetInventory;
import community.flock.wirespec.integration.spring.java.generated.endpoint.PlaceOrder;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetOrderById;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeleteOrder;
import community.flock.wirespec.integration.spring.java.generated.endpoint.CreateUser;
import community.flock.wirespec.integration.spring.java.generated.endpoint.CreateUsersWithListInput;
import community.flock.wirespec.integration.spring.java.generated.endpoint.LoginUser;
import community.flock.wirespec.integration.spring.java.generated.endpoint.LogoutUser;
import community.flock.wirespec.integration.spring.java.generated.endpoint.GetUserByName;
import community.flock.wirespec.integration.spring.java.generated.endpoint.UpdateUser;
import community.flock.wirespec.integration.spring.java.generated.endpoint.DeleteUser;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.integration.spring.java.generated.model.FindPetsByStatusParameterStatus;
import community.flock.wirespec.integration.spring.java.generated.model.ApiResponse;
import community.flock.wirespec.integration.spring.java.generated.model.Order;
import community.flock.wirespec.integration.spring.java.generated.model.User;
import community.flock.wirespec.integration.spring.java.generated.model.OrderStatus;
import community.flock.wirespec.integration.spring.java.generated.model.Address;
import community.flock.wirespec.integration.spring.java.generated.model.Category;
import community.flock.wirespec.integration.spring.java.generated.model.Tag;
import community.flock.wirespec.integration.spring.java.generated.model.PetStatus;   

public class Client {
  private final java.util.function.Function<Wirespec.Request<?>, java.util.concurrent.CompletableFuture<Wirespec.Response<?>>> handler;

  public Client(java.util.function.Function<Wirespec.Request<?>, java.util.concurrent.CompletableFuture<Wirespec.Response<?>>> handler) {
    this.handler = handler;
  }

  public <Req extends Wirespec.Request<?>, Res extends Wirespec.Response<?>> java.util.concurrent.CompletableFuture<Res> handle(Req req) {
    return (java.util.concurrent.CompletableFuture<Res>) this.handler.apply(req);
}

  public java.util.concurrent.CompletableFuture<AddPet.Response<?>> addPet(Pet body) {
    var req = new AddPet.Request(body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<UpdatePet.Response<?>> updatePet(Pet body) {
    var req = new UpdatePet.Request(body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<FindPetsByStatus.Response<?>> findPetsByStatus(java.util.Optional<FindPetsByStatusParameterStatus> status) {
    var req = new FindPetsByStatus.Request(status);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<FindPetsByTags.Response<?>> findPetsByTags(java.util.Optional<java.util.List<String>> tags) {
    var req = new FindPetsByTags.Request(tags);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<GetPetById.Response<?>> getPetById(Long petId) {
    var req = new GetPetById.Request(petId);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<UpdatePetWithForm.Response<?>> updatePetWithForm(Long petId, java.util.Optional<String> name, java.util.Optional<String> status) {
    var req = new UpdatePetWithForm.Request(petId, name, status);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<DeletePet.Response<?>> deletePet(Long petId, java.util.Optional<String> api_key) {
    var req = new DeletePet.Request(petId, api_key);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<UploadFile.Response<?>> uploadFile(Long petId, java.util.Optional<String> additionalMetadata, String body) {
    var req = new UploadFile.Request(petId, additionalMetadata, body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<GetInventory.Response<?>> getInventory() {
    var req = new GetInventory.Request();
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<PlaceOrder.Response<?>> placeOrder(Order body) {
    var req = new PlaceOrder.Request(body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<GetOrderById.Response<?>> getOrderById(Long orderId) {
    var req = new GetOrderById.Request(orderId);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<DeleteOrder.Response<?>> deleteOrder(Long orderId) {
    var req = new DeleteOrder.Request(orderId);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<CreateUser.Response<?>> createUser(User body) {
    var req = new CreateUser.Request(body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<CreateUsersWithListInput.Response<?>> createUsersWithListInput(java.util.List<User> body) {
    var req = new CreateUsersWithListInput.Request(body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<LoginUser.Response<?>> loginUser(java.util.Optional<String> username, java.util.Optional<String> password) {
    var req = new LoginUser.Request(username, password);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<LogoutUser.Response<?>> logoutUser() {
    var req = new LogoutUser.Request();
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<GetUserByName.Response<?>> getUserByName(String username) {
    var req = new GetUserByName.Request(username);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<UpdateUser.Response<?>> updateUser(String username, User body) {
    var req = new UpdateUser.Request(username, body);
    return handle(req); 
  }

  public java.util.concurrent.CompletableFuture<DeleteUser.Response<?>> deleteUser(String username) {
    var req = new DeleteUser.Request(username);
    return handle(req); 
  }

}