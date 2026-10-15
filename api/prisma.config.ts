// Prisma 7: CLI config (connection URL + schema/migration paths).
// dotenv is required here because the Prisma CLI does not read Next.js env files.
import "dotenv/config";
import { defineConfig, env } from "prisma/config";

export default defineConfig({
  schema: "prisma/schema.prisma",
  migrations: {
    path: "prisma/migrations",
  },
  datasource: {
    url: env("DATABASE_URL"),
  },
});
