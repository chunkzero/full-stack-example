import { defineConfig } from "#rpp/config";
import window from "#plugins/window";

export default defineConfig({
  pack: {
    name: "full-stack-example",
    description: "Chunkzero full stack example",
    packFormat: 88,
  },
  build: { source: "src", output: "dist" },
  plugins: [
    window({
      hudShaders: true,
      namespace: "example",
      kotlin: {
        package: "dev.chunkzero.example.ui.generated",
        output: "../shared/src/main/kotlin/dev/chunkzero/example/ui/generated",
      },
    }),
  ],
});
