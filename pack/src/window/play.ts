import { button, label, panel, slot, slotRects, ui } from "#plugins/window";
import { colors, heading, text, titlePlate } from "./theme.ts";

/** A small-caps label and a right-aligned value on one line. */
function stat(name: string, title: string, y: number) {
  return [
    label(title, { ...text, x: 14, y, width: 48, color: colors.muted }),
    slot(name, { ...text, x: 62, y, width: 100, align: "right" }),
  ];
}

export default ui({
  name: "play",
  container: "generic_9x2",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 150 }),
    ...titlePlate("Chunk Golf"),
    panel({ frame: "recess", x: 8, y: 17, width: 160, height: 66 }),
    slot("course", { ...heading, x: 12, y: 23, width: 152, align: "center" }),
    slot("course_info", { ...text, x: 12, y: 34, width: 152, align: "center", color: colors.muted }),
    panel({ frame: "rule", x: 14, y: 45, width: 148, height: 1 }),
    ...stat("record", "Record", 50),
    ...stat("best", "Your best", 60),
    ...stat("players", "Playing", 70),
    button("tee_off", {
      frame: "button_confirm",
      transform: { section: "player", x: 0, y: 1, width: 9, height: 2 },
      tooltip: { title: "Tee off", lines: ["Join the next round", "9 holes at your own pace"] },
      children: [label("TEE OFF", { ...heading, color: colors.white })],
    }),
    button("shop", {
      frame: "button",
      transform: { section: "hotbar", x: 0, y: 0, width: 3, height: 1 },
      tooltip: { title: "Ball shop", lines: ["Spend coins on new ball skins"] },
      children: [label("Shop", text)],
    }),
    button("profile", {
      frame: "button",
      transform: { section: "hotbar", x: 3, y: 0, width: 3, height: 1 },
      tooltip: { title: "Profile", lines: ["Your level, stats and equipped ball"] },
      children: [label("Profile", text)],
    }),
    button("exit", {
      frame: "button",
      transform: { section: "hotbar", x: 6, y: 0, width: 3, height: 1 },
      default: "close",
      tooltip: "Close",
      children: [label("Close", text)],
    }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 2 },
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
