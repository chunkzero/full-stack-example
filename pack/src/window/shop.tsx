import { Box, Button, Container, Hotbar, Icon, Item, Player, Repeater, Row, Text, Window } from "#plugins/window";
import type { StateProps } from "#plugins/window";
import { colors, text, TitlePlate } from "./theme.tsx";

const hitbox = "example:gui/hitbox";

function pageStates(name: string): Record<string, StateProps> {
  return {
    enabled: { itemModel: hitbox, tooltip: name, sprite: "pager" },
    disabled: { itemModel: hitbox, tooltip: name + " unavailable", sprite: "pager_disabled" },
  };
}

/**
 * Each card is 3x2 slots: the ball sits in the top-left slot with its price beside it, and the name runs
 * below on one line or wrapped onto two. `card` overlays the equipped and empty card styles.
 */
export default (
  <Window name="shop" container="generic_9x6" frame="shell" text={text}>
    <TitlePlate title="Ball Shop" />
    <Container>
      <Repeater name="entry" cell={[3, 2]} columns={3} rows={3} frame="button">
        <Icon bind="card" size={[52, 34]} x={0} y={0} />
        <Item name="icon" cellSlot={1} />
        <Text bind="price" x={18} y={4} width={31} align="right" />
        <Text bind="name" x={3} y={20} width={46} />
        <Text bind="name_top" x={3} y={17} width={46} />
        <Text bind="name_bottom" x={3} y={24} width={46} />
      </Repeater>
    </Container>
    <Box x={8} y={131} width={160} height={1} frame="rule" />
    <Box x={62} y={139} width={52} height={16} frame="recess">
      <Text bind="page" align="center" />
    </Box>
    <Box x={8} y={157} width={160} height={34} frame="recess" text={{ align: "center" }}>
      <Text bind="message" x={4} y={13} width={152} />
      <Text bind="message_top" x={4} y={8} width={152} />
      <Text bind="message_bottom" x={4} y={18} width={152} />
    </Box>
    <Row x={8} y={197} width={106} height={16} frame="recess" padding={{ left: 6, right: 6 }}>
      <Text width={40} color={colors.muted}>
        Coins
      </Text>
      <Text bind="balance" align="right" color={colors.gold} />
    </Row>
    <Player>
      <Button name="prev" span={3} states={pageStates("Previous page")}>
        <Text bind="prev_label" />
      </Button>
      <Button name="next" at={[6, 0]} span={3} states={pageStates("Next page")}>
        <Text bind="next_label" />
      </Button>
    </Player>
    <Hotbar>
      <Button name="exit" at={[6, 0]} span={3} frame="button" close tooltip="Close the shop">
        Close
      </Button>
    </Hotbar>
  </Window>
);
