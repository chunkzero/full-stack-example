import { mutation, query, v } from "#chunk";
import { callerPlayer, ensureProfile } from "./common.ts";

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

export const record = mutation({
  args: {
    round: v.string(),
    course: v.string(),
    strokes: v.array(v.integer()),
    pars: v.array(v.integer()),
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
  handler: ({ db, caller }, { round, course, strokes, pars, shots }) => {
    const player = callerPlayer(caller);
    const earlier = db
      .query("results")
      .withIndex("by_round", (q) => q.eq("round", round).eq("player", player))
      .unique();
    if (earlier) {
      const { coins, xp, total, par, personalBest, courseRecord } = earlier;
      return { coins, xp, total, par, personalBest, courseRecord };
    }

    if (strokes.length < 1 || strokes.length > 18 || pars.length !== strokes.length || shots.length !== strokes.length)
      throw new Error("Round needs 1 to 18 holes with matching strokes, pars and shots");
    if (!strokes.every((s) => s >= 1 && s <= 20)) throw new Error("Strokes must be between 1 and 20");
    if (!pars.every((p) => p >= 1)) throw new Error("Pars must be positive");
    if (!shots.every((hole, i) => hole.length === strokes[i])) throw new Error("Shots must match strokes");
    if (!shots.every((hole) => hole.every(({ yaw, power }) => Number.isFinite(yaw) && power >= 0 && power <= 1)))
      throw new Error("Shots need a finite yaw and power from 0 to 1");

    const total = sum(strokes);
    const par = sum(pars);
    const aces = strokes.filter((s) => s === 1).length;
    const coins = finishCoins + sum(strokes.map((s, i) => holeCoins(s, pars[i])));
    const xp = finishXp + holeXp * strokes.length;

    const personalBest =
      (db
        .query("results")
        .withIndex("by_player_course", (q) => q.eq("player", player).eq("course", course))
        .first()?.total ?? Infinity) > total;
    const courseRecord =
      (db
        .query("results")
        .withIndex("by_course", (q) => q.eq("course", course))
        .first()?.total ?? Infinity) > total;

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
    db.insert("results", {
      round,
      player,
      course,
      total,
      par,
      shots,
      coins,
      xp,
      personalBest,
      courseRecord,
      at: Date.now(),
    });
    return { coins, xp, total, par, personalBest, courseRecord };
  },
});

export const courseRecord = query({
  args: { course: v.string() },
  returns: v.nullable(best),
  handler: ({ db }, { course }) => {
    const result = db
      .query("results")
      .withIndex("by_course", (q) => q.eq("course", course))
      .first();
    if (!result) return null;
    const holder = db
      .query("profiles")
      .withIndex("by_player", (q) => q.eq("player", result.player))
      .unique();
    return { name: holder?.name ?? "Player", total: result.total, shots: result.shots };
  },
});

export const personalBest = query({
  args: { course: v.string() },
  returns: v.nullable(v.integer()),
  handler: ({ db, caller }, { course }) =>
    db
      .query("results")
      .withIndex("by_player_course", (q) => q.eq("player", callerPlayer(caller)).eq("course", course))
      .first()?.total ?? null,
});
