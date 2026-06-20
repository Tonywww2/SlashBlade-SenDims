package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.ForgeConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int CHUNK_SIZE = 16;
	private static final int HALF_CHUNK_SIZE = 8;
	private static final float SKY_DISC_RADIUS = 512.0F;
	private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
	private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
	private static final int MIN_FOG_DISTANCE = 32;
	private static final int RAIN_RADIUS = 10;
	private static final int RAIN_DIAMETER = 21;
	private static final int TRANSPARENT_SORT_COUNT = 15;
	private static final int HALF_A_SECOND_IN_MILLIS = 500;
	private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
	private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
	private static final ResourceLocation CLOUDS_LOCATION = new ResourceLocation("textures/environment/clouds.png");
	private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
	private static final ResourceLocation FORCEFIELD_LOCATION = new ResourceLocation("textures/misc/forcefield.png");
	private static final ResourceLocation RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
	private static final ResourceLocation SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private final Minecraft minecraft;
	private final EntityRenderDispatcher entityRenderDispatcher;
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	private final RenderBuffers renderBuffers;
	@Nullable
	private ClientLevel level;
	private final BlockingQueue<ChunkRenderDispatcher.RenderChunk> recentlyCompiledChunks = new LinkedBlockingQueue();
	private final AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference();
	private final ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum = new ObjectArrayList<>(10000);
	private final Set<BlockEntity> globalBlockEntities = Sets.<BlockEntity>newHashSet();
	@Nullable
	private Future<?> lastFullRenderChunkUpdate;
	@Nullable
	private ViewArea viewArea;
	@Nullable
	private VertexBuffer starBuffer;
	@Nullable
	private VertexBuffer skyBuffer;
	@Nullable
	private VertexBuffer darkBuffer;
	private boolean generateClouds = true;
	@Nullable
	private VertexBuffer cloudBuffer;
	private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
	private int ticks;
	private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
	private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
	private final Map<BlockPos, SoundInstance> playingRecords = Maps.<BlockPos, SoundInstance>newHashMap();
	@Nullable
	private RenderTarget entityTarget;
	@Nullable
	private PostChain entityEffect;
	@Nullable
	private RenderTarget translucentTarget;
	@Nullable
	private RenderTarget itemEntityTarget;
	@Nullable
	private RenderTarget particlesTarget;
	@Nullable
	private RenderTarget weatherTarget;
	@Nullable
	private RenderTarget cloudsTarget;
	@Nullable
	private PostChain transparencyChain;
	private double lastCameraX = Double.MIN_VALUE;
	private double lastCameraY = Double.MIN_VALUE;
	private double lastCameraZ = Double.MIN_VALUE;
	private int lastCameraChunkX = Integer.MIN_VALUE;
	private int lastCameraChunkY = Integer.MIN_VALUE;
	private int lastCameraChunkZ = Integer.MIN_VALUE;
	private double prevCamX = Double.MIN_VALUE;
	private double prevCamY = Double.MIN_VALUE;
	private double prevCamZ = Double.MIN_VALUE;
	private double prevCamRotX = Double.MIN_VALUE;
	private double prevCamRotY = Double.MIN_VALUE;
	private int prevCloudX = Integer.MIN_VALUE;
	private int prevCloudY = Integer.MIN_VALUE;
	private int prevCloudZ = Integer.MIN_VALUE;
	private Vec3 prevCloudColor = Vec3.ZERO;
	@Nullable
	private CloudStatus prevCloudsType;
	@Nullable
	private ChunkRenderDispatcher chunkRenderDispatcher;
	private int lastViewDistance = -1;
	private int renderedEntities;
	private int culledEntities;
	private Frustum cullingFrustum;
	private boolean captureFrustum;
	@Nullable
	private Frustum capturedFrustum;
	private final Vector4f[] frustumPoints = new Vector4f[8];
	private final Vector3d frustumPos = new Vector3d(0.0, 0.0, 0.0);
	private double xTransparentOld;
	private double yTransparentOld;
	private double zTransparentOld;
	private boolean needsFullRenderChunkUpdate = true;
	private final AtomicLong nextFullUpdateMillis = new AtomicLong(0L);
	private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
	private int rainSoundTime;
	private final float[] rainSizeX = new float[1024];
	private final float[] rainSizeZ = new float[1024];

	public LevelRenderer(Minecraft arg, EntityRenderDispatcher arg2, BlockEntityRenderDispatcher arg3, RenderBuffers arg4) {
		this.minecraft = arg;
		this.entityRenderDispatcher = arg2;
		this.blockEntityRenderDispatcher = arg3;
		this.renderBuffers = arg4;

		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				float f = (float)(j - 16);
				float f1 = (float)(i - 16);
				float f2 = Mth.sqrt(f * f + f1 * f1);
				this.rainSizeX[i << 5 | j] = -f1 / f2;
				this.rainSizeZ[i << 5 | j] = f / f2;
			}
		}

		this.createStars();
		this.createLightSky();
		this.createDarkSky();
	}

	private void renderSnowAndRain(LightTexture arg, float g, double d, double e, double h) {
		if (!this.level.effects().renderSnowAndRain(this.level, this.ticks, g, arg, d, e, h)) {
			float f = this.minecraft.level.getRainLevel(g);
			if (!(f <= 0.0F)) {
				arg.turnOnLightLayer();
				Level level = this.minecraft.level;
				int i = Mth.floor(d);
				int j = Mth.floor(e);
				int k = Mth.floor(h);
				Tesselator tesselator = Tesselator.getInstance();
				BufferBuilder bufferbuilder = tesselator.getBuilder();
				RenderSystem.disableCull();
				RenderSystem.enableBlend();
				RenderSystem.enableDepthTest();
				int l = 5;
				if (Minecraft.useFancyGraphics()) {
					l = 10;
				}

				RenderSystem.depthMask(Minecraft.useShaderTransparency());
				int i1 = -1;
				float f1 = (float)this.ticks + g;
				RenderSystem.setShader(GameRenderer::getParticleShader);
				BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

				for (int j1 = k - l; j1 <= k + l; j1++) {
					for (int k1 = i - l; k1 <= i + l; k1++) {
						int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
						double d0 = (double)this.rainSizeX[l1] * 0.5;
						double d1 = (double)this.rainSizeZ[l1] * 0.5;
						blockpos$mutableblockpos.set((double)k1, e, (double)j1);
						Biome biome = level.getBiome(blockpos$mutableblockpos).value();
						if (biome.hasPrecipitation()) {
							int i2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, k1, j1);
							int j2 = j - l;
							int k2 = j + l;
							if (j2 < i2) {
								j2 = i2;
							}

							if (k2 < i2) {
								k2 = i2;
							}

							int l2 = i2;
							if (i2 < j) {
								l2 = j;
							}

							if (j2 != k2) {
								RandomSource randomsource = RandomSource.create((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
								blockpos$mutableblockpos.set(k1, j2, j1);
								Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos$mutableblockpos);
								if (biome$precipitation == Biome.Precipitation.RAIN) {
									if (i1 != 0) {
										if (i1 >= 0) {
											tesselator.end();
										}

										i1 = 0;
										RenderSystem.setShaderTexture(0, RAIN_LOCATION);
										bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
									}

									int i3 = this.ticks + k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 31;
									float f2 = -((float)i3 + g) / 32.0F * (3.0F + randomsource.nextFloat());
									double d2 = (double)k1 + 0.5 - d;
									double d4 = (double)j1 + 0.5 - h;
									float f3 = (float)Math.sqrt(d2 * d2 + d4 * d4) / (float)l;
									float f4 = ((1.0F - f3 * f3) * 0.5F + 0.5F) * f;
									blockpos$mutableblockpos.set(k1, l2, j1);
									int j3 = getLightColor(level, blockpos$mutableblockpos);
									bufferbuilder.vertex((double)k1 - d - d0 + 0.5, (double)k2 - e, (double)j1 - h - d1 + 0.5)
										.uv(0.0F, (float)j2 * 0.25F + f2)
										.color(1.0F, 1.0F, 1.0F, f4)
										.uv2(j3)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d + d0 + 0.5, (double)k2 - e, (double)j1 - h + d1 + 0.5)
										.uv(1.0F, (float)j2 * 0.25F + f2)
										.color(1.0F, 1.0F, 1.0F, f4)
										.uv2(j3)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d + d0 + 0.5, (double)j2 - e, (double)j1 - h + d1 + 0.5)
										.uv(1.0F, (float)k2 * 0.25F + f2)
										.color(1.0F, 1.0F, 1.0F, f4)
										.uv2(j3)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d - d0 + 0.5, (double)j2 - e, (double)j1 - h - d1 + 0.5)
										.uv(0.0F, (float)k2 * 0.25F + f2)
										.color(1.0F, 1.0F, 1.0F, f4)
										.uv2(j3)
										.endVertex();
								} else if (biome$precipitation == Biome.Precipitation.SNOW) {
									if (i1 != 1) {
										if (i1 >= 0) {
											tesselator.end();
										}

										i1 = 1;
										RenderSystem.setShaderTexture(0, SNOW_LOCATION);
										bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
									}

									float f5 = -((float)(this.ticks & 511) + g) / 512.0F;
									float f6 = (float)(randomsource.nextDouble() + (double)f1 * 0.01 * (double)((float)randomsource.nextGaussian()));
									float f7 = (float)(randomsource.nextDouble() + (double)(f1 * (float)randomsource.nextGaussian()) * 0.001);
									double d3 = (double)k1 + 0.5 - d;
									double d5 = (double)j1 + 0.5 - h;
									float f8 = (float)Math.sqrt(d3 * d3 + d5 * d5) / (float)l;
									float f9 = ((1.0F - f8 * f8) * 0.3F + 0.5F) * f;
									blockpos$mutableblockpos.set(k1, l2, j1);
									int k3 = getLightColor(level, blockpos$mutableblockpos);
									int l3 = k3 >> 16 & 65535;
									int i4 = k3 & 65535;
									int j4 = (l3 * 3 + 240) / 4;
									int k4 = (i4 * 3 + 240) / 4;
									bufferbuilder.vertex((double)k1 - d - d0 + 0.5, (double)k2 - e, (double)j1 - h - d1 + 0.5)
										.uv(0.0F + f6, (float)j2 * 0.25F + f5 + f7)
										.color(1.0F, 1.0F, 1.0F, f9)
										.uv2(k4, j4)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d + d0 + 0.5, (double)k2 - e, (double)j1 - h + d1 + 0.5)
										.uv(1.0F + f6, (float)j2 * 0.25F + f5 + f7)
										.color(1.0F, 1.0F, 1.0F, f9)
										.uv2(k4, j4)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d + d0 + 0.5, (double)j2 - e, (double)j1 - h + d1 + 0.5)
										.uv(1.0F + f6, (float)k2 * 0.25F + f5 + f7)
										.color(1.0F, 1.0F, 1.0F, f9)
										.uv2(k4, j4)
										.endVertex();
									bufferbuilder.vertex((double)k1 - d - d0 + 0.5, (double)j2 - e, (double)j1 - h - d1 + 0.5)
										.uv(0.0F + f6, (float)k2 * 0.25F + f5 + f7)
										.color(1.0F, 1.0F, 1.0F, f9)
										.uv2(k4, j4)
										.endVertex();
								}
							}
						}
					}
				}

				if (i1 >= 0) {
					tesselator.end();
				}

				RenderSystem.enableCull();
				RenderSystem.disableBlend();
				arg.turnOffLightLayer();
			}
		}
	}

	public void tickRain(Camera arg) {
		if (!this.level.effects().tickRain(this.level, this.ticks, arg)) {
			float f = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
			if (!(f <= 0.0F)) {
				RandomSource randomsource = RandomSource.create((long)this.ticks * 312987231L);
				LevelReader levelreader = this.minecraft.level;
				BlockPos blockpos = BlockPos.containing(arg.getPosition());
				BlockPos blockpos1 = null;
				int i = (int)(100.0F * f * f) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

				for (int j = 0; j < i; j++) {
					int k = randomsource.nextInt(21) - 10;
					int l = randomsource.nextInt(21) - 10;
					BlockPos blockpos2 = levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
					if (blockpos2.getY() > levelreader.getMinBuildHeight() && blockpos2.getY() <= blockpos.getY() + 10 && blockpos2.getY() >= blockpos.getY() - 10) {
						Biome biome = levelreader.getBiome(blockpos2).value();
						if (biome.getPrecipitationAt(blockpos2) == Biome.Precipitation.RAIN) {
							blockpos1 = blockpos2.below();
							if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
								break;
							}

							double d0 = randomsource.nextDouble();
							double d1 = randomsource.nextDouble();
							BlockState blockstate = levelreader.getBlockState(blockpos1);
							FluidState fluidstate = levelreader.getFluidState(blockpos1);
							VoxelShape voxelshape = blockstate.getCollisionShape(levelreader, blockpos1);
							double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
							double d3 = (double)fluidstate.getHeight(levelreader, blockpos1);
							double d4 = Math.max(d2, d3);
							ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA) && !blockstate.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockstate)
								? ParticleTypes.RAIN
								: ParticleTypes.SMOKE;
							this.minecraft
								.level
								.addParticle(particleoptions, (double)blockpos1.getX() + d0, (double)blockpos1.getY() + d4, (double)blockpos1.getZ() + d1, 0.0, 0.0, 0.0);
						}
					}
				}

				if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
					this.rainSoundTime = 0;
					if (blockpos1.getY() > blockpos.getY() + 1
						&& levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
						this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
					} else {
						this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
					}
				}
			}
		}
	}

	public void close() {
		if (this.entityEffect != null) {
			this.entityEffect.close();
		}

		if (this.transparencyChain != null) {
			this.transparencyChain.close();
		}
	}

	@Override
	public void onResourceManagerReload(ResourceManager arg) {
		this.initOutline();
		if (Minecraft.useShaderTransparency()) {
			this.initTransparency();
		}
	}

	public void initOutline() {
		if (this.entityEffect != null) {
			this.entityEffect.close();
		}

		ResourceLocation resourcelocation = new ResourceLocation("shaders/post/entity_outline.json");

		try {
			this.entityEffect = new PostChain(
				this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation
			);
			this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
			this.entityTarget = this.entityEffect.getTempTarget("final");
		} catch (IOException var3) {
			LOGGER.warn("Failed to load shader: {}", resourcelocation, var3);
			this.entityEffect = null;
			this.entityTarget = null;
		} catch (JsonSyntaxException var4) {
			LOGGER.warn("Failed to parse shader: {}", resourcelocation, var4);
			this.entityEffect = null;
			this.entityTarget = null;
		}
	}

	private void initTransparency() {
		this.deinitTransparency();
		ResourceLocation resourcelocation = new ResourceLocation("shaders/post/transparency.json");

		try {
			PostChain postchain = new PostChain(
				this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation
			);
			postchain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
			RenderTarget rendertarget1 = postchain.getTempTarget("translucent");
			RenderTarget rendertarget2 = postchain.getTempTarget("itemEntity");
			RenderTarget rendertarget3 = postchain.getTempTarget("particles");
			RenderTarget rendertarget4 = postchain.getTempTarget("weather");
			RenderTarget rendertarget = postchain.getTempTarget("clouds");
			this.transparencyChain = postchain;
			this.translucentTarget = rendertarget1;
			this.itemEntityTarget = rendertarget2;
			this.particlesTarget = rendertarget3;
			this.weatherTarget = rendertarget4;
			this.cloudsTarget = rendertarget;
		} catch (Exception var8) {
			String s = var8 instanceof JsonSyntaxException ? "parse" : "load";
			String s1 = "Failed to " + s + " shader: " + resourcelocation;
			LevelRenderer.TransparencyShaderException levelrenderer$transparencyshaderexception = new LevelRenderer.TransparencyShaderException(s1, var8);
			if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
				Component component = (Component)this.minecraft
					.getResourceManager()
					.listPacks()
					.findFirst()
					.map(arg -> Component.literal(arg.packId()))
					.orElse((MutableComponent)null);
				this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
				this.minecraft.clearResourcePacksOnError(levelrenderer$transparencyshaderexception, component);
			} else {
				CrashReport crashreport = this.minecraft.fillReport(new CrashReport(s1, levelrenderer$transparencyshaderexception));
				this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
				this.minecraft.options.save();
				LOGGER.error(LogUtils.FATAL_MARKER, s1, (Throwable)levelrenderer$transparencyshaderexception);
				this.minecraft.emergencySave();
				Minecraft.crash(crashreport);
			}
		}
	}

	private void deinitTransparency() {
		if (this.transparencyChain != null) {
			this.transparencyChain.close();
			this.translucentTarget.destroyBuffers();
			this.itemEntityTarget.destroyBuffers();
			this.particlesTarget.destroyBuffers();
			this.weatherTarget.destroyBuffers();
			this.cloudsTarget.destroyBuffers();
			this.transparencyChain = null;
			this.translucentTarget = null;
			this.itemEntityTarget = null;
			this.particlesTarget = null;
			this.weatherTarget = null;
			this.cloudsTarget = null;
		}
	}

	public void doEntityOutline() {
		if (this.shouldShowEntityOutlines()) {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE
			);
			this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
		}
	}

	public boolean shouldShowEntityOutlines() {
		return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null;
	}

	private void createDarkSky() {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		if (this.darkBuffer != null) {
			this.darkBuffer.close();
		}

		this.darkBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = buildSkyDisc(bufferbuilder, -16.0F);
		this.darkBuffer.bind();
		this.darkBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
	}

	private void createLightSky() {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		if (this.skyBuffer != null) {
			this.skyBuffer.close();
		}

		this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = buildSkyDisc(bufferbuilder, 16.0F);
		this.skyBuffer.bind();
		this.skyBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
	}

	private static BufferBuilder.RenderedBuffer buildSkyDisc(BufferBuilder arg, float g) {
		float f = Math.signum(g) * 512.0F;
		float f1 = 512.0F;
		RenderSystem.setShader(GameRenderer::getPositionShader);
		arg.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
		arg.vertex(0.0, (double)g, 0.0).endVertex();

		for (int i = -180; i <= 180; i += 45) {
			arg.vertex((double)(f * Mth.cos((float)i * (float) (Math.PI / 180.0))), (double)g, (double)(512.0F * Mth.sin((float)i * (float) (Math.PI / 180.0))))
				.endVertex();
		}

		return arg.end();
	}

	private void createStars() {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		if (this.starBuffer != null) {
			this.starBuffer.close();
		}

		this.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = this.drawStars(bufferbuilder);
		this.starBuffer.bind();
		this.starBuffer.upload(bufferbuilder$renderedbuffer);
		VertexBuffer.unbind();
	}

	private BufferBuilder.RenderedBuffer drawStars(BufferBuilder arg) {
		RandomSource randomsource = RandomSource.create(10842L);
		arg.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

		for (int i = 0; i < 1500; i++) {
			double d0 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
			double d1 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
			double d2 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
			double d3 = (double)(0.15F + randomsource.nextFloat() * 0.1F);
			double d4 = d0 * d0 + d1 * d1 + d2 * d2;
			if (d4 < 1.0 && d4 > 0.01) {
				d4 = 1.0 / Math.sqrt(d4);
				d0 *= d4;
				d1 *= d4;
				d2 *= d4;
				double d5 = d0 * 100.0;
				double d6 = d1 * 100.0;
				double d7 = d2 * 100.0;
				double d8 = Math.atan2(d0, d2);
				double d9 = Math.sin(d8);
				double d10 = Math.cos(d8);
				double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
				double d12 = Math.sin(d11);
				double d13 = Math.cos(d11);
				double d14 = randomsource.nextDouble() * Math.PI * 2.0;
				double d15 = Math.sin(d14);
				double d16 = Math.cos(d14);

				for (int j = 0; j < 4; j++) {
					double d17 = 0.0;
					double d18 = (double)((j & 2) - 1) * d3;
					double d19 = (double)((j + 1 & 2) - 1) * d3;
					double d20 = 0.0;
					double d21 = d18 * d16 - d19 * d15;
					double d22 = d19 * d16 + d18 * d15;
					double d23 = d21 * d12 + 0.0 * d13;
					double d24 = 0.0 * d12 - d21 * d13;
					double d25 = d24 * d9 - d22 * d10;
					double d26 = d22 * d9 + d24 * d10;
					arg.vertex(d5 + d25, d6 + d23, d7 + d26).endVertex();
				}
			}
		}

		return arg.end();
	}

	public void setLevel(@Nullable ClientLevel arg) {
		this.lastCameraX = Double.MIN_VALUE;
		this.lastCameraY = Double.MIN_VALUE;
		this.lastCameraZ = Double.MIN_VALUE;
		this.lastCameraChunkX = Integer.MIN_VALUE;
		this.lastCameraChunkY = Integer.MIN_VALUE;
		this.lastCameraChunkZ = Integer.MIN_VALUE;
		this.entityRenderDispatcher.setLevel(arg);
		this.level = arg;
		if (arg != null) {
			this.allChanged();
		} else {
			if (this.viewArea != null) {
				this.viewArea.releaseAllBuffers();
				this.viewArea = null;
			}

			if (this.chunkRenderDispatcher != null) {
				this.chunkRenderDispatcher.dispose();
			}

			this.chunkRenderDispatcher = null;
			this.globalBlockEntities.clear();
			this.renderChunkStorage.set((LevelRenderer.RenderChunkStorage)null);
			this.renderChunksInFrustum.clear();
		}
	}

	public void graphicsChanged() {
		if (Minecraft.useShaderTransparency()) {
			this.initTransparency();
		} else {
			this.deinitTransparency();
		}
	}

	public void allChanged() {
		if (this.level != null) {
			this.graphicsChanged();
			this.level.clearTintCaches();
			if (this.chunkRenderDispatcher == null) {
				this.chunkRenderDispatcher = new ChunkRenderDispatcher(
					this.level, this, Util.backgroundExecutor(), this.minecraft.is64Bit(), this.renderBuffers.fixedBufferPack()
				);
			} else {
				this.chunkRenderDispatcher.setLevel(this.level);
			}

			this.needsFullRenderChunkUpdate = true;
			this.generateClouds = true;
			this.recentlyCompiledChunks.clear();
			ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
			this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
			if (this.viewArea != null) {
				this.viewArea.releaseAllBuffers();
			}

			this.chunkRenderDispatcher.blockUntilClear();
			synchronized (this.globalBlockEntities) {
				this.globalBlockEntities.clear();
			}

			this.viewArea = new ViewArea(this.chunkRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
			if (this.lastFullRenderChunkUpdate != null) {
				try {
					this.lastFullRenderChunkUpdate.get();
					this.lastFullRenderChunkUpdate = null;
				} catch (Exception var3) {
					LOGGER.warn("Full update failed", (Throwable)var3);
				}
			}

			this.renderChunkStorage.set(new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length));
			this.renderChunksInFrustum.clear();
			Entity entity = this.minecraft.getCameraEntity();
			if (entity != null) {
				this.viewArea.repositionCamera(entity.getX(), entity.getZ());
			}
		}
	}

	public void resize(int i, int j) {
		this.needsUpdate();
		if (this.entityEffect != null) {
			this.entityEffect.resize(i, j);
		}

		if (this.transparencyChain != null) {
			this.transparencyChain.resize(i, j);
		}
	}

	public String getChunkStatistics() {
		int i = this.viewArea.chunks.length;
		int j = this.countRenderedChunks();
		return String.format(
			Locale.ROOT,
			"C: %d/%d %sD: %d, %s",
			j,
			i,
			this.minecraft.smartCull ? "(s) " : "",
			this.lastViewDistance,
			this.chunkRenderDispatcher == null ? "null" : this.chunkRenderDispatcher.getStats()
		);
	}

	public ChunkRenderDispatcher getChunkRenderDispatcher() {
		return this.chunkRenderDispatcher;
	}

	public double getTotalChunks() {
		return (double)this.viewArea.chunks.length;
	}

	public double getLastViewDistance() {
		return (double)this.lastViewDistance;
	}

	public int countRenderedChunks() {
		int i = 0;

		for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
			if (!levelrenderer$renderchunkinfo.chunk.getCompiledChunk().hasNoRenderableLayers()) {
				i++;
			}
		}

		return i;
	}

	public String getEntityStatistics() {
		return "E: "
			+ this.renderedEntities
			+ "/"
			+ this.level.getEntityCount()
			+ ", B: "
			+ this.culledEntities
			+ ", SD: "
			+ this.level.getServerSimulationDistance();
	}

	private void setupRender(Camera arg, Frustum arg2, boolean bl, boolean bl2) {
		Vec3 vec3 = arg.getPosition();
		if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
			this.allChanged();
		}

		this.level.getProfiler().push("camera");
		double d0 = this.minecraft.player.getX();
		double d1 = this.minecraft.player.getY();
		double d2 = this.minecraft.player.getZ();
		int i = SectionPos.posToSectionCoord(d0);
		int j = SectionPos.posToSectionCoord(d1);
		int k = SectionPos.posToSectionCoord(d2);
		if (this.lastCameraChunkX != i || this.lastCameraChunkY != j || this.lastCameraChunkZ != k) {
			this.lastCameraX = d0;
			this.lastCameraY = d1;
			this.lastCameraZ = d2;
			this.lastCameraChunkX = i;
			this.lastCameraChunkY = j;
			this.lastCameraChunkZ = k;
			this.viewArea.repositionCamera(d0, d2);
		}

		this.chunkRenderDispatcher.setCamera(vec3);
		this.level.getProfiler().popPush("cull");
		this.minecraft.getProfiler().popPush("culling");
		BlockPos blockpos = arg.getBlockPosition();
		double d3 = Math.floor(vec3.x / 8.0);
		double d4 = Math.floor(vec3.y / 8.0);
		double d5 = Math.floor(vec3.z / 8.0);
		this.needsFullRenderChunkUpdate = this.needsFullRenderChunkUpdate || d3 != this.prevCamX || d4 != this.prevCamY || d5 != this.prevCamZ;
		this.nextFullUpdateMillis.updateAndGet(l -> {
			if (l > 0L && System.currentTimeMillis() > l) {
				this.needsFullRenderChunkUpdate = true;
				return 0L;
			} else {
				return l;
			}
		});
		this.prevCamX = d3;
		this.prevCamY = d4;
		this.prevCamZ = d5;
		this.minecraft.getProfiler().popPush("update");
		boolean flag = this.minecraft.smartCull;
		if (bl2 && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos)) {
			flag = false;
		}

		if (!bl) {
			if (this.needsFullRenderChunkUpdate && (this.lastFullRenderChunkUpdate == null || this.lastFullRenderChunkUpdate.isDone())) {
				this.minecraft.getProfiler().push("full_update_schedule");
				this.needsFullRenderChunkUpdate = false;
				boolean flag1 = flag;
				this.lastFullRenderChunkUpdate = Util.backgroundExecutor().submit(() -> {
					Queue<LevelRenderer.RenderChunkInfo> queue1 = Queues.<LevelRenderer.RenderChunkInfo>newArrayDeque();
					this.initializeQueueForFullUpdate(arg, queue1);
					LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage1 = new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length);
					this.updateRenderChunks(levelrenderer$renderchunkstorage1.renderChunks, levelrenderer$renderchunkstorage1.renderInfoMap, vec3, queue1, flag1);
					this.renderChunkStorage.set(levelrenderer$renderchunkstorage1);
					this.needsFrustumUpdate.set(true);
				});
				this.minecraft.getProfiler().pop();
			}

			LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage = (LevelRenderer.RenderChunkStorage)this.renderChunkStorage.get();
			if (!this.recentlyCompiledChunks.isEmpty()) {
				this.minecraft.getProfiler().push("partial_update");
				Queue<LevelRenderer.RenderChunkInfo> queue = Queues.<LevelRenderer.RenderChunkInfo>newArrayDeque();

				while (!this.recentlyCompiledChunks.isEmpty()) {
					ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = (ChunkRenderDispatcher.RenderChunk)this.recentlyCompiledChunks.poll();
					LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo = levelrenderer$renderchunkstorage.renderInfoMap.get(chunkrenderdispatcher$renderchunk);
					if (levelrenderer$renderchunkinfo != null && levelrenderer$renderchunkinfo.chunk == chunkrenderdispatcher$renderchunk) {
						queue.add(levelrenderer$renderchunkinfo);
					}
				}

				this.updateRenderChunks(levelrenderer$renderchunkstorage.renderChunks, levelrenderer$renderchunkstorage.renderInfoMap, vec3, queue, flag);
				this.needsFrustumUpdate.set(true);
				this.minecraft.getProfiler().pop();
			}

			double d6 = Math.floor((double)(arg.getXRot() / 2.0F));
			double d7 = Math.floor((double)(arg.getYRot() / 2.0F));
			if (this.needsFrustumUpdate.compareAndSet(true, false) || d6 != this.prevCamRotX || d7 != this.prevCamRotY) {
				this.applyFrustum(new Frustum(arg2).offsetToFullyIncludeCameraCube(8));
				this.prevCamRotX = d6;
				this.prevCamRotY = d7;
			}
		}

		this.minecraft.getProfiler().pop();
	}

	private void applyFrustum(Frustum arg) {
		if (!Minecraft.getInstance().isSameThread()) {
			throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
		} else {
			this.minecraft.getProfiler().push("apply_frustum");
			this.renderChunksInFrustum.clear();

			for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : ((LevelRenderer.RenderChunkStorage)this.renderChunkStorage.get()).renderChunks) {
				if (arg.isVisible(levelrenderer$renderchunkinfo.chunk.getBoundingBox())) {
					this.renderChunksInFrustum.add(levelrenderer$renderchunkinfo);
				}
			}

			this.minecraft.getProfiler().pop();
		}
	}

	private void initializeQueueForFullUpdate(Camera arg, Queue<LevelRenderer.RenderChunkInfo> queue) {
		int i = 16;
		Vec3 vec3 = arg.getPosition();
		BlockPos blockpos = arg.getBlockPosition();
		ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = this.viewArea.getRenderChunkAt(blockpos);
		if (chunkrenderdispatcher$renderchunk == null) {
			boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
			int j = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
			int k = Mth.floor(vec3.x / 16.0) * 16;
			int l = Mth.floor(vec3.z / 16.0) * 16;
			List<LevelRenderer.RenderChunkInfo> list = Lists.<LevelRenderer.RenderChunkInfo>newArrayList();

			for (int i1 = -this.lastViewDistance; i1 <= this.lastViewDistance; i1++) {
				for (int j1 = -this.lastViewDistance; j1 <= this.lastViewDistance; j1++) {
					ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 = this.viewArea
						.getRenderChunkAt(new BlockPos(k + SectionPos.sectionToBlockCoord(i1, 8), j, l + SectionPos.sectionToBlockCoord(j1, 8)));
					if (chunkrenderdispatcher$renderchunk1 != null) {
						list.add(new LevelRenderer.RenderChunkInfo(chunkrenderdispatcher$renderchunk1, (Direction)null, 0));
					}
				}
			}

			list.sort(Comparator.comparingDouble(arg2 -> blockpos.distSqr(arg2.chunk.getOrigin().offset(8, 8, 8))));
			queue.addAll(list);
		} else {
			queue.add(new LevelRenderer.RenderChunkInfo(chunkrenderdispatcher$renderchunk, (Direction)null, 0));
		}
	}

	public void addRecentlyCompiledChunk(ChunkRenderDispatcher.RenderChunk arg) {
		this.recentlyCompiledChunks.add(arg);
	}

	private void updateRenderChunks(
		LinkedHashSet<LevelRenderer.RenderChunkInfo> linkedHashSet,
		LevelRenderer.RenderInfoMap arg,
		Vec3 arg2,
		Queue<LevelRenderer.RenderChunkInfo> queue,
		boolean bl
	) {
		int i = 16;
		BlockPos blockpos = new BlockPos(Mth.floor(arg2.x / 16.0) * 16, Mth.floor(arg2.y / 16.0) * 16, Mth.floor(arg2.z / 16.0) * 16);
		BlockPos blockpos1 = blockpos.offset(8, 8, 8);
		Entity.setViewScale(
			Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get()
		);

		while (!queue.isEmpty()) {
			LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo = (LevelRenderer.RenderChunkInfo)queue.poll();
			ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
			linkedHashSet.add(levelrenderer$renderchunkinfo);
			boolean flag = Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getX() - blockpos.getX()) > 60
				|| Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getY() - blockpos.getY()) > 60
				|| Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getZ() - blockpos.getZ()) > 60;

			for (Direction direction : DIRECTIONS) {
				ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 = this.getRelativeFrom(blockpos, chunkrenderdispatcher$renderchunk, direction);
				if (chunkrenderdispatcher$renderchunk1 != null && (!bl || !levelrenderer$renderchunkinfo.hasDirection(direction.getOpposite()))) {
					if (bl && levelrenderer$renderchunkinfo.hasSourceDirections()) {
						ChunkRenderDispatcher.CompiledChunk chunkrenderdispatcher$compiledchunk = chunkrenderdispatcher$renderchunk.getCompiledChunk();
						boolean flag1 = false;

						for (int j = 0; j < DIRECTIONS.length; j++) {
							if (levelrenderer$renderchunkinfo.hasSourceDirection(j)
								&& chunkrenderdispatcher$compiledchunk.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction)) {
								flag1 = true;
								break;
							}
						}

						if (!flag1) {
							continue;
						}
					}

					if (bl && flag) {
						BlockPos blockpos2 = chunkrenderdispatcher$renderchunk1.getOrigin();
						byte b0;
						if (direction.getAxis() == Direction.Axis.X ? blockpos1.getX() > blockpos2.getX() : blockpos1.getX() < blockpos2.getX()) {
							b0 = 16;
						} else {
							b0 = 0;
						}

						byte b1;
						if (direction.getAxis() == Direction.Axis.Y ? blockpos1.getY() > blockpos2.getY() : blockpos1.getY() < blockpos2.getY()) {
							b1 = 16;
						} else {
							b1 = 0;
						}

						byte b2;
						if (direction.getAxis() == Direction.Axis.Z ? blockpos1.getZ() > blockpos2.getZ() : blockpos1.getZ() < blockpos2.getZ()) {
							b2 = 16;
						} else {
							b2 = 0;
						}

						BlockPos blockpos3 = blockpos2.offset(b0, b1, b2);
						Vec3 vec31 = new Vec3((double)blockpos3.getX(), (double)blockpos3.getY(), (double)blockpos3.getZ());
						Vec3 vec3 = arg2.subtract(vec31).normalize().scale(CEILED_SECTION_DIAGONAL);
						boolean flag2 = true;

						while (arg2.subtract(vec31).lengthSqr() > 3600.0) {
							vec31 = vec31.add(vec3);
							if (vec31.y > (double)this.level.getMaxBuildHeight() || vec31.y < (double)this.level.getMinBuildHeight()) {
								break;
							}

							ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk2 = this.viewArea.getRenderChunkAt(BlockPos.containing(vec31.x, vec31.y, vec31.z));
							if (chunkrenderdispatcher$renderchunk2 == null || arg.get(chunkrenderdispatcher$renderchunk2) == null) {
								flag2 = false;
								break;
							}
						}

						if (!flag2) {
							continue;
						}
					}

					LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo1 = arg.get(chunkrenderdispatcher$renderchunk1);
					if (levelrenderer$renderchunkinfo1 != null) {
						levelrenderer$renderchunkinfo1.addSourceDirection(direction);
					} else if (!chunkrenderdispatcher$renderchunk1.hasAllNeighbors()) {
						if (!this.closeToBorder(blockpos, chunkrenderdispatcher$renderchunk)) {
							this.nextFullUpdateMillis.set(System.currentTimeMillis() + 500L);
						}
					} else {
						LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo2 = new LevelRenderer.RenderChunkInfo(
							chunkrenderdispatcher$renderchunk1, direction, levelrenderer$renderchunkinfo.step + 1
						);
						levelrenderer$renderchunkinfo2.setDirections(levelrenderer$renderchunkinfo.directions, direction);
						queue.add(levelrenderer$renderchunkinfo2);
						arg.put(chunkrenderdispatcher$renderchunk1, levelrenderer$renderchunkinfo2);
					}
				}
			}
		}
	}

	@Nullable
	private ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos arg, ChunkRenderDispatcher.RenderChunk arg2, Direction arg3) {
		BlockPos blockpos = arg2.getRelativeOrigin(arg3);
		if (Mth.abs(arg.getX() - blockpos.getX()) > this.lastViewDistance * 16) {
			return null;
		} else if (Mth.abs(arg.getY() - blockpos.getY()) <= this.lastViewDistance * 16
			&& blockpos.getY() >= this.level.getMinBuildHeight()
			&& blockpos.getY() < this.level.getMaxBuildHeight()) {
			return Mth.abs(arg.getZ() - blockpos.getZ()) > this.lastViewDistance * 16 ? null : this.viewArea.getRenderChunkAt(blockpos);
		} else {
			return null;
		}
	}

	private boolean closeToBorder(BlockPos arg, ChunkRenderDispatcher.RenderChunk arg2) {
		int i = SectionPos.blockToSectionCoord(arg.getX());
		int j = SectionPos.blockToSectionCoord(arg.getZ());
		BlockPos blockpos = arg2.getOrigin();
		int k = SectionPos.blockToSectionCoord(blockpos.getX());
		int l = SectionPos.blockToSectionCoord(blockpos.getZ());
		return !ChunkMap.isChunkInRange(k, l, i, j, this.lastViewDistance - 3);
	}

	private void captureFrustum(Matrix4f matrix4f2, Matrix4f matrix4f3, double d, double e, double f, Frustum arg) {
		this.capturedFrustum = arg;
		Matrix4f matrix4f = new Matrix4f(matrix4f3);
		matrix4f.mul(matrix4f2);
		matrix4f.invert();
		this.frustumPos.x = d;
		this.frustumPos.y = e;
		this.frustumPos.z = f;
		this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
		this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
		this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
		this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
		this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
		this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
		this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

		for (int i = 0; i < 8; i++) {
			matrix4f.transform(this.frustumPoints[i]);
			this.frustumPoints[i].div(this.frustumPoints[i].w());
		}
	}

	public void prepareCullFrustum(PoseStack arg, Vec3 arg2, Matrix4f matrix4f2) {
		Matrix4f matrix4f = arg.last().pose();
		double d0 = arg2.x();
		double d1 = arg2.y();
		double d2 = arg2.z();
		this.cullingFrustum = new Frustum(matrix4f, matrix4f2);
		this.cullingFrustum.prepare(d0, d1, d2);
	}

	public void renderLevel(PoseStack arg, float g, long l, boolean bl, Camera arg2, GameRenderer arg3, LightTexture arg4, Matrix4f matrix4f2) {
		RenderSystem.setShaderGameTime(this.level.getGameTime(), g);
		this.blockEntityRenderDispatcher.prepare(this.level, arg2, this.minecraft.hitResult);
		this.entityRenderDispatcher.prepare(this.level, arg2, this.minecraft.crosshairPickEntity);
		ProfilerFiller profilerfiller = this.level.getProfiler();
		profilerfiller.popPush("light_update_queue");
		this.level.pollLightUpdates();
		profilerfiller.popPush("light_updates");
		this.level.getChunkSource().getLightEngine().runLightUpdates();
		Vec3 vec3 = arg2.getPosition();
		double d0 = vec3.x();
		double d1 = vec3.y();
		double d2 = vec3.z();
		Matrix4f matrix4f = arg.last().pose();
		profilerfiller.popPush("culling");
		boolean flag = this.capturedFrustum != null;
		Frustum frustum;
		if (flag) {
			frustum = this.capturedFrustum;
			frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
		} else {
			frustum = this.cullingFrustum;
		}

		this.minecraft.getProfiler().popPush("captureFrustum");
		if (this.captureFrustum) {
			this.captureFrustum(matrix4f, matrix4f2, vec3.x, vec3.y, vec3.z, flag ? new Frustum(matrix4f, matrix4f2) : frustum);
			this.captureFrustum = false;
		}

		profilerfiller.popPush("clear");
		FogRenderer.setupColor(arg2, g, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), arg3.getDarkenWorldAmount(g));
		FogRenderer.levelFogColor();
		RenderSystem.clear(16640, Minecraft.ON_OSX);
		float f = arg3.getRenderDistance();
		boolean flag1 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
		FogRenderer.setupFog(arg2, FogRenderer.FogMode.FOG_SKY, f, flag1, g);
		profilerfiller.popPush("sky");
		RenderSystem.setShader(GameRenderer::getPositionShader);
		this.renderSky(arg, matrix4f2, g, arg2, flag1, () -> FogRenderer.setupFog(arg2, FogRenderer.FogMode.FOG_SKY, f, flag1, g));
		ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_SKY, this, arg, matrix4f2, this.ticks, arg2, frustum);
		profilerfiller.popPush("fog");
		FogRenderer.setupFog(arg2, FogRenderer.FogMode.FOG_TERRAIN, Math.max(f, 32.0F), flag1, g);
		profilerfiller.popPush("terrain_setup");
		this.setupRender(arg2, frustum, flag, this.minecraft.player.isSpectator());
		profilerfiller.popPush("compilechunks");
		this.compileChunks(arg2);
		profilerfiller.popPush("terrain");
		this.renderChunkLayer(RenderType.solid(), arg, d0, d1, d2, matrix4f2);
		this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0);
		this.renderChunkLayer(RenderType.cutoutMipped(), arg, d0, d1, d2, matrix4f2);
		this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
		this.renderChunkLayer(RenderType.cutout(), arg, d0, d1, d2, matrix4f2);
		if (this.level.effects().constantAmbientLight()) {
			Lighting.setupNetherLevel(arg.last().pose());
		} else {
			Lighting.setupLevel(arg.last().pose());
		}

		profilerfiller.popPush("entities");
		this.renderedEntities = 0;
		this.culledEntities = 0;
		if (this.itemEntityTarget != null) {
			this.itemEntityTarget.clear(Minecraft.ON_OSX);
			this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
			this.minecraft.getMainRenderTarget().bindWrite(false);
		}

		if (this.weatherTarget != null) {
			this.weatherTarget.clear(Minecraft.ON_OSX);
		}

		if (this.shouldShowEntityOutlines()) {
			this.entityTarget.clear(Minecraft.ON_OSX);
			this.minecraft.getMainRenderTarget().bindWrite(false);
		}

		boolean flag2 = false;
		MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

		for (Entity entity : this.level.entitiesForRendering()) {
			if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
				BlockPos blockpos = entity.blockPosition();
				if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isChunkCompiled(blockpos))
					&& (entity != arg2.getEntity() || arg2.isDetached() || arg2.getEntity() instanceof LivingEntity && ((LivingEntity)arg2.getEntity()).isSleeping())
					&& (!(entity instanceof LocalPlayer) || arg2.getEntity() == entity || entity == this.minecraft.player && !this.minecraft.player.isSpectator())) {
					this.renderedEntities++;
					if (entity.tickCount == 0) {
						entity.xOld = entity.getX();
						entity.yOld = entity.getY();
						entity.zOld = entity.getZ();
					}

					MultiBufferSource multibuffersource;
					if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity)) {
						flag2 = true;
						OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
						multibuffersource = outlinebuffersource;
						int i = entity.getTeamColor();
						outlinebuffersource.setColor(FastColor.ARGB32.red(i), FastColor.ARGB32.green(i), FastColor.ARGB32.blue(i), 255);
					} else {
						if (this.shouldShowEntityOutlines() && entity.hasCustomOutlineRendering(this.minecraft.player)) {
							flag2 = true;
						}

						multibuffersource = multibuffersource$buffersource;
					}

					this.renderEntity(entity, d0, d1, d2, g, arg, multibuffersource);
				}
			}
		}

		multibuffersource$buffersource.endLastBatch();
		this.checkPoseStack(arg);
		multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
		multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
		multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
		multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
		ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_ENTITIES, this, arg, matrix4f2, this.ticks, arg2, frustum);
		profilerfiller.popPush("blockentities");

		for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
			List<BlockEntity> list = levelrenderer$renderchunkinfo.chunk.getCompiledChunk().getRenderableBlockEntities();
			if (!list.isEmpty()) {
				for (BlockEntity blockentity1 : list) {
					if (frustum.isVisible(blockentity1.getRenderBoundingBox())) {
						BlockPos blockpos4 = blockentity1.getBlockPos();
						MultiBufferSource multibuffersource1 = multibuffersource$buffersource;
						arg.pushPose();
						arg.translate((double)blockpos4.getX() - d0, (double)blockpos4.getY() - d1, (double)blockpos4.getZ() - d2);
						SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos4.asLong());
						if (sortedset != null && !sortedset.isEmpty()) {
							int j = ((BlockDestructionProgress)sortedset.last()).getProgress();
							if (j >= 0) {
								PoseStack.Pose posestack$pose = arg.last();
								VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
									this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(j)),
									posestack$pose.pose(),
									posestack$pose.normal(),
									1.0F
								);
								multibuffersource1 = arg3x -> {
									VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(arg3x);
									return arg3x.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
								};
							}
						}

						if (this.shouldShowEntityOutlines() && blockentity1.hasCustomOutlineRendering(this.minecraft.player)) {
							flag2 = true;
						}

						this.blockEntityRenderDispatcher.render(blockentity1, g, arg, multibuffersource1);
						arg.popPose();
					}
				}
			}
		}

		synchronized (this.globalBlockEntities) {
			for (BlockEntity blockentity : this.globalBlockEntities) {
				if (frustum.isVisible(blockentity.getRenderBoundingBox())) {
					BlockPos blockpos3 = blockentity.getBlockPos();
					arg.pushPose();
					arg.translate((double)blockpos3.getX() - d0, (double)blockpos3.getY() - d1, (double)blockpos3.getZ() - d2);
					if (this.shouldShowEntityOutlines() && blockentity.hasCustomOutlineRendering(this.minecraft.player)) {
						flag2 = true;
					}

					this.blockEntityRenderDispatcher.render(blockentity, g, arg, multibuffersource$buffersource);
					arg.popPose();
				}
			}
		}

		this.checkPoseStack(arg);
		multibuffersource$buffersource.endBatch(RenderType.solid());
		multibuffersource$buffersource.endBatch(RenderType.endPortal());
		multibuffersource$buffersource.endBatch(RenderType.endGateway());
		multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
		multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
		multibuffersource$buffersource.endBatch(Sheets.bedSheet());
		multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
		multibuffersource$buffersource.endBatch(Sheets.signSheet());
		multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
		multibuffersource$buffersource.endBatch(Sheets.chestSheet());
		this.renderBuffers.outlineBufferSource().endOutlineBatch();
		if (flag2) {
			this.entityEffect.process(g);
			this.minecraft.getMainRenderTarget().bindWrite(false);
		}

		ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES, this, arg, matrix4f2, this.ticks, arg2, frustum);
		profilerfiller.popPush("destroyProgress");

		for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
			BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
			double d3 = (double)blockpos2.getX() - d0;
			double d4 = (double)blockpos2.getY() - d1;
			double d5 = (double)blockpos2.getZ() - d2;
			if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0)) {
				SortedSet<BlockDestructionProgress> sortedset1 = (SortedSet<BlockDestructionProgress>)entry.getValue();
				if (sortedset1 != null && !sortedset1.isEmpty()) {
					int k = ((BlockDestructionProgress)sortedset1.last()).getProgress();
					arg.pushPose();
					arg.translate((double)blockpos2.getX() - d0, (double)blockpos2.getY() - d1, (double)blockpos2.getZ() - d2);
					PoseStack.Pose posestack$pose1 = arg.last();
					VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(
						this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(k)),
						posestack$pose1.pose(),
						posestack$pose1.normal(),
						1.0F
					);
					ModelData modelData = this.level.getModelDataManager().getAt(blockpos2);
					this.minecraft
						.getBlockRenderer()
						.renderBreakingTexture(this.level.getBlockState(blockpos2), blockpos2, this.level, arg, vertexconsumer1, modelData == null ? ModelData.EMPTY : modelData);
					arg.popPose();
				}
			}
		}

		this.checkPoseStack(arg);
		HitResult hitresult = this.minecraft.hitResult;
		if (bl && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
			profilerfiller.popPush("outline");
			BlockPos blockpos1 = ((BlockHitResult)hitresult).getBlockPos();
			BlockState blockstate = this.level.getBlockState(blockpos1);
			if (!ForgeHooksClient.onDrawHighlight(this, arg2, hitresult, g, arg, multibuffersource$buffersource)
				&& !blockstate.isAir()
				&& this.level.getWorldBorder().isWithinBounds(blockpos1)) {
				VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
				this.renderHitOutline(arg, vertexconsumer2, arg2.getEntity(), d0, d1, d2, blockpos1, blockstate);
			}
		} else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
			ForgeHooksClient.onDrawHighlight(this, arg2, hitresult, g, arg, multibuffersource$buffersource);
		}

		this.minecraft.debugRenderer.render(arg, multibuffersource$buffersource, d0, d1, d2);
		multibuffersource$buffersource.endLastBatch();
		PoseStack posestack = RenderSystem.getModelViewStack();
		RenderSystem.applyModelViewMatrix();
		multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
		multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
		multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
		multibuffersource$buffersource.endBatch(RenderType.armorGlint());
		multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
		multibuffersource$buffersource.endBatch(RenderType.glint());
		multibuffersource$buffersource.endBatch(RenderType.glintDirect());
		multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
		multibuffersource$buffersource.endBatch(RenderType.entityGlint());
		multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
		multibuffersource$buffersource.endBatch(RenderType.waterMask());
		this.renderBuffers.crumblingBufferSource().endBatch();
		if (this.transparencyChain != null) {
			multibuffersource$buffersource.endBatch(RenderType.lines());
			multibuffersource$buffersource.endBatch();
			this.translucentTarget.clear(Minecraft.ON_OSX);
			this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
			profilerfiller.popPush("translucent");
			this.renderChunkLayer(RenderType.translucent(), arg, d0, d1, d2, matrix4f2);
			profilerfiller.popPush("string");
			this.renderChunkLayer(RenderType.tripwire(), arg, d0, d1, d2, matrix4f2);
			this.particlesTarget.clear(Minecraft.ON_OSX);
			this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
			RenderStateShard.PARTICLES_TARGET.setupRenderState();
			profilerfiller.popPush("particles");
			this.minecraft.particleEngine.render(arg, multibuffersource$buffersource, arg4, arg2, g, frustum);
			ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_PARTICLES, this, arg, matrix4f2, this.ticks, arg2, frustum);
			RenderStateShard.PARTICLES_TARGET.clearRenderState();
		} else {
			profilerfiller.popPush("translucent");
			if (this.translucentTarget != null) {
				this.translucentTarget.clear(Minecraft.ON_OSX);
			}

			this.renderChunkLayer(RenderType.translucent(), arg, d0, d1, d2, matrix4f2);
			multibuffersource$buffersource.endBatch(RenderType.lines());
			multibuffersource$buffersource.endBatch();
			profilerfiller.popPush("string");
			this.renderChunkLayer(RenderType.tripwire(), arg, d0, d1, d2, matrix4f2);
			profilerfiller.popPush("particles");
			this.minecraft.particleEngine.render(arg, multibuffersource$buffersource, arg4, arg2, g, frustum);
			ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_PARTICLES, this, arg, matrix4f2, this.ticks, arg2, frustum);
		}

		posestack.pushPose();
		posestack.mulPoseMatrix(arg.last().pose());
		RenderSystem.applyModelViewMatrix();
		if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
			if (this.transparencyChain != null) {
				this.cloudsTarget.clear(Minecraft.ON_OSX);
				RenderStateShard.CLOUDS_TARGET.setupRenderState();
				profilerfiller.popPush("clouds");
				this.renderClouds(arg, matrix4f2, g, d0, d1, d2);
				RenderStateShard.CLOUDS_TARGET.clearRenderState();
			} else {
				profilerfiller.popPush("clouds");
				RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
				this.renderClouds(arg, matrix4f2, g, d0, d1, d2);
			}
		}

		if (this.transparencyChain != null) {
			RenderStateShard.WEATHER_TARGET.setupRenderState();
			profilerfiller.popPush("weather");
			this.renderSnowAndRain(arg4, g, d0, d1, d2);
			ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_WEATHER, this, arg, matrix4f2, this.ticks, arg2, frustum);
			this.renderWorldBorder(arg2);
			RenderStateShard.WEATHER_TARGET.clearRenderState();
			this.transparencyChain.process(g);
			this.minecraft.getMainRenderTarget().bindWrite(false);
		} else {
			RenderSystem.depthMask(false);
			profilerfiller.popPush("weather");
			this.renderSnowAndRain(arg4, g, d0, d1, d2);
			ForgeHooksClient.dispatchRenderStage(RenderLevelStageEvent.Stage.AFTER_WEATHER, this, arg, matrix4f2, this.ticks, arg2, frustum);
			this.renderWorldBorder(arg2);
			RenderSystem.depthMask(true);
		}

		posestack.popPose();
		RenderSystem.applyModelViewMatrix();
		this.renderDebug(arg, multibuffersource$buffersource, arg2);
		multibuffersource$buffersource.endLastBatch();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		FogRenderer.setupNoFog();
	}

	private void checkPoseStack(PoseStack arg) {
		if (!arg.clear()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderEntity(Entity arg, double d, double e, double g, float h, PoseStack arg2, MultiBufferSource arg3) {
		double d0 = Mth.lerp((double)h, arg.xOld, arg.getX());
		double d1 = Mth.lerp((double)h, arg.yOld, arg.getY());
		double d2 = Mth.lerp((double)h, arg.zOld, arg.getZ());
		float f = Mth.lerp(h, arg.yRotO, arg.getYRot());
		this.entityRenderDispatcher.render(arg, d0 - d, d1 - e, d2 - g, f, h, arg2, arg3, this.entityRenderDispatcher.getPackedLightCoords(arg, h));
	}

	private void renderChunkLayer(RenderType arg, PoseStack arg2, double d, double e, double f, Matrix4f matrix4f) {
		RenderSystem.assertOnRenderThread();
		arg.setupRenderState();
		if (arg == RenderType.translucent()) {
			this.minecraft.getProfiler().push("translucent_sort");
			double d0 = d - this.xTransparentOld;
			double d1 = e - this.yTransparentOld;
			double d2 = f - this.zTransparentOld;
			if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0) {
				int j = SectionPos.posToSectionCoord(d);
				int k = SectionPos.posToSectionCoord(e);
				int l = SectionPos.posToSectionCoord(f);
				boolean flag = j != SectionPos.posToSectionCoord(this.xTransparentOld)
					|| l != SectionPos.posToSectionCoord(this.zTransparentOld)
					|| k != SectionPos.posToSectionCoord(this.yTransparentOld);
				this.xTransparentOld = d;
				this.yTransparentOld = e;
				this.zTransparentOld = f;
				int i1 = 0;

				for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
					if (i1 < 15
						&& (flag || levelrenderer$renderchunkinfo.isAxisAlignedWith(j, k, l))
						&& levelrenderer$renderchunkinfo.chunk.resortTransparency(arg, this.chunkRenderDispatcher)) {
						i1++;
					}
				}
			}

			this.minecraft.getProfiler().pop();
		}

		this.minecraft.getProfiler().push("filterempty");
		this.minecraft.getProfiler().popPush((Supplier<String>)(() -> "render_" + arg));
		boolean flag1 = arg != RenderType.translucent();
		ObjectListIterator<LevelRenderer.RenderChunkInfo> objectlistiterator = this.renderChunksInFrustum.listIterator(flag1 ? 0 : this.renderChunksInFrustum.size());
		ShaderInstance shaderinstance = RenderSystem.getShader();

		for (int i = 0; i < 12; i++) {
			int j1 = RenderSystem.getShaderTexture(i);
			shaderinstance.setSampler("Sampler" + i, j1);
		}

		if (shaderinstance.MODEL_VIEW_MATRIX != null) {
			shaderinstance.MODEL_VIEW_MATRIX.set(arg2.last().pose());
		}

		if (shaderinstance.PROJECTION_MATRIX != null) {
			shaderinstance.PROJECTION_MATRIX.set(matrix4f);
		}

		if (shaderinstance.COLOR_MODULATOR != null) {
			shaderinstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		}

		if (shaderinstance.GLINT_ALPHA != null) {
			shaderinstance.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
		}

		if (shaderinstance.FOG_START != null) {
			shaderinstance.FOG_START.set(RenderSystem.getShaderFogStart());
		}

		if (shaderinstance.FOG_END != null) {
			shaderinstance.FOG_END.set(RenderSystem.getShaderFogEnd());
		}

		if (shaderinstance.FOG_COLOR != null) {
			shaderinstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		}

		if (shaderinstance.FOG_SHAPE != null) {
			shaderinstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		}

		if (shaderinstance.TEXTURE_MATRIX != null) {
			shaderinstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		}

		if (shaderinstance.GAME_TIME != null) {
			shaderinstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
		}

		RenderSystem.setupShaderLights(shaderinstance);
		shaderinstance.apply();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		while (flag1 ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
			LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo1 = flag1
				? (LevelRenderer.RenderChunkInfo)objectlistiterator.next()
				: objectlistiterator.previous();
			ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo1.chunk;
			if (!chunkrenderdispatcher$renderchunk.getCompiledChunk().isEmpty(arg)) {
				VertexBuffer vertexbuffer = chunkrenderdispatcher$renderchunk.getBuffer(arg);
				BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
				if (uniform != null) {
					uniform.set((float)((double)blockpos.getX() - d), (float)((double)blockpos.getY() - e), (float)((double)blockpos.getZ() - f));
					uniform.upload();
				}

				vertexbuffer.bind();
				vertexbuffer.draw();
			}
		}

		if (uniform != null) {
			uniform.set(0.0F, 0.0F, 0.0F);
		}

		shaderinstance.clear();
		VertexBuffer.unbind();
		this.minecraft.getProfiler().pop();
		ForgeHooksClient.dispatchRenderStage(arg, this, arg2, matrix4f, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.getFrustum());
		arg.clearRenderState();
	}

	private void renderDebug(PoseStack arg, MultiBufferSource arg2, Camera arg3) {
		if (this.minecraft.chunkPath || this.minecraft.chunkVisibility) {
			double d0 = arg3.getPosition().x();
			double d1 = arg3.getPosition().y();
			double d2 = arg3.getPosition().z();

			for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
				ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
				BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
				arg.pushPose();
				arg.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
				Matrix4f matrix4f = arg.last().pose();
				if (this.minecraft.chunkPath) {
					VertexConsumer vertexconsumer1 = arg2.getBuffer(RenderType.lines());
					int i = levelrenderer$renderchunkinfo.step == 0 ? 0 : Mth.hsvToRgb((float)levelrenderer$renderchunkinfo.step / 50.0F, 0.9F, 0.9F);
					int j = i >> 16 & 0xFF;
					int k = i >> 8 & 0xFF;
					int l = i & 0xFF;

					for (int i1 = 0; i1 < DIRECTIONS.length; i1++) {
						if (levelrenderer$renderchunkinfo.hasSourceDirection(i1)) {
							Direction direction = DIRECTIONS[i1];
							vertexconsumer1.vertex(matrix4f, 8.0F, 8.0F, 8.0F)
								.color(j, k, l, 255)
								.normal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ())
								.endVertex();
							vertexconsumer1.vertex(matrix4f, (float)(8 - 16 * direction.getStepX()), (float)(8 - 16 * direction.getStepY()), (float)(8 - 16 * direction.getStepZ()))
								.color(j, k, l, 255)
								.normal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ())
								.endVertex();
						}
					}
				}

				if (this.minecraft.chunkVisibility && !chunkrenderdispatcher$renderchunk.getCompiledChunk().hasNoRenderableLayers()) {
					VertexConsumer vertexconsumer3 = arg2.getBuffer(RenderType.lines());
					int j1 = 0;

					for (Direction direction2 : DIRECTIONS) {
						for (Direction direction1 : DIRECTIONS) {
							boolean flag = chunkrenderdispatcher$renderchunk.getCompiledChunk().facesCanSeeEachother(direction2, direction1);
							if (!flag) {
								j1++;
								vertexconsumer3.vertex(matrix4f, (float)(8 + 8 * direction2.getStepX()), (float)(8 + 8 * direction2.getStepY()), (float)(8 + 8 * direction2.getStepZ()))
									.color(255, 0, 0, 255)
									.normal((float)direction2.getStepX(), (float)direction2.getStepY(), (float)direction2.getStepZ())
									.endVertex();
								vertexconsumer3.vertex(matrix4f, (float)(8 + 8 * direction1.getStepX()), (float)(8 + 8 * direction1.getStepY()), (float)(8 + 8 * direction1.getStepZ()))
									.color(255, 0, 0, 255)
									.normal((float)direction1.getStepX(), (float)direction1.getStepY(), (float)direction1.getStepZ())
									.endVertex();
							}
						}
					}

					if (j1 > 0) {
						VertexConsumer vertexconsumer4 = arg2.getBuffer(RenderType.debugQuads());
						float f = 0.5F;
						float f1 = 0.2F;
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 0.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 15.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 15.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
						vertexconsumer4.vertex(matrix4f, 0.5F, 0.5F, 15.5F).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
					}
				}

				arg.popPose();
			}
		}

		if (this.capturedFrustum != null) {
			arg.pushPose();
			arg.translate(
				(float)(this.frustumPos.x - arg3.getPosition().x), (float)(this.frustumPos.y - arg3.getPosition().y), (float)(this.frustumPos.z - arg3.getPosition().z)
			);
			Matrix4f matrix4f1 = arg.last().pose();
			VertexConsumer vertexconsumer = arg2.getBuffer(RenderType.debugQuads());
			this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 2, 3, 0, 1, 1);
			this.addFrustumQuad(vertexconsumer, matrix4f1, 4, 5, 6, 7, 1, 0, 0);
			this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 5, 4, 1, 1, 0);
			this.addFrustumQuad(vertexconsumer, matrix4f1, 2, 3, 7, 6, 0, 0, 1);
			this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 4, 7, 3, 0, 1, 0);
			this.addFrustumQuad(vertexconsumer, matrix4f1, 1, 5, 6, 2, 1, 0, 1);
			VertexConsumer vertexconsumer2 = arg2.getBuffer(RenderType.lines());
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
			this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
			arg.popPose();
		}
	}

	private void addFrustumVertex(VertexConsumer arg, Matrix4f matrix4f, int i) {
		arg.vertex(matrix4f, this.frustumPoints[i].x(), this.frustumPoints[i].y(), this.frustumPoints[i].z())
			.color(0, 0, 0, 255)
			.normal(0.0F, 0.0F, -1.0F)
			.endVertex();
	}

	private void addFrustumQuad(VertexConsumer arg, Matrix4f matrix4f, int i, int j, int k, int l, int m, int n, int o) {
		float f = 0.25F;
		arg.vertex(matrix4f, this.frustumPoints[i].x(), this.frustumPoints[i].y(), this.frustumPoints[i].z()).color((float)m, (float)n, (float)o, 0.25F).endVertex();
		arg.vertex(matrix4f, this.frustumPoints[j].x(), this.frustumPoints[j].y(), this.frustumPoints[j].z()).color((float)m, (float)n, (float)o, 0.25F).endVertex();
		arg.vertex(matrix4f, this.frustumPoints[k].x(), this.frustumPoints[k].y(), this.frustumPoints[k].z()).color((float)m, (float)n, (float)o, 0.25F).endVertex();
		arg.vertex(matrix4f, this.frustumPoints[l].x(), this.frustumPoints[l].y(), this.frustumPoints[l].z()).color((float)m, (float)n, (float)o, 0.25F).endVertex();
	}

	public void captureFrustum() {
		this.captureFrustum = true;
	}

	public void killFrustum() {
		this.capturedFrustum = null;
	}

	public void tick() {
		this.ticks++;
		if (this.ticks % 20 == 0) {
			Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

			while (iterator.hasNext()) {
				BlockDestructionProgress blockdestructionprogress = (BlockDestructionProgress)iterator.next();
				int i = blockdestructionprogress.getUpdatedRenderTick();
				if (this.ticks - i > 400) {
					iterator.remove();
					this.removeProgress(blockdestructionprogress);
				}
			}
		}
	}

	private void removeProgress(BlockDestructionProgress arg) {
		long i = arg.getPos().asLong();
		Set<BlockDestructionProgress> set = (Set<BlockDestructionProgress>)this.destructionProgress.get(i);
		set.remove(arg);
		if (set.isEmpty()) {
			this.destructionProgress.remove(i);
		}
	}

	private void renderEndSky(PoseStack arg) {
		RenderSystem.enableBlend();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();

		for (int i = 0; i < 6; i++) {
			arg.pushPose();
			if (i == 1) {
				arg.mulPose(Axis.XP.rotationDegrees(90.0F));
			}

			if (i == 2) {
				arg.mulPose(Axis.XP.rotationDegrees(-90.0F));
			}

			if (i == 3) {
				arg.mulPose(Axis.XP.rotationDegrees(180.0F));
			}

			if (i == 4) {
				arg.mulPose(Axis.ZP.rotationDegrees(90.0F));
			}

			if (i == 5) {
				arg.mulPose(Axis.ZP.rotationDegrees(-90.0F));
			}

			Matrix4f matrix4f = arg.last().pose();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).color(40, 40, 40, 255).endVertex();
			bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).color(40, 40, 40, 255).endVertex();
			tesselator.end();
			arg.popPose();
		}

		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	public void renderSky(PoseStack arg, Matrix4f matrix4f2, float g, Camera arg2, boolean bl, Runnable runnable) {
		if (!this.level.effects().renderSky(this.level, this.ticks, g, arg, arg2, matrix4f2, bl, runnable)) {
			runnable.run();
			if (!bl) {
				FogType fogtype = arg2.getFluidInCamera();
				if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(arg2)) {
					if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
						this.renderEndSky(arg);
					} else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
						Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), g);
						float f = (float)vec3.x;
						float f1 = (float)vec3.y;
						float f2 = (float)vec3.z;
						FogRenderer.levelFogColor();
						BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
						RenderSystem.depthMask(false);
						RenderSystem.setShaderColor(f, f1, f2, 1.0F);
						ShaderInstance shaderinstance = RenderSystem.getShader();
						this.skyBuffer.bind();
						this.skyBuffer.drawWithShader(arg.last().pose(), matrix4f2, shaderinstance);
						VertexBuffer.unbind();
						RenderSystem.enableBlend();
						float[] afloat = this.level.effects().getSunriseColor(this.level.getTimeOfDay(g), g);
						if (afloat != null) {
							RenderSystem.setShader(GameRenderer::getPositionColorShader);
							RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
							arg.pushPose();
							arg.mulPose(Axis.XP.rotationDegrees(90.0F));
							float f3 = Mth.sin(this.level.getSunAngle(g)) < 0.0F ? 180.0F : 0.0F;
							arg.mulPose(Axis.ZP.rotationDegrees(f3));
							arg.mulPose(Axis.ZP.rotationDegrees(90.0F));
							float f4 = afloat[0];
							float f5 = afloat[1];
							float f6 = afloat[2];
							Matrix4f matrix4f = arg.last().pose();
							bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
							bufferbuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(f4, f5, f6, afloat[3]).endVertex();
							int i = 16;

							for (int j = 0; j <= 16; j++) {
								float f7 = (float)j * (float) (Math.PI * 2) / 16.0F;
								float f8 = Mth.sin(f7);
								float f9 = Mth.cos(f7);
								bufferbuilder.vertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
							}

							BufferUploader.drawWithShader(bufferbuilder.end());
							arg.popPose();
						}

						RenderSystem.blendFuncSeparate(
							GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
						);
						arg.pushPose();
						float f11 = 1.0F - this.level.getRainLevel(g);
						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f11);
						arg.mulPose(Axis.YP.rotationDegrees(-90.0F));
						arg.mulPose(Axis.XP.rotationDegrees(this.level.getTimeOfDay(g) * 360.0F));
						Matrix4f matrix4f1 = arg.last().pose();
						float f12 = 30.0F;
						RenderSystem.setShader(GameRenderer::getPositionTexShader);
						RenderSystem.setShaderTexture(0, SUN_LOCATION);
						bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
						bufferbuilder.vertex(matrix4f1, -f12, 100.0F, -f12).uv(0.0F, 0.0F).endVertex();
						bufferbuilder.vertex(matrix4f1, f12, 100.0F, -f12).uv(1.0F, 0.0F).endVertex();
						bufferbuilder.vertex(matrix4f1, f12, 100.0F, f12).uv(1.0F, 1.0F).endVertex();
						bufferbuilder.vertex(matrix4f1, -f12, 100.0F, f12).uv(0.0F, 1.0F).endVertex();
						BufferUploader.drawWithShader(bufferbuilder.end());
						f12 = 20.0F;
						RenderSystem.setShaderTexture(0, MOON_LOCATION);
						int k = this.level.getMoonPhase();
						int l = k % 4;
						int i1 = k / 4 % 2;
						float f13 = (float)(l + 0) / 4.0F;
						float f14 = (float)(i1 + 0) / 2.0F;
						float f15 = (float)(l + 1) / 4.0F;
						float f16 = (float)(i1 + 1) / 2.0F;
						bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
						bufferbuilder.vertex(matrix4f1, -f12, -100.0F, f12).uv(f15, f16).endVertex();
						bufferbuilder.vertex(matrix4f1, f12, -100.0F, f12).uv(f13, f16).endVertex();
						bufferbuilder.vertex(matrix4f1, f12, -100.0F, -f12).uv(f13, f14).endVertex();
						bufferbuilder.vertex(matrix4f1, -f12, -100.0F, -f12).uv(f15, f14).endVertex();
						BufferUploader.drawWithShader(bufferbuilder.end());
						float f10 = this.level.getStarBrightness(g) * f11;
						if (f10 > 0.0F) {
							RenderSystem.setShaderColor(f10, f10, f10, f10);
							FogRenderer.setupNoFog();
							this.starBuffer.bind();
							this.starBuffer.drawWithShader(arg.last().pose(), matrix4f2, GameRenderer.getPositionShader());
							VertexBuffer.unbind();
							runnable.run();
						}

						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						RenderSystem.disableBlend();
						RenderSystem.defaultBlendFunc();
						arg.popPose();
						RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
						double d0 = this.minecraft.player.getEyePosition(g).y - this.level.getLevelData().getHorizonHeight(this.level);
						if (d0 < 0.0) {
							arg.pushPose();
							arg.translate(0.0F, 12.0F, 0.0F);
							this.darkBuffer.bind();
							this.darkBuffer.drawWithShader(arg.last().pose(), matrix4f2, shaderinstance);
							VertexBuffer.unbind();
							arg.popPose();
						}

						RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
						RenderSystem.depthMask(true);
					}
				}
			}
		}
	}

	private boolean doesMobEffectBlockSky(Camera arg) {
		return !(arg.getEntity() instanceof LivingEntity livingentity)
			? false
			: livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS);
	}

	public void renderClouds(PoseStack arg, Matrix4f matrix4f, float g, double d, double e, double h) {
		if (!this.level.effects().renderClouds(this.level, this.ticks, g, arg, d, e, h, matrix4f)) {
			float f = this.level.effects().getCloudHeight();
			if (!Float.isNaN(f)) {
				RenderSystem.disableCull();
				RenderSystem.enableBlend();
				RenderSystem.enableDepthTest();
				RenderSystem.blendFuncSeparate(
					GlStateManager.SourceFactor.SRC_ALPHA,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE,
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
				);
				RenderSystem.depthMask(true);
				float f1 = 12.0F;
				float f2 = 4.0F;
				double d0 = 2.0E-4;
				double d1 = (double)(((float)this.ticks + g) * 0.03F);
				double d2 = (d + d1) / 12.0;
				double d3 = (double)(f - (float)e + 0.33F);
				double d4 = h / 12.0 + 0.33F;
				d2 -= (double)(Mth.floor(d2 / 2048.0) * 2048);
				d4 -= (double)(Mth.floor(d4 / 2048.0) * 2048);
				float f3 = (float)(d2 - (double)Mth.floor(d2));
				float f4 = (float)(d3 / 4.0 - (double)Mth.floor(d3 / 4.0)) * 4.0F;
				float f5 = (float)(d4 - (double)Mth.floor(d4));
				Vec3 vec3 = this.level.getCloudColor(g);
				int i = (int)Math.floor(d2);
				int j = (int)Math.floor(d3 / 4.0);
				int k = (int)Math.floor(d4);
				if (i != this.prevCloudX
					|| j != this.prevCloudY
					|| k != this.prevCloudZ
					|| this.minecraft.options.getCloudsType() != this.prevCloudsType
					|| this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4) {
					this.prevCloudX = i;
					this.prevCloudY = j;
					this.prevCloudZ = k;
					this.prevCloudColor = vec3;
					this.prevCloudsType = this.minecraft.options.getCloudsType();
					this.generateClouds = true;
				}

				if (this.generateClouds) {
					this.generateClouds = false;
					BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
					if (this.cloudBuffer != null) {
						this.cloudBuffer.close();
					}

					this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
					BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = this.buildClouds(bufferbuilder, d2, d3, d4, vec3);
					this.cloudBuffer.bind();
					this.cloudBuffer.upload(bufferbuilder$renderedbuffer);
					VertexBuffer.unbind();
				}

				RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
				RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
				FogRenderer.levelFogColor();
				arg.pushPose();
				arg.scale(12.0F, 1.0F, 12.0F);
				arg.translate(-f3, f4, -f5);
				if (this.cloudBuffer != null) {
					this.cloudBuffer.bind();
					int l = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

					for (int i1 = l; i1 < 2; i1++) {
						if (i1 == 0) {
							RenderSystem.colorMask(false, false, false, false);
						} else {
							RenderSystem.colorMask(true, true, true, true);
						}

						ShaderInstance shaderinstance = RenderSystem.getShader();
						this.cloudBuffer.drawWithShader(arg.last().pose(), matrix4f, shaderinstance);
					}

					VertexBuffer.unbind();
				}

				arg.popPose();
				RenderSystem.enableCull();
				RenderSystem.disableBlend();
				RenderSystem.defaultBlendFunc();
			}
		}
	}

	private BufferBuilder.RenderedBuffer buildClouds(BufferBuilder arg, double d, double e, double g, Vec3 arg2) {
		float f = 4.0F;
		float f1 = 0.00390625F;
		int i = 8;
		int j = 4;
		float f2 = 9.765625E-4F;
		float f3 = (float)Mth.floor(d) * 0.00390625F;
		float f4 = (float)Mth.floor(g) * 0.00390625F;
		float f5 = (float)arg2.x;
		float f6 = (float)arg2.y;
		float f7 = (float)arg2.z;
		float f8 = f5 * 0.9F;
		float f9 = f6 * 0.9F;
		float f10 = f7 * 0.9F;
		float f11 = f5 * 0.7F;
		float f12 = f6 * 0.7F;
		float f13 = f7 * 0.7F;
		float f14 = f5 * 0.8F;
		float f15 = f6 * 0.8F;
		float f16 = f7 * 0.8F;
		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		arg.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		float f17 = (float)Math.floor(e / 4.0) * 4.0F;
		if (this.prevCloudsType == CloudStatus.FANCY) {
			for (int k = -3; k <= 4; k++) {
				for (int l = -3; l <= 4; l++) {
					float f18 = (float)(k * 8);
					float f19 = (float)(l * 8);
					if (f17 > -5.0F) {
						arg.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F))
							.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
							.color(f11, f12, f13, 0.8F)
							.normal(0.0F, -1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F))
							.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
							.color(f11, f12, f13, 0.8F)
							.normal(0.0F, -1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F))
							.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
							.color(f11, f12, f13, 0.8F)
							.normal(0.0F, -1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F))
							.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
							.color(f11, f12, f13, 0.8F)
							.normal(0.0F, -1.0F, 0.0F)
							.endVertex();
					}

					if (f17 <= 5.0F) {
						arg.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 8.0F))
							.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
							.color(f5, f6, f7, 0.8F)
							.normal(0.0F, 1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 8.0F))
							.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
							.color(f5, f6, f7, 0.8F)
							.normal(0.0F, 1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 0.0F))
							.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
							.color(f5, f6, f7, 0.8F)
							.normal(0.0F, 1.0F, 0.0F)
							.endVertex();
						arg.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 0.0F))
							.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
							.color(f5, f6, f7, 0.8F)
							.normal(0.0F, 1.0F, 0.0F)
							.endVertex();
					}

					if (k > -1) {
						for (int i1 = 0; i1 < 8; i1++) {
							arg.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F))
								.uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(-1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + 8.0F))
								.uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(-1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + 0.0F))
								.uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(-1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F))
								.uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(-1.0F, 0.0F, 0.0F)
								.endVertex();
						}
					}

					if (k <= 1) {
						for (int j2 = 0; j2 < 8; j2++) {
							arg.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 0.0F), (double)(f19 + 8.0F))
								.uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 4.0F), (double)(f19 + 8.0F))
								.uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 4.0F), (double)(f19 + 0.0F))
								.uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(1.0F, 0.0F, 0.0F)
								.endVertex();
							arg.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 0.0F), (double)(f19 + 0.0F))
								.uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
								.color(f8, f9, f10, 0.8F)
								.normal(1.0F, 0.0F, 0.0F)
								.endVertex();
						}
					}

					if (l > -1) {
						for (int k2 = 0; k2 < 8; k2++) {
							arg.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + (float)k2 + 0.0F))
								.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, -1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F), (double)(f19 + (float)k2 + 0.0F))
								.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, -1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + (float)k2 + 0.0F))
								.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, -1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + (float)k2 + 0.0F))
								.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, -1.0F)
								.endVertex();
						}
					}

					if (l <= 1) {
						for (int l2 = 0; l2 < 8; l2++) {
							arg.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F))
								.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, 1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F))
								.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, 1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F))
								.uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, 1.0F)
								.endVertex();
							arg.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F))
								.uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
								.color(f14, f15, f16, 0.8F)
								.normal(0.0F, 0.0F, 1.0F)
								.endVertex();
						}
					}
				}
			}
		} else {
			int j1 = 1;
			int k1 = 32;

			for (int l1 = -32; l1 < 32; l1 += 32) {
				for (int i2 = -32; i2 < 32; i2 += 32) {
					arg.vertex((double)(l1 + 0), (double)f17, (double)(i2 + 32))
						.uv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
						.color(f5, f6, f7, 0.8F)
						.normal(0.0F, -1.0F, 0.0F)
						.endVertex();
					arg.vertex((double)(l1 + 32), (double)f17, (double)(i2 + 32))
						.uv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
						.color(f5, f6, f7, 0.8F)
						.normal(0.0F, -1.0F, 0.0F)
						.endVertex();
					arg.vertex((double)(l1 + 32), (double)f17, (double)(i2 + 0))
						.uv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
						.color(f5, f6, f7, 0.8F)
						.normal(0.0F, -1.0F, 0.0F)
						.endVertex();
					arg.vertex((double)(l1 + 0), (double)f17, (double)(i2 + 0))
						.uv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
						.color(f5, f6, f7, 0.8F)
						.normal(0.0F, -1.0F, 0.0F)
						.endVertex();
				}
			}
		}

		return arg.end();
	}

	private void compileChunks(Camera arg) {
		this.minecraft.getProfiler().push("populate_chunks_to_compile");
		LevelLightEngine levellightengine = this.level.getLightEngine();
		RenderRegionCache renderregioncache = new RenderRegionCache();
		BlockPos blockpos = arg.getBlockPosition();
		List<ChunkRenderDispatcher.RenderChunk> list = Lists.<ChunkRenderDispatcher.RenderChunk>newArrayList();

		for (LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
			ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
			SectionPos sectionpos = SectionPos.of(chunkrenderdispatcher$renderchunk.getOrigin());
			if (chunkrenderdispatcher$renderchunk.isDirty() && levellightengine.lightOnInSection(sectionpos)) {
				boolean flag = false;
				if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
					BlockPos blockpos1 = chunkrenderdispatcher$renderchunk.getOrigin().offset(8, 8, 8);
					flag = !ForgeConfig.CLIENT.alwaysSetupTerrainOffThread.get()
						&& (blockpos1.distSqr(blockpos) < 768.0 || chunkrenderdispatcher$renderchunk.isDirtyFromPlayer());
				} else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
					flag = chunkrenderdispatcher$renderchunk.isDirtyFromPlayer();
				}

				if (flag) {
					this.minecraft.getProfiler().push("build_near_sync");
					this.chunkRenderDispatcher.rebuildChunkSync(chunkrenderdispatcher$renderchunk, renderregioncache);
					chunkrenderdispatcher$renderchunk.setNotDirty();
					this.minecraft.getProfiler().pop();
				} else {
					list.add(chunkrenderdispatcher$renderchunk);
				}
			}
		}

		this.minecraft.getProfiler().popPush("upload");
		this.chunkRenderDispatcher.uploadAllPendingUploads();
		this.minecraft.getProfiler().popPush("schedule_async_compile");

		for (ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 : list) {
			chunkrenderdispatcher$renderchunk1.rebuildChunkAsync(this.chunkRenderDispatcher, renderregioncache);
			chunkrenderdispatcher$renderchunk1.setNotDirty();
		}

		this.minecraft.getProfiler().pop();
	}

	private void renderWorldBorder(Camera arg) {
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		WorldBorder worldborder = this.level.getWorldBorder();
		double d0 = (double)(this.minecraft.options.getEffectiveRenderDistance() * 16);
		if (!(arg.getPosition().x < worldborder.getMaxX() - d0)
			|| !(arg.getPosition().x > worldborder.getMinX() + d0)
			|| !(arg.getPosition().z < worldborder.getMaxZ() - d0)
			|| !(arg.getPosition().z > worldborder.getMinZ() + d0)) {
			double d1 = 1.0 - worldborder.getDistanceToBorder(arg.getPosition().x, arg.getPosition().z) / d0;
			d1 = Math.pow(d1, 4.0);
			d1 = Mth.clamp(d1, 0.0, 1.0);
			double d2 = arg.getPosition().x;
			double d3 = arg.getPosition().z;
			double d4 = (double)this.minecraft.gameRenderer.getDepthFar();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.blendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
			);
			RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
			RenderSystem.depthMask(Minecraft.useShaderTransparency());
			PoseStack posestack = RenderSystem.getModelViewStack();
			posestack.pushPose();
			RenderSystem.applyModelViewMatrix();
			int i = worldborder.getStatus().getColor();
			float f = (float)(i >> 16 & 0xFF) / 255.0F;
			float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
			float f2 = (float)(i & 0xFF) / 255.0F;
			RenderSystem.setShaderColor(f, f1, f2, (float)d1);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.polygonOffset(-3.0F, -3.0F);
			RenderSystem.enablePolygonOffset();
			RenderSystem.disableCull();
			float f3 = (float)(Util.getMillis() % 3000L) / 3000.0F;
			float f4 = (float)(-Mth.frac(arg.getPosition().y * 0.5));
			float f5 = f4 + (float)d4;
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			double d5 = Math.max((double)Mth.floor(d3 - d0), worldborder.getMinZ());
			double d6 = Math.min((double)Mth.ceil(d3 + d0), worldborder.getMaxZ());
			float f6 = (float)(Mth.floor(d5) & 1) * 0.5F;
			if (d2 > worldborder.getMaxX() - d0) {
				float f7 = f6;

				for (double d7 = d5; d7 < d6; f7 += 0.5F) {
					double d8 = Math.min(1.0, d6 - d7);
					float f8 = (float)d8 * 0.5F;
					bufferbuilder.vertex(worldborder.getMaxX() - d2, -d4, d7 - d3).uv(f3 - f7, f3 + f5).endVertex();
					bufferbuilder.vertex(worldborder.getMaxX() - d2, -d4, d7 + d8 - d3).uv(f3 - (f8 + f7), f3 + f5).endVertex();
					bufferbuilder.vertex(worldborder.getMaxX() - d2, d4, d7 + d8 - d3).uv(f3 - (f8 + f7), f3 + f4).endVertex();
					bufferbuilder.vertex(worldborder.getMaxX() - d2, d4, d7 - d3).uv(f3 - f7, f3 + f4).endVertex();
					d7++;
				}
			}

			if (d2 < worldborder.getMinX() + d0) {
				float f9 = f6;

				for (double d9 = d5; d9 < d6; f9 += 0.5F) {
					double d12 = Math.min(1.0, d6 - d9);
					float f12 = (float)d12 * 0.5F;
					bufferbuilder.vertex(worldborder.getMinX() - d2, -d4, d9 - d3).uv(f3 + f9, f3 + f5).endVertex();
					bufferbuilder.vertex(worldborder.getMinX() - d2, -d4, d9 + d12 - d3).uv(f3 + f12 + f9, f3 + f5).endVertex();
					bufferbuilder.vertex(worldborder.getMinX() - d2, d4, d9 + d12 - d3).uv(f3 + f12 + f9, f3 + f4).endVertex();
					bufferbuilder.vertex(worldborder.getMinX() - d2, d4, d9 - d3).uv(f3 + f9, f3 + f4).endVertex();
					d9++;
				}
			}

			d5 = Math.max((double)Mth.floor(d2 - d0), worldborder.getMinX());
			d6 = Math.min((double)Mth.ceil(d2 + d0), worldborder.getMaxX());
			f6 = (float)(Mth.floor(d5) & 1) * 0.5F;
			if (d3 > worldborder.getMaxZ() - d0) {
				float f10 = f6;

				for (double d10 = d5; d10 < d6; f10 += 0.5F) {
					double d13 = Math.min(1.0, d6 - d10);
					float f13 = (float)d13 * 0.5F;
					bufferbuilder.vertex(d10 - d2, -d4, worldborder.getMaxZ() - d3).uv(f3 + f10, f3 + f5).endVertex();
					bufferbuilder.vertex(d10 + d13 - d2, -d4, worldborder.getMaxZ() - d3).uv(f3 + f13 + f10, f3 + f5).endVertex();
					bufferbuilder.vertex(d10 + d13 - d2, d4, worldborder.getMaxZ() - d3).uv(f3 + f13 + f10, f3 + f4).endVertex();
					bufferbuilder.vertex(d10 - d2, d4, worldborder.getMaxZ() - d3).uv(f3 + f10, f3 + f4).endVertex();
					d10++;
				}
			}

			if (d3 < worldborder.getMinZ() + d0) {
				float f11 = f6;

				for (double d11 = d5; d11 < d6; f11 += 0.5F) {
					double d14 = Math.min(1.0, d6 - d11);
					float f14 = (float)d14 * 0.5F;
					bufferbuilder.vertex(d11 - d2, -d4, worldborder.getMinZ() - d3).uv(f3 - f11, f3 + f5).endVertex();
					bufferbuilder.vertex(d11 + d14 - d2, -d4, worldborder.getMinZ() - d3).uv(f3 - (f14 + f11), f3 + f5).endVertex();
					bufferbuilder.vertex(d11 + d14 - d2, d4, worldborder.getMinZ() - d3).uv(f3 - (f14 + f11), f3 + f4).endVertex();
					bufferbuilder.vertex(d11 - d2, d4, worldborder.getMinZ() - d3).uv(f3 - f11, f3 + f4).endVertex();
					d11++;
				}
			}

			BufferUploader.drawWithShader(bufferbuilder.end());
			RenderSystem.enableCull();
			RenderSystem.polygonOffset(0.0F, 0.0F);
			RenderSystem.disablePolygonOffset();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			posestack.popPose();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.depthMask(true);
		}
	}

	private void renderHitOutline(PoseStack arg, VertexConsumer arg2, Entity arg3, double d, double e, double f, BlockPos arg4, BlockState arg5) {
		renderShape(
			arg,
			arg2,
			arg5.getShape(this.level, arg4, CollisionContext.of(arg3)),
			(double)arg4.getX() - d,
			(double)arg4.getY() - e,
			(double)arg4.getZ() - f,
			0.0F,
			0.0F,
			0.0F,
			0.4F
		);
	}

	private static Vec3 mixColor(float g) {
		float f = 5.99999F;
		int i = (int)(Mth.clamp(g, 0.0F, 1.0F) * 5.99999F);
		float f1 = g * 5.99999F - (float)i;

		return switch (i) {
			case 0 -> new Vec3(1.0, (double)f1, 0.0);
			case 1 -> new Vec3((double)(1.0F - f1), 1.0, 0.0);
			case 2 -> new Vec3(0.0, 1.0, (double)f1);
			case 3 -> new Vec3(0.0, 1.0 - (double)f1, 1.0);
			case 4 -> new Vec3((double)f1, 0.0, 1.0);
			case 5 -> new Vec3(1.0, 0.0, 1.0 - (double)f1);
			default -> throw new IllegalStateException("Unexpected value: " + i);
		};
	}

	private static Vec3 shiftHue(float f, float g, float h, float i) {
		Vec3 vec3 = mixColor(i).scale((double)f);
		Vec3 vec31 = mixColor((i + 0.33333334F) % 1.0F).scale((double)g);
		Vec3 vec32 = mixColor((i + 0.6666667F) % 1.0F).scale((double)h);
		Vec3 vec33 = vec3.add(vec31).add(vec32);
		double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
		return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
	}

	public static void renderVoxelShape(
		PoseStack arg, VertexConsumer arg2, VoxelShape arg3, double d, double e, double g, float h, float k, float l, float m, boolean bl
	) {
		List<AABB> list = arg3.toAabbs();
		if (!list.isEmpty()) {
			int i = bl ? list.size() : list.size() * 8;
			renderShape(arg, arg2, Shapes.create((AABB)list.get(0)), d, e, g, h, k, l, m);

			for (int j = 1; j < list.size(); j++) {
				AABB aabb = (AABB)list.get(j);
				float f = (float)j / (float)i;
				Vec3 vec3 = shiftHue(h, k, l, f);
				renderShape(arg, arg2, Shapes.create(aabb), d, e, g, (float)vec3.x, (float)vec3.y, (float)vec3.z, m);
			}
		}
	}

	private static void renderShape(PoseStack arg, VertexConsumer arg2, VoxelShape arg3, double d, double e, double f, float g, float h, float i, float j) {
		PoseStack.Pose posestack$pose = arg.last();
		arg3.forAllEdges((l, m, n, o, p, q) -> {
			float fx = (float)(o - l);
			float f1 = (float)(p - m);
			float f2 = (float)(q - n);
			float f3 = Mth.sqrt(fx * fx + f1 * f1 + f2 * f2);
			fx /= f3;
			f1 /= f3;
			f2 /= f3;
			arg2.vertex(posestack$pose.pose(), (float)(l + d), (float)(m + e), (float)(n + f)).color(g, h, i, j).normal(posestack$pose.normal(), fx, f1, f2).endVertex();
			arg2.vertex(posestack$pose.pose(), (float)(o + d), (float)(p + e), (float)(q + f)).color(g, h, i, j).normal(posestack$pose.normal(), fx, f1, f2).endVertex();
		});
	}

	public static void renderLineBox(VertexConsumer arg, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m) {
		renderLineBox(new PoseStack(), arg, d, e, f, g, h, i, j, k, l, m, j, k, l);
	}

	public static void renderLineBox(PoseStack arg, VertexConsumer arg2, AABB arg3, float f, float g, float h, float i) {
		renderLineBox(arg, arg2, arg3.minX, arg3.minY, arg3.minZ, arg3.maxX, arg3.maxY, arg3.maxZ, f, g, h, i, f, g, h);
	}

	public static void renderLineBox(
		PoseStack arg, VertexConsumer arg2, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m
	) {
		renderLineBox(arg, arg2, d, e, f, g, h, i, j, k, l, m, j, k, l);
	}

	public static void renderLineBox(
		PoseStack arg, VertexConsumer arg2, double d, double e, double g, double h, double i, double j, float k, float l, float m, float n, float o, float p, float q
	) {
		Matrix4f matrix4f = arg.last().pose();
		Matrix3f matrix3f = arg.last().normal();
		float f = (float)d;
		float f1 = (float)e;
		float f2 = (float)g;
		float f3 = (float)h;
		float f4 = (float)i;
		float f5 = (float)j;
		arg2.vertex(matrix4f, f, f1, f2).color(k, p, q, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f2).color(k, p, q, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f1, f2).color(o, l, q, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f2).color(o, l, q, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f1, f2).color(o, p, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		arg2.vertex(matrix4f, f, f1, f5).color(o, p, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f2).color(k, l, m, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f2).color(k, l, m, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f2).color(k, l, m, n).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f2).color(k, l, m, n).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f2).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f5).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f5).color(k, l, m, n).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f1, f5).color(k, l, m, n).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f, f1, f5).color(k, l, m, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f5).color(k, l, m, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f5).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f2).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
		arg2.vertex(matrix4f, f, f4, f5).color(k, l, m, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f5).color(k, l, m, n).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f1, f5).color(k, l, m, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f5).color(k, l, m, n).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f2).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		arg2.vertex(matrix4f, f3, f4, f5).color(k, l, m, n).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
	}

	public static void addChainedFilledBoxVertices(
		PoseStack arg, VertexConsumer arg2, double d, double e, double f, double g, double h, double i, float j, float k, float l, float m
	) {
		addChainedFilledBoxVertices(arg, arg2, (float)d, (float)e, (float)f, (float)g, (float)h, (float)i, j, k, l, m);
	}

	public static void addChainedFilledBoxVertices(
		PoseStack arg, VertexConsumer arg2, float f, float g, float h, float i, float j, float k, float l, float m, float n, float o
	) {
		Matrix4f matrix4f = arg.last().pose();
		arg2.vertex(matrix4f, f, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, g, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, f, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, h).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, k).color(l, m, n, o).endVertex();
		arg2.vertex(matrix4f, i, j, k).color(l, m, n, o).endVertex();
	}

	public void blockChanged(BlockGetter arg, BlockPos arg2, BlockState arg3, BlockState arg4, int i) {
		this.setBlockDirty(arg2, (i & 8) != 0);
	}

	private void setBlockDirty(BlockPos arg, boolean bl) {
		for (int i = arg.getZ() - 1; i <= arg.getZ() + 1; i++) {
			for (int j = arg.getX() - 1; j <= arg.getX() + 1; j++) {
				for (int k = arg.getY() - 1; k <= arg.getY() + 1; k++) {
					this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), bl);
				}
			}
		}
	}

	public void setBlocksDirty(int l, int m, int n, int o, int p, int q) {
		for (int i = n - 1; i <= q + 1; i++) {
			for (int j = l - 1; j <= o + 1; j++) {
				for (int k = m - 1; k <= p + 1; k++) {
					this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
				}
			}
		}
	}

	public void setBlockDirty(BlockPos arg, BlockState arg2, BlockState arg3) {
		if (this.minecraft.getModelManager().requiresRender(arg2, arg3)) {
			this.setBlocksDirty(arg.getX(), arg.getY(), arg.getZ(), arg.getX(), arg.getY(), arg.getZ());
		}
	}

	public void setSectionDirtyWithNeighbors(int l, int m, int n) {
		for (int i = n - 1; i <= n + 1; i++) {
			for (int j = l - 1; j <= l + 1; j++) {
				for (int k = m - 1; k <= m + 1; k++) {
					this.setSectionDirty(j, k, i);
				}
			}
		}
	}

	public void setSectionDirty(int i, int j, int k) {
		this.setSectionDirty(i, j, k, false);
	}

	private void setSectionDirty(int i, int j, int k, boolean bl) {
		this.viewArea.setDirty(i, j, k, bl);
	}

	public Frustum getFrustum() {
		return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
	}

	public int getTicks() {
		return this.ticks;
	}

	@Deprecated
	public void playStreamingMusic(@Nullable SoundEvent arg, BlockPos arg2) {
		this.playStreamingMusic(arg, arg2, arg == null ? null : RecordItem.getBySound(arg));
	}

	public void playStreamingMusic(@Nullable SoundEvent arg, BlockPos arg2, @Nullable RecordItem musicDiscItem) {
		SoundInstance soundinstance = (SoundInstance)this.playingRecords.get(arg2);
		if (soundinstance != null) {
			this.minecraft.getSoundManager().stop(soundinstance);
			this.playingRecords.remove(arg2);
		}

		if (arg != null) {
			if (musicDiscItem != null) {
				this.minecraft.gui.setNowPlaying(musicDiscItem.getDisplayName());
			}

			SoundInstance simplesoundinstance = SimpleSoundInstance.forRecord(arg, Vec3.atCenterOf(arg2));
			this.playingRecords.put(arg2, simplesoundinstance);
			this.minecraft.getSoundManager().play(simplesoundinstance);
		}

		this.notifyNearbyEntities(this.level, arg2, arg != null);
	}

	private void notifyNearbyEntities(Level arg, BlockPos arg2, boolean bl) {
		for (LivingEntity livingentity : arg.getEntitiesOfClass(LivingEntity.class, new AABB(arg2).inflate(3.0))) {
			livingentity.setRecordPlayingNearby(arg2, bl);
		}
	}

	public void addParticle(ParticleOptions arg, boolean bl, double d, double e, double f, double g, double h, double i) {
		this.addParticle(arg, bl, false, d, e, f, g, h, i);
	}

	public void addParticle(ParticleOptions arg, boolean bl, boolean bl2, double d, double e, double f, double g, double h, double i) {
		try {
			this.addParticleInternal(arg, bl, bl2, d, e, f, g, h, i);
		} catch (Throwable var19) {
			CrashReport crashreport = CrashReport.forThrowable(var19, "Exception while adding particle");
			CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
			crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(arg.getType()));
			crashreportcategory.setDetail("Parameters", arg.writeToString());
			crashreportcategory.setDetail("Position", (CrashReportDetail<String>)(() -> CrashReportCategory.formatLocation(this.level, d, e, f)));
			throw new ReportedException(crashreport);
		}
	}

	private <T extends ParticleOptions> void addParticle(T arg, double d, double e, double f, double g, double h, double i) {
		this.addParticle(arg, arg.getType().getOverrideLimiter(), d, e, f, g, h, i);
	}

	@Nullable
	private Particle addParticleInternal(ParticleOptions arg, boolean bl, double d, double e, double f, double g, double h, double i) {
		return this.addParticleInternal(arg, bl, false, d, e, f, g, h, i);
	}

	@Nullable
	private Particle addParticleInternal(ParticleOptions arg, boolean bl, boolean bl2, double d, double e, double f, double g, double h, double i) {
		Camera camera = this.minecraft.gameRenderer.getMainCamera();
		ParticleStatus particlestatus = this.calculateParticleLevel(bl2);
		if (bl) {
			return this.minecraft.particleEngine.createParticle(arg, d, e, f, g, h, i);
		} else if (camera.getPosition().distanceToSqr(d, e, f) > 1024.0) {
			return null;
		} else {
			return particlestatus == ParticleStatus.MINIMAL ? null : this.minecraft.particleEngine.createParticle(arg, d, e, f, g, h, i);
		}
	}

	private ParticleStatus calculateParticleLevel(boolean bl) {
		ParticleStatus particlestatus = this.minecraft.options.particles().get();
		if (bl && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
			particlestatus = ParticleStatus.DECREASED;
		}

		if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
			particlestatus = ParticleStatus.MINIMAL;
		}

		return particlestatus;
	}

	public void clear() {
	}

	public void globalLevelEvent(int i, BlockPos arg, int j) {
		switch (i) {
			case 1023:
			case 1028:
			case 1038:
				Camera camera = this.minecraft.gameRenderer.getMainCamera();
				if (camera.isInitialized()) {
					double d0 = (double)arg.getX() - camera.getPosition().x;
					double d1 = (double)arg.getY() - camera.getPosition().y;
					double d2 = (double)arg.getZ() - camera.getPosition().z;
					double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
					double d4 = camera.getPosition().x;
					double d5 = camera.getPosition().y;
					double d6 = camera.getPosition().z;
					if (d3 > 0.0) {
						d4 += d0 / d3 * 2.0;
						d5 += d1 / d3 * 2.0;
						d6 += d2 / d3 * 2.0;
					}

					if (i == 1023) {
						this.level.playLocalSound(d4, d5, d6, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
					} else if (i == 1038) {
						this.level.playLocalSound(d4, d5, d6, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
					} else {
						this.level.playLocalSound(d4, d5, d6, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
					}
				}
		}
	}

	public void levelEvent(int m, BlockPos arg, int n) {
		RandomSource randomsource = this.level.random;
		switch (m) {
			case 1000:
				this.level.playLocalSound(arg, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 1001:
				this.level.playLocalSound(arg, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
				break;
			case 1002:
				this.level.playLocalSound(arg, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
				break;
			case 1003:
				this.level.playLocalSound(arg, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
				break;
			case 1004:
				this.level.playLocalSound(arg, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
				break;
			case 1009:
				if (n == 0) {
					this.level
						.playLocalSound(arg, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false);
				} else if (n == 1) {
					this.level
						.playLocalSound(
							arg, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.4F, false
						);
				}
				break;
			case 1010:
				if (Item.byId(n) instanceof RecordItem recorditem) {
					this.playStreamingMusic(recorditem.getSound(), arg, recorditem);
				}
				break;
			case 1011:
				this.playStreamingMusic((SoundEvent)null, arg);
				break;
			case 1015:
				this.level
					.playLocalSound(arg, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1016:
				this.level
					.playLocalSound(arg, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1017:
				this.level
					.playLocalSound(
						arg, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1018:
				this.level
					.playLocalSound(arg, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1019:
				this.level
					.playLocalSound(
						arg, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1020:
				this.level
					.playLocalSound(
						arg, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1021:
				this.level
					.playLocalSound(
						arg, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1022:
				this.level
					.playLocalSound(arg, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1024:
				this.level
					.playLocalSound(arg, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1025:
				this.level
					.playLocalSound(arg, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1026:
				this.level
					.playLocalSound(arg, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false);
				break;
			case 1027:
				this.level
					.playLocalSound(
						arg, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1029:
				this.level.playLocalSound(arg, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1030:
				this.level.playLocalSound(arg, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1031:
				this.level.playLocalSound(arg, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1032:
				this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomsource.nextFloat() * 0.4F + 0.8F, 0.25F));
				break;
			case 1033:
				this.level.playLocalSound(arg, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 1034:
				this.level.playLocalSound(arg, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 1035:
				this.level.playLocalSound(arg, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 1039:
				this.level.playLocalSound(arg, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1040:
				this.level
					.playLocalSound(
						arg, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1041:
				this.level
					.playLocalSound(
						arg, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1042:
				this.level.playLocalSound(arg, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1043:
				this.level.playLocalSound(arg, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1044:
				this.level.playLocalSound(arg, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1045:
				this.level.playLocalSound(arg, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1046:
				this.level
					.playLocalSound(arg, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1047:
				this.level
					.playLocalSound(arg, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 1048:
				this.level
					.playLocalSound(
						arg, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
					);
				break;
			case 1500:
				ComposterBlock.handleFill(this.level, arg, n > 0);
				break;
			case 1501:
				this.level
					.playLocalSound(arg, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false);

				for (int j3 = 0; j3 < 8; j3++) {
					this.level
						.addParticle(
							ParticleTypes.LARGE_SMOKE,
							(double)arg.getX() + randomsource.nextDouble(),
							(double)arg.getY() + 1.2,
							(double)arg.getZ() + randomsource.nextDouble(),
							0.0,
							0.0,
							0.0
						);
				}
				break;
			case 1502:
				this.level
					.playLocalSound(
						arg, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
					);

				for (int i3 = 0; i3 < 5; i3++) {
					double d16 = (double)arg.getX() + randomsource.nextDouble() * 0.6 + 0.2;
					double d22 = (double)arg.getY() + randomsource.nextDouble() * 0.6 + 0.2;
					double d27 = (double)arg.getZ() + randomsource.nextDouble() * 0.6 + 0.2;
					this.level.addParticle(ParticleTypes.SMOKE, d16, d22, d27, 0.0, 0.0, 0.0);
				}
				break;
			case 1503:
				this.level.playLocalSound(arg, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

				for (int l2 = 0; l2 < 16; l2++) {
					double d15 = (double)arg.getX() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
					double d21 = (double)arg.getY() + 0.8125;
					double d26 = (double)arg.getZ() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
					this.level.addParticle(ParticleTypes.SMOKE, d15, d21, d26, 0.0, 0.0, 0.0);
				}
				break;
			case 1504:
				PointedDripstoneBlock.spawnDripParticle(this.level, arg, this.level.getBlockState(arg));
				break;
			case 1505:
				BoneMealItem.addGrowthParticles(this.level, arg, n);
				this.level.playLocalSound(arg, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 2000:
				Direction direction1 = Direction.from3DDataValue(n);
				int j = direction1.getStepX();
				int i1 = direction1.getStepY();
				int k1 = direction1.getStepZ();
				double d10 = (double)arg.getX() + (double)j * 0.6 + 0.5;
				double d14 = (double)arg.getY() + (double)i1 * 0.6 + 0.5;
				double d20 = (double)arg.getZ() + (double)k1 * 0.6 + 0.5;

				for (int i4 = 0; i4 < 10; i4++) {
					double d28 = randomsource.nextDouble() * 0.2 + 0.01;
					double d2 = d10 + (double)j * 0.01 + (randomsource.nextDouble() - 0.5) * (double)k1 * 0.5;
					double d3 = d14 + (double)i1 * 0.01 + (randomsource.nextDouble() - 0.5) * (double)i1 * 0.5;
					double d32 = d20 + (double)k1 * 0.01 + (randomsource.nextDouble() - 0.5) * (double)j * 0.5;
					double d4 = (double)j * d28 + randomsource.nextGaussian() * 0.01;
					double d5 = (double)i1 * d28 + randomsource.nextGaussian() * 0.01;
					double d6 = (double)k1 * d28 + randomsource.nextGaussian() * 0.01;
					this.addParticle(ParticleTypes.SMOKE, d2, d3, d32, d4, d5, d6);
				}
				break;
			case 2001:
				BlockState blockstate1 = Block.stateById(n);
				if (!blockstate1.isAir()) {
					SoundType soundtype = blockstate1.getSoundType(this.level, arg, null);
					this.level.playLocalSound(arg, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false);
				}

				this.level.addDestroyBlockEffect(arg, blockstate1);
				break;
			case 2002:
			case 2007:
				Vec3 vec3 = Vec3.atBottomCenterOf(arg);

				for (int i = 0; i < 8; i++) {
					this.addParticle(
						new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
						vec3.x,
						vec3.y,
						vec3.z,
						randomsource.nextGaussian() * 0.15,
						randomsource.nextDouble() * 0.2,
						randomsource.nextGaussian() * 0.15
					);
				}

				float f3 = (float)(n >> 16 & 0xFF) / 255.0F;
				float f4 = (float)(n >> 8 & 0xFF) / 255.0F;
				float f6 = (float)(n >> 0 & 0xFF) / 255.0F;
				ParticleOptions particleoptions = m == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

				for (int k2 = 0; k2 < 100; k2++) {
					double d13 = randomsource.nextDouble() * 4.0;
					double d19 = randomsource.nextDouble() * Math.PI * 2.0;
					double d25 = Math.cos(d19) * d13;
					double d30 = 0.01 + randomsource.nextDouble() * 0.5;
					double d31 = Math.sin(d19) * d13;
					Particle particle1 = this.addParticleInternal(
						particleoptions, particleoptions.getType().getOverrideLimiter(), vec3.x + d25 * 0.1, vec3.y + 0.3, vec3.z + d31 * 0.1, d25, d30, d31
					);
					if (particle1 != null) {
						float f2 = 0.75F + randomsource.nextFloat() * 0.25F;
						particle1.setColor(f3 * f2, f4 * f2, f6 * f2);
						particle1.setPower((float)d13);
					}
				}

				this.level.playLocalSound(arg, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
				break;
			case 2003:
				double d0 = (double)arg.getX() + 0.5;
				double d7 = (double)arg.getY();
				double d9 = (double)arg.getZ() + 0.5;

				for (int k3 = 0; k3 < 8; k3++) {
					this.addParticle(
						new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
						d0,
						d7,
						d9,
						randomsource.nextGaussian() * 0.15,
						randomsource.nextDouble() * 0.2,
						randomsource.nextGaussian() * 0.15
					);
				}

				for (double d12 = 0.0; d12 < Math.PI * 2; d12 += Math.PI / 20) {
					this.addParticle(ParticleTypes.PORTAL, d0 + Math.cos(d12) * 5.0, d7 - 0.4, d9 + Math.sin(d12) * 5.0, Math.cos(d12) * -5.0, 0.0, Math.sin(d12) * -5.0);
					this.addParticle(ParticleTypes.PORTAL, d0 + Math.cos(d12) * 5.0, d7 - 0.4, d9 + Math.sin(d12) * 5.0, Math.cos(d12) * -7.0, 0.0, Math.sin(d12) * -7.0);
				}
				break;
			case 2004:
				for (int l = 0; l < 20; l++) {
					double d8 = (double)arg.getX() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
					double d11 = (double)arg.getY() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
					double d17 = (double)arg.getZ() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
					this.level.addParticle(ParticleTypes.SMOKE, d8, d11, d17, 0.0, 0.0, 0.0);
					this.level.addParticle(ParticleTypes.FLAME, d8, d11, d17, 0.0, 0.0, 0.0);
				}
				break;
			case 2005:
				BoneMealItem.addGrowthParticles(this.level, arg, n);
				break;
			case 2006:
				for (int j2 = 0; j2 < 200; j2++) {
					float f10 = randomsource.nextFloat() * 4.0F;
					float f11 = randomsource.nextFloat() * (float) (Math.PI * 2);
					double d18 = (double)(Mth.cos(f11) * f10);
					double d24 = 0.01 + randomsource.nextDouble() * 0.5;
					double d29 = (double)(Mth.sin(f11) * f10);
					Particle particle = this.addParticleInternal(
						ParticleTypes.DRAGON_BREATH, false, (double)arg.getX() + d18 * 0.1, (double)arg.getY() + 0.3, (double)arg.getZ() + d29 * 0.1, d18, d24, d29
					);
					if (particle != null) {
						particle.setPower(f10);
					}
				}

				if (n == 1) {
					this.level.playLocalSound(arg, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
				}
				break;
			case 2008:
				this.level.addParticle(ParticleTypes.EXPLOSION, (double)arg.getX() + 0.5, (double)arg.getY() + 0.5, (double)arg.getZ() + 0.5, 0.0, 0.0, 0.0);
				break;
			case 2009:
				for (int i2 = 0; i2 < 8; i2++) {
					this.level
						.addParticle(
							ParticleTypes.CLOUD,
							(double)arg.getX() + randomsource.nextDouble(),
							(double)arg.getY() + 1.2,
							(double)arg.getZ() + randomsource.nextDouble(),
							0.0,
							0.0,
							0.0
						);
				}
				break;
			case 3000:
				this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double)arg.getX() + 0.5, (double)arg.getY() + 0.5, (double)arg.getZ() + 0.5, 0.0, 0.0, 0.0);
				this.level
					.playLocalSound(
						arg,
						SoundEvents.END_GATEWAY_SPAWN,
						SoundSource.BLOCKS,
						10.0F,
						(1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
						false
					);
				break;
			case 3001:
				this.level.playLocalSound(arg, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
				break;
			case 3002:
				if (n >= 0 && n < Direction.Axis.VALUES.length) {
					ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.VALUES[n], this.level, arg, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
				} else {
					ParticleUtils.spawnParticlesOnBlockFaces(this.level, arg, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
				}
				break;
			case 3003:
				ParticleUtils.spawnParticlesOnBlockFaces(this.level, arg, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
				this.level.playLocalSound(arg, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
				break;
			case 3004:
				ParticleUtils.spawnParticlesOnBlockFaces(this.level, arg, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
				break;
			case 3005:
				ParticleUtils.spawnParticlesOnBlockFaces(this.level, arg, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
				break;
			case 3006:
				int k = n >> 6;
				if (k > 0) {
					if (randomsource.nextFloat() < 0.3F + (float)k * 0.1F) {
						float f5 = 0.15F + 0.02F * (float)k * (float)k * randomsource.nextFloat();
						float f7 = 0.4F + 0.3F * (float)k * randomsource.nextFloat();
						this.level.playLocalSound(arg, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, f5, f7, false);
					}

					byte b0 = (byte)(n & 63);
					IntProvider intprovider = UniformInt.of(0, k);
					float f = 0.005F;
					Supplier<Vec3> supplier = () -> new Vec3(
							Mth.nextDouble(randomsource, -0.005F, 0.005F), Mth.nextDouble(randomsource, -0.005F, 0.005F), Mth.nextDouble(randomsource, -0.005F, 0.005F)
						);
					if (b0 == 0) {
						for (Direction direction : Direction.values()) {
							float f1 = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
							double d1 = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
							ParticleUtils.spawnParticlesOnBlockFace(this.level, arg, new SculkChargeParticleOptions(f1), intprovider, direction, supplier, d1);
						}
					} else {
						for (Direction direction2 : MultifaceBlock.unpack(b0)) {
							float f13 = direction2 == Direction.UP ? (float) Math.PI : 0.0F;
							double d23 = 0.35;
							ParticleUtils.spawnParticlesOnBlockFace(this.level, arg, new SculkChargeParticleOptions(f13), intprovider, direction2, supplier, 0.35);
						}
					}
				} else {
					this.level.playLocalSound(arg, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
					boolean flag1 = this.level.getBlockState(arg).isCollisionShapeFullBlock(this.level, arg);
					int l1 = flag1 ? 40 : 20;
					float f8 = flag1 ? 0.45F : 0.25F;
					float f9 = 0.07F;

					for (int l3 = 0; l3 < l1; l3++) {
						float f12 = 2.0F * randomsource.nextFloat() - 1.0F;
						float f14 = 2.0F * randomsource.nextFloat() - 1.0F;
						float f15 = 2.0F * randomsource.nextFloat() - 1.0F;
						this.level
							.addParticle(
								ParticleTypes.SCULK_CHARGE_POP,
								(double)arg.getX() + 0.5 + (double)(f12 * f8),
								(double)arg.getY() + 0.5 + (double)(f14 * f8),
								(double)arg.getZ() + 0.5 + (double)(f15 * f8),
								(double)(f12 * 0.07F),
								(double)(f14 * 0.07F),
								(double)(f15 * 0.07F)
							);
					}
				}
				break;
			case 3007:
				for (int j1 = 0; j1 < 10; j1++) {
					this.level
						.addParticle(
							new ShriekParticleOption(j1 * 5),
							false,
							(double)arg.getX() + 0.5,
							(double)arg.getY() + SculkShriekerBlock.TOP_Y,
							(double)arg.getZ() + 0.5,
							0.0,
							0.0,
							0.0
						);
				}

				BlockState blockstate2 = this.level.getBlockState(arg);
				boolean flag = blockstate2.hasProperty(BlockStateProperties.WATERLOGGED) && (Boolean)blockstate2.getValue(BlockStateProperties.WATERLOGGED);
				if (!flag) {
					this.level
						.playLocalSound(
							(double)arg.getX() + 0.5,
							(double)arg.getY() + SculkShriekerBlock.TOP_Y,
							(double)arg.getZ() + 0.5,
							SoundEvents.SCULK_SHRIEKER_SHRIEK,
							SoundSource.BLOCKS,
							2.0F,
							0.6F + this.level.random.nextFloat() * 0.4F,
							false
						);
				}
				break;
			case 3008:
				BlockState blockstate = Block.stateById(n);
				if (blockstate.getBlock() instanceof BrushableBlock brushableblock) {
					this.level.playLocalSound(arg, brushableblock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
				}

				this.level.addDestroyBlockEffect(arg, blockstate);
				break;
			case 3009:
				ParticleUtils.spawnParticlesOnBlockFaces(this.level, arg, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
		}
	}

	public void destroyBlockProgress(int i, BlockPos arg, int j) {
		if (j >= 0 && j < 10) {
			BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(i);
			if (blockdestructionprogress1 != null) {
				this.removeProgress(blockdestructionprogress1);
			}

			if (blockdestructionprogress1 == null
				|| blockdestructionprogress1.getPos().getX() != arg.getX()
				|| blockdestructionprogress1.getPos().getY() != arg.getY()
				|| blockdestructionprogress1.getPos().getZ() != arg.getZ()) {
				blockdestructionprogress1 = new BlockDestructionProgress(i, arg);
				this.destroyingBlocks.put(i, blockdestructionprogress1);
			}

			blockdestructionprogress1.setProgress(j);
			blockdestructionprogress1.updateTick(this.ticks);
			this.destructionProgress
				.computeIfAbsent(
					blockdestructionprogress1.getPos().asLong(),
					(Long2ObjectFunction<? extends SortedSet<BlockDestructionProgress>>)(l -> Sets.<BlockDestructionProgress>newTreeSet())
				)
				.add(blockdestructionprogress1);
		} else {
			BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(i);
			if (blockdestructionprogress != null) {
				this.removeProgress(blockdestructionprogress);
			}
		}
	}

	public boolean hasRenderedAllChunks() {
		return this.chunkRenderDispatcher.isQueueEmpty();
	}

	public void needsUpdate() {
		this.needsFullRenderChunkUpdate = true;
		this.generateClouds = true;
	}

	public void updateGlobalBlockEntities(Collection<BlockEntity> collection, Collection<BlockEntity> collection2) {
		synchronized (this.globalBlockEntities) {
			this.globalBlockEntities.removeAll(collection);
			this.globalBlockEntities.addAll(collection2);
		}
	}

	public static int getLightColor(BlockAndTintGetter arg, BlockPos arg2) {
		return getLightColor(arg, arg.getBlockState(arg2), arg2);
	}

	public static int getLightColor(BlockAndTintGetter arg, BlockState arg2, BlockPos arg3) {
		if (arg2.emissiveRendering(arg, arg3)) {
			return 15728880;
		} else {
			int i = arg.getBrightness(LightLayer.SKY, arg3);
			int j = arg.getBrightness(LightLayer.BLOCK, arg3);
			int k = arg2.getLightEmission(arg, arg3);
			if (j < k) {
				j = k;
			}

			return i << 20 | j << 4;
		}
	}

	public boolean isChunkCompiled(BlockPos arg) {
		ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = this.viewArea.getRenderChunkAt(arg);
		return chunkrenderdispatcher$renderchunk != null && chunkrenderdispatcher$renderchunk.compiled.get() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
	}

	@Nullable
	public RenderTarget entityTarget() {
		return this.entityTarget;
	}

	@Nullable
	public RenderTarget getTranslucentTarget() {
		return this.translucentTarget;
	}

	@Nullable
	public RenderTarget getItemEntityTarget() {
		return this.itemEntityTarget;
	}

	@Nullable
	public RenderTarget getParticlesTarget() {
		return this.particlesTarget;
	}

	@Nullable
	public RenderTarget getWeatherTarget() {
		return this.weatherTarget;
	}

	@Nullable
	public RenderTarget getCloudsTarget() {
		return this.cloudsTarget;
	}

	@OnlyIn(Dist.CLIENT)
	static class RenderChunkInfo {
		final ChunkRenderDispatcher.RenderChunk chunk;
		private byte sourceDirections;
		byte directions;
		final int step;

		RenderChunkInfo(ChunkRenderDispatcher.RenderChunk arg, @Nullable Direction arg2, int i) {
			this.chunk = arg;
			if (arg2 != null) {
				this.addSourceDirection(arg2);
			}

			this.step = i;
		}

		public void setDirections(byte b, Direction arg) {
			this.directions = (byte)(this.directions | b | 1 << arg.ordinal());
		}

		public boolean hasDirection(Direction arg) {
			return (this.directions & 1 << arg.ordinal()) > 0;
		}

		public void addSourceDirection(Direction arg) {
			this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << arg.ordinal());
		}

		public boolean hasSourceDirection(int i) {
			return (this.sourceDirections & 1 << i) > 0;
		}

		public boolean hasSourceDirections() {
			return this.sourceDirections != 0;
		}

		public boolean isAxisAlignedWith(int i, int j, int k) {
			BlockPos blockpos = this.chunk.getOrigin();
			return i == blockpos.getX() / 16 || k == blockpos.getZ() / 16 || j == blockpos.getY() / 16;
		}

		public int hashCode() {
			return this.chunk.getOrigin().hashCode();
		}

		public boolean equals(Object object) {
			return object instanceof LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo
				? this.chunk.getOrigin().equals(levelrenderer$renderchunkinfo.chunk.getOrigin())
				: false;
		}
	}

	@OnlyIn(Dist.CLIENT)
	static class RenderChunkStorage {
		public final LevelRenderer.RenderInfoMap renderInfoMap;
		public final LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunks;

		public RenderChunkStorage(int i) {
			this.renderInfoMap = new LevelRenderer.RenderInfoMap(i);
			this.renderChunks = new LinkedHashSet(i);
		}
	}

	@OnlyIn(Dist.CLIENT)
	static class RenderInfoMap {
		private final LevelRenderer.RenderChunkInfo[] infos;

		RenderInfoMap(int i) {
			this.infos = new LevelRenderer.RenderChunkInfo[i];
		}

		public void put(ChunkRenderDispatcher.RenderChunk arg, LevelRenderer.RenderChunkInfo arg2) {
			this.infos[arg.index] = arg2;
		}

		@Nullable
		public LevelRenderer.RenderChunkInfo get(ChunkRenderDispatcher.RenderChunk arg) {
			int i = arg.index;
			return i >= 0 && i < this.infos.length ? this.infos[i] : null;
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class TransparencyShaderException extends RuntimeException {
		public TransparencyShaderException(String string, Throwable throwable) {
			super(string, throwable);
		}
	}
}
