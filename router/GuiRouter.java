package factorization.wrath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.chunk.Chunk;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.shared.Core;
import factorization.shared.FactorizationGui;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.wrath.ButtonSet.Predicate;

public class GuiRouter extends FactorizationGui implements IClickable {
	TileEntityRouter router;
	final int mode_button_id = 1, upgrade_button_id = 2;
	final int direction_button_id = 10, slot_up = 11, slot_down = 12;
	final int strict_entity = 20, next_entity = 21;
	final int eject_direction = 30;

	GuiButton mode_button, direction_button, upgrade_button;
	GuiButton slot_up_button, slot_down_button;
	GuiButton strict_entity_button, next_entity_button;
	GuiButton eject_direction_button;

	ArrayList<String> inv_names;
	ButtonSet global_buttons = new ButtonSet();
	ButtonSet main_buttons = new ButtonSet();
	ButtonSet item_filter_buttons = new ButtonSet();
	ButtonSet machine_filter_buttons = new ButtonSet();
	ButtonSet speed_buttons = new ButtonSet();
	ButtonSet thorough_buttons = new ButtonSet();
	ButtonSet bandwidth_buttons = new ButtonSet();
	ButtonSet ejector_buttons = new ButtonSet();

	ButtonSet allSets[] = { main_buttons, item_filter_buttons, machine_filter_buttons, speed_buttons, thorough_buttons, bandwidth_buttons, ejector_buttons };
	ButtonSet current_set = allSets[0];

	String[] side_names = { "bottom sides", "top sides", "§asouth§r sides", "§3north§r sides", "§eeast§r sides",
			"§5west§r sides" };
	String[] ejection_side_names = { "eject up", "eject down", "eject §asouth§r", "eject §3north§r", "eject §eeast§r", "eject §5west§r" };
	static final String any_inv = "all machines in network";

	public GuiRouter(ContainerFactorization cont) {
		super(cont);

		this.router = (TileEntityRouter) cont.factory;
		HashMap<String, Integer> names = new HashMap<String, Integer>();
		int max_dist = 32 * 32;
		ArrayList<TileEntity> entities = new ArrayList();
		for (int cdx = -3; cdx <= 3; cdx++) {
			for (int cdz = -3; cdz <= 3; cdz++) {
				Chunk chunk = router.worldObj.getChunkFromBlockCoords(router.xCoord + cdx*16, router.zCoord + cdz*16);
				for (Object te : chunk.chunkTileEntityMap.values()) {
					if (te instanceof IInventory) {
						entities.add((TileEntity) te);
					}
				}
			}
		}
		for (TileEntity ent : entities) {
			double invDistance = ent.getDistanceFrom(router.xCoord, router.yCoord, router.zCoord);
			if (invDistance > max_dist) {
				continue;
			}
			if (!(ent instanceof IInventory)) {
				continue;
			}
			String invName = router.getIInventoryName((IInventory) ent);
			if (invName == null) {
				continue;
			}
			Integer orig = names.get(invName);
			if (orig == null || invDistance < orig) {
				names.put(invName, new Integer((int) invDistance));
			}
		}
		names.remove(router.getIInventoryName(router));
		class NamesComparator implements Comparator<String> {
			HashMap<String, Integer> src;

			NamesComparator(HashMap<String, Integer> s) {
				src = s;
			}

			@Override
			public int compare(String a, String b) {
				return src.get(a) - src.get(b);
			}
		}
		inv_names = new ArrayList<String>(names.keySet());
		Collections.sort(inv_names, new NamesComparator(names));
		inv_names.add(router.getIInventoryName(router));
	}

	@Override
	public void initGui() {
		super.initGui();
		final int bh = 20;
		final int LEFT = guiLeft + 7;
		final int TOP = guiTop + bh;
		final int WIDTH = xSize - 16;
		final int row_top = TOP;
		final int row2_top = row_top + bh + 2;
		global_buttons.clear();
		int mode_width = 51;
		mode_button = global_buttons.add(mode_button_id, LEFT, row_top, mode_width, bh, "Insert");
		upgrade_button = global_buttons.add(upgrade_button_id, global_buttons.currentRight + 24 + 1, -1, bh, bh, "--");

		main_buttons.clear();
		int dbw = 16; //delta button width
		slot_down_button = main_buttons.add(slot_down, LEFT, row2_top, dbw, bh, "-");
		direction_button = main_buttons.add(direction_button_id, -1, -1, WIDTH - dbw * 2, bh, "Slot...");
		slot_up_button = main_buttons.add(slot_up, -1, -1, dbw, bh, "+");

		item_filter_buttons.clear();
		item_filter_buttons.add(global_buttons.currentRight + 8, row_top + 6, "Item Filter");
		item_filter_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeItemFilter;
			}
		});

		machine_filter_buttons.clear();
		int sebl = global_buttons.currentRight + 4;
		int sebw = 60; //xSize - 8 - sebl;
		strict_entity_button = machine_filter_buttons.add(strict_entity, sebl, row_top, sebw, bh, "visit all"); //visit near/visit all
		next_entity_button = machine_filter_buttons.add(next_entity, LEFT, row2_top, xSize - 16, bh, any_inv);
		machine_filter_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeMachineFilter;
			}
		});

		int lh = 9;
		int line1 = row2_top + 2;
		int line2 = line1 + lh;
		int line3 = line2 + lh;

		speed_buttons.clear();
		speed_buttons.add(LEFT, line1, Core.registry.router_speed.getItemDisplayName(null));
		speed_buttons.add(LEFT, line2, "No delay when visiting machines");
		speed_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeSpeed;
			}
		});

		thorough_buttons.clear();
		thorough_buttons.add(LEFT, line1, Core.registry.router_thorough.getItemDisplayName(null));
		thorough_buttons.add(LEFT, line2, "Always finish serving machines");
		thorough_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeThorough;
			}
		});

		bandwidth_buttons.clear();
		bandwidth_buttons.add(LEFT, line1, Core.registry.router_throughput.getItemDisplayName(null));
		bandwidth_buttons.add(LEFT, line2, "Move stacks at a time");
		bandwidth_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeThroughput;
			}
		});

		ejector_buttons.clear();
		eject_direction_button = ejector_buttons.add(eject_direction, LEFT, row2_top, xSize - 16, bh, "to ...");
		ejector_buttons.setTest(new Predicate<TileEntity>() {
			@Override
			public boolean test(TileEntity a) {
				return ((TileEntityRouter) a).upgradeEject;
			}
		});

		if (router.guiLastButtonSet >= 0 && router.guiLastButtonSet < allSets.length) {
			current_set = allSets[router.guiLastButtonSet];
			if (!current_set.canShow(router)) {
				selectNextUpgrade(true);
			}
		}
		updateGui();
	}

	void updateGui() {
		if (router.is_input) {
			mode_button.displayString = "Insert";
		} else {
			mode_button.displayString = "Extract";
		}

		upgrade_button.enabled = false;
		for (int i = 1; i < allSets.length; i++) {
			if (allSets[i].canShow(router)) {
				upgrade_button.enabled = true;
				break;
			}
		}
		upgrade_button.drawButton = upgrade_button.enabled;

		String m = "";
		if (router.target_slot < 0) {
			m = side_names[router.target_side];
			slot_up_button.enabled = false;
			slot_down_button.enabled = false;
		} else {
			m = "slot " + router.target_slot;
			slot_up_button.enabled = true;
			slot_down_button.enabled = true;
		}
		if (router.is_input) {
			m = "into " + m;
		}
		else {
			m = "from " + m;
		}
		direction_button.displayString = m;
		if (router.match == null || router.match.equals("")) {
			next_entity_button.displayString = any_inv;
		} else {
			next_entity_button.displayString = router.match;
		}

		for (int i = 1; i <= 9; i++) {
			Slot s = (Slot) inventorySlots.inventorySlots.get(i);
			if (router.upgradeItemFilter && current_set == item_filter_buttons) {
				if (s.yDisplayPosition < 0) {
					s.yDisplayPosition += 0xFFFFFF;
				}
			}
			else {
				if (s.yDisplayPosition > 0) {
					s.yDisplayPosition -= 0xFFFFFF;
				}
			}
		}

		eject_direction_button.displayString = ejection_side_names[router.eject_direction];

		next_entity_button.displayString = StatCollector.translateToLocal(next_entity_button.displayString);
		strict_entity_button.displayString = router.match_to_visit ? "visit near" : "visit all";
	}

	protected void drawGuiContainerForegroundLayer() {
		fontRenderer.drawString("Item Router", 60, 6, 0x404040);
		fontRenderer.drawString("Inventory", 8, (ySize - 96) + 2, 0x404040);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Core.bindGuiTexture("routergui");
		int l = (width - xSize) / 2;
		int i1 = (height - ySize) / 2;
		drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
		updateGui();
		if (current_set == item_filter_buttons) {
			drawTexturedModalRect(l + 8 - 1, i1 + 44 - 1, 0, 238, 162, 18);
		}
		global_buttons.draw(mc, i, j);
		current_set.draw(mc, i, j);
		String msg = "";
		if (global_buttons.focused_id == upgrade_button_id && current_set != main_buttons) {
			msg = "Press DELETE to remove this upgrade. ";
		}
		fontRenderer.drawString(msg, guiLeft, guiTop + ySize + 2, 0x707070);
	}

	void selectNextUpgrade(boolean forward) {
		for (int i = 0; i < allSets.length; i++) {
			if (allSets[i] == current_set && forward) {
				for (int j = i + 1; j != i; j++) {
					if (j == allSets.length) {
						j = 0;
					}
					if (allSets[j].canShow(router)) {
						current_set = allSets[j];
						router.guiLastButtonSet = j;
						return;
					}
				}
				router.guiLastButtonSet = 0;
				return;
			} else if (allSets[i] == current_set && !forward) {
				for (int j = i - 1; j != i; j--) {
					if (j == -1) {
						j = allSets.length - 1;
					}
					if (allSets[j].canShow(router)) {
						current_set = allSets[j];
						router.guiLastButtonSet = j;
						return;
					}
				}
				router.guiLastButtonSet = 0;
				return;
			}
		}
	}

	@Override
	public void actionPerformedMouse(GuiButton guibutton, boolean rightClick) {
		switch (guibutton.id) {
		case upgrade_button_id:
			selectNextUpgrade(!rightClick);
			break;
		case mode_button_id:
			router.is_input = !router.is_input;
			router.broadcastItem(MessageType.RouterIsInput, null);
			break;
		case direction_button_id:
			if (0 <= router.target_slot) {
				router.target_slot = ~router.target_slot;
				if (rightClick) {
					router.target_side = 5;
				} else {
					router.target_side = 0;
				}
			} else {
				if (rightClick) {
					router.target_side--;
				} else {
					router.target_side++;
				}
				if (router.target_side < 0) {
					router.target_slot = ~router.target_slot;
				} else if (router.target_side >= 6) {
					router.target_slot = ~router.target_slot;
					router.target_side = 0;
				}
			}
			router.broadcastItem(MessageType.RouterTargetSide, null);
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case slot_up:
			if (router.target_slot < 0) {
				router.target_slot = ~router.target_slot;
			} else {
				if (rightClick) {
					router.target_slot += 10;
				} else {
					router.target_slot++;
				}
			}
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case slot_down:
			if (router.target_slot < 0) {
				router.target_slot = ~router.target_slot;
			} else {
				if (rightClick) {
					router.target_slot -= 10;
				} else {
					router.target_slot--;
				}
				if (router.target_slot < 0) {
					router.target_slot = 0;
				}
			}
			router.broadcastItem(MessageType.RouterSlot, null);
			break;
		case strict_entity:
			router.match_to_visit = !router.match_to_visit;
			router.broadcastItem(MessageType.RouterMatchToVisit, null);
			break;
		case next_entity:
			if (inv_names.size() == 0) {
				// empty
				router.match = "";
				router.broadcastItem(MessageType.RouterMatch, null);
				return;
			}
			int i = inv_names.indexOf(router.match);

			if (rightClick) {
				i--;
			} else {
				i++;
			}

			if (i >= inv_names.size() || i == -1) {
				// at an end
				router.match = "";
			} else if (i < -1) {
				// went backwards
				router.match = inv_names.get(inv_names.size() - 1);
			} else {
				router.match = inv_names.get(i);
			}

			router.broadcastItem(MessageType.RouterMatch, null);
			break;
		case eject_direction:
			if (rightClick) {
				router.eject_direction--;
				if (router.eject_direction < 0) {
					router.eject_direction = 5;
				}
			} else {
				router.eject_direction++;
				if (router.eject_direction > 5) {
					router.eject_direction = 0;
				}
			}

			router.broadcastItem(MessageType.RouterEjectDirection, null);
			break;
		default:
			return;
		}
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);
		global_buttons.handleClick(this, mc, x, y, button);
		current_set.handleClick(this, mc, x, y, button);
		updateGui();
	}

	@Override
	public void onGuiClosed() {
		router.broadcastItem(MessageType.RouterMatch, null);
		super.onGuiClosed();
	}

	@Override
	protected void keyTyped(char c, int i) {
		if (i == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
			super.keyTyped(c, i);
			return;
		}
		if (i == org.lwjgl.input.Keyboard.KEY_DELETE && global_buttons.focused_id == upgrade_button_id) {
			int upgrade_id = -1;

			if (current_set == item_filter_buttons) {
				upgrade_id = Core.registry.router_item_filter.upgradeId;
			}
			else if (current_set == machine_filter_buttons) {
				upgrade_id = Core.registry.router_machine_filter.upgradeId;
			}
			else if (current_set == speed_buttons) {
				upgrade_id = Core.registry.router_speed.upgradeId;
			}
			else if (current_set == thorough_buttons) {
				upgrade_id = Core.registry.router_thorough.upgradeId;
			}
			else if (current_set == bandwidth_buttons) {
				upgrade_id = Core.registry.router_throughput.upgradeId;
			}
			else if (current_set == ejector_buttons) {
				upgrade_id = Core.registry.router_eject.upgradeId;
			}
			if (upgrade_id == -1) {
				return;
			}
			router.removeUpgrade(upgrade_id, Core.proxy.getClientPlayer());
			selectNextUpgrade(false);
			router.broadcastMessage(null, MessageType.RouterDowngrade, upgrade_id);
			return;
		}
		if (current_set.focused_id == next_entity) {
			if (c == '\n') {
				router.broadcastItem(MessageType.RouterMatch, null);
			} else if (i == org.lwjgl.input.Keyboard.KEY_BACK) {
				String text = router.match;
				if (text != null && text.length() > 0) {
					router.match = text.substring(0, text.length() - 1);
				}
			} else if (c != 0) {
				if (router.match == null) {
					router.match = "";
				}
				router.match = router.match + c;
				router.match = router.match.replaceAll("\\p{Cntrl}", "");
			}
			return;
		}
		if (current_set.focused_id == direction_button.id) {
			boolean change = false;
			int add_digit = -1;
			if (i == org.lwjgl.input.Keyboard.KEY_BACK) {
				change = true;
			} else if (c != 0) {
				try {
					add_digit = Integer.parseInt(c + "");
					change = true;
				} catch (NumberFormatException e) {

				}
			}
			if (change) {
				if (router.target_slot < 0) {
					router.target_slot = ~router.target_slot;
				}
				if (add_digit == -1) {
					router.target_slot /= 10;
				} else {
					router.target_slot *= 10;
					router.target_slot += add_digit;
				}
				router.broadcastItem(MessageType.RouterSlot, null);
			}
			return;
		}
		super.keyTyped(c, i);
	}
}
