import { command, defineApp } from "#chunk";
import { apps } from "#chunk/apps";

export default defineApp({
  id: "golf",
  runtime: { machineProfile: "local", maxPlayers: 8 },
  destinations: {
    oakmere: { implementation: "default", key: "oakmere", emptyTimeoutSeconds: 30 },
  },
  commands: {
    lobby: command("lobby", {
      handler: async (ctx) => {
        await ctx.routing.enter(apps.lobby.destinations.main);
      },
    }),
  },
});
