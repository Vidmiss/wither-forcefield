package com.example.witherforcefield;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Draws the Wither's phase-two "armor" swirl around every worn piece of enchanted netherite armor.
 * Each piece gets its own shell on the body part it covers, so a single piece works on its own:
 * helmet = head, chestplate = torso and arms, leggings = legs, boots = lower legs.
 */
public class ForcefieldLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

	/** Vanilla Wither armor swirl texture. */
	private static final Identifier TEXTURE =
			Identifier.withDefaultNamespace("textures/entity/wither/wither_armor.png");

	/**
	 * How far the shell sits outside the player's skin, in model pixels (1 pixel = 1/16 block).
	 * Vanilla armor sits 1.0 pixel out (leggings 0.5), so 1.5 puts the shell just above all armor.
	 * Raise it to push the shell further out, lower it to hug the armor.
	 */
	private static final float OFFSET_PIXELS = 1.5f;

	/** Boots sit slightly further out than leggings so the two shells don't overlap exactly. */
	private static final float BOOT_EXTRA_PIXELS = 0.25f;

	/** How tall the boots shell is, in model pixels, measured up from the feet (a leg is 12 tall). */
	private static final float BOOT_HEIGHT_PIXELS = 5.0f;

	private enum Piece { HEAD, CHEST, LEGS, FEET }

	/** One inflated shell model per armor piece, indexed by Piece.ordinal(). */
	private final HumanoidModel<AvatarRenderState>[] shells;

	@SuppressWarnings("unchecked")
	public ForcefieldLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
		Piece[] pieces = Piece.values();
		this.shells = new HumanoidModel[pieces.length];
		for (Piece piece : pieces) {
			this.shells[piece.ordinal()] = bake(piece);
		}
	}

	/**
	 * Builds a copy of the humanoid model with every box inflated (the same trick armor models use),
	 * with only the body parts for this piece switched on. These flags are set once here and never
	 * changed, because drawing is deferred and the model instance is shared between all players.
	 */
	private static HumanoidModel<AvatarRenderState> bake(Piece piece) {
		CubeDeformation deformation = new CubeDeformation(
				piece == Piece.FEET ? OFFSET_PIXELS + BOOT_EXTRA_PIXELS : OFFSET_PIXELS);
		MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0f);

		if (piece == Piece.FEET) {
			// Replace each leg with a short box covering only the bottom of the leg.
			float top = 12.0f - BOOT_HEIGHT_PIXELS;
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("right_leg",
					CubeListBuilder.create().texOffs(0, 16)
							.addBox(-2.0f, top, -2.0f, 4.0f, BOOT_HEIGHT_PIXELS, 4.0f, deformation),
					PartPose.offset(-1.9f, 12.0f, 0.0f));
			root.addOrReplaceChild("left_leg",
					CubeListBuilder.create().texOffs(0, 16)
							.addBox(-2.0f, top, -2.0f, 4.0f, BOOT_HEIGHT_PIXELS, 4.0f, deformation),
					PartPose.offset(1.9f, 12.0f, 0.0f));
		}

		HumanoidModel<AvatarRenderState> model =
				new HumanoidModel<>(LayerDefinition.create(mesh, 64, 64).bakeRoot());
		model.head.visible = piece == Piece.HEAD;
		model.body.visible = piece == Piece.CHEST;
		model.rightArm.visible = piece == Piece.CHEST;
		model.leftArm.visible = piece == Piece.CHEST;
		model.rightLeg.visible = piece == Piece.LEGS || piece == Piece.FEET;
		model.leftLeg.visible = piece == Piece.LEGS || piece == Piece.FEET;
		return model;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
					   AvatarRenderState state, float yRot, float xRot) {

		ForcefieldConfig config = ForcefieldConfig.get();
		if (!config.enabled || state.isInvisible) {
			return;
		}

		// Same scrolling the Wither uses: u = cos(age*0.02)*3, v = age*0.01 (both wrapped to 0..1),
		// with "age" sped up or slowed down by the configured speed.
		float age = state.ageInTicks * config.speed;
		float u = (Mth.cos(age * 0.02f) * 3.0f) % 1.0f;
		float v = (age * 0.01f) % 1.0f;

		submitPiece(Piece.HEAD, state.headEquipment, Items.NETHERITE_HELMET,
				poseStack, collector, lightCoords, state, u, v);
		submitPiece(Piece.CHEST, state.chestEquipment, Items.NETHERITE_CHESTPLATE,
				poseStack, collector, lightCoords, state, u, v);
		submitPiece(Piece.LEGS, state.legsEquipment, Items.NETHERITE_LEGGINGS,
				poseStack, collector, lightCoords, state, u, v);
		submitPiece(Piece.FEET, state.feetEquipment, Items.NETHERITE_BOOTS,
				poseStack, collector, lightCoords, state, u, v);
	}

	private void submitPiece(Piece piece, ItemStack worn, Item expected,
							 PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
							 AvatarRenderState state, float u, float v) {
		if (!enchanted(worn, expected)) {
			return;
		}

		// The inflated model gets posed from the same render state at draw time, so sneaking,
		// swimming, arm swings etc. follow the player. Order 1 = drawn after the base model.
		collector.order(1).submitModel(
				this.shells[piece.ordinal()],
				state,
				poseStack,
				RenderTypes.energySwirl(TEXTURE, u, v),
				lightCoords,
				OverlayTexture.NO_OVERLAY,
				ForcefieldConfig.get().tint(),
				null,
				state.outlineColor
		);
	}

	private static boolean enchanted(ItemStack stack, Item expected) {
		return stack.is(expected) && stack.isEnchanted();
	}
}
