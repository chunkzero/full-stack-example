import { Box, Case, Items, Region, Section, Switch, Text, containerLayout } from "plugin:window/ui";
import { action, flag, items, text, value } from "plugin:window/bind";
import { Button, Row, Window, art, close, palette } from "./theme/index.ts";

const cards = 9;

const pick = action("entry", { shape: [cards] });
const state = value("entry_state", ["available", "equipped", "empty"], { shape: [cards] });
const icon = items("entry_icon", { shape: [cards] });
const hover = items("entry_hover", { shape: [cards] });
const name = text("entry_name", { shape: [cards] });
const price = text("entry_price", { shape: [cards] });
const paged = flag("paged");

/**
 * A card is 3x2 slots: the ball sits in the top-left slot with its price beside it, and the name wraps onto up to two
 * lines below. Its face follows the entry's state. The other five slots hold `entry_hover`, an invisible copy of the
 * ball's stack, so the ball's tooltip shows anywhere on the card.
 */
function Card(props: { index: number }) {
  const i = props.index;
  const face = { width: 52, height: 34 };
  return (
    <Box span={[3, 2]} padding={1}>
      <Box grow>
        <Switch bind={state.at(i)} x={0} y={0}>
          <Case value="available" frame={art.button} {...face} />
          <Case value="equipped" frame={art.cardEquipped} {...face} />
          <Case value="empty" frame={art.recess} {...face} />
        </Switch>
        <Box x={0} y={0} width={16} height={16}>
          <Items bind={icon.at(i)} />
        </Box>
        <Box x={18} y={0} width={34} height={16}>
          <Items bind={hover.at(i)} />
        </Box>
        <Box x={0} y={18} width={52} height={16}>
          <Items bind={hover.at(i)} />
        </Box>
        <Text bind={price.at(i)} x={17} y={4} width={31} align="right" />
        <Text bind={name.at(i)} x={3} y={17} width={46} lines={2} lineHeight={7} />
        <Region onClick={pick.at(i)} />
      </Box>
    </Box>
  );
}

const { player, hotbar } = containerLayout("generic_9x6").sections;

export default (
  <Window name="shop" container="generic_9x6" title="Ball Shop">
    <Section of="container">
      {Array.from({ length: cards }, (_, i) => (
        <Card index={i} />
      ))}
    </Section>
    <Box x={8} y={player.bounds.y - 8} width={160} height={1} frame={art.rule} />
    <Row x={8} y={player.bounds.y + 18} width={160} height={34} frame={art.recess} padding={{ left: 4, right: 4 }}>
      <Text bind={text("message")} align="center" lines={2} />
    </Row>
    <Row x={8} y={hotbar.bounds.y} width={106} height={16} frame={art.recess} padding={{ left: 6, right: 6 }}>
      <Text width={40} color={palette.muted}>
        Coins
      </Text>
      <Text bind={text("balance")} align="right" color={palette.gold} />
    </Row>
    <Section of="player">
      <Switch bind={paged} span={9}>
        <Case value="true" direction="row">
          <Button onClick={action("previous")} enabled={flag("can_previous")} basis={54} tooltip="Previous page">
            {"< Prev"}
          </Button>
          <Box basis={54} padding={1}>
            <Row grow frame={art.recess}>
              <Text bind={text("page")} align="center" />
            </Row>
          </Box>
          <Button onClick={action("next")} enabled={flag("can_next")} basis={54} tooltip="Next page">
            {"Next >"}
          </Button>
        </Case>
        <Case value="false" />
      </Switch>
    </Section>
    <Section of="hotbar">
      <Button onClick={close} at={[6, 0]} span={3} tooltip="Close the shop">
        Close
      </Button>
    </Section>
  </Window>
);
