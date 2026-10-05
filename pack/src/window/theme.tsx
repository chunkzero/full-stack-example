import { Box, Header, presets, Sprite, Text, theme } from "#plugins/window";
import type { TextProps } from "#plugins/window";

export const colors = {
  gold: "#ffd75e",
  white: "#ffffff",
  muted: "#a9d9b5",
  green: "#6fdc8c",
  red: "#ff6b6b",
  ink: "#2b1900",
};

/** Small caps with a drop shadow: the default style for labels and values. */
export const text: TextProps = { smallCaps: true, shadow: true, color: colors.white };
/** Vanilla bold with a drop shadow, for headings and the primary action. */
export const heading: TextProps = { smallCaps: false, bold: true, shadow: true, color: colors.gold };

/** A wood plate across the top of a window, with brass nails at its ends and a static or bound title centered on it. */
export function TitlePlate(props: { title?: string; bind?: string }) {
  return (
    <Header padding={{ top: 3 }} align="start">
      <Box frame="wood" width={160} height={12} align="center">
        <Sprite name="nail" absolute left={3} top={4} />
        <Sprite name="nail" absolute right={3} top={4} />
        <Text bind={props.bind} width={160} align="center" color={colors.gold}>
          {props.title}
        </Text>
      </Box>
    </Header>
  );
}

const industrial = presets.industrial({
  shell_fill: "#1d3d27",
  panel_fill: "#2a6b3c",
  recess_fill: "#0f2618",
  slot_fill: "#16402a",
  button_fill: "#3a8f52",
  selected_fill: "#1f5a35",
  disabled_fill: "#2c4535",
  border_color: "#06120b",
  highlight_color: "#6fdc8c",
  accent_color: "#e8a317",
  confirm_color: "#2fa84f",
  hud_fill: "#0f2618",
  lamp_color: "#6fdc8c",
});

const wood = {
  kind: "panel",
  fill: "#6e4424",
  border_color: "#1f0f02",
  border_width: 1,
  radius: 0,
  inset_depth: 1,
  highlight_color: "#9a663a",
  shadow_color: "#4a2a12",
} as const;

/** State sprites draw at a fixed size, so each button size gets its own copy of the frame style. */
function sized(frame: keyof typeof industrial.frames, width: number, height: number) {
  return { ...industrial.frames[frame], width, height };
}

export default theme({
  frames: {
    ...industrial.frames,
    wood,
    rule: { kind: "panel", fill: "#2e5a3c", border_width: 0, inset_depth: 0 },
    stripe: { kind: "panel", fill: "#14301e", border_width: 0, inset_depth: 0 },
  },
  sprites: {
    ...industrial.sprites,
    nail: {
      ...industrial.sprites.rivet,
      width: 3,
      height: 3,
      fill: "#c9a24a",
      highlight_color: "#ffe9a8",
      shadow_color: "#4a3008",
    },
    card_equipped: sized("button_selected", 52, 34),
    card_empty: sized("recess", 52, 34),
    pager: sized("button", 52, 16),
    pager_disabled: sized("button_disabled", 52, 16),
  },
});
