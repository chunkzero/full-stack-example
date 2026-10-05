import { Box, Button, Column, Container, Hotbar, Player, Row, Text, Window } from "#plugins/window";
import { colors, heading, text, TitlePlate } from "./theme.tsx";

/** A small-caps label and a right-aligned value on one line. */
function Stat(props: { bind: string; title: string }) {
  return (
    <Row>
      <Text width={48} color={colors.muted}>
        {props.title}
      </Text>
      <Text bind={props.bind} align="right" />
    </Row>
  );
}

export default (
  <Window name="play" container="generic_9x2" frame="shell" text={text}>
    <TitlePlate title="Chunk Golf" />
    <Column x={8} y={17} width={160} height={66} frame="recess" padding={{ top: 6, left: 4, right: 4 }} gap={3}>
      <Text bind="course" {...heading} align="center" />
      <Text bind="course_info" align="center" color={colors.muted} />
      <Box frame="rule" height={1} margin={{ left: 2, right: 2, bottom: 1 }} />
      <Column gap={2} padding={{ left: 2, right: 2 }}>
        <Stat bind="record" title="Record" />
        <Stat bind="best" title="Your best" />
        <Stat bind="players" title="Playing" />
      </Column>
    </Column>
    <Container />
    <Player>
      <Button
        name="tee_off"
        at={[0, 1]}
        span={[9, 2]}
        frame="button_confirm"
        tooltip={{ title: "Tee off", lines: ["Join the next round", "9 holes at your own pace"] }}
      >
        <Text {...heading} color={colors.white}>
          TEE OFF
        </Text>
      </Button>
    </Player>
    <Hotbar>
      <Button
        name="shop"
        span={3}
        frame="button"
        tooltip={{ title: "Ball shop", lines: ["Spend coins on new ball skins"] }}
      >
        Shop
      </Button>
      <Button
        name="profile"
        span={3}
        frame="button"
        tooltip={{ title: "Profile", lines: ["Your level, stats and equipped ball"] }}
      >
        Profile
      </Button>
      <Button name="exit" span={3} frame="button" close tooltip="Close">
        Close
      </Button>
    </Hotbar>
  </Window>
);
