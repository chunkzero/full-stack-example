import { button, label, panel, slot, slotRects, ui } from "#plugins/window";
import { colors, plate, plateText, text } from "./theme.ts";

const holes = Array.from({ length: 9 }, (_, i) => i + 1);
const players = 8;
const holeX = (hole: number) => 58 + (hole - 1) * 8;
const headerY = 21;
const parY = 33;
const playerY = (row: number) => 47 + row * 11;

/** Hole, total and score-to-par cells for one table row. */
function cells(prefix: string, y: number, color: string = colors.white) {
  return [
    ...holes.map((hole) =>
      slot(`${prefix}_h${hole}`, { ...text, x: holeX(hole), y, width: 8, align: "center", color }),
    ),
    slot(`${prefix}_total`, { ...text, x: 132, y, width: 16, align: "center", color }),
    slot(`${prefix}_score`, { ...text, x: 150, y, width: 14, align: "right", color }),
  ];
}

export default ui({
  name: "scorecard",
  container: "generic_9x4",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 186 }),
    ...plate(),
    slot("course", plateText),
    panel({ frame: "recess", x: 8, y: 17, width: 160, height: 119 }),
    ...Array.from({ length: players / 2 }, (_, i) =>
      panel({ frame: "stripe", x: 10, y: playerY(i * 2 + 1) - 1, width: 156, height: 11 }),
    ),
    label("Player", { ...text, x: 12, y: headerY, width: 44, color: colors.muted }),
    ...holes.map((hole) =>
      label(String(hole), { ...text, x: holeX(hole), y: headerY, width: 8, align: "center", color: colors.muted }),
    ),
    label("Tot", { ...text, x: 132, y: headerY, width: 16, align: "center", color: colors.muted }),
    label("+/-", { ...text, x: 150, y: headerY, width: 14, align: "right", color: colors.muted }),
    panel({ frame: "rule", x: 10, y: 30, width: 156, height: 1 }),
    label("Par", { ...text, x: 12, y: parY, width: 44, color: colors.gold }),
    ...cells("par", parY, colors.gold),
    panel({ frame: "rule", x: 10, y: 43, width: 156, height: 1 }),
    ...Array.from({ length: players }, (_, i) => [
      slot(`p${i}_name`, { ...text, x: 12, y: playerY(i), width: 44 }),
      ...cells(`p${i}`, playerY(i)),
    ]).flat(),
    panel({ frame: "recess", x: 8, y: 139, width: 160, height: 18 }),
    label("Under par", { ...text, x: 8, y: 144, width: 54, align: "center", color: colors.green }),
    label("Even", { ...text, x: 62, y: 144, width: 52, align: "center" }),
    label("Over par", { ...text, x: 114, y: 144, width: 54, align: "center", color: colors.red }),
    button("back", {
      frame: "button",
      transform: { section: "hotbar", x: 0, y: 0, width: 9, height: 1 },
      tooltip: { title: "Back to clubhouse", lines: ["Leave the course"] },
      children: [label("Back to clubhouse", text)],
    }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 4 },
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
