import { button, item, label, panel, pattern, repeater, slot, slotRects, spriteSlot, ui } from "#plugins/window";
import { colors, text, titlePlate } from "./theme.ts";

const hitbox = "example:gui/hitbox";

function pageStates(name: string) {
  return {
    enabled: { item_model: hitbox, tooltip: name, sprite: "pager" },
    disabled: { item_model: hitbox, tooltip: name + " unavailable", sprite: "pager_disabled" },
  };
}

/**
 * Each card is 3x2 slots: the ball sits in the top-left slot with its price beside it, and the name runs
 * below on one line or wrapped onto two. `card` overlays the equipped and empty card styles.
 */
export default ui({
  name: "shop",
  container: "generic_9x6",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 222 }),
    ...titlePlate("Ball Shop"),
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
        spriteSlot("card", { x: 0, y: 0, width: 52, height: 34 }),
        item("icon", { cell_slot: 1 }),
        slot("price", { ...text, x: 18, y: 4, width: 31, align: "right" }),
        slot("name", { ...text, x: 3, y: 20, width: 46 }),
        slot("name_top", { ...text, x: 3, y: 17, width: 46 }),
        slot("name_bottom", { ...text, x: 3, y: 24, width: 46 }),
      ],
    }),
    panel({ frame: "rule", x: 8, y: 131, width: 160, height: 1 }),
    button("prev", {
      transform: { section: "player", x: 0, y: 0, width: 3, height: 1 },
      states: pageStates("Previous page"),
      children: [slot("prev_label", text)],
    }),
    panel({ frame: "recess", x: 62, y: 139, width: 52, height: 16 }),
    slot("page", { ...text, x: 62, y: 143, width: 52, align: "center" }),
    button("next", {
      transform: { section: "player", x: 6, y: 0, width: 3, height: 1 },
      states: pageStates("Next page"),
      children: [slot("next_label", text)],
    }),
    panel({ frame: "recess", x: 8, y: 157, width: 160, height: 34 }),
    slot("message", { ...text, x: 12, y: 170, width: 152, align: "center" }),
    slot("message_top", { ...text, x: 12, y: 165, width: 152, align: "center" }),
    slot("message_bottom", { ...text, x: 12, y: 175, width: 152, align: "center" }),
    panel({ frame: "recess", x: 8, y: 197, width: 106, height: 16 }),
    label("Coins", { ...text, x: 14, y: 201, width: 40, color: colors.muted }),
    slot("balance", { ...text, x: 54, y: 201, width: 54, align: "right", color: colors.gold }),
    button("exit", {
      frame: "button",
      transform: { section: "hotbar", x: 6, y: 0, width: 3, height: 1 },
      default: "close",
      tooltip: "Close the shop",
      children: [label("Close", text)],
    }),
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
