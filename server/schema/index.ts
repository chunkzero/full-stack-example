import { defineSchema, defineTable, v } from "#chunk/schema";

export default defineSchema({
  profiles: defineTable({
    player: v.player(),
    name: v.string(),
    coins: v.integer(),
    xp: v.integer(),
    rounds: v.integer(),
    holesInOne: v.integer(),
    ball: v.string(),
    // Indexes sort ascending only, so the leaderboards rank by these negated totals.
    rankCoins: v.integer(),
    rankHolesInOne: v.integer(),
  })
    .index("by_player", ["player"])
    .index("by_coins", ["rankCoins"])
    .index("by_holes_in_one", ["rankHolesInOne"]),
  owned: defineTable({ player: v.player(), item: v.string() }).index("by_player", ["player", "item"]),
  results: defineTable({
    round: v.string(),
    player: v.player(),
    course: v.string(),
    total: v.integer(),
    par: v.integer(),
    shots: v.array(v.array(v.object({ yaw: v.number(), power: v.number() }))),
    coins: v.integer(),
    xp: v.integer(),
    personalBest: v.boolean(),
    courseRecord: v.boolean(),
    // Orders equal totals so the earliest result holds the record.
    at: v.integer(),
  })
    .index("by_round", ["round", "player"])
    .index("by_course", ["course", "total", "at"])
    .index("by_player_course", ["player", "course", "total"]),
});
