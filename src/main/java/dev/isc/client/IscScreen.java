package dev.isc.client;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/**
 * Custom ISC-Client GUI (own look instead of the vanilla button textures).
 * Three columns: Flight | NoFall | Fake Lag.
 * A module's settings box appears under its button when the module is on.
 *
 * The widgets are normal vanilla widgets (so clicking, dragging and keyboard work as usual);
 * after they are drawn, this screen paints its own look over them.
 */
public class IscScreen extends Screen {
	// ---- theme (colors taken from the logo) ----
	private static final int ACCENT     = 0xFFAAA0E7;
	private static final int ACCENT_DIM = 0xFF5E577F;
	private static final int PANEL_BG   = 0xFF19161C;
	private static final int CARD_BG    = 0xFF221E2A;
	private static final int TEXT_MUTED = 0xFF8C86A8;
	private static final int WHITE      = 0xFFFFFFFF;
	private static final int GREEN      = 0xFF8DFFB0;
	private static final int RED        = 0xFFFF7A7A;

	private static final Identifier LOGO = Identifier.of(IscClientMod.MOD_ID, "textures/gui/logo.png");
	private static final int LOGO_W = 101;
	private static final int LOGO_H = 56;

	// ---- layout ----
	private static final int COL_W = 140;
	private static final int GAP = 8;
	private static final int PANEL_PAD = 12;
	private static final int PANEL_W = 3 * COL_W + 2 * GAP + 2 * PANEL_PAD;
	private static final int PANEL_H = 220;
	private static final int HEADER_H = 78;     // logo + divider, columns start below this
	private static final int BTN_H = 22;
	private static final int PAD = 10;          // space between a box edge and its widgets
	private static final int WIDGET_W = COL_W - 2 * PAD;

	private static final int FLIGHT_BOX_H = 74;
	private static final int FAKELAG_BOX_H = 98;

	private int panelX, panelY;
	private int flightBoxX, flightBoxY;
	private int fakeLagBoxX, fakeLagBoxY;

	// widgets whose look is painted by this screen
	private final List<Toggle> toggles = new ArrayList<>();
	private final List<ValueSlider> sliders = new ArrayList<>();

	public IscScreen() {
		super(Text.literal("ISC-Client"));
	}

	@Override
	protected void init() {
		toggles.clear();
		sliders.clear();

		panelX = this.width / 2 - PANEL_W / 2;
		panelY = Math.max(4, this.height / 2 - PANEL_H / 2);

		int top = panelY + HEADER_H;
		int col1 = panelX + PANEL_PAD;
		int col2 = col1 + COL_W + GAP;
		int col3 = col2 + COL_W + GAP;

		// ---- Flight ----
		addToggle(col1, top, COL_W, BTN_H, "Flight", FlightModule::isEnabled, () -> {
			FlightModule.setEnabled(!FlightModule.isEnabled());
			rebuild();
		});

		flightBoxX = col1;
		flightBoxY = top + BTN_H + 4;
		if (FlightModule.isEnabled()) {
			addSlider(new ValueSlider(col1 + PAD, flightBoxY + 20, WIDGET_W, 20,
					FlightModule.MIN_SPEED, FlightModule.MAX_SPEED, 0.5,
					FlightModule::getSpeed, FlightModule::setSpeed,
					v -> String.format(Locale.ROOT, "Speed: x%.1f", v)));

			addToggle(col1 + PAD, flightBoxY + 46, WIDGET_W, 20, "Anti-Kick", FlightModule::isAntiKick,
					() -> FlightModule.setAntiKick(!FlightModule.isAntiKick()));
		}

		// ---- NoFall ----
		addToggle(col2, top, COL_W, BTN_H, "NoFall", NoFallModule::isEnabled,
				() -> NoFallModule.setEnabled(!NoFallModule.isEnabled()));

		// ---- Fake Lag ----
		addToggle(col3, top, COL_W, BTN_H, "Fake Lag", FakeLagModule::isEnabled, () -> {
			FakeLagModule.setEnabled(!FakeLagModule.isEnabled());
			rebuild();
		});

		fakeLagBoxX = col3;
		fakeLagBoxY = top + BTN_H + 4;
		if (FakeLagModule.isEnabled()) {
			addSlider(new ValueSlider(col3 + PAD, fakeLagBoxY + 20, WIDGET_W, 20,
					FakeLagModule.MIN_LAG_MS, FakeLagModule.MAX_LAG_MS, 50,
					() -> FakeLagModule.getLagMs(), v -> FakeLagModule.setLagMs((int) Math.round(v)),
					v -> "Lag: " + Math.round(v) + " ms"));

			addSlider(new ValueSlider(col3 + PAD, fakeLagBoxY + 44, WIDGET_W, 20,
					FakeLagModule.MIN_INTERVAL_MS, FakeLagModule.MAX_INTERVAL_MS, 100,
					() -> FakeLagModule.getIntervalMs(), v -> FakeLagModule.setIntervalMs((int) Math.round(v)),
					v -> "Every: " + Math.round(v) + " ms"));

			addToggle(col3 + PAD, fakeLagBoxY + 68, WIDGET_W, 20, "See server place", FakeLagModule::isSeeServerPlace,
					() -> FakeLagModule.setSeeServerPlace(!FakeLagModule.isSeeServerPlace()));
		}
	}

	private void addToggle(int x, int y, int w, int h, String name, BooleanSupplier state, Runnable action) {
		ButtonWidget button = ButtonWidget.builder(Text.literal(name), b -> action.run())
				.dimensions(x, y, w, h).build();
		this.addDrawableChild(button);
		toggles.add(new Toggle(button, name, state));
	}

	private void addSlider(ValueSlider slider) {
		this.addDrawableChild(slider);
		sliders.add(slider);
	}

	private void rebuild() {
		this.clearChildren();
		this.init();
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);

		// main panel
		drawRounded(context, panelX, panelY, PANEL_W, PANEL_H, ACCENT_DIM, PANEL_BG);
		// accent bar on top of the panel
		context.fill(panelX + 3, panelY + 1, panelX + PANEL_W - 3, panelY + 3, ACCENT);

		// logo (centered in the header)
		context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO,
				this.width / 2 - LOGO_W / 2, panelY + 8,
				0f, 0f, LOGO_W, LOGO_H, LOGO_W, LOGO_H);

		// divider under the header
		int divY = panelY + HEADER_H - 8;
		context.fill(panelX + PANEL_PAD, divY, panelX + PANEL_W - PANEL_PAD, divY + 1, ACCENT_DIM);

		if (FlightModule.isEnabled()) {
			drawRounded(context, flightBoxX, flightBoxY, COL_W, FLIGHT_BOX_H, ACCENT_DIM, CARD_BG);
		}
		if (FakeLagModule.isEnabled()) {
			drawRounded(context, fakeLagBoxX, fakeLagBoxY, COL_W, FAKELAG_BOX_H, ACCENT_DIM, CARD_BG);
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// vanilla widgets (they handle input), then our look is painted over them
		super.render(context, mouseX, mouseY, delta);

		for (Toggle toggle : toggles) {
			paintToggle(context, toggle);
		}
		for (ValueSlider slider : sliders) {
			paintSlider(context, slider);
		}

		if (FlightModule.isEnabled()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Flight settings"),
					flightBoxX + COL_W / 2, flightBoxY + 6, TEXT_MUTED);
		}
		if (FakeLagModule.isEnabled()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Fake Lag settings"),
					fakeLagBoxX + COL_W / 2, fakeLagBoxY + 6, TEXT_MUTED);
		}

		String hint = "ESC to close";
		context.drawTextWithShadow(this.textRenderer, hint,
				panelX + PANEL_W - PANEL_PAD - this.textRenderer.getWidth(hint), panelY + PANEL_H - 12, TEXT_MUTED);
	}

	private void paintToggle(DrawContext context, Toggle toggle) {
		ButtonWidget b = toggle.button;
		boolean on = toggle.state.getAsBoolean();
		boolean hover = b.isHovered();
		int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();

		int fill = on ? (hover ? 0xFF3C3466 : 0xFF2F2850) : (hover ? 0xFF2D2838 : 0xFF221E2A);
		int border = (on || b.isFocused()) ? ACCENT : (hover ? 0xFF7A72A3 : 0xFF3A3552);
		drawRounded(context, x, y, w, h, border, fill);

		int ty = y + (h - 8) / 2;
		context.drawTextWithShadow(this.textRenderer, toggle.name, x + 8, ty, WHITE);

		String s = on ? "ON" : "OFF";
		context.drawTextWithShadow(this.textRenderer, s, x + w - 8 - this.textRenderer.getWidth(s), ty, on ? GREEN : RED);
	}

	private void paintSlider(DrawContext context, ValueSlider slider) {
		int x = slider.getX(), y = slider.getY(), w = slider.getWidth(), h = slider.getHeight();
		boolean hover = slider.isHovered();

		int border = (hover || slider.isFocused()) ? ACCENT : 0xFF3A3552;
		drawRounded(context, x, y, w, h, border, 0xFF16131B);

		int fillW = (int) Math.round(slider.ratio() * (w - 4));
		if (fillW > 0) {
			context.fill(x + 2, y + 2, x + 2 + fillW, y + h - 2, hover ? 0xFF7B70D0 : 0xFF6A5FC0);
		}
		int hx = x + 2 + fillW;
		context.fill(Math.max(x + 2, hx - 1), y + 1, Math.min(x + w - 2, hx + 1), y + h - 1, ACCENT);

		context.drawCenteredTextWithShadow(this.textRenderer, slider.getMessage(), x + w / 2, y + (h - 8) / 2, WHITE);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/** Rectangle with a 1px border and cut corners (looks rounded). */
	private static void drawRounded(DrawContext c, int x, int y, int w, int h, int border, int fill) {
		c.fill(x + 1, y, x + w - 1, y + h, border);
		c.fill(x, y + 1, x + w, y + h - 1, border);
		c.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
		c.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
	}

	private record Toggle(ButtonWidget button, String name, BooleanSupplier state) {
	}

	/** Slider for a number between min and max, rounded to a step. */
	private static class ValueSlider extends SliderWidget {
		private final double min;
		private final double max;
		private final double step;
		private final DoubleConsumer setter;
		private final DoubleFunction<String> formatter;
		private final DoubleSupplier getter;

		ValueSlider(int x, int y, int width, int height, double min, double max, double step,
					DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> formatter) {
			super(x, y, width, height, Text.empty(), (getter.getAsDouble() - min) / (max - min));
			this.min = min;
			this.max = max;
			this.step = step;
			this.getter = getter;
			this.setter = setter;
			this.formatter = formatter;
			updateMessage();
		}

		/** Position of the handle, 0..1. */
		double ratio() {
			return this.value;
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(formatter.apply(getter.getAsDouble())));
		}

		@Override
		protected void applyValue() {
			double raw = min + this.value * (max - min);
			setter.accept(Math.round(raw / step) * step);
		}
	}
}
