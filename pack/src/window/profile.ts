import { label, panel, slot, slotRects, item, ui } from "#plugins/window";

const muted = "#a9d9b5";

function tile(name: string, title: string, x: number) {
  return [
    panel({ frame: "recess", x, y: 90, width: 50, height: 32 }),
    label(title, { x: x + 2, y: 96, width: 46, align: "center", color: muted }),
    slot(name, { x: x + 2, y: 108, width: 46, align: "center", color: "#ffd75e", bold: true }),
  ];
}

export default ui({
  name: "profile",
  container: "generic_9x3",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 166 }),
    panel({ frame: "title_bar", x: 4, y: 3, width: 168, height: 13 }),
    label("PROFILE", { x: 8, y: 6, width: 160, align: "center", color: "#ffd75e", bold: true }),
    panel({ frame: "recess", x: 7, y: 19, width: 162, height: 56 }),
    panel({ frame: "slot_cell", x: 8, y: 20, width: 18, height: 18 }),
    item("ball", { transform: { section: "container", x: 0, y: 0, width: 1, height: 1 } }),
    slot("name", { x: 32, y: 22, width: 132, color: "#ffffff", bold: true }),
    slot("ball_name", { x: 32, y: 32, width: 132, color: muted }),
    slot("level", { x: 12, y: 44, width: 60, color: "#ffd75e", bold: true }),
    slot("xp_text", { x: 72, y: 44, width: 92, align: "right", color: muted }),
    panel({ frame: "title_bar", x: 12, y: 55, width: 152, height: 14 }),
    slot("xp_bar", { x: 14, y: 58, width: 148, align: "center", color: "#6fdc8c" }),
    ...tile("coins", "COINS", 8),
    ...tile("rounds", "ROUNDS", 63),
    ...tile("aces", "ACES", 118),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 3 },
      claim: "unowned",
    }),
    slotRects("player_fill", {
      transform: { section: "player", x: 0, y: 0, width: 9, height: 3 },
      claim: "unowned",
    }),
    slotRects("hotbar_fill", {
      transform: { section: "hotbar", x: 0, y: 0, width: 9, height: 1 },
      claim: "unowned",
    }),
  ],
});
