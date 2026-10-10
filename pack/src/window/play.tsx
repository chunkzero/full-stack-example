import { Box, Image, Section, Text } from "plugin:window/ui";
import { action, text } from "plugin:window/bind";
import type { TextHandle } from "plugin:window/bind";
import { Button, Column, Row, Window, art, close, palette, styles } from "./theme/index.ts";

/** A small-caps label and a right-aligned value on one line. */
function Stat(props: { bind: TextHandle; title: string }) {
  return (
    <Row>
      <Text width={48} color={palette.muted}>
        {props.title}
      </Text>
      <Text bind={props.bind} align="right" />
    </Row>
  );
}

export default (
  <Window name="play" container="generic_9x2" title="Chunk Golf">
    <Column x={8} y={18} width={160} height={65} frame={art.recess} padding={{ top: 6, left: 6, right: 6 }} gap={3}>
      <Text bind={text("course")} style={styles.heading} align="center" overflow="ellipsis" />
      <Text bind={text("course_info")} align="center" color={palette.muted} />
      <Box frame={art.rule} height={1} margin={{ bottom: 1 }} />
      <Column gap={2}>
        <Stat bind={text("record")} title="Record" />
        <Stat bind={text("best")} title="Your best" />
        <Stat bind={text("players")} title="Playing" />
      </Column>
    </Column>
    <Section of="container" />
    <Section of="player">
      <Button
        onClick={action("tee_off")}
        at={[0, 1]}
        span={[9, 2]}
        primary
        tooltip={{ title: "Tee off", lines: ["Join the next round", "9 holes at your own pace"] }}
      >
        <Image art={art.ball} />
        <Text style={styles.heading} color={palette.ink}>
          TEE OFF
        </Text>
        <Image art={art.ball} />
      </Button>
    </Section>
    <Section of="hotbar">
      <Button
        onClick={action("shop")}
        span={3}
        tooltip={{ title: "Ball shop", lines: ["Spend coins on new ball skins"] }}
      >
        Shop
      </Button>
      <Button
        onClick={action("profile")}
        span={3}
        tooltip={{ title: "Profile", lines: ["Your level, stats and equipped ball"] }}
      >
        Profile
      </Button>
      <Button onClick={close} span={3} tooltip="Close">
        Close
      </Button>
    </Section>
  </Window>
);
