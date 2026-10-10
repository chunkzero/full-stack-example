import {
  Box,
  Case,
  Image,
  Region,
  Switch,
  Window as BaseWindow,
  containerLayout,
  create,
  variants,
} from "plugin:window/ui";
import type { BoxProps, Child, ItemStyle, TextProps, Tooltip, WindowProps as BaseWindowProps } from "plugin:window/ui";
import { builtin } from "plugin:window/bind";
import type { ClickAction, Condition } from "plugin:window/bind";
import { art, palette } from "./art.ts";

/** Small caps with a drop shadow: the default style of labels and values. */
export const labels: TextProps = { smallCaps: true, shadow: true, color: palette.cream };

export const styles = create({
  /** Vanilla bold, for headings and the primary action. */
  heading: { smallCaps: false, bold: true, color: palette.gold },
  /** Text engraved on brass or written on paper. */
  ink: { color: palette.ink, shadow: false },
  face: { direction: "row", justify: "center", align: "center", gap: 3, text: { align: "center" } },
  button: variants({
    base: { frame: art.button },
    primary: { frame: art.buttonPrimary, text: { color: palette.ink, shadow: false } },
    disabled: { frame: art.buttonDisabled },
  }),
});

export function Row(props: BoxProps) {
  return <Box {...props} style={[{ align: "center" }, props.style]} direction="row" />;
}

export function Column(props: BoxProps) {
  return <Box {...props} direction="column" />;
}

/** A click that closes the window. */
export const close = builtin("window:close");

export interface WindowProps extends Omit<BaseWindowProps, "frame" | "bleed" | "style"> {
  /** A label or bound `<Text>` engraved on the brass plaque at the top of the rail. */
  title: Child;
}

/** A felt table inside a wooden rail, with brass nails in the rail's corners and the title on a brass plaque. */
export function Window(props: WindowProps) {
  const { title, text, children, ...rest } = props;
  const layout = containerLayout(props.container);
  const { hotbar } = layout.sections;
  const bottom = hotbar.bounds.y + hotbar.bounds.height + 5;
  const nails = [1, bottom + 3].flatMap((y) => [-3, 176].map((x) => <Image art={art.nail} x={x} y={y} />));
  return (
    <BaseWindow
      {...rest}
      bleed={{ top: 3, right: 6, bottom: Math.max(0, bottom + 8 - layout.height), left: 6 }}
      text={{ ...labels, ...text }}
    >
      <Box frame={art.rail} x={-6} y={-3} width={188} height={bottom + 11} />
      <Box frame={art.felt} x={2} y={15} width={172} height={bottom - 15} />
      {nails}
      <Row x={28} y={3} width={120} height={12} frame={art.plaque} justify="center" text={styles.ink}>
        {title}
      </Row>
      {children}
    </BaseWindow>
  );
}

export interface ButtonProps extends ItemStyle {
  onClick: ClickAction;
  /** Draws the button disabled, taking no clicks, while the condition is false. */
  enabled?: Condition;
  /** The brass call-to-action look. */
  primary?: boolean;
  tooltip?: string | Tooltip;
  children?: Child;
}

/** A raised face over its slots, inset 1px so it covers the slot interiors, with its content centered in a row. */
export function Button(props: ButtonProps) {
  const { onClick, enabled, primary, tooltip, children, ...item } = props;
  const look = [styles.face, primary ? styles.button.primary : styles.button.base];
  const region = <Region onClick={onClick} tooltip={tooltip} />;
  return (
    <Box {...item} padding={1}>
      {enabled === undefined ? (
        <Box grow style={look}>
          {children}
          {region}
        </Box>
      ) : (
        <Switch bind={enabled} grow>
          <Case value="true" style={look}>
            {children}
            {region}
          </Case>
          <Case value="false" style={[styles.face, styles.button.disabled]}>
            {children}
          </Case>
        </Switch>
      )}
    </Box>
  );
}
