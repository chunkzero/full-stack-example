import { createHook, defineScope, v } from "#chunk";
import { apps } from "#chunk/apps";

const status = {
  kind: "query" as const,
  path: "shared/proxy/status",
  arguments: v.object({ host: v.string() }),
  result: v.serverStatus(),
};

export default defineScope({
  hooks: {
    ping: createHook("server.ping", (ctx) => ctx.runQuery(status, { host: ctx.host })),
    route: createHook("player.route", () => apps.lobby.destinations.main),
  },
});
