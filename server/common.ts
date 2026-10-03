import { v } from "#chunk";
import type { JsonValue, MutationContext, PlayerId, QueryContext } from "#chunk";

const identity = v.object({ session: v.session(), app: v.string(), player: v.player() });

export const callerPlayer = (caller: JsonValue): PlayerId => identity.parse(caller).player;

export const defaultBall = "classic";

export const findProfile = (db: QueryContext["db"], player: PlayerId) =>
  db
    .query("profiles")
    .withIndex("by_player", (q) => q.eq("player", player))
    .unique();

/** Returns the player's profile, creating an empty one first when they have none. */
export function ensureProfile(db: MutationContext["db"], player: PlayerId, name = "Player") {
  const existing = findProfile(db, player);
  if (existing) return existing;
  const fresh = {
    player,
    name,
    coins: 0,
    xp: 0,
    rounds: 0,
    holesInOne: 0,
    ball: defaultBall,
    rankCoins: 0,
    rankHolesInOne: 0,
  };
  return { ...fresh, _id: db.insert("profiles", fresh) };
}

export const hasItem = (db: QueryContext["db"], player: PlayerId, item: string) =>
  item === defaultBall ||
  db
    .query("owned")
    .withIndex("by_player", (q) => q.eq("player", player).eq("item", item))
    .first() !== null;
