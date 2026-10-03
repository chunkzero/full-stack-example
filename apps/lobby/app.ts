import { command, defineApp } from "#chunk";
import { apps } from "#chunk/apps";

export default defineApp({
  id: "lobby",
  runtime: { machineProfile: "local", maxPlayers: 16 },
  destinations: {
    main: { implementation: "default", key: "lobby" },
  },
  commands: {
    play: command("play", {
      handler: async (ctx) => {
        await ctx.routing.enter(apps.golf.destinations.oakmere);
      },
    }),
  },
});
