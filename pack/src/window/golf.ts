import { hud, label, panel, slot } from "#plugins/window";

const segments = 12;
const segmentWidth = 9;

function lerp(a: number, b: number, t: number) {
  return Math.round(a + (b - a) * t);
}

function meterColor(index: number) {
  const t = index / (segments - 1);
  const [from, to, local] =
    t < 0.5 ? [[0x6f, 0xdc, 0x8c], [0xff, 0xe1, 0x4d], t * 2] : [[0xff, 0xe1, 0x4d], [0xff, 0x6b, 0x6b], (t - 0.5) * 2];
  return (
    "#" +
    [0, 1, 2]
      .map((c) =>
        lerp(from[c], to[c], local as number)
          .toString(16)
          .padStart(2, "0"),
      )
      .join("")
  );
}

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
    slot("status_info", { x: 80, y: 19, width: 132, align: "right", color: "#ffd75e" }),
    slot("status_good", { x: 80, y: 19, width: 132, align: "right", color: "#6fdc8c" }),
    slot("status_bad", { x: 80, y: 19, width: 132, align: "right", color: "#ff6b6b" }),
    ...Array.from({ length: segments }, (_, i) =>
      slot(`power_${i}`, {
        x: (220 - segments * segmentWidth) / 2 + i * segmentWidth,
        y: 36,
        width: segmentWidth,
        align: "left",
        color: meterColor(i),
        shadow: true,
      }),
    ),
  ],
});
