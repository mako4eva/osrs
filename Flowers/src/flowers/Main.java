package flowers;

import com.epicbot.api.shared.APIContext;
import com.epicbot.api.shared.GameType;
import com.epicbot.api.shared.entity.Player;
import com.epicbot.api.shared.methods.IBankAPI.WithdrawMode;
import com.epicbot.api.shared.methods.IInventoryAPI;
import com.epicbot.api.shared.model.Spell;
import com.epicbot.api.shared.model.Tile;
import com.epicbot.api.shared.script.LoopScript;
import com.epicbot.api.shared.script.ScriptManifest;
import com.epicbot.api.shared.util.paint.frame.PaintFrame;
import com.epicbot.api.shared.util.time.Time;
import java.awt.*;

//Script Manifest stuff for your script selector
@ScriptManifest(name = "Flowers", gameType = GameType.OS)
public class Main extends LoopScript {
	// instantiate variable to be used later.
	private Player localPlayer;
	private IInventoryAPI inventory;
	private long startTime;
	private State state;
	private String playerState;
	private boolean buyingSeeds = false;
	private boolean sellingFlowers = false;
	private int lastItemCount = 0;
	private int gPMade = 0;
	private APIContext ctx;

	@Override
	public boolean onStart(String... strings) {
		// when the script starts, startTime is set equal to the current millisecond
		// which is used for the runtime later on.
		startTime = System.currentTimeMillis();
		System.out.println("Starting Flowers!");
		return true;
	}

	@Override
	protected void onPause() {
		playerState = "Script is paused";
		ctx.mouse().moveOffScreen();
	}

	// An enum of all the States
	private enum State {
		FINDING_SEEDS, BUYING_SEEDS, PLANTING, BANKING, ENABLING_RUN, SELLING_FLOWERS

	}

	// The actual loop method
	@Override
	protected int loop() {
		ctx = getAPIContext();

		if (!ctx.client().isLoggedIn()) {
			return 1200;
		}
		State state = getPlayerState();
		updateState();
		System.out.println(playerState);
		if (state == State.SELLING_FLOWERS) {
			int playerX = localPlayer.getLocation().getX();
			int playerY = localPlayer.getLocation().getY();
			int playerZ = localPlayer.getLocation().getPlane();
			// player is not in GE and has no seeds
			if ((Math.abs(playerX - 3165) > 10 ||  Math.abs(playerY - 3487) > 10 || playerZ != 0) && !(inventory.contains("Adamant Seeds"))) {
				// walk to GE
				ctx.webWalking().walkTo(new Tile(3165, 3487, 0));
			}

			if (playerX - 3165 + playerY - 3487 < 10 && playerZ == 0) {
				while (getItemsToSell("flowers")) {
					sellAllItemsInInventory();
				}
				sellingFlowers = false;
				buyingSeeds = true;
			}
		}
		if (state == State.BUYING_SEEDS) {

			int playerX = localPlayer.getLocation().getX();
			int playerY = localPlayer.getLocation().getY();
			int playerZ = localPlayer.getLocation().getPlane();

			// player is in GE and has no seeds
			if (Math.abs(playerX - 3165) < 10 && Math.abs(playerY - 3487) < 10 && playerZ == 0 && !(inventory.contains("Adamant Seeds"))) {
				ctx.bank().open();
				waitt();
				ctx.bank().withdrawAll("Coins");
				waitt();
				int seedsToBuy = ctx.inventory().getItem("Coins").getStackSize() / 300;
				ctx.bank().close();
				waitt();
				ctx.grandExchange().open();
				waitt();
				ctx.grandExchange().newBuyOffer("Adamant Seeds");
				waitt();
				ctx.mouse().move(new Point(81, 381));
				waitt();
				ctx.mouse().click();
				waitt();
				ctx.grandExchange().setPrice(300);
				waitt();
				ctx.grandExchange().setQuantity(seedsToBuy);
				waitt();
				ctx.grandExchange().confirmOffer();
				while (!ctx.inventory().contains("Adamant Seeds")) {
					waitt();
					ctx.grandExchange().collectToInventory();
				}
				ctx.grandExchange().close();
				waitt();
				if (ctx.inventory().contains("Coins")) {
					ctx.bank().open();
					waitt();
					ctx.bank().depositInventory();
					waitt();
					ctx.bank().close();
					waitt();
				}

				buyingSeeds = false;


			}
			// player is not in GE and has no seeds
			if ((Math.abs(playerX - 3165) > 10 && Math.abs(playerY - 3487) > 10 || playerZ != 0) && !(inventory.contains("Adamant Seeds"))) {
				// walk to GE
				ctx.webWalking().walkTo(new Tile(3165, 3487, 0));
				waitt();
			}
		}

		if (state == State.ENABLING_RUN) {
			ctx.walking().setRun(true);
			waitt();
		}
		if (state == State.FINDING_SEEDS) {
			ctx.bank().open();
			if (!ctx.inventory().contains("Adamant Seeds") && ctx.bank().contains("Adamant Seeds")
					&& ctx.bank().isOpen()) {
				ctx.bank().depositInventory();
				ctx.bank().withdrawAll("Adamant Seeds");
				waitt();
				waitt();
				ctx.bank().close();
				waitt();
			} else if (!ctx.inventory().contains("Adamant Seeds") && !ctx.bank().contains("Adamant Seeds")
					&& ctx.bank().isOpen()) {
				sellingFlowers = true;


			}
		}
		if (state == State.BANKING) {
			waitt();
			ctx.bank().open();
			waitt();
			ctx.bank().depositInventory();
			waitt();
			ctx.bank().withdrawAll("Adamant Seeds");
			waitt();
			ctx.bank().close();
			waitt();
		}
		if (state == State.PLANTING) {
			int playerX = localPlayer.getLocation().getX();
			int playerY = localPlayer.getLocation().getY();
			if ((Math.abs(playerX - 3165) < 10 && Math.abs(playerY - 3487) < 10)) {
				ctx.magic().cast(Spell.Modern.HOME_TELEPORT);
				waitt();
			}
			int newItemCount = ctx.inventory().getCount();
			if (newItemCount - lastItemCount == 1) {
				int newItemWorth = ctx.grandExchange().getItemDetails(ctx.inventory().getItemAt(lastItemCount).getId())
						.getCurrentPrice();
				gPMade += newItemWorth / 2;
				int seedWorth = ctx.grandExchange().getItemDetails(29458).getCurrentPrice();
				gPMade -= seedWorth * 2;
			}
			lastItemCount = newItemCount;
			Tile seedSpot1 = new Tile(3205, 3220, 2);
			Tile seedSpot2 = new Tile(3206, 3220, 2);
			Tile playerTile = localPlayer.getLocation();
			if ((playerTile.equals(seedSpot2) || playerTile.equals(seedSpot1))) {
				if (!ctx.dialogues().isDialogueOpen() && ctx.objects().query().named("Flowers").results().size() > 0) {
					System.out.println("ERROR: To many flowers, switching world");
					ctx.world().openWorldMenu();
					waitt();
					int newWorld = (int)(302 + 6 * Math.random());
					ctx.world().hop(newWorld);
					waitt();
				} else if (!ctx.dialogues().isDialogueOpen()
						&& ctx.objects().query().named("Flowers").results().isEmpty()) {
					if (Math.random() > .01) {
						ctx.inventory().getItem("Adamant Seeds").click();
						Time.sleep(1200, () -> ctx.dialogues().isDialogueOpen());
					} else {
						if (ctx.inventory().getItemAt(1) != null) {
							ctx.inventory().getItemAt(1).click();
							System.out.println("Anti-ban: Misclick");
						}
					}
				} else if (ctx.dialogues().isDialogueOpen()) {
					if (Math.random() > .01)
						ctx.dialogues().selectOption("Pick the flowers.");
					else {
						ctx.dialogues().selectOption("Leave the flowers.");
					}
				}

			} else {
				ctx.bank().close();
				waitt();
				ctx.camera().setPitch(90);
				ctx.camera().setYaw(0);
				waitt();
				if (!ctx.bank().isOpen())
					seedSpot1.click();
				waitt();
				playerTile = localPlayer.getLocation();
				if (!playerTile.equals(seedSpot1))
					ctx.webWalking().walkTo(seedSpot1);
			}
		}
		return (int) Math.random() * 600 + 600;
	}

	private void sellAllItemsInInventory() {
		ctx.grandExchange().open();
		waitt();
		for (int i = 0; i < 28; i++) {
			boolean sold = false;
			int loops = 0;
			String itemName = ctx.inventory().getItemAt(i).getName();
			while (!sold) {
				if (!ctx.inventory().contains(itemName)) {
					sold = true;
					System.out.println("Out of " + itemName);
				} else {
					loops++;
					System.out.println("Loop #" + loops);
					if (ctx.grandExchange().getSlot(0).inUse()) {
						ctx.grandExchange().getSlot(0).abortOffer();
						waitt();
						ctx.grandExchange().collectToInventory();
						waitt();
					}
					ctx.inventory().getItem(itemName).click();
					waitt();
					ctx.grandExchange().setPrice(
							(int) (ctx.grandExchange().getOfferPrice() * (1 - .05 * (Math.pow(2, loops - 1)))));
					waitt();
					ctx.grandExchange().confirmOffer();
					waitt();
					waitt();
					waitt();
					ctx.grandExchange().collectToInventory();
					waitt();
					if (!ctx.grandExchange().getSlot(0).inUse()) {
						System.out.println("Slot not in use... So it is sold");
						sold = true;
					} else {
						ctx.grandExchange().getSlot(0).abortOffer();
						waitt();
						ctx.grandExchange().collectToInventory();
						waitt();
						if (loops > 4) {
							System.out.println("Could not sell");
							sold = true;
							waitt();
						}
					}
				}

			}

		}
		ctx.grandExchange().close();
		waitt();

	}

	private boolean getItemsToSell(String item) {
		boolean done = false;
		int count = 0;
		ctx.bank().open();
		waitt();
		waitt();
		int bankCount = getBankCount();
		System.out.println(bankCount);
		ctx.bank().depositInventory();
		waitt();
		ctx.bank().selectWithdrawMode(WithdrawMode.NOTE);
		waitt();
		int bankSpot = 0;
		while (!done && count < 27) {
			if (ctx.bank().getItemAt(bankSpot) != null && ctx.bank().getItemAt(bankSpot).getName().contains(item)) {
				ctx.bank().withdrawAll(ctx.bank().getItemAt(bankSpot).getId());
				count++;
				waitt();
			}
			bankSpot++;
			if (bankSpot >= bankCount && count > 0) {
				ctx.bank().close();
				done = true;
				waitt();
				System.out.println("Returning true but no more in bank");
				return true;
			} else if (bankSpot >= bankCount) {
				done = true;
			}

		}
		ctx.bank().close();
		waitt();
		if (count == 0) {
			System.out.println("Returning false, shopping done");
			return false;
		}
		System.out.println("Returning true, shopping not done");
		return true;

	}

	private int getBankCount() {
		int count = 0;
		while (true) {
			if (ctx.bank().getItemAt(count) == null)
				break;
			count++;
		}
		return count;
	}

	private void waitt() {
		Time.sleep((int) (Math.random() * 800 + 400));

	}

	// A getter for the player's state and defining what causes the state to change
	private State getPlayerState() {

		if (ctx.client().isLoggedIn() && ctx.localPlayer().get() != null) {
			localPlayer = ctx.localPlayer().get();
			inventory = ctx.inventory();
			if (ctx.walking().getRunEnergy() > 15 && !ctx.walking().isRunEnabled()) {
				state = State.ENABLING_RUN;
			} else if (inventory.isFull()) {
				state = State.BANKING;
			} else if (inventory.contains("Adamant Seeds")) {
				state = State.PLANTING;
			} else if (!buyingSeeds && !sellingFlowers) {
				state = State.FINDING_SEEDS;
			} else if (buyingSeeds) {
				state = State.BUYING_SEEDS;
			} else
				state = State.SELLING_FLOWERS;
		}
		return state;
	}

	private void updateState() {

		switch (state) {
		case FINDING_SEEDS:
			playerState = "Finding Seeds";
			break;
		case BUYING_SEEDS:
			playerState = "Buying Seeds";
			break;
		case PLANTING:
			playerState = "Planting";
			break;
		case BANKING:
			playerState = "Banking";
			break;
		case ENABLING_RUN:
			playerState = "Enabling Run";
			break;
		case SELLING_FLOWERS:
			playerState = "Selling Flowers";
			break;
		default:
			playerState = "ERROR";
			break;
		}

	}

	// paint shit.
	@Override
	protected void onPaint(Graphics2D g, APIContext ctx) {

		if (ctx.client().isLoggedIn()) {
			PaintFrame frame = new PaintFrame();
			frame.setTitle("Flowers");
			frame.addLine("Runtime: ", Time.getFormattedRuntime(startTime)); // we use startTime here from the very
																				// beginning
			frame.addLine("State: ", playerState); // we get whatever the player's state is equal to and print it onto
			frame.addLine("GP:", gPMade);
			frame.addLine("GP/Hr: ", (int) (gPMade / ((double) Time.getRuntime(startTime) / 1000 / 60 / 60)));
			frame.draw(g, 0, 90, ctx); // drawing the actual frame.

		}
	}
}