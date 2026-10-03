import { query, v } from "#chunk";

const board = v.enum("course_record", "holes_in_one", "coins");
const entry = v.object({ rank: v.integer(), name: v.string(), value: v.integer() });
const size = 10;
// Scanned results per course-record board, enough to find ten distinct players in practice.
const scan = 200;

export const leaderboard = query({
  args: { board, course: v.optional(v.string()) },
  returns: v.array(entry),
  handler: ({ db }, { board, course }) => {
    let rows: { name: string; value: number }[];
    if (board === "course_record") {
      if (course === undefined) throw new Error("course_record requires a course");
      const seen = new Set<string>();
      rows = db
        .query("results")
        .withIndex("by_course", (q) => q.eq("course", course))
        .collect(scan)
        .filter(({ player }) => !seen.has(player) && seen.add(player))
        .slice(0, size)
        .map(({ player, total }) => ({
          name:
            db
              .query("profiles")
              .withIndex("by_player", (q) => q.eq("player", player))
              .unique()?.name ?? "Player",
          value: total,
        }));
    } else {
      const index = board === "coins" ? "by_coins" : "by_holes_in_one";
      rows = db
        .query("profiles")
        .withIndex(index)
        .collect(size)
        .map(({ name, coins, holesInOne }) => ({ name, value: board === "coins" ? coins : holesInOne }));
    }
    return rows.map((row, i) => ({ rank: i + 1, ...row }));
  },
});
