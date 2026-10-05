import { hud, label, panel, slot } from "#plugins/window";
import { colors, heading, text } from "./theme.ts";

const segments = 12;
const segmentWidth = 9;
const meterX = (220 - segments * segmentWidth) / 2;

function lerp(a: number, b: number, t: number) {
  return Math.round(a + (b - a) * t);
}

/** Green through yellow to red along the power meter. */
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

/** Shader HUDs draw every slot in its authored color, so each status color and meter segment is its own slot. */
export default hud({
  name: "golf",
  channel: "actionbar",
  width: 220,
  height: 46,
  shader: { source_bottom: 59, origin: { x: 0.5, y: 0.0 }, anchor: { x: 0.5, y: 0.0 }, x: 0, y: 8 },
  children: [
    panel({ frame: "hud", x: 0, y: 0, width: 220, height: 32 }),
    panel({ frame: "rule", x: 6, y: 16, width: 208, height: 1 }),
    slot("hole", { ...heading, x: 8, y: 5, width: 150 }),
    slot("par", { ...text, x: 162, y: 6, width: 50, align: "right" }),
    label("Strokes", { ...text, x: 8, y: 21, width: 38, color: colors.muted }),
    slot("stroke", { ...heading, x: 48, y: 20, width: 24, color: colors.white }),
    slot("status_info", { ...text, x: 80, y: 21, width: 132, align: "right", color: colors.gold }),
    slot("status_good", { ...text, x: 80, y: 21, width: 132, align: "right", color: colors.green }),
    slot("status_bad", { ...text, x: 80, y: 21, width: 132, align: "right", color: colors.red }),
    ...Array.from({ length: segments }, (_, i) => [
      slot(`power_track_${i}`, { x: meterX + i * segmentWidth, y: 36, width: segmentWidth, color: "#2c4535" }),
      slot(`power_${i}`, {
        x: meterX + i * segmentWidth,
        y: 36,
        width: segmentWidth,
        color: meterColor(i),
        shadow: true,
      }),
    ]).flat(),
  ],
});
