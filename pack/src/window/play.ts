import { button, label, panel, slot, slotRects, ui } from "#plugins/window";

const muted = "#a9d9b5";

function stat(name: string, text: string, y: number) {
  return [
    label(text, { x: 12, y, width: 40, color: muted }),
    slot(name, { x: 52, y, width: 112, align: "right", color: "#ffffff" }),
  ];
}

export default ui({
  name: "play",
  container: "generic_9x3",
  children: [
    panel({ frame: "shell", x: 0, y: 0, width: 176, height: 166 }),
    panel({ frame: "title_bar", x: 4, y: 3, width: 168, height: 13 }),
    label("CHUNK GOLF", { x: 8, y: 6, width: 160, align: "center", color: "#ffd75e", bold: true }),
    panel({ frame: "recess", x: 7, y: 19, width: 162, height: 60 }),
    slot("course", { x: 12, y: 23, width: 152, align: "center", color: "#ffd75e", bold: true }),
    slot("course_info", { x: 12, y: 34, width: 152, align: "center", color: "#ffffff" }),
    panel({ frame: "header", x: 12, y: 45, width: 152, height: 1 }),
    ...stat("record", "Record", 51),
    ...stat("best", "Best", 61),
    ...stat("players", "Playing", 70),
    button("tee_off", {
      frame: "button_confirm",
      transform: { section: "player", x: 0, y: 0, width: 9, height: 2 },
      tooltip: { title: "Tee off", lines: ["Join the next round", "9 holes at your own pace"] },
      children: [label("TEE OFF", { color: "#ffffff", bold: true })],
    }),
    button("shop", {
      frame: "button",
      transform: { section: "player", x: 0, y: 2, width: 4, height: 1 },
      tooltip: { title: "Ball shop", lines: ["Spend coins on new ball skins"] },
      children: [label("SHOP", { color: "#ffffff", bold: true })],
    }),
    button("profile", {
      frame: "button_accent",
      transform: { section: "player", x: 5, y: 2, width: 4, height: 1 },
      tooltip: { title: "Profile", lines: ["Your level, stats and equipped ball"] },
      children: [label("PROFILE", { color: "#2b1900", bold: true })],
    }),
    slotRects("container_fill", {
      transform: { section: "container", x: 0, y: 0, width: 9, height: 3 },
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
