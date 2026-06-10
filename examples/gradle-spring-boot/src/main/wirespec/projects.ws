type ProjectId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)
type TaskId = Integer(0, 99999)
type MemberId = String(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/g)

enum TaskStatus {
    TODO, IN_PROGRESS, DONE
}

type ProjectList {
    projects: Project[]
}

type Project {
    @Seed id: ProjectId,
    ref:String,
    name: String,
    description: String?,
    owner: Member,
    ownerId: MemberId,
    tags: {String}
}

type ProjectInput {
    ref:String,
    name: String,
    description: String?,
    owner: Member?,
    ownerId: MemberId
}

type TaskList {
    tasks: Task[]
}

type Task {
    @Seed id: TaskId,
    projectId: ProjectId,
    title: String,
    description: String?,
    status: TaskStatus,
    assigneeId: MemberId?
}

type TaskInput {
    title: String,
    description: String?,
    status: TaskStatus,
    assigneeId: MemberId?
}

type Member {
    @Seed id: MemberId,
    ref:String,
    @Generator("fullname") name: String,
    @Generator("email") email: String
}

type MemberInput {
    ref:String,
    @Generator("fullname") name: String,
    @Generator("email") email: String
}

type Error {
    code: Integer,
    message: String
}

endpoint GetProjects GET /projects -> {
    200 -> Project[]
}

endpoint GetProject GET /projects/{id: ProjectId} -> {
    200 -> Project
    404 -> Error
}

endpoint CreateProject POST ProjectInput /projects -> {
    201 -> Project
}

endpoint UpdateProject PUT ProjectInput /projects/{id: ProjectId} -> {
    200 -> Project
    404 -> Error
}

endpoint DeleteProject DELETE /projects/{id: ProjectId} -> {
    204 -> Unit
    404 -> Error
}

endpoint GetTasksForProject GET /projects/{projectId: ProjectId}/tasks ?{ status: TaskStatus? } -> {
    200 -> Task[]
    404 -> Error
}

endpoint CreateTaskForProject POST TaskInput /projects/{projectId: ProjectId}/tasks -> {
    201 -> Task
    404 -> Error
}

endpoint GetTask GET /tasks/{id: TaskId} -> {
    200 -> Task
    404 -> Error
}

endpoint UpdateTask PUT TaskInput /tasks/{id: TaskId} -> {
    200 -> Task
    404 -> Error
}

endpoint DeleteTask DELETE /tasks/{id: TaskId} -> {
    204 -> Unit
    404 -> Error
}

endpoint GetMembers GET /members -> {
    200 -> Member[]
}

endpoint GetMember GET /members/{id: MemberId} -> {
    200 -> Member
    404 -> Error
}

endpoint CreateMember POST MemberInput /members -> {
    201 -> Member
}
