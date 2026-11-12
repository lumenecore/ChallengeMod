# Port notes: Minecraft 1.21.2–1.21.4

## Target

- Minecraft compile target: 1.21.2 (`1.21.2+build.1` Yarn mappings)
- Intended runtime range: 1.21.2, 1.21.3, and 1.21.4
- Java: 21
- Fabric Loader compile baseline: 0.16.9
- Fabric API compile baseline: 0.106.1+1.21.2
- Fabric Loom: 1.9.2
- Gradle Wrapper: 8.12

The same remapped jar is intended for all three patch versions. Fabric API and other optional client mods must be installed in builds compatible with the Minecraft version being launched. Mod Menu is optional; its major version differs in this target range (v12 for 1.21.2/1.21.3 and v13 for 1.21.4).

## Compatibility changes

1. Replaced the post-1.21.4 `HudLayerRegistrationCallback` registration with the older `HudRenderCallback`. The HUD renderer keeps the same `DrawContext` and `RenderTickCounter` callback signature.
2. Added `NbtCompat` and redirected NBT reads to the 1.21.2-compatible getters. This avoids compiling against the post-1.21.4 Optional-returning NBT API. NBT list element types are checked explicitly where a typed list is read.
3. Replaced the `ServerWorld.tickThunder(WorldChunk)` hook with `ServerWorld.tickWeather()`. The `tickWeather()` method is present in 1.21.2 and 1.21.4 (same intermediary method identity); this keeps weather timers frozen while a run has not started.
4. Retained the mod's objective generation, conditions, modifiers, networking, screens, recipe hooks, and resources rather than disabling features for the older branch.
5. Restricted the declared Minecraft dependency to `>=1.21.2 <=1.21.4`, updated project naming, and configured the remapped jar name for this version group.

## Follow-up fixes from the first Windows build log

The first user-side `gradlew build` reached `:compileJava` and reported seven errors plus two unresolved-target warnings. The following corrections are now included:

1. `CowEntityMixin` now targets `CowEntity`, which exists in 1.21.2–1.21.4; `AbstractCowEntity` is not part of these versions.
2. `ChallengeState` now uses `PersistentState.Type<ChallengeState>`, its `(NbtCompound, WrapperLookup) -> state` deserializer, `writeNbt(...)`, and `PersistentStateManager#getOrCreate(type, id)` as required by the 1.21.2 API.
3. `InputAccessor` now accesses `Input.movementForward` and `Input.movementSideways`. The zero-input and inversion modifiers update those backing fields together with `PlayerInput`, so behavior is retained without relying on the later `movementVector` field.
4. `TameableEntityMixin` now injects into the actual `setOwner(PlayerEntity)` method using an explicit descriptor.
5. Horse taming is detected by checking the tame-state transition across `AbstractHorseEntity#interactMob(PlayerEntity, Hand)`. This passes the interacting server player directly and avoids treating an entity load as a new tame event.

The corrections are aligned with the published Yarn 1.21.2 API signatures for `PersistentState.Type`, `PersistentState#writeNbt`, `Input`, `TameableEntity#setOwner`, and `AbstractHorseEntity#interactMob` / `isTame`.

## Updated verification status

- The first build log supplied by the user confirms Gradle 8.12 and Loom 1.9.2 configure successfully and that the failure occurs during Java compilation.
- The seven compiler errors and both unresolved-target warnings from that log have been addressed in source.
- A second Gradle build could not be run in this preparation environment: outbound DNS resolution for `services.gradle.org` is unavailable. No successful post-fix build, runtime launch, or in-game behavior test is claimed. Please run `gradlew clean build` on Windows with the now-cached Gradle distribution and send the next complete log if anything remains.

## Follow-up fixes from the second Windows build log

The second user-side `gradlew build` reached `:compileJava` and reported seven unresolved API symbols. The following corrections are now included:

1. Replaced `PlayerInventory#getMainStacks()` with the public `PlayerInventory.main` list in the two inventory conditions and the start-condition check. In 1.21.2–1.21.4 this is the 36-slot main inventory list, so armor and offhand semantics are unchanged.
2. Replaced the later record-style `VillagerData` accessors with `getProfession()` and `getLevel()`.
3. Replaced `ItemStack#getCustomName()` with `ItemStack#get(DataComponentTypes.CUSTOM_NAME)`, which checks the custom-name component rather than the item's fallback display name.
4. Changed the compile-baseline boolean packet codec to `PacketCodecs.BOOL`. Yarn names this field `BOOL` in 1.21.2 and `BOOLEAN` in 1.21.4, but both mappings point to the same intermediary field `field_48547`; this is appropriate for a remapped jar compiled against 1.21.2.

## Verification status after follow-up fixes

- The second user-side build log confirms that the previous seven errors and the earlier two Mixin-target warnings are no longer reported; compilation advanced to the next seven API errors.
- All seven errors in that second log have been addressed in this source tree and checked against Yarn 1.21.2 and 1.21.4 documentation.
- This archive has not yet been successfully built or launched. Run `gradlew clean build` with the local Gradle distribution. A clean build is still required before treating this port as compile-verified; then test the resulting same jar on 1.21.2, 1.21.3, and 1.21.4.

## Runtime correction after first client launch (FIX 3)

- `ItemMixin` now targets the 1.21.2–1.21.4 descriptor of `Item.onCraftByPlayer`: `(ItemStack, World, PlayerEntity) -> void`.
- The callback handler now includes the `World` argument. In 1.21.5 this argument was removed, which is why the later-version signature compiled but failed during mixin application on 1.21.2.
- This preserves the existing crafted-item hook; it does not change the objective logic.

## Runtime correction after 1.21.2 village objective test (FIX 4)

- The objective generator stores an exact biome-specific village structure ID (`village_plains`, `village_desert`, `village_savanna`, `village_snowy`, or `village_taiga`), while every one of those IDs is deliberately displayed as the same generic text, “Find a village.” The evaluator now treats all five IDs as one objective group so any vanilla village fulfills the displayed task.
- The previous check called `ChunkGenerator#locateStructure` and rejected results whose locate point was more than 128 blocks away. The locate point is not the same as the full footprint of a large structure, so this could reject a player already inside the village. The evaluator now asks `StructureAccessor#getStructureAt` about the player's current block position and verifies that the returned start has children.
- The same visible-name grouping is now applied to desert/jungle temples and cold/warm ocean ruins, matching the generic task labels for those variants.
- The result cache is keyed by the exact block position instead of the chunk. This prevents a negative/positive result from one part of a chunk being reused after the player moves to another part of that same chunk. A same-position negative is retried every 20 ticks.
- `StructureAccessor#getStructureAt(BlockPos, Structure)` is present in Yarn 1.21.2 and 1.21.4 with the same intermediary signature. This change has been statically checked against those API docs, but this archive still requires a user-side `gradlew clean build` and in-game test on 1.21.2, 1.21.3, and 1.21.4.


## FIX 5 — local-world join networking

The client previously sent the `camera_perspective` C2S custom payload immediately after every world became available, including integrated single-player worlds. In single-player, the client and integrated server share the same process, so this state update is now applied directly on the integrated server thread. The custom payload remains registered and is still used for remote server connections. This avoids sending that custom payload during the integrated-server join transition.

This is a targeted compatibility workaround for a reported 1.21.4 disconnect. It has not been runtime-verified here; test entering a new and an existing local challenge world on 1.21.4. If the disconnect persists, the exact `logs/disconnect-*-client.txt` file (or latest.log lines around `Failed to handle packet` / `DecoderException`) is needed to identify the packet that actually fails.


## FIX 6 — input mixin owner and descriptor split

The 1.21.4 runtime crash showed that the old client mixin still looked for `KeyboardInput#tick(ZF)V` (`class_743;method_3129(ZF)V`). Yarn documents the 1.21.2/1.21.3 method with the `(ZF)V` descriptor, while 1.21.4 uses `Input#tick()V` (`class_744;method_3129()V`) and `KeyboardInput` inherits that implementation. The input restriction logic is now shared in `InputRestrictionHandler` and dispatched by two narrowly targeted client mixins:

- `KeyboardInputMixin` handles the old keyboard override. Its injection is optional on 1.21.4, where that override no longer exists.
- `InputTickMixin` handles the new `Input#method_3129()V` implementation. Its injection is optional on 1.21.2/1.21.3, where the no-argument method does not exist.

Both mixins use `require = 0` so the inactive version-specific hook does not abort startup. In each target version, the matching hook remains active; both call the same shared handler, preserving countdown/menu input locking and the inversion modifier.

This is based on the published Yarn 1.21.2, 1.21.3, and 1.21.4 method declarations. The updated archive still requires a user-side `gradlew clean build` and launch test on 1.21.2, 1.21.3, and 1.21.4; no successful build or full runtime verification is claimed here.
