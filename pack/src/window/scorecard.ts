import { button, label, panel, slot, slotRects, ui } from "#plugins/window";

const holes = Array.from({ length: 9 }, (_, i) => i + 1);
const holeX = (hole: number) => 44 + (hole - 1) * 10;
const firstLineY = 31;
const lineGap = 10;
const players = 8;

function line(prefix: string, y: number) {
  return [
    ...holes.map((hole) =>
      slot(`${prefix}_h${hole}`, { x: holeX(hole), y, width: 10, align: "center", color: "#ffffff" }),
    ),
    slot(`${prefix}_total`, { x: 135, y, width: 17, align: "center", color: "#ffffff", bold: true }),
    slot(`${prefix}_score`, { x: 153, y, width: 20, align: "right", color: "#ffffff", bold: true }),
  ];
}

export default ui({
  name: "scorecard",
  container: "generic_9x6",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 222 }),
    panel({ frame: "panel", x: 4, y: 3, width: 168, height: 13 }),
    slot("course", { x: 8, y: 6, width: 160, align: "center", color: "#ffd75e", bold: true }),
    panel({ frame: "recess", x: 3, y: 17, width: 170, height: 110 }),
    label("PLAYER", { x: 5, y: 20, width: 38, color: "#a9d9b5" }),
    ...holes.map((hole) =>
      label(String(hole), { x: holeX(hole), y: 20, width: 10, align: "center", color: "#a9d9b5" }),
    ),
    label("TOT", { x: 135, y: 20, width: 17, align: "center", color: "#a9d9b5" }),
    label("+/-", { x: 153, y: 20, width: 20, align: "right", color: "#a9d9b5" }),
    panel({ frame: "recess", x: 5, y: 29, width: 166, height: 1 }),
    label("PAR", { x: 5, y: firstLineY, width: 38, color: "#ffd75e", bold: true }),
    ...line("par", firstLineY),
    panel({ frame: "recess", x: 5, y: firstLineY + 9, width: 166, height: 1 }),
    ...Array.from({ length: players }, (_, i) => {
      const y = firstLineY + lineGap + 2 + i * lineGap;
      return [slot(`p${i}_name`, { x: 5, y, width: 38, color: "#ffffff" }), ...line(`p${i}`, y)];
    }).flat(),
    label("Under par", { x: 8, y: 150, width: 52, align: "center", color: "#6fdc8c" }),
    label("Par", { x: 62, y: 150, width: 52, align: "center", color: "#ffffff" }),
    label("Over par", { x: 116, y: 150, width: 52, align: "center", color: "#ff6b6b" }),
    button("back", {
      frame: "button_accent",
      transform: { section: "hotbar", x: 0, y: 0, width: 9, height: 1 },
      tooltip: { title: "Back to clubhouse", lines: ["Leave the course"] },
      children: [label("BACK TO CLUBHOUSE", { color: "#2b1900", bold: true })],
    }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 6 },
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
