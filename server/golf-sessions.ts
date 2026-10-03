import { sessionMethod, v } from "#chunk";

/** Opens the scorecard for a player in their golf session; false if they aren't playing a round there. */
export const scorecard = sessionMethod({
  app: "golf",
  session: "default",
  name: "scorecard",
  args: { player: v.string() },
  returns: v.boolean(),
});
