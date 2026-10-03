import { mutation, query, v } from "#chunk";
import { callerPlayer, ensureProfile, findProfile } from "./common.ts";
import { courses } from "./courses.ts";

const shot = v.object({ yaw: v.number(), power: v.number() });
const best = v.object({ name: v.string(), total: v.integer(), shots: v.array(v.array(shot)) });

const finishCoins = 10;
const finishXp = 20;
const holeXp = 5;

function holeCoins(strokes: number, par: number) {
  if (strokes === 1) return 15;
  if (strokes <= par - 2) return 6;
  return strokes === par - 1 ? 3 : 0;
}

const sum = (values: number[]) => values.reduce((a, b) => a + b, 0);

const golfSession = v.object({ session: v.session(), app: v.literal("golf"), player: v.optional(v.player()) });

/** Saves a finished round for `player`. Only a golf session acting for itself may call it, so it works after the player left. */
export const record = mutation({
  args: {
    player: v.player(),
    round: v.string(),
    course: v.string(),
    strokes: v.array(v.integer()),
    shots: v.array(v.array(shot)),
  },
  returns: v.object({
    coins: v.integer(),
    xp: v.integer(),
    total: v.integer(),
    par: v.integer(),
    personalBest: v.boolean(),
    courseRecord: v.boolean(),
  }),
  handler: ({ db, caller }, { player, round, course, strokes, shots }) => {
    if (golfSession.parse(caller).player !== undefined)
      throw new Error("Rounds are recorded by the session, not a player");
    const earlier = db
      .query("results")
      .withIndex("by_round", (q) => q.eq("round", round).eq("player", player))
      .unique();
    if (earlier) {
      const { coins, xp, total, par, personalBest, courseRecord } = earlier;
      return { coins, xp, total, par, personalBest, courseRecord };
    }

    const pars = courses[course];
    if (!pars) throw new Error(`Unknown course: ${course}`);
    if (strokes.length !== pars.length || shots.length !== pars.length)
      throw new Error(`${course} has ${pars.length} holes`);
    if (!strokes.every((s) => s >= 1 && s <= 20)) throw new Error("Strokes must be between 1 and 20");
    // Penalties are strokes without shots, and a hole that timed out may have none.
    if (!shots.every((hole, i) => hole.length <= strokes[i])) throw new Error("A hole has more shots than strokes");
    if (!shots.every((hole) => hole.every(({ yaw, power }) => Number.isFinite(yaw) && power >= 0 && power <= 1)))
      throw new Error("Shots need a finite yaw and power from 0 to 1");

    const total = sum(strokes);
    const par = sum(pars);
    const aces = strokes.filter((s) => s === 1).length;
    const coins = finishCoins + sum(strokes.map((s, i) => holeCoins(s, pars[i])));
    const xp = finishXp + holeXp * strokes.length;

    const best = db
      .query("bests")
      .withIndex("by_player_course", (q) => q.eq("player", player).eq("course", course))
      .unique();
    const personalBest = !best || total < best.total;
    if (!best) db.insert("bests", { player, course, total });
    else if (personalBest) db.patch(best._id, { total });

    const holder = db
      .query("records")
      .withIndex("by_course", (q) => q.eq("course", course))
      .unique();
    const courseRecord = !holder || total < holder.total;
    if (!holder) db.insert("records", { course, player, total, shots });
    else if (courseRecord) db.patch(holder._id, { player, total, shots });

    const profile = ensureProfile(db, player);
    const holesInOne = profile.holesInOne + aces;
    const totalCoins = profile.coins + coins;
    db.patch(profile._id, {
      coins: totalCoins,
      rankCoins: -totalCoins,
      xp: profile.xp + xp,
      rounds: profile.rounds + 1,
      holesInOne,
      rankHolesInOne: -holesInOne,
    });
    db.insert("results", { round, player, course, total, par, coins, xp, personalBest, courseRecord });
    return { coins, xp, total, par, personalBest, courseRecord };
  },
});

export const courseRecord = query({
  args: { course: v.string() },
  returns: v.nullable(best),
  handler: ({ db }, { course }) => {
    const holder = db
      .query("records")
      .withIndex("by_course", (q) => q.eq("course", course))
      .unique();
    if (!holder) return null;
    return { name: findProfile(db, holder.player)?.name ?? "Player", total: holder.total, shots: holder.shots };
  },
});

export const personalBest = query({
  args: { course: v.string() },
  returns: v.nullable(v.integer()),
  handler: ({ db, caller }, { course }) =>
    db
      .query("bests")
      .withIndex("by_player_course", (q) => q.eq("player", callerPlayer(caller)).eq("course", course))
      .unique()?.total ?? null,
});
