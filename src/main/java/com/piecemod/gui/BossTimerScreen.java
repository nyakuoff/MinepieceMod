package com.piecemod.gui;

import com.piecemod.config.ConfigManager;
import com.piecemod.config.ModConfig;
import com.piecemod.data.BossTimerManager;
import com.piecemod.data.TimerEntry;
import com.piecemod.fetch.SilentFetcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BossTimerScreen extends Screen {

    // ---- Layout ----
    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 10;
    private static final int PANEL_W = 175;
    private static final int ROW_H   = 14;
    private static final int GAP     = 1;

    // ---- Option arrays ----
    private static final ModConfig.HudPosition[] POSITIONS = ModConfig.HudPosition.values();
    private static final ModConfig.Theme[]        THEMES    = ModConfig.Theme.values();

    // ---- Dropdown / section state ----
    private boolean positionDropdownOpen = false;
    private boolean themeDropdownOpen    = false;
    private boolean bossSectionOpen      = false;
    private boolean customCmdSectionOpen = false;

    private static final int BOSS_MAX_VISIBLE = 6;
    private int bossScrollOffset = 0;
    private TextFieldWidget commandInput;

    // ---- Boss list (snapshot on open) ----
    private final List<String> bossNames = new ArrayList<>();

    public BossTimerScreen() { super(Text.empty()); }

    @Override public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        bossNames.clear();
        bossNames.addAll(BossTimerManager.getInstance().getAll().keySet());
        String prevText = commandInput != null ? commandInput.getText() : "";
        commandInput = new TextFieldWidget(textRenderer, PANEL_X + 5, 0, PANEL_W - 50, ROW_H, Text.empty());
        commandInput.setMaxLength(64);
        commandInput.setText(prevText);
        addDrawableChild(commandInput);
    }

    // ---- Row geometry ----
    // Row 0 = Show HUD, Row 1 = Position, [pos options], Theme, [theme options], Bosses, [boss rows], Refresh

    private int dropdownRows()  { return positionDropdownOpen ? POSITIONS.length : 0; }
    private int themeRowIdx()   { return 2 + dropdownRows(); }
    private int themeDropRows() { return themeDropdownOpen ? THEMES.length : 0; }
    private int bossHeaderRow()    { return themeRowIdx() + 1 + themeDropRows(); }
    private int visibleBossRows()  { return bossSectionOpen ? Math.min(bossNames.size(), BOSS_MAX_VISIBLE) : 0; }
    private int refreshRow()       { return bossHeaderRow() + 1 + visibleBossRows(); }
    private List<String> customCmds() { return ModConfig.getInstance().getCustomCommands(); }
    private int customCmdHdrRow()  { return refreshRow() + 1; }
    private int customCmdVisRows() { return customCmdSectionOpen ? customCmds().size() : 0; }
    private int addCmdRow()        { return customCmdHdrRow() + 1 + customCmdVisRows(); }
    private int totalRows()        { return addCmdRow() + 1; }

    private int rowToY(int i)   { return PANEL_Y + ROW_H + GAP + i * (ROW_H + GAP); }

    private int hitRow(int mx, int my) {
        if (mx < PANEL_X || mx >= PANEL_X + PANEL_W) return -1;
        int rel = my - (PANEL_Y + ROW_H + GAP);
        if (rel < 0) return -1;
        int idx = rel / (ROW_H + GAP);
        if (rel % (ROW_H + GAP) >= ROW_H || idx >= totalRows()) return -1;
        return idx;
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (click.button() != 0) return super.mouseClicked(click, bl);
        int row = hitRow((int) click.x(), (int) click.y());
        if (row < 0) { closeDropdowns(); return super.mouseClicked(click, bl); }

        ModConfig cfg     = ModConfig.getInstance();
        int themeIdx      = themeRowIdx();
        int themeOptStart = themeIdx + 1;
        int bossHdr       = bossHeaderRow();
        int refresh       = refreshRow();

        if (row == 0) {
            cfg.setHudVisible(!cfg.isHudVisible());
            closeDropdowns();
            return true;
        }
        if (row == 1) {
            positionDropdownOpen = !positionDropdownOpen;
            themeDropdownOpen = false;
            return true;
        }
        if (positionDropdownOpen && row >= 2 && row < 2 + POSITIONS.length) {
            cfg.setHudPosition(POSITIONS[row - 2]);
            closeDropdowns();
            return true;
        }
        if (row == themeIdx) {
            themeDropdownOpen = !themeDropdownOpen;
            positionDropdownOpen = false;
            return true;
        }
        if (themeDropdownOpen && row >= themeOptStart && row < themeOptStart + THEMES.length) {
            cfg.setTheme(THEMES[row - themeOptStart]);
            closeDropdowns();
            return true;
        }
        if (row == bossHdr) {
            bossSectionOpen = !bossSectionOpen;
            closeDropdowns();
            return true;
        }
        if (bossSectionOpen && row > bossHdr && row < refresh) {
            int idx = row - bossHdr - 1 + bossScrollOffset;
            if (idx >= 0 && idx < bossNames.size()) {
                String boss = bossNames.get(idx);
                cfg.setBossEnabled(boss, !cfg.isBossEnabled(boss));
                closeDropdowns();
                return true;
            }
        }
        if (row == refresh) {
            SilentFetcher.triggerNow(MinecraftClient.getInstance());
            closeDropdowns();
            return true;
        }
        int customHdr = customCmdHdrRow();
        if (row == customHdr) {
            customCmdSectionOpen = !customCmdSectionOpen;
            closeDropdowns();
            return true;
        }
        if (customCmdSectionOpen && row > customHdr && row < addCmdRow()) {
            int idx = row - customHdr - 1;
            List<String> cmds = customCmds();
            if (idx >= 0 && idx < cmds.size()) {
                ModConfig.getInstance().removeCustomCommand(cmds.get(idx));
                ConfigManager.save(ModConfig.getInstance());
                return true;
            }
        }
        if (row == addCmdRow() && (int) click.x() >= PANEL_X + PANEL_W - 45) {
            addCustomCommand();
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    private void addCustomCommand() {
        if (commandInput == null) return;
        String cmd = commandInput.getText().trim();
        if (cmd.isEmpty()) return;
        if (!cmd.startsWith("/")) cmd = "/" + cmd;
        ModConfig.getInstance().addCustomCommand(cmd);
        ConfigManager.save(ModConfig.getInstance());
        commandInput.setText("");
    }

    private void closeDropdowns() {
        positionDropdownOpen = false;
        themeDropdownOpen    = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (bossSectionOpen && bossNames.size() > BOSS_MAX_VISIBLE) {
            int bossFirst = rowToY(bossHeaderRow() + 1);
            int bossLast  = rowToY(bossHeaderRow() + visibleBossRows() + 1);
            if (mouseX >= PANEL_X && mouseX < PANEL_X + PANEL_W
                    && mouseY >= bossFirst && mouseY < bossLast) {
                int maxScroll = bossNames.size() - BOSS_MAX_VISIBLE;
                bossScrollOffset = Math.max(0,
                        Math.min(bossScrollOffset - (int) Math.signum(verticalAmount), maxScroll));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ---- Rendering ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (commandInput != null) commandInput.setPosition(PANEL_X + 5, rowToY(addCmdRow()));
        ModConfig cfg    = ModConfig.getInstance();
        ModConfig.Theme t = cfg.getTheme();
        int hov          = hitRow(mouseX, mouseY);
        int themeIdx     = themeRowIdx();
        int bossHdr      = bossHeaderRow();
        int refresh      = refreshRow();

        int panelH = ROW_H + GAP + totalRows() * (ROW_H + GAP);
        ctx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + panelH, t.bg);

        // Header
        ctx.fill(PANEL_X, PANEL_Y, PANEL_X + PANEL_W, PANEL_Y + ROW_H, t.header);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Boss Timers"),
                PANEL_X + PANEL_W / 2, PANEL_Y + (ROW_H - 8) / 2, t.headerText);

        // Rows
        for (int i = 0; i < totalRows(); i++) {
            int ry = rowToY(i);
            ctx.fill(PANEL_X, ry, PANEL_X + PANEL_W, ry + ROW_H, rowBg(i, hov, t, themeIdx, bossHdr, refresh));
            drawRow(ctx, i, ry, cfg, t, themeIdx, bossHdr, refresh);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    private int rowBg(int i, int hov, ModConfig.Theme t, int themeIdx, int bossHdr, int refresh) {
        int customHdr = customCmdHdrRow();
        int addCmd    = addCmdRow();
        boolean isSub = (positionDropdownOpen && i >= 2 && i < 2 + POSITIONS.length)
                     || (themeDropdownOpen && i > themeIdx && i < themeIdx + 1 + THEMES.length)
                     || (bossSectionOpen && i > bossHdr && i < refresh)
                     || (customCmdSectionOpen && i > customHdr && i < addCmd);
        if (isSub)      return i == hov ? t.subHover : t.sub;
        if (i == refresh || i == addCmd) return i == hov ? t.actionHover : t.action;
        return i == hov ? t.rowHover : t.row;
    }

    private void drawRow(DrawContext ctx, int i, int ry, ModConfig cfg, ModConfig.Theme t,
                         int themeIdx, int bossHdr, int refresh) {
        int ty = ry + (ROW_H - 8) / 2;
        String on = "ON", off = "OFF";

        if (i == 0) {
            text(ctx, "Show HUD", PANEL_X + 5, ty, t.textPrimary);
            boolean vis = cfg.isHudVisible();
            right(ctx, vis ? on : off, ty, vis ? t.colorOn : t.colorOff);

        } else if (i == 1) {
            text(ctx, "Position", PANEL_X + 5, ty, t.textPrimary);
            right(ctx, cfg.getHudPosition().label + arrow(positionDropdownOpen), ty, t.colorAccent);

        } else if (positionDropdownOpen && i >= 2 && i < 2 + POSITIONS.length) {
            ModConfig.HudPosition opt = POSITIONS[i - 2];
            boolean sel = opt == cfg.getHudPosition();
            text(ctx, bullet(sel) + opt.label, PANEL_X + 15, ty, sel ? t.colorAccent : t.textDim);

        } else if (i == themeIdx) {
            text(ctx, "Theme", PANEL_X + 5, ty, t.textPrimary);
            right(ctx, cfg.getTheme().label + arrow(themeDropdownOpen), ty, t.colorAccent);

        } else if (themeDropdownOpen && i > themeIdx && i < themeIdx + 1 + THEMES.length) {
            ModConfig.Theme opt = THEMES[i - themeIdx - 1];
            boolean sel = opt == cfg.getTheme();
            text(ctx, bullet(sel) + opt.label, PANEL_X + 15, ty, sel ? t.colorAccent : t.textDim);

        } else if (i == bossHdr) {
            long enabled = bossNames.stream().filter(cfg::isBossEnabled).count();
            text(ctx, "Bosses", PANEL_X + 5, ty, t.textPrimary);
            right(ctx, enabled + "/" + bossNames.size() + arrow(bossSectionOpen), ty, t.textDim);

        } else if (bossSectionOpen && i > bossHdr && i < refresh) {
            int idx = i - bossHdr - 1 + bossScrollOffset;
            if (idx < 0 || idx >= bossNames.size()) return;
            String boss = bossNames.get(idx);
            text(ctx, "  " + boss, PANEL_X + 5, ty, t.textDim);
            if (cfg.isBossEnabled(boss)) {
                TimerEntry entry = BossTimerManager.getInstance().getAll().get(boss);
                right(ctx, entry != null ? formatTimer(entry) : "---", ty, t.colorTimer);
            } else {
                right(ctx, off, ty, t.colorOff);
            }

        } else if (i == refresh) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Refresh"),
                    PANEL_X + PANEL_W / 2, ty, t.colorAccent);

        } else if (i == customCmdHdrRow()) {
            List<String> cmds = customCmds();
            text(ctx, "Commands", PANEL_X + 5, ty, t.textPrimary);
            right(ctx, cmds.size() + arrow(customCmdSectionOpen), ty, t.textDim);

        } else if (customCmdSectionOpen && i > customCmdHdrRow() && i < addCmdRow()) {
            int idx = i - customCmdHdrRow() - 1;
            List<String> cmds = customCmds();
            if (idx >= 0 && idx < cmds.size()) {
                text(ctx, "  " + cmds.get(idx), PANEL_X + 5, ty, t.textDim);
                right(ctx, "\u00d7", ty, t.colorOff);
            }

        } else if (i == addCmdRow()) {
            // TextFieldWidget is drawn by super.render(); show the Add button label on the right
            right(ctx, "\u2192 Add", ty, t.colorAccent);
        }
    }

    // ---- Helpers ----

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, s, x, y, color);
    }

    private void right(DrawContext ctx, String s, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, s, PANEL_X + PANEL_W - textRenderer.getWidth(s) - 5, y, color);
    }

    private static String arrow(boolean open) { return open ? " \u25B2" : " \u25BC"; }
    private static String bullet(boolean sel) { return sel ? "\u25CF " : "  "; }

    private static String formatTimer(TimerEntry timer) {
        if (timer.isSpawning()) return "SPAWNING";
        long s = timer.getLiveSecondsRemaining();
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
