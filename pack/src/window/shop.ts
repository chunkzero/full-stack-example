import { button, item, label, panel, pattern, repeater, slot, slotRects, ui } from "#plugins/window";

const hitbox = "example:gui/hitbox";

function pageStates(name: string) {
  return {
    enabled: { item_model: hitbox, tooltip: name },
    disabled: { item_model: hitbox, tooltip: name + " unavailable" },
  };
}

export default ui({
  name: "shop",
  container: "generic_9x6",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 222 }),
    panel({ frame: "title_bar", x: 4, y: 3, width: 168, height: 13 }),
    label("BALL SHOP", { x: 8, y: 6, width: 80, color: "#ffd75e", bold: true }),
    slot("balance", { x: 88, y: 6, width: 80, align: "right", color: "#ffffff", bold: true }),
    repeater("entry", {
      frame: "button",
      pattern: pattern.grid({
        section: "container",
        x: 0,
        y: 0,
        columns: 3,
        rows: 3,
        cell_width: 3,
        cell_height: 2,
      }),
      children: [
        item("icon", { cell_slot: 2 }),
        slot("name", { x: 1, y: 19, width: 52, align: "center", color: "#ffffff" }),
        slot("price", { x: 1, y: 27, width: 52, align: "center", color: "#ffd75e" }),
      ],
    }),
    button("prev", {
      frame: "button",
      transform: { section: "player", x: 0, y: 0, width: 2, height: 1 },
      states: pageStates("Previous page"),
      children: [slot("prev_label", { color: "#ffffff", bold: true })],
    }),
    panel({ frame: "recess", x: 44, y: 141, width: 88, height: 16 }),
    slot("page", { x: 48, y: 145, width: 80, align: "center", color: "#ffffff" }),
    button("next", {
      frame: "button",
      transform: { section: "player", x: 7, y: 0, width: 2, height: 1 },
      states: pageStates("Next page"),
      children: [slot("next_label", { color: "#ffffff", bold: true })],
    }),
    panel({ frame: "recess", x: 7, y: 159, width: 162, height: 16 }),
    slot("message", { x: 11, y: 163, width: 154, align: "center", color: "#ffffff" }),
    label("Click a ball to buy or equip it", { x: 8, y: 180, width: 160, align: "center", color: "#a9d9b5" }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 6 },
      claim: "unowned",
    }),
    slotRects("player_fill", {
      transform: { section: "player", x: 0, y: 0, width: 9, height: 3 },
      claim: "unowned",
    }),
    slotRects("hotbar_fill", {
      transform: { section: "hotbar", x: 0, y: 0, width: 9, height: 1 },
      claim: "unowned",
    }),
  ],
});
