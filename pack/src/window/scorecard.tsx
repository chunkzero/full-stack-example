import { Box, Button, Column, Container, Hotbar, Player, Row, Text, Window } from "#plugins/window";
import type { Child, TextElementProps, TextProps } from "#plugins/window";
import { colors, text, TitlePlate } from "./theme.tsx";

const players = 8;

interface TableColumn {
  key: string;
  title: string;
  width: number;
  align: TextProps["align"];
  /** Space before the column, separating the name, holes, total and score groups. */
  gap: number;
}

const columns: TableColumn[] = [
  ...Array.from({ length: 9 }, (_, i) => ({
    key: `h${i + 1}`,
    title: String(i + 1),
    width: 8,
    align: "center" as const,
    gap: i === 0 ? 2 : 0,
  })),
  { key: "total", title: "Tot", width: 16, align: "center", gap: 2 },
  { key: "score", title: "+/-", width: 14, align: "right", gap: 2 },
];

/** One table row: a 44px name, then each column's text with the props `cell` gives it. */
function TableRow(props: { name: Child; cell: (column: TableColumn) => TextElementProps }) {
  return (
    <Row padding={{ left: 2, right: 2 }}>
      <Box width={44}>{props.name}</Box>
      {columns.map((column) => (
        <Text width={column.width} align={column.align} margin={{ left: column.gap }} {...props.cell(column)} />
      ))}
    </Row>
  );
}

export default (
  <Window name="scorecard" container="generic_9x4" frame="shell" text={text}>
    <TitlePlate bind="course" />
    <Column x={8} y={17} width={160} height={119} frame="recess" padding={{ top: 4, left: 2, right: 2 }}>
      <TableRow
        name={<Text color={colors.muted}>Player</Text>}
        cell={(column) => ({ children: column.title, color: colors.muted })}
      />
      <Box frame="rule" height={1} margin={{ top: 1 }} />
      <Box margin={{ top: 2 }}>
        <TableRow
          name={<Text color={colors.gold}>Par</Text>}
          cell={(column) => ({ bind: `par_${column.key}`, color: colors.gold })}
        />
      </Box>
      <Box frame="rule" height={1} margin={{ top: 2 }} />
      <Column margin={{ top: 2 }}>
        {Array.from({ length: players }, (_, i) => (
          <Box height={11} padding={{ top: 1, bottom: 2 }} frame={i % 2 === 1 ? "stripe" : undefined}>
            <TableRow name={<Text bind={`p${i}_name`} />} cell={(column) => ({ bind: `p${i}_${column.key}` })} />
          </Box>
        ))}
      </Column>
    </Column>
    <Row x={8} y={139} width={160} height={18} frame="recess">
      <Text width={54} align="center" color={colors.green}>
        Under par
      </Text>
      <Text width={52} align="center">
        Even
      </Text>
      <Text width={54} align="center" color={colors.red}>
        Over par
      </Text>
    </Row>
    <Container />
    <Player />
    <Hotbar>
      <Button name="back" span={9} frame="button" tooltip={{ title: "Back to clubhouse", lines: ["Leave the course"] }}>
        Back to clubhouse
      </Button>
    </Hotbar>
  </Window>
);
