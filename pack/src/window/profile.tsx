import { Box, Button, Column, Container, Hotbar, Item, Player, Row, Text, Window } from "#plugins/window";
import { colors, heading, text, TitlePlate } from "./theme.tsx";

/** A recessed stat tile over three slot columns, with a caption above a large value. */
function Tile(props: { bind: string; title: string }) {
  return (
    <Column width={52} height={34} frame="recess" padding={{ top: 6 }} gap={4}>
      <Text width={52} align="center" color={colors.muted}>
        {props.title}
      </Text>
      <Text bind={props.bind} {...heading} width={52} align="center" />
    </Column>
  );
}

export default (
  <Window name="profile" container="generic_9x1" frame="shell" text={text}>
    <TitlePlate title="Profile" />
    <Box x={7} y={17} width={18} height={18} frame="slot" />
    <Column x={30} y={18} width={136} gap={1}>
      <Text bind="name" {...heading} color={colors.white} />
      <Text bind="ball_name" color={colors.muted} />
    </Column>
    <Column x={8} y={38} width={160} height={27} frame="recess" padding={{ top: 4, left: 4, right: 4 }} gap={2}>
      <Row padding={{ left: 2, right: 2 }}>
        <Text bind="level" width={70} color={colors.gold} />
        <Text bind="xp_text" align="right" color={colors.muted} />
      </Row>
      <Text bind="xp_bar" align="center" color="#404040" shadow={false} smallCaps={false} />
    </Column>
    <Row x={8} y={67} gap={2}>
      <Tile bind="coins" title="Coins" />
      <Tile bind="rounds" title="Rounds" />
      <Tile bind="aces" title="Aces" />
    </Row>
    <Container>
      <Item name="ball" />
    </Container>
    <Player />
    <Hotbar>
      <Button name="exit" span={9} frame="button" close tooltip="Close">
        Close
      </Button>
    </Hotbar>
  </Window>
);
