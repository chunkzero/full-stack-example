import { hud, label, panel, slot } from "#plugins/window";

export default hud({
  name: "golf",
  channel: "actionbar",
  width: 220,
  height: 46,
  shader: { source_bottom: 59, origin: { x: 0.5, y: 0.0 }, anchor: { x: 0.5, y: 0.0 }, x: 0, y: 8 },
  children: [
    panel({ frame: "hud_panel", x: 0, y: 0, width: 220, height: 32 }),
    slot("hole", { x: 8, y: 5, width: 150, align: "left", color: "#ffd75e", bold: true }),
    slot("par", { x: 162, y: 5, width: 50, align: "right", color: "#ffffff" }),
    label("Strokes", { x: 8, y: 19, width: 44, color: "#a9d9b5" }),
    slot("stroke", { x: 54, y: 19, width: 20, align: "left", color: "#ffffff", bold: true }),
    slot("status", { x: 80, y: 19, width: 132, align: "right", color: "#ffd75e" }),
    slot("power", { x: 0, y: 36, width: 220, align: "center", color: "#ffffff", shadow: true }),
  ],
});
