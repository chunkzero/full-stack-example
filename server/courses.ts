import { query, v } from "#chunk";

/** Par for each hole, by course id. */
export const courses: Record<string, number[]> = {
  oakmere: [2, 3, 3, 4, 3, 3, 3, 3, 4],
};

const names: Record<string, string> = { oakmere: "Oakmere Links" };

export const course = query({
  args: { course: v.string() },
  returns: v.nullable(v.object({ name: v.string(), pars: v.array(v.integer()) })),
  handler: (_ctx, { course }) => (courses[course] ? { name: names[course], pars: courses[course] } : null),
});
