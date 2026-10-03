import { mutation, query, v } from "#chunk";
import { callerPlayer, defaultBall, ensureProfile, findProfile } from "./common.ts";

/** Level 1 starts at 0 xp; advancing from level n takes 100 * n xp. */
function levelOf(xp: number) {
  let level = 1;
  let start = 0;
  while (xp >= start + 100 * level) {
    start += 100 * level;
    level += 1;
  }
  return { level, levelXp: xp - start, nextLevelXp: 100 * level };
}

export const join = mutation({
  args: { name: v.string() },
  returns: v.null(),
  handler: ({ db, caller }, { name }) => {
    const profile = ensureProfile(db, callerPlayer(caller), name);
    if (profile.name !== name) db.patch(profile._id, { name });
    return null;
  },
});

export const profile = query({
  args: {},
  returns: v.object({
    name: v.string(),
    coins: v.integer(),
    xp: v.integer(),
    level: v.integer(),
    levelXp: v.integer(),
    nextLevelXp: v.integer(),
    rounds: v.integer(),
    holesInOne: v.integer(),
    ball: v.string(),
    owned: v.array(v.string()),
  }),
  handler: ({ db, caller }) => {
    const player = callerPlayer(caller);
    const found = findProfile(db, player);
    const owned = db
      .query("owned")
      .withIndex("by_player", (q) => q.eq("player", player))
      .collect(1000)
      .map(({ item }) => item);
    return {
      name: found?.name ?? "Player",
      coins: found?.coins ?? 0,
      xp: found?.xp ?? 0,
      ...levelOf(found?.xp ?? 0),
      rounds: found?.rounds ?? 0,
      holesInOne: found?.holesInOne ?? 0,
      ball: found?.ball ?? defaultBall,
      owned,
    };
  },
});
