import { defineWindows } from "plugin:window/ui";

import golf from "./golf.tsx";
import lobby from "./lobby.tsx";
import play from "./play.tsx";
import profile from "./profile.tsx";
import scorecard from "./scorecard.tsx";
import shop from "./shop.tsx";

export default defineWindows({ windows: [play, shop, profile, scorecard], huds: [golf, ...lobby] });
