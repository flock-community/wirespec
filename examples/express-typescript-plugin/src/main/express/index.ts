import express, { Express, Request, Response } from "express";
import todoRoutes from "./TodoController";

const app: Express = express();
const port = 8080;

app.use(express.json());
app.use("/", todoRoutes);

app.listen(port, () => {
  console.log(`Server is running at http://localhost:${port}`);
});
