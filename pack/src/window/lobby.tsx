import { Hud, Image, Switch, Text } from "plugin:window/ui";
import type { TextProps } from "plugin:window/ui";
import { flag, text, value } from "plugin:window/bind";
import { Row, art, labels, palette, styles } from "./theme/index.ts";

const xp = flag("xp_bar", { shape: [20] });
const best = text("best");
const bestTone = value("best_tone", ["none", "even", "under", "over"]);

const right: TextProps = { ...labels, align: "right" };
/** Quiet lines beside the values. */
const soft: TextProps = { color: palette.soft };
const strong: TextProps = { color: palette.cream };

/**
 * The clubhouse HUDs, one per corner, drawn straight over the world: values cream, quiet lines soft. Bold stays off
 * the small-caps font, whose one-pixel digit counters it fills in. Shader HUDs draw text in its authored color, so the
 * best round picks a tone.
 */
export default [
  <Hud name="lobby_golfer" anchor="top-left" offset={[6, 6]} gap={4} text={labels}>
    <Row gap={4}>
      <Row width={26} height={10} frame={art.plaque} justify="center" text={styles.ink}>
        <Text bind={text("level")} width={24} align="center" />
      </Row>
      <Text bind={text("name")} style={styles.heading} color={palette.cream} width={100} overflow="ellipsis" />
    </Row>
    <Row gap={4}>
      <Row>
        {Array.from({ length: xp.shape[0] }, (_, i) => (
          <Switch on={xp.at(i)}>{{ true: <Image art={art.xpOn} />, false: <Image art={art.xpOff} /> }}</Switch>
        ))}
      </Row>
      <Text bind={text("xp_text")} width={46} style={strong} />
      <Text style={soft}>XP</Text>
    </Row>
  </Hud>,

  <Hud name="lobby_wallet" anchor="top-right" offset={[-6, 6]} gap={3} text={right}>
    <Text bind={text("coins")} style={styles.heading} width={96} margin={{ bottom: 1 }} />
    <Switch on={bestTone}>
      {{
        none: <Text bind={best} width={96} style={soft} />,
        even: <Text bind={best} width={96} style={strong} />,
        under: <Text bind={best} width={96} color={palette.green} />,
        over: <Text bind={best} width={96} color={palette.red} />,
      }}
    </Switch>
    <Text bind={text("tally")} width={96} style={soft} />
  </Hud>,

  <Hud name="lobby_course" anchor="bottom-right" offset={[-6, -6]} gap={3} text={right}>
    <Text width={110} style={styles.heading}>
      Oakmere Links
    </Text>
    <Text bind={text("golfers")} width={110} style={soft} />
  </Hud>,
];
