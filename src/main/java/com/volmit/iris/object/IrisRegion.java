package com.volmit.iris.object;

import java.util.concurrent.locks.ReentrantLock;

import com.volmit.iris.Iris;
import com.volmit.iris.util.CNG;
import com.volmit.iris.util.Desc;
import com.volmit.iris.util.DontObfuscate;
import com.volmit.iris.util.KList;
import com.volmit.iris.util.KMap;
import com.volmit.iris.util.KSet;
import com.volmit.iris.util.RNG;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Desc("Represents an iris region")
@Data
@EqualsAndHashCode(callSuper = false)
public class IrisRegion extends IrisRegistrant
{
	@DontObfuscate
	@Desc("The name of the region")
	private String name = "A Region";

	@DontObfuscate
	@Desc("The shore ration (How much percent of land should be a shore)")
	private double shoreRatio = 0.13;

	@DontObfuscate
	@Desc("The min shore height")
	private double shoreHeightMin = 1.2;

	@DontObfuscate
	@Desc("The the max shore height")
	private double shoreHeightMax = 3.2;

	@DontObfuscate
	@Desc("The varience of the shore height")
	private double shoreHeightZoom = 3.14;

	@DontObfuscate
	@Desc("How large land biomes are in this region")
	private double landBiomeZoom = 1;

	@DontObfuscate
	@Desc("How large shore biomes are in this region")
	private double shoreBiomeZoom = 1;

	@DontObfuscate
	@Desc("How large sea biomes are in this region")
	private double seaBiomeZoom = 1;

	@DontObfuscate
	@Desc("How large island biomes are in this region")
	private double islandBiomeZoom = 1;

	@DontObfuscate
	@Desc("How large cave biomes are in this region")
	private double caveBiomeZoom = 1;

	@DontObfuscate
	@Desc("How large skyland biomes are in this region")
	private double skylandBiomeZoom = 1;

	@DontObfuscate
	@Desc("The biome implosion ratio, how much to implode biomes into children (chance)")
	private double biomeImplosionRatio = 0.4;

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> landBiomes = new KList<>();

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> seaBiomes = new KList<>();

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> shoreBiomes = new KList<>();

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> caveBiomes = new KList<>();

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> islandBiomes = new KList<>();

	@DontObfuscate
	@Desc("A list of root-level biomes in this region. Don't specify child biomes of other biomes here. Just the root parents.")
	private KList<String> skylandBiomes = new KList<>();

	@DontObfuscate
	@Desc("Ridge biomes create a vein-like network like rivers through this region")
	private KList<IrisRegionRidge> ridgeBiomes = new KList<>();

	@DontObfuscate
	@Desc("Spot biomes splotch themselves across this region like lakes")
	private KList<IrisRegionSpot> spotBiomes = new KList<>();

	@Desc("Define regional deposit generators that add onto the global deposit generators")
	private KList<IrisDepositGenerator> deposits = new KList<>();

	private transient KList<String> cacheRidge;
	private transient KList<String> cacheSpot;
	private transient CNG shoreHeightGenerator;
	private transient ReentrantLock lock = new ReentrantLock();

	private transient KList<IrisBiome> realLandBiomes;
	private transient KList<IrisBiome> realSeaBiomes;
	private transient KList<IrisBiome> realShoreBiomes;
	private transient KList<IrisBiome> realIslandBiomes;
	private transient KList<IrisBiome> realSkylandBiomes;
	private transient KList<IrisBiome> realCaveBiomes;

	public double getBiomeZoom(InferredType t)
	{
		switch(t)
		{
			case CAVE:
				return caveBiomeZoom;
			case ISLAND:
				return islandBiomeZoom;
			case LAND:
				return landBiomeZoom;
			case SEA:
				return seaBiomeZoom;
			case SHORE:
				return shoreBiomeZoom;
			case SKYLAND:
				return skylandBiomeZoom;
			default:
				break;
		}

		return 1;
	}

	public KList<String> getRidgeBiomeKeys()
	{
		lock.lock();

		if(cacheRidge == null)
		{
			cacheRidge = new KList<String>();
			ridgeBiomes.forEach((i) -> cacheRidge.add(i.getBiome()));
		}

		lock.unlock();

		return cacheRidge;
	}

	public KList<String> getSpotBiomeKeys()
	{
		lock.lock();

		if(cacheSpot == null)
		{
			cacheSpot = new KList<String>();
			spotBiomes.forEach((i) -> cacheSpot.add(i.getBiome()));
		}

		lock.unlock();

		return cacheSpot;
	}

	public double getShoreHeight(double x, double z)
	{
		if(shoreHeightGenerator == null)
		{
			lock.lock();
			shoreHeightGenerator = CNG.signature(new RNG((long) (getName().length() + getIslandBiomes().size() + getLandBiomeZoom() + getLandBiomes().size() + 3458612)));
			lock.unlock();
		}

		return shoreHeightGenerator.fitDoubleD(shoreHeightMin, shoreHeightMax, x / shoreHeightZoom, z / shoreHeightZoom);
	}

	public KList<IrisBiome> getAllBiomes()
	{
		KMap<String, IrisBiome> b = new KMap<>();
		KSet<String> names = new KSet<>();
		names.addAll(landBiomes);
		names.addAll(seaBiomes);
		names.addAll(shoreBiomes);
		spotBiomes.forEach((i) -> names.add(i.getBiome()));
		ridgeBiomes.forEach((i) -> names.add(i.getBiome()));

		while(!names.isEmpty())
		{
			for(String i : new KList<>(names))
			{
				if(b.containsKey(i))
				{
					names.remove(i);
					continue;
				}

				IrisBiome biome = Iris.data.getBiomeLoader().load(i);
				b.put(biome.getLoadKey(), biome);
				names.remove(i);
				names.addAll(biome.getChildren());
			}
		}

		return b.v();
	}

	public KList<IrisBiome> getBiomes(InferredType type)
	{
		if(type.equals(InferredType.LAND))
		{
			return getRealLandBiomes();
		}

		else if(type.equals(InferredType.SEA))
		{
			return getRealSeaBiomes();
		}

		else if(type.equals(InferredType.SHORE))
		{
			return getRealShoreBiomes();
		}

		else if(type.equals(InferredType.CAVE))
		{
			return getRealCaveBiomes();
		}

		else if(type.equals(InferredType.ISLAND))
		{
			return getRealIslandBiomes();
		}

		else if(type.equals(InferredType.SKYLAND))
		{
			return getRealSkylandBiomes();
		}

		return new KList<>();
	}

	public KList<IrisBiome> getRealCaveBiomes()
	{
		lock.lock();

		if(realCaveBiomes == null)
		{
			realCaveBiomes = new KList<>();

			for(String i : getCaveBiomes())
			{
				realCaveBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realCaveBiomes;
	}

	public KList<IrisBiome> getRealSkylandBiomes()
	{
		lock.lock();

		if(realSkylandBiomes == null)
		{
			realSkylandBiomes = new KList<>();

			for(String i : getSkylandBiomes())
			{
				realSkylandBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realSkylandBiomes;
	}

	public KList<IrisBiome> getRealIslandBiomes()
	{
		lock.lock();

		if(realIslandBiomes == null)
		{
			realIslandBiomes = new KList<>();

			for(String i : getIslandBiomes())
			{
				realIslandBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realIslandBiomes;
	}

	public KList<IrisBiome> getRealShoreBiomes()
	{
		lock.lock();

		if(realShoreBiomes == null)
		{
			realShoreBiomes = new KList<>();

			for(String i : getShoreBiomes())
			{
				realShoreBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realShoreBiomes;
	}

	public KList<IrisBiome> getRealSeaBiomes()
	{
		lock.lock();

		if(realSeaBiomes == null)
		{
			realSeaBiomes = new KList<>();

			for(String i : getSeaBiomes())
			{
				realSeaBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realSeaBiomes;
	}

	public KList<IrisBiome> getRealLandBiomes()
	{
		lock.lock();

		if(realLandBiomes == null)
		{
			realLandBiomes = new KList<>();

			for(String i : getLandBiomes())
			{
				realLandBiomes.add(Iris.data.getBiomeLoader().load(i));
			}

		}

		lock.unlock();
		return realLandBiomes;
	}
}