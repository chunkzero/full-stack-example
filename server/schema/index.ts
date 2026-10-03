import { defineSchema, defineTable, v } from "#chunk/schema";

export default defineSchema({
  // Legacy table kept untouched so retained older deployments keep working; golf data lives in `golfers`.
  profiles: defineTable({ player: v.player(), coins: v.integer(), visits: v.integer() }).index("by_player", ["player"]),
  golfers: defineTable({
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
    coins: v.integer(),
    xp: v.integer(),
    personalBest: v.boolean(),
    courseRecord: v.boolean(),
  }).index("by_round", ["round", "player"]),
  // One row per (player, course), replaced only when the player improves.
  bests: defineTable({ player: v.player(), course: v.string(), total: v.integer() })
    .index("by_player_course", ["player", "course"])
    .index("by_course", ["course", "total"]),
  // One row per course, replaced only by a strictly lower total, so the earliest holder keeps ties.
  records: defineTable({
    course: v.string(),
    player: v.player(),
    total: v.integer(),
    shots: v.array(v.array(v.object({ yaw: v.number(), power: v.number() }))),
  }).index("by_course", ["course"]),
});
