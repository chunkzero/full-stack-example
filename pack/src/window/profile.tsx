import { Box, Image, Items, Section, Switch, Text } from "plugin:window/ui";
import { flag, items, text } from "plugin:window/bind";
import type { TextHandle } from "plugin:window/bind";
import { Button, Column, Row, Window, art, close, palette, styles } from "./theme/index.ts";

const xp = flag("xp", { shape: [16] });

/** A recessed stat tile over three slot columns, with a caption above a large value. */
function Tile(props: { bind: TextHandle; title: string }) {
  return (
    <Column width={52} height={34} frame={art.recess} padding={{ top: 6 }} gap={4}>
      <Text width={52} align="center" color={palette.muted}>
        {props.title}
      </Text>
      <Text bind={props.bind} style={styles.heading} align="center" />
    </Column>
  );
}

export default (
  <Window name="profile" container="generic_9x1" title="Profile">
    <Box x={7} y={17} width={18} height={18} frame={art.slot} />
    <Column x={30} y={18} width={136} gap={1}>
      <Text bind={text("name")} style={styles.heading} color={palette.cream} overflow="ellipsis" />
      <Text bind={text("ball_name")} color={palette.muted} overflow="ellipsis" />
    </Column>
    <Column x={8} y={38} width={160} height={27} frame={art.recess} padding={{ top: 4, left: 4, right: 4 }} gap={3}>
      <Row padding={{ left: 2, right: 2 }}>
        <Text bind={text("level")} width={70} color={palette.gold} />
        <Text bind={text("xp_text")} align="right" color={palette.muted} />
      </Row>
      <Row justify="center" gap={1}>
        {Array.from({ length: xp.shape[0] }, (_, i) => (
          <Switch on={xp.at(i)}>{{ true: <Image art={art.pipOn} />, false: <Image art={art.pipOff} /> }}</Switch>
        ))}
      </Row>
    </Column>
    <Row x={8} y={67} gap={2}>
      <Tile bind={text("coins")} title="Coins" />
      <Tile bind={text("rounds")} title="Rounds" />
      <Tile bind={text("aces")} title="Aces" />
    </Row>
    <Section of="container">
      <Items bind={items("ball")} />
    </Section>
    <Section of="player" />
    <Section of="hotbar">
      <Button onClick={close} span={9} tooltip="Close">
        Close
      </Button>
    </Section>
  </Window>
);
