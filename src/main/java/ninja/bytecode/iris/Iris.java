package ninja.bytecode.iris;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import mortar.api.rift.Rift;
import mortar.api.sched.S;
import mortar.bukkit.command.Command;
import mortar.bukkit.plugin.Control;
import mortar.bukkit.plugin.MortarPlugin;
import mortar.util.reflection.V;
import mortar.util.text.C;
import ninja.bytecode.iris.command.CommandIris;
import ninja.bytecode.iris.controller.ExecutionController;
import ninja.bytecode.iris.controller.PackController;
import ninja.bytecode.iris.controller.WandController;
import ninja.bytecode.iris.generator.IrisGenerator;
import ninja.bytecode.iris.util.Direction;
import ninja.bytecode.iris.util.HotswapGenerator;
import ninja.bytecode.shuriken.logging.L;

public class Iris extends MortarPlugin
{
	public static Thread primaryThread;
	public static Settings settings;
	public static IrisMetrics metrics;
	private ExecutionController executionController;

	public static Iris instance;

	@Control
	private PackController packController;

	@Control
	private WandController wandController;

	@Command
	private CommandIris commandIris;

	private Rift r;

	@Override
	public void onEnable()
	{
		instance = this;
		executionController = new ExecutionController();
		executionController.start();
		primaryThread = Thread.currentThread();
		L.consoleConsumer = (s) -> Bukkit.getConsoleSender().sendMessage(s);
		Direction.calculatePermutations();
		settings = new Settings();
		getServer().getPluginManager().registerEvents((Listener) this, this);
		super.onEnable();
	}

	public File getObjectCacheFolder()
	{
		return getDataFolder("cache", "object");
	}

	public static boolean isGen(World world)
	{
		IrisGenerator g = getGen(world);
		return g != null && g instanceof IrisGenerator;
	}

	public static IrisGenerator getGen(World world)
	{
		try
		{
			return (IrisGenerator) ((HotswapGenerator) world.getGenerator()).getGenerator();
		}

		catch(Throwable e)
		{

		}

		return null;
	}

	@Override
	public void start()
	{
		instance = this;
		packController.compile();

		new S(0)
		{
			@Override
			public void run()
			{
				instance = Iris.this;
				for(World i : Bukkit.getWorlds())
				{
					try
					{
						new V(i.getGenerator()).invoke("setGenerator", new IrisGenerator());
						L.i("Hotloading Generator for World " + i.getName());
					}

					catch(Throwable e)
					{

					}
				}
			}
		};

		instance = this;
	}

	@Override
	public void stop()
	{
		if(settings.performance.debugMode && r != null)
		{
			r.colapse();
		}

		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks(this);
		executionController.stop();
		packController.dispose();
	}

	@EventHandler
	public void on(PlayerJoinEvent e)
	{
		if(settings.performance.debugMode && r != null)
		{
			e.getPlayer().teleport(r.getSpawn());
		}
	}

	public void reload()
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(Iris.instance, () ->
		{
			onDisable();
			onEnable();
		});
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id)
	{
		return new HotswapGenerator(new IrisGenerator());
	}

	@Override
	public String getTag(String arg0)
	{
		return makeTag(C.GREEN, C.DARK_GRAY, C.GRAY, C.BOLD + "Iris" + C.RESET);
	}

	public static String makeTag(C brace, C tag, C text, String tagName)
	{
		return brace + "\u3008" + tag + tagName + brace + "\u3009" + " " + text;
	}

	public static PackController pack()
	{
		return instance.packController;
	}

	public static ExecutionController exec()
	{
		if(instance == null)
		{
			instance = (Iris) Bukkit.getPluginManager().getPlugin("Iris");
		}

		if(instance.executionController == null)
		{
			instance.executionController = new ExecutionController();
			instance.executionController.start();
		}

		return instance.executionController;
	}

	public static WandController wand()
	{
		return instance.wandController;
	}
}
