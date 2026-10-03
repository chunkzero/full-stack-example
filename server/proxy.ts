import { query, v } from "#chunk";

export const status = query({
  args: { host: v.string() },
  returns: v.serverStatus(),
  handler: () => ({ motd: "Chunk Golf | Oakmere Links", online: 0, max: 32 }),
});
