import { mutation, query, v } from "#chunk";
import { callerPlayer, defaultBall, ensureProfile, hasItem } from "./common.ts";

const balls = [
  { id: defaultBall, name: "Classic", description: "A plain white golf ball.", price: 0, item: "minecraft:snowball" },
  { id: "slime", name: "Slime", description: "Bouncy and green.", price: 50, item: "minecraft:slime_ball" },
  {
    id: "gold",
    name: "Gold Nugget",
    description: "Flashy and a little heavy.",
    price: 100,
    item: "minecraft:gold_nugget",
  },
  {
    id: "amethyst",
    name: "Amethyst",
    description: "A shard that hums when struck.",
    price: 200,
    item: "minecraft:amethyst_shard",
  },
  { id: "magma", name: "Magma", description: "Hot off the tee.", price: 300, item: "minecraft:magma_cream" },
  {
    id: "ember",
    name: "Ember",
    description: "Leaves the green smoldering.",
    price: 450,
    item: "minecraft:fire_charge",
  },
  {
    id: "pearl",
    name: "Ender Pearl",
    description: "Rumored to find its own way.",
    price: 650,
    item: "minecraft:ender_pearl",
  },
  {
    id: "ocean",
    name: "Heart of the Sea",
    description: "The rarest ball on the course.",
    price: 1000,
    item: "minecraft:heart_of_the_sea",
  },
];

const ball = v.object({
  id: v.string(),
  name: v.string(),
  description: v.string(),
  price: v.integer(),
  item: v.string(),
});

export const catalog = query({
  args: {},
  returns: v.array(ball),
  handler: () => balls,
});

export const buy = mutation({
  args: { item: v.string() },
  returns: v.enum("bought", "owned", "insufficient", "unknown"),
  handler: ({ db, caller }, { item }) => {
    const choice = balls.find(({ id }) => id === item);
    if (!choice) return "unknown";
    const player = callerPlayer(caller);
    if (hasItem(db, player, item)) return "owned";
    const profile = ensureProfile(db, player);
    if (profile.coins < choice.price) return "insufficient";
    const coins = profile.coins - choice.price;
    db.patch(profile._id, { coins, rankCoins: -coins });
    db.insert("owned", { player, item });
    return "bought";
  },
});

export const equip = mutation({
  args: { item: v.string() },
  returns: v.boolean(),
  handler: ({ db, caller }, { item }) => {
    const player = callerPlayer(caller);
    if (!balls.some(({ id }) => id === item) || !hasItem(db, player, item)) return false;
    db.patch(ensureProfile(db, player)._id, { ball: item });
    return true;
  },
});
