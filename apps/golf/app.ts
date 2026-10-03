import { command, defineApp } from "#chunk";
import { apps } from "#chunk/apps";

import { scorecard } from "../../server/golf-sessions.ts";

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
    scorecard: command("scorecard", {
      handler: async (ctx) => {
        if (!(await ctx.session.call(scorecard, { player: ctx.player.uuid })))
          await ctx.player.message("Your scorecard opens once your round starts.");
      },
    }),
  },
});
