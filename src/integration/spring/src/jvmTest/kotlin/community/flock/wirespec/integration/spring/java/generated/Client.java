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
  private final Wirespec.Serialization<String> serialization;
  private final java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler;

  public Client(Wirespec.Serialization<String> serialization, java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler) {
    this.serialization = serialization;
    this.handler = handler;
  }

  public java.util.concurrent.CompletableFuture<AddPet.Response<?>> addPet(Pet body) {
    var req = new AddPet.Request(body);
    var rawReq = AddPet.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> AddPet.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<UpdatePet.Response<?>> updatePet(Pet body) {
    var req = new UpdatePet.Request(body);
    var rawReq = UpdatePet.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> UpdatePet.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<FindPetsByStatus.Response<?>> findPetsByStatus(java.util.Optional<FindPetsByStatusParameterStatus> status) {
    var req = new FindPetsByStatus.Request(status);
    var rawReq = FindPetsByStatus.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> FindPetsByStatus.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<FindPetsByTags.Response<?>> findPetsByTags(java.util.Optional<java.util.List<String>> tags) {
    var req = new FindPetsByTags.Request(tags);
    var rawReq = FindPetsByTags.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> FindPetsByTags.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<GetPetById.Response<?>> getPetById(Long petId) {
    var req = new GetPetById.Request(petId);
    var rawReq = GetPetById.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> GetPetById.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<UpdatePetWithForm.Response<?>> updatePetWithForm(Long petId, java.util.Optional<String> name, java.util.Optional<String> status) {
    var req = new UpdatePetWithForm.Request(petId, name, status);
    var rawReq = UpdatePetWithForm.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> UpdatePetWithForm.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<DeletePet.Response<?>> deletePet(Long petId, java.util.Optional<String> api_key) {
    var req = new DeletePet.Request(petId, api_key);
    var rawReq = DeletePet.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> DeletePet.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<UploadFile.Response<?>> uploadFile(Long petId, java.util.Optional<String> additionalMetadata, String body) {
    var req = new UploadFile.Request(petId, additionalMetadata, body);
    var rawReq = UploadFile.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> UploadFile.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<GetInventory.Response<?>> getInventory() {
    var req = new GetInventory.Request();
    var rawReq = GetInventory.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> GetInventory.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<PlaceOrder.Response<?>> placeOrder(Order body) {
    var req = new PlaceOrder.Request(body);
    var rawReq = PlaceOrder.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> PlaceOrder.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<GetOrderById.Response<?>> getOrderById(Long orderId) {
    var req = new GetOrderById.Request(orderId);
    var rawReq = GetOrderById.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> GetOrderById.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<DeleteOrder.Response<?>> deleteOrder(Long orderId) {
    var req = new DeleteOrder.Request(orderId);
    var rawReq = DeleteOrder.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> DeleteOrder.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<CreateUser.Response<?>> createUser(User body) {
    var req = new CreateUser.Request(body);
    var rawReq = CreateUser.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> CreateUser.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<CreateUsersWithListInput.Response<?>> createUsersWithListInput(java.util.List<User> body) {
    var req = new CreateUsersWithListInput.Request(body);
    var rawReq = CreateUsersWithListInput.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> CreateUsersWithListInput.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<LoginUser.Response<?>> loginUser(java.util.Optional<String> username, java.util.Optional<String> password) {
    var req = new LoginUser.Request(username, password);
    var rawReq = LoginUser.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> LoginUser.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<LogoutUser.Response<?>> logoutUser() {
    var req = new LogoutUser.Request();
    var rawReq = LogoutUser.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> LogoutUser.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<GetUserByName.Response<?>> getUserByName(String username) {
    var req = new GetUserByName.Request(username);
    var rawReq = GetUserByName.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> GetUserByName.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<UpdateUser.Response<?>> updateUser(String username, User body) {
    var req = new UpdateUser.Request(username, body);
    var rawReq = UpdateUser.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> UpdateUser.Handler.fromResponse(serialization, rawRes));
  }
  public java.util.concurrent.CompletableFuture<DeleteUser.Response<?>> deleteUser(String username) {
    var req = new DeleteUser.Request(username);
    var rawReq = DeleteUser.Handler.toRequest(serialization, req);
    return handler.apply(rawReq)
     .thenApply(rawRes -> DeleteUser.Handler.fromResponse(serialization, rawRes));
  }
}