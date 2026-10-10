import { Box, Case, Section, Switch, Text } from "plugin:window/ui";
import type { Child, TextElementProps } from "plugin:window/ui";
import { action, flag, text } from "plugin:window/bind";
import { Button, Column, Row, Window, art, palette, styles } from "./theme/index.ts";

const players = 8;
const holes = 9;

const par = text("hole_par", { shape: [holes] });
const strokes = text("strokes", { shape: [players, holes] });
const viewer = flag("viewer", { shape: [players] });

interface TableColumn {
  title: string;
  width: number;
  align: TextElementProps["align"];
  /** Space before the column, separating the name, holes, total and score groups. */
  gap: number;
}

const holeColumns: TableColumn[] = Array.from({ length: holes }, (_, i) => ({
  title: String(i + 1),
  width: 8,
  align: "center",
  gap: i === 0 ? 2 : 0,
}));
const total: TableColumn = { title: "Tot", width: 16, align: "center", gap: 2 };
const score: TableColumn = { title: "+/-", width: 14, align: "right", gap: 2 };

/** One table row: a 44px name, the nine holes, then the total and score cells. */
function TableRow(props: {
  name: Child;
  hole: (index: number) => TextElementProps;
  total: TextElementProps;
  score?: TextElementProps;
}) {
  const cell = (column: TableColumn, cell: TextElementProps) => (
    <Text width={column.width} align={column.align} margin={{ left: column.gap }} {...cell} />
  );
  return (
    <Row padding={{ left: 2, right: 2 }}>
      <Box width={44}>{props.name}</Box>
      {holeColumns.map((column, i) => cell(column, props.hole(i)))}
      {cell(total, props.total)}
      {props.score === undefined ? null : cell(score, props.score)}
    </Row>
  );
}

const rule = <Box frame={art.paperRule} height={1} />;

export default (
  <Window
    name="scorecard"
    container="generic_9x4"
    title={<Text bind={text("course")} width={112} align="center" overflow="ellipsis" />}
  >
    <Column
      x={8}
      y={18}
      width={160}
      height={119}
      frame={art.paper}
      padding={{ top: 4, left: 2, right: 2 }}
      gap={2}
      text={styles.ink}
    >
      <TableRow
        name={<Text color={palette.inkMuted}>Player</Text>}
        hole={(i) => ({ children: holeColumns[i]!.title, color: palette.inkMuted })}
        total={{ children: total.title, color: palette.inkMuted }}
        score={{ children: score.title, color: palette.inkMuted }}
      />
      {rule}
      <TableRow name={<Text bold>Par</Text>} hole={(i) => ({ bind: par.at(i) })} total={{ bind: text("par_total") }} />
      {rule}
      <Column>
        {Array.from({ length: players }, (_, i) => (
          <Column height={11} justify="center" frame={i % 2 === 1 ? art.paperStripe : undefined}>
            <Switch bind={viewer.at(i)} x={0} y={0}>
              <Case value="true" frame={art.paperMark} width={156} height={11} />
              <Case value="false" width={156} height={11} />
            </Switch>
            <TableRow
              name={<Text bind={text("player_name", { shape: [players] }).at(i)} overflow="ellipsis" />}
              hole={(hole) => ({ bind: strokes.at(i, hole) })}
              total={{ bind: text("total", { shape: [players] }).at(i) }}
              score={{ bind: text("score", { shape: [players] }).at(i) }}
            />
          </Column>
        ))}
      </Column>
    </Column>
    <Row x={8} y={139} width={160} height={16} frame={art.recess}>
      <Text width={54} align="center" color={palette.green}>
        Under par
      </Text>
      <Text width={52} align="center">
        Even
      </Text>
      <Text width={54} align="center" color={palette.red}>
        Over par
      </Text>
    </Row>
    <Section of="container" />
    <Section of="player" />
    <Section of="hotbar">
      <Button onClick={action("back")} span={9} tooltip={{ title: "Back to clubhouse", lines: ["Leave the course"] }}>
        Back to clubhouse
      </Button>
    </Section>
  </Window>
);
