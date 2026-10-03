import { defineMigration } from "#chunk";

export default defineMigration("0002_golf", {
  profiles: {
    to: (old) => ({
      name: "Player",
      xp: 0,
      rounds: 0,
      holesInOne: 0,
      ball: "classic",
      rankCoins: -old.coins,
      rankHolesInOne: 0,
    }),
  },
});
