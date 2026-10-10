import { Box, Hud, Image, Switch, Text } from "plugin:window/ui";
import { flag, text } from "plugin:window/bind";
import type { TextHandle } from "plugin:window/bind";
import { Row, art, labels, palette, styles } from "./theme/index.ts";

const xp = flag("xp", { shape: [16] });

/**
 * The HUD is drawn as a vanilla sidebar's title, which the client right-aligns 3px from the edge over a translucent
 * backdrop: a 10px title band plus 9px per score line, 2px wider on each side. The lobby's `StatsBoard` adds `lines`
 * blank score lines, which also lift the sidebar towards the top, and the frame bleeds over that backdrop.
 */
const lines = 15;
const width = 132;
const height = 9 * lines + 9;

/** A muted label and a right-aligned value on one line; a long value ends in an ellipsis. */
function Stat(props: { bind: TextHandle; title: string }) {
  return (
    <Row>
      <Text width={52} color={palette.muted}>
        {props.title}
      </Text>
      <Text bind={props.bind} align="right" overflow="ellipsis" />
    </Row>
  );
}

const Rule = () => <Box frame={art.rule} height={1} margin={{ top: 1, bottom: 1 }} />;

export default (
  <Hud
    name="lobby"
    channel="sidebar"
    width={width}
    height={height}
    frame={art.hud}
    bleed={{ top: 1, right: 2, bottom: 0, left: 2 }}
    padding={{ top: 4, left: 5, right: 5 }}
    gap={2}
    text={labels}
  >
    <Row height={10} frame={art.plaque} justify="center" text={styles.ink}>
      Oakmere Links
    </Row>
    <Row justify="center">
      <Text color={palette.muted}>Clubhouse</Text>
    </Row>
    <Rule />
    <Row>
      <Text bind={text("name")} style={styles.heading} color={palette.cream} overflow="ellipsis" />
      <Text bind={text("level")} width={40} align="right" color={palette.gold} />
    </Row>
    <Row justify="center" gap={1}>
      {Array.from({ length: xp.shape[0] }, (_, i) => (
        <Switch on={xp.at(i)}>
          {{ true: <Image art={art.pipSmallOn} />, false: <Image art={art.pipSmallOff} /> }}
        </Switch>
      ))}
    </Row>
    <Text bind={text("xp_text")} align="right" color={palette.muted} />
    <Rule />
    <Stat bind={text("coins")} title="Coins" />
    <Stat bind={text("best")} title="Best round" />
    <Stat bind={text("rounds")} title="Rounds" />
    <Stat bind={text("aces")} title="Aces" />
    <Rule />
    <Row>
      <Text width={34} color={palette.muted}>
        Record
      </Text>
      <Text bind={text("record")} align="right" overflow="ellipsis" />
    </Row>
    <Text bind={text("golfers")} />
    <Rule />
    <Row justify="center">
      <Text color={palette.muted}>Gate or Caddie to tee off</Text>
    </Row>
  </Hud>
);
