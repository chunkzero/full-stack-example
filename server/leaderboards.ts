import { query, v } from "#chunk";
import { findProfile } from "./common.ts";

const board = v.enum("course_record", "holes_in_one", "coins");
const entry = v.object({ rank: v.integer(), name: v.string(), value: v.integer() });
const size = 10;

export const leaderboard = query({
  args: { board, course: v.optional(v.string()) },
  returns: v.array(entry),
  handler: ({ db }, { board, course }) => {
    let rows: { name: string; value: number }[];
    if (board === "course_record") {
      if (course === undefined) throw new Error("course_record requires a course");
      rows = db
        .query("bests")
        .withIndex("by_course", (q) => q.eq("course", course))
        .collect(size)
        .map(({ player, total }) => ({ name: findProfile(db, player)?.name ?? "Player", value: total }));
    } else {
      const index = board === "coins" ? "by_coins" : "by_holes_in_one";
      rows = db
        .query("golfers")
        .withIndex(index)
        .collect(size)
        .map(({ name, coins, holesInOne }) => ({ name, value: board === "coins" ? coins : holesInOne }));
    }
    return rows.map((row, i) => ({ rank: i + 1, ...row }));
  },
});
