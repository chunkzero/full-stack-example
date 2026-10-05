import { Box, Column, Hud, Row, Text } from "#plugins/window";
import { colors, heading, text } from "./theme.tsx";

const segments = 12;
const segmentWidth = 9;

function lerp(a: number, b: number, t: number) {
  return Math.round(a + (b - a) * t);
}

/** Green through yellow to red along the power meter. */
function meterColor(index: number) {
  const t = index / (segments - 1);
  const [from, to, local] =
    t < 0.5 ? [[0x6f, 0xdc, 0x8c], [0xff, 0xe1, 0x4d], t * 2] : [[0xff, 0xe1, 0x4d], [0xff, 0x6b, 0x6b], (t - 0.5) * 2];
  return (
    "#" +
    [0, 1, 2]
      .map((c) =>
        lerp(from[c], to[c], local as number)
          .toString(16)
          .padStart(2, "0"),
      )
      .join("")
  );
}

/** Shader HUDs draw every slot in its authored color, so each status color and meter segment is its own slot. */
export default (
  <Hud
    name="golf"
    channel="actionbar"
    width={220}
    height={46}
    anchor={{ x: 0.5, y: 0 }}
    offset={[0, 8]}
    sourceBottom={59}
  >
    <Column height={32} frame="hud" padding={{ top: 5, left: 8, right: 8 }} gap={8} text={text}>
      <Box frame="rule" absolute left={6} right={6} top={16} height={1} />
      <Row gap={4}>
        <Text bind="hole" {...heading} width={150} />
        <Text bind="par" align="right" translate={[0, 1]} />
      </Row>
      <Row>
        <Text width={38} color={colors.muted}>
          Strokes
        </Text>
        <Text bind="stroke" {...heading} width={24} color={colors.white} margin={{ left: 2 }} translate={[0, -1]} />
        <Box width={132} height={8} margin={{ left: 8 }} text={{ align: "right" }}>
          <Text bind="status_info" x={0} y={0} width={132} color={colors.gold} />
          <Text bind="status_good" x={0} y={0} width={132} color={colors.green} />
          <Text bind="status_bad" x={0} y={0} width={132} color={colors.red} />
        </Box>
      </Row>
    </Column>
    <Row justify="center" margin={{ top: 4 }}>
      {Array.from({ length: segments }, (_, i) => (
        <Box width={segmentWidth} height={8}>
          <Text bind={`power_track_${i}`} x={0} y={0} width={segmentWidth} color="#2c4535" />
          <Text bind={`power_${i}`} x={0} y={0} width={segmentWidth} color={meterColor(i)} shadow />
        </Box>
      ))}
    </Row>
  </Hud>
);
