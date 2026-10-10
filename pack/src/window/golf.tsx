import { Box, Hud, Image, Switch, Text } from "plugin:window/ui";
import type { Color } from "plugin:window/ui";
import { text, value } from "plugin:window/bind";
import { Column, Row, art, labels, meterSegment, palette, styles } from "./theme/index.ts";

const segments = 12;

const status = text("status");
const tone = value("status_tone", ["info", "good", "bad"]);
const power = value("power", ["idle", "track", "lit"], { shape: [segments] });
const time = text("time");
const clock = value("clock", ["hidden", "low", "critical"]);

/** Green through yellow to red along the power meter. */
function meterColor(index: number): Color {
  const stops = [
    [0x6f, 0xdc, 0x8c],
    [0xff, 0xe1, 0x4d],
    [0xff, 0x6b, 0x6b],
  ];
  const t = (index / (segments - 1)) * 2;
  const from = stops[Math.min(Math.floor(t), 1)]!;
  const to = stops[Math.min(Math.floor(t), 1) + 1]!;
  const channel = (c: number) => Math.round(from[c]! + (to[c]! - from[c]!) * (t - Math.min(Math.floor(t), 1)));
  return `#${[0, 1, 2].map((c) => channel(c).toString(16).padStart(2, "0")).join("")}`;
}

/** Shader HUDs draw text in its authored color, so the status and the clock have one case per tone. */
export default (
  <Hud name="golf" channel="actionbar" width={220} height={46} anchor="top" offset={[0, 8]} sourceBottom={59}>
    <Column height={32} frame={art.hud} padding={{ top: 5, left: 8, right: 8 }} gap={8} text={labels}>
      <Box frame={art.rule} absolute left={6} right={6} top={16} height={1} />
      <Row gap={4}>
        <Text bind={text("hole")} style={styles.heading} width={46} />
        <Text bind={text("hole_name")} color={palette.muted} overflow="ellipsis" />
        <Switch on={clock}>
          {{
            hidden: null,
            low: <Text bind={time} width={22} align="right" color={palette.muted} />,
            critical: <Text bind={time} width={22} align="right" color={palette.red} />,
          }}
        </Switch>
        <Text bind={text("par")} width={34} align="right" />
      </Row>
      <Row>
        <Text width={38} color={palette.muted}>
          Strokes
        </Text>
        <Text bind={text("stroke")} style={styles.heading} width={8} color={palette.cream} margin={{ left: 3 }} />
        <Text bind={text("stroke_cap")} width={16} color={palette.muted} margin={{ left: 2 }} />
        <Switch on={tone} grow margin={{ left: 4 }} text={{ align: "right" }}>
          {{
            info: <Text bind={status} color={palette.gold} overflow="ellipsis" />,
            good: <Text bind={status} color={palette.green} overflow="ellipsis" />,
            bad: <Text bind={status} color={palette.red} overflow="ellipsis" />,
          }}
        </Switch>
      </Row>
    </Column>
    <Row justify="center" gap={1} margin={{ top: 4 }}>
      {Array.from({ length: segments }, (_, i) => (
        <Switch on={power.at(i)}>
          {{ idle: null, track: <Image art={art.meterTrack} />, lit: <Image art={meterSegment(meterColor(i))} /> }}
        </Switch>
      ))}
    </Row>
  </Hud>
);
