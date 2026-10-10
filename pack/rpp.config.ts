import { defineConfig } from "rpp:config";
import window from "plugin:window";

export default defineConfig({
  pack: {
    name: "full-stack-example",
    description: "Chunkzero full stack example",
    format: 88,
  },
  build: { source: "src", output: "dist" },
  plugins: [
    window({
      hudShaders: true,
      namespace: "example",
      kotlin: {
        target: "multistom",
        package: "com.chunkzero.example.ui.generated",
        output: "../shared/src/main/kotlin/com/chunkzero/example/ui/generated",
      },
    }),
  ],
});
