import { button, item, label, panel, slot, slotRects, ui } from "#plugins/window";
import { colors, heading, text, titlePlate } from "./theme.ts";

/** A recessed stat tile over three slot columns, with a caption above a large value. */
function tile(name: string, title: string, x: number) {
  return [
    panel({ frame: "recess", x, y: 67, width: 52, height: 34 }),
    label(title, { ...text, x, y: 73, width: 52, align: "center", color: colors.muted }),
    slot(name, { ...heading, x, y: 85, width: 52, align: "center" }),
  ];
}

export default ui({
  name: "profile",
  container: "generic_9x1",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 132 }),
    ...titlePlate("Profile"),
    panel({ frame: "slot", x: 7, y: 17, width: 18, height: 18 }),
    item("ball", { transform: { section: "container", x: 0, y: 0, width: 1, height: 1 } }),
    slot("name", { ...heading, x: 30, y: 18, width: 136, color: colors.white }),
    slot("ball_name", { ...text, x: 30, y: 27, width: 136, color: colors.muted }),
    panel({ frame: "recess", x: 8, y: 38, width: 160, height: 27 }),
    slot("level", { ...text, x: 14, y: 42, width: 70, color: colors.gold }),
    slot("xp_text", { ...text, x: 84, y: 42, width: 78, align: "right", color: colors.muted }),
    slot("xp_bar", { x: 12, y: 52, width: 152, align: "center" }),
    ...tile("coins", "Coins", 8),
    ...tile("rounds", "Rounds", 62),
    ...tile("aces", "Aces", 116),
    button("exit", {
      frame: "button",
      transform: { section: "hotbar", x: 0, y: 0, width: 9, height: 1 },
      default: "close",
      tooltip: "Close",
      children: [label("Close", text)],
    }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 1 },
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
