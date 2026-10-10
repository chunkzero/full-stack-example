import { defineVars, derive, mix, shape } from "plugin:window/ui";
import type { Color, ShapeArt, ShapeStyle, Token, Var } from "plugin:window/ui";

/** Surface colors of the clubhouse theme; `createTheme(colors, …)` recolors every art value under it. */
export const colors = defineVars({
  outline: "#0b1a10",
  wood: "#6e4424",
  felt: "#1f5a35",
  rough: "#123620",
  fairway: "#3a8f52",
  brass: "#d9a441",
  paper: "#efe6c8",
  ball: "#f7f7f2",
});

/** Text colors. Kotlin mirrors the ones it picks at runtime in `UiColors`. */
export const palette = {
  cream: "#fff6d6",
  gold: "#ffd75e",
  muted: "#a9d9b5",
  /** Quiet HUD text over the world, where the felt green of `muted` would sink into grass and sky. */
  soft: "#e8e2cf",
  green: "#6fdc8c",
  red: "#ff6b6b",
  ink: "#2b1900",
  inkMuted: "#7a6a4a",
  inkGreen: "#1f7a3a",
  inkRed: "#b3261e",
} as const;

const light = "#fff3b0";
const dark = "#04140a";

type Bevel = Omit<ShapeStyle, "kind" | "fill"> & { kind?: "panel" | "button" | "slot" };

/** A bordered surface lit from the top-left, or pressed in when `sunken`. */
function bevel(fill: Token<Color>, sunken: boolean, options: Bevel = {}): Var<ShapeArt> {
  return derive((get) => {
    const base = get(fill);
    return shape({
      kind: "panel",
      fill: base,
      border_color: get(colors.outline),
      border_width: 1,
      radius: 0,
      inset_depth: 1,
      highlight_color: sunken ? mix(base, dark, 0.5) : mix(base, light, 0.4),
      shadow_color: sunken ? mix(base, light, 0.25) : mix(base, dark, 0.45),
      ...options,
    });
  });
}

const raised = (fill: Token<Color>, options?: Bevel) => bevel(fill, false, options);
const sunken = (fill: Token<Color>, options?: Bevel) => bevel(fill, true, options);

/** A borderless fill, for rules and row stripes. */
const flat = (fill: (get: <T>(token: Token<T>) => T) => Color) =>
  derive((get) => shape({ kind: "panel", fill: fill(get), border_width: 0, inset_depth: 0, radius: 0 }));

/** A small round stud of `size` pixels. */
const stud = (fill: Token<Color>, size: number) =>
  raised(fill, { kind: "button", width: size, height: size, radius: Math.ceil(size / 2), border_width: 0 });

/** A flat 3x3 square. */
const tick = (fill: Token<Color>) =>
  derive((get) =>
    shape({ kind: "panel", fill: get(fill), width: 3, height: 3, border_width: 0, inset_depth: 0, radius: 0 }),
  );

/** A lit segment of the HUD power meter. */
export const meterSegment = (fill: Color) => raised(fill, { kind: "button", width: 8, height: 8 });

export const art = {
  /** The wooden rail around a window. */
  rail: raised(colors.wood),
  /** The felt the window's content sits on. */
  felt: sunken(colors.felt),
  recess: sunken(colors.rough),
  plaque: raised(colors.brass, { kind: "button" }),
  button: raised(colors.fairway, { kind: "button" }),
  buttonPrimary: raised(colors.brass, { kind: "button" }),
  buttonDisabled: derive((get) =>
    shape({
      kind: "button",
      fill: mix(get(colors.fairway), get(colors.rough), 0.7),
      border_color: get(colors.outline),
      border_width: 1,
      radius: 0,
      inset_depth: 0,
    }),
  ),
  cardEquipped: sunken(colors.felt, { kind: "button", border_color: colors.brass }),
  slot: sunken(colors.rough, { kind: "slot" }),
  rule: flat((get) => mix(get(colors.felt), get(colors.brass), 0.35)),
  /** The scorecard sheet, with its ruled lines, zebra rows, and the viewer's highlighted row. */
  paper: raised(colors.paper),
  paperRule: flat((get) => mix(get(colors.paper), get(colors.wood), 0.5)),
  paperStripe: flat((get) => mix(get(colors.paper), get(colors.wood), 0.12)),
  paperMark: flat((get) => mix(get(colors.paper), get(colors.brass), 0.55)),
  hud: derive((get) =>
    shape({
      kind: "panel",
      fill: get(colors.rough),
      border_color: mix(get(colors.brass), get(colors.outline), 0.35),
      border_width: 1,
      radius: 0,
      inset_depth: 0,
    }),
  ),
  /** 3x3. */
  nail: stud(colors.brass, 3),
  /** 5x5. */
  ball: stud(colors.ball, 5),
  /** 8x6 segments of a progress bar. */
  pipOn: raised(colors.brass, { kind: "button", width: 8, height: 6 }),
  pipOff: sunken(colors.rough, { kind: "button", width: 8, height: 6 }),
  /** 3x3 segments of the lobby HUD's XP bar: brass over a dark track. */
  xpOn: tick(colors.brass),
  xpOff: tick(colors.outline),
  /** 8x8, an unlit segment of the HUD power meter. */
  meterTrack: sunken(colors.rough, { kind: "button", width: 8, height: 8 }),
};
