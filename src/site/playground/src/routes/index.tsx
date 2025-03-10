import { createFileRoute, redirect } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  beforeLoad: () =>
    redirect({ to: "/compiler", search: { emitter: "typescript" } }),
});
