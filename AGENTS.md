# Repository Agent Rules

## Canonical Policy
- This file is the single source of truth for repository agent rules.
- `AGENT.md` is a wrapper that must only point to this file.
- Policy-Version: `1.3.32`
- Last-Updated: `2026-04-05`

## Architecture Direction
- Keep `runelite-plugin/src/main/java/com/xptool/executor/CommandExecutor.java` as orchestration and wiring only.
- New behavior logic must be implemented in dedicated components/services (plug-and-play), then injected into `CommandExecutor`.
- Do not add new state-machine logic, cursor path algorithms, camera path algorithms, or gating logic directly inside `CommandExecutor`.

## Plugin Surface Policy
- Do not add new RuneLite plugin-surface settings/UI/config entries unless absolutely required.
- Prefer backend/system/runtime wiring changes over plugin panel/config expansion.
- If plugin-surface additions are unavoidable, call that out explicitly before implementing.

## CommandExecutor Guardrails
- `CommandExecutor` should only:
  - wire dependencies,
  - route commands/ticks/events,
  - delegate to runtime/service classes,
  - emit telemetry/results.
- Avoid adding large private helper methods to `CommandExecutor` when they can live in a focused class.
- Avoid adding new nested classes to `CommandExecutor` unless they are tiny compatibility wrappers.

## Gating and Motor Ownership
- Use one motor/gating authority per concern to avoid double-gate behavior.
- Idle behavior should use runtime/component gating contracts, not ad-hoc checks scattered across systems.
- If a new system needs timing windows or cooldowns, keep that state with that system/runtime class.

## Refactor Policy
- For any feature touching cursor/camera/drop/idle behavior:
  - first check whether an existing component can own it,
  - if not, create a new focused component and wire it,
  - keep `CommandExecutor` changes minimal and orchestration-focused.

## Implementation Pattern
- Prefer `Host` adapter interfaces for system boundaries.
- Keep reusable motion/path logic in dedicated classes.
- Keep DTO/spec/result models as top-level package classes where practical (not nested in `CommandExecutor`).

## Project Structure Contract
- Treat global runtime/orchestration code as protected: `src/runelite_planner/runtime_core/`, `src/runelite_planner/runner.py`, `src/runelite_planner/bridge.py`, `src/runelite_planner/control_plane.py`, `src/runelite_planner/remote_planner.py`, and executor runtime wiring classes under `runelite-plugin/src/main/java/com/xptool/executor/`.
- Treat activity/task behavior as isolated modules under `src/runelite_planner/activities/` and activity-specific strategy files (`woodcutting.py`, `mining.py`, `fishing.py`, `agility.py`, `combat.py`, `store_bank.py`, probes).
- Treat executor activity runtime state transitions as isolated coordinator components (`SkillingRuntimeCoordinator`, `CombatRuntimeCoordinator`) rather than embedding those transitions directly in `CommandExecutor`.
- Treat executor activity transition contracts as package boundaries under `runelite-plugin/src/main/java/com/xptool/executor/activity/` (`SkillingRuntimeTransitions`, `CombatRuntimeTransitions`); `CommandExecutor` should depend on these interfaces, not concrete runtime classes.
- Treat executor host/supplier assembly as dedicated factory classes (`ExecutorSkillingHostFactories`, `ExecutorCombatHostFactories`, `ExecutorBankHostFactories`, `ExecutorGameplayHostFactories`, `ExecutorResolverHostFactories`) and keep large host-construction lambdas out of `CommandExecutor`.
- For gameplay runtime wiring, pass typed input objects (`ExecutorGameplayRuntimeInputs.ServiceHosts`, `ExecutorGameplayRuntimeInputs.RuntimeInputs`) into `ExecutorGameplayRuntimeWiring.createBundle` instead of raw long positional argument lists.
- Apply the same typed-input pattern for motor/account runtime wiring (`ExecutorMotorRuntimeInputs.Inputs`, `ExecutorAccountRuntimeInputs.Inputs`) and avoid raw positional constructor calls in `CommandExecutor`.
- Activity evolution path: introduce `activity/*ActivityRuntime` interfaces and route behavior through per-activity runtime adapters (for example `FishingActivityRuntime`) before changing shared/global runtime code.
- Current activity-runtime baseline: `FishingActivityRuntime`, `MiningActivityRuntime`, and `WoodcuttingActivityRuntime` are active paths; prefer adding/changing behavior there rather than in `CommandExecutor` or shared coordinator internals.
- Resolve activity runtimes from `ActivityRuntimeRegistry` in `CommandExecutor` (typed `require(key, type)` lookups) rather than direct field-only coupling.
- Keep per-activity idle defaults centralized in `ActivityIdlePolicyRegistry`; `CommandExecutor` may read resolved policy values but should not embed per-activity idle default constants.
- Keep idle behavior/cadence defaults policy-driven: `IdleRuntime` should resolve profile/cadence via `ActivityIdlePolicyRegistry` (through host wiring) instead of hardcoded per-context constants.
- Treat idle arming as activity-scoped state (per activity key/context), not a single global armed/offscreen toggle shared by all activities.
- Keep idle arming/mapping mutations in `IdleArmingService` (or equivalent focused component), with `CommandExecutor` delegating instead of mutating arming collections directly.
- Centralize plannerTag/activity/context resolution in the idle arming component and include arming source tags in idle telemetry (`command_queue`, `fishing_mode_override`, `bootstrap`) for debugging parity runs.
- Keep command-type routing and command handler branches in `CommandDispatchService` (or equivalent dispatcher), with `CommandExecutor.execute(...)` delegating rather than hosting branch logic.
- Keep idle action/camera/interact window gating and idle gate snapshot/event telemetry assembly in `IdleGateTelemetryService` (or equivalent component), with `CommandExecutor` delegating.
- Keep login/logout/break start/stop orchestration helpers in `AccountRuntimeOrchestrator` (or equivalent component), with `CommandExecutor` delegating.
- Keep runtime teardown/reset flow in `RuntimeShutdownService` (or equivalent component), with `CommandExecutor` delegating.
- Keep login/logout runtime client-tick advance sequencing and cadence in `AccountRuntimeTickCoordinator` (or equivalent component), with `CommandExecutor` delegating.
- Keep command ingest queue pumping and command outcome emit mapping in `CommandIngestLifecycleService` (or equivalent component), with `CommandExecutor` delegating.
- For fishing runtime-window/attempt/dispatch transitions, use `FishingRuntimeService` as the activity service boundary; do not reintroduce those transition helpers directly in `CommandExecutor`.
- For woodcutting runtime-window/attempt/dispatch transitions, use `WoodcuttingRuntimeService` as the activity service boundary.
- For mining runtime-window/suppression transitions, use `MiningRuntimeService` as the activity service boundary.
- Keep activity target-lock/preferred-selection mutable state in dedicated activity state services (`WoodcuttingTargetStateService`, `MiningTargetStateService`, `FishingTargetStateService`) instead of direct `CommandExecutor` fields.
- Keep activity selected-target collections in dedicated services (`WoodcuttingSelectionService`, `MiningSelectionService`) instead of direct `CommandExecutor` sets.
- Keep UI selection/toggle handlers in dedicated controllers (`WoodcuttingSelectionController`, `MiningSelectionController`) and keep `CommandExecutor` methods as delegation only.
- Route world-view lookup through `WorldViewResolver` (or equivalent adapter) rather than direct scattered `client.getWorldView` / `client.getTopLevelWorldView` calls in `CommandExecutor`.
- Keep scene/NPC/object/ground-item scan queries in `SceneQueryService` (or equivalent runtime component) instead of embedding scan loops in `CommandExecutor`.
- Keep item/object display-name lookups in a dedicated resolver (`ItemObjectNameResolver` or equivalent) instead of direct lookup helpers in `CommandExecutor`.
- Keep scene/ground interaction click-point resolution in a dedicated service (`SceneInteractionPointService` or equivalent) instead of inline helper methods in `CommandExecutor`.
- For GUI/headless activity selection, use centralized registry metadata in `src/runelite_planner/activities/registry.py` instead of hardcoded per-file lists/defaults.
- New activity/task edits should land in activity modules/builders first; only touch global runtime/orchestration when contracts or shared interfaces must change.
- Do not reintroduce activity-specific parsing/validation branches directly in global GUI/orchestrator start methods when activity builder modules can own that logic.

## Mouse Analytics Pipeline Policy
- Treat SQLite DB as the single source of truth for mouse analytics reads and derived metric outputs.
- Use `C:\Users\ericb\source\repos\mouse-analytics\mouse_analytics.db` tables (`sessions`, `events`, `segment_metrics`, `session_metrics`, `drop_cycles`) for pipeline consumption.
- Do not add CSV/JSONL fallback ingestion paths for production metric computation.
- Do not split metric authority across DB and file-based recomputation; keep one canonical DB-backed pipeline.
- When adding or changing metrics, update the DB-backed pipeline components (schema/storage writers + metric computation + exports/tests) so new fields are persisted and queryable from DB.
- Preserve metric naming and semantics already used by DB outputs unless an explicit migration is planned.
- For runtime behavior that consumes manual metrics (drop/idle/random-event/login/logout/break), do not fallback to hardcoded default timing profiles when manual-metric signal is missing; disable/skip that runtime path instead and surface telemetry/reason codes.

## Activity Profile Policy
- New behavior tuning is `DB_PARITY`-only (legacy `STABLE`/`TUNING` paths are removed).
- Keep activity behavior profile definitions and resolvers centralized in `src/runelite_planner/activity_profiles.py`.
- Activity configs should expose `tuning_profile` and resolve once in strategy/runtime init; avoid scattered magic constants.
- Every GUI activity/task must set and pass an explicit default profile the same way fishing does.
- Current GUI defaults: fishing/mining/woodcutting/combat/agility/store_bank use `DB_PARITY`.
- GUI profile selectors must be single-value (`DB_PARITY`) and reject non-DB_PARITY inputs before strategy start.
- For future activities, add profile tests that validate DB_PARITY resolver behavior and strategy wiring before changing runtime logic.

## Bridge Mirroring Policy (`1C + 2B`)
- Treat migration as **runtime mirroring first**, behavior rewrites second.
- Preserve plugin parity contracts for tick cadence and command semantics while introducing bridge/external paths.
- Keep RuneLite-facing access behind focused adapters/registries; avoid raw reflection access spread across behavior code.
- Add strict validation/security as dedicated components (for example command envelope verifiers), then wire into orchestrators.
- Use one authority point per concern:
  - command validation at ingest/dispatch boundary,
  - replay/signature checks in verifier services,
  - runtime gating/motor ownership in runtime components.
- Roll out strictness with feature gates first, then tighten defaults after parity is confirmed.
- Keep compatibility payloads forward-tolerant where possible (do not break existing command bus consumers when adding metadata).

## Bridge Runtime-Only Plugin Policy
- Assume bridge mode is authoritative and `runelite.xptoolplugin=false` in runtime operation.
- Do not add new runtime behavior (including overlays, dispatch wiring, tick handlers, boundary visualization, or command execution paths) to `runelite-plugin/src/main/java/com/xptool/XPToolPlugin.java`.
- Implement bridge-mode runtime behavior in bridge runtime components (`runelite-plugin/src/main/java/com/xptool/bridge/*`) and/or executor runtime services, not plugin startup hooks.
- Treat `XPToolPlugin` as compatibility/config stub only; no new operational ownership should be assigned to it.

## Security Rollout Policy
- Prefer authenticated/encrypted channels and signed envelopes for new bridge-facing flows.
- Keep nonce/timestamp replay protection state in dedicated security components, not in `CommandExecutor`.
- If signature verification is enabled, canonicalization and key handling must live in focused verifier/adapter classes.
- Any security failure should reject safely with explicit reason codes and telemetry.

## Local-Only Bridge Policy
- Do not implement or enable remote bridge operation.
- Do not set bridge bind address to non-loopback values (`0.0.0.0`, LAN/WAN IPs, hostnames resolving outside loopback).
- Do not add relay/tunneling/port-forwarding workflows or documentation that expose bridge endpoints beyond localhost.
- Keep bridge auth token required for bridge launch/config/health tooling; do not add unauthenticated fallback paths.
- If asked to add remote access behavior, refuse and keep local-only enforcement.

## Native Client Migration Policy
- Strategic direction is full ownership of a native client/runtime stack (no long-term RuneLite dependency).
- During migration, treat Java/RuneLite code as transitional compatibility shims only.
- Do not add new long-lived behavior ownership to RuneLite plugin classes (`XPToolPlugin`, `CommandExecutor`, bridge overlays) unless required for temporary parity.
- Any temporary Java/RuneLite behavior required for parity must be marked with a clear migration note (`NATIVE_MIGRATION_TODO`) and a target native owner component.
- New runtime behavior should prefer native-owned contracts first (IPC schema, runtime services, telemetry envelopes), then bind through minimal Java adapters only when unavoidable.
- Keep policy and decision logic in native runtime services, not duplicated across Java and native layers.
- Bridge protocol changes must be backward-tolerant during migration and versioned explicitly; do not break existing consumers without a staged cutover.
- Maintain local-only bridge enforcement in native runtime too (loopback bind only, auth token required, no remote exposure fallbacks).
- Prioritize incremental replacement order:
  - ingest/dispatch contracts,
  - runtime coordinators/services,
  - activity runtimes,
  - overlays/UX adapters,
  - final Java shim removal.
- Do not remove Java shims until native parity is validated with telemetry and regression tests for tick cadence, command semantics, and activity outcomes.
- For new features during migration, implement native-first; if Java glue is needed, keep it thin and orchestration-only.

## Motion Humanization Policy
- Do not add robotic motion fallbacks.
- Never add deterministic movement paths or deterministic movement fallback branches, including when non-humanized timing mode is active.
- Do not add linear interpolation fallbacks for cursor or camera movement.
- Do not add direct snap-to-target fallbacks for movement, except unavoidable terminal correction when a path has already completed.
- If a movement path fails to resolve, prefer abort/retry or a new curved path sample instead of degrading to deterministic motion.
- Keep repeat-avoidance active for target point selection; do not reintroduce exact-pixel reuse fallback behavior.

## Deterministic Fallback Prohibition
- Do not introduce deterministic fallback behavior for cursor movement, camera movement, or interaction target-point resolution.
- Do not use fixed-coordinate fallback anchors (for example center/top-ratio points or parity-based curve-sign selection) when randomized/humanized sampling fails.
- When stochastic sampling fails, prefer defer/retry/reacquire/abort with explicit reason codes instead of deterministic substitutions.
- Do not introduce fallback substitutions (deterministic or stochastic) unless a user task explicitly asks for a fallback path.
- Default failure handling is defer/retry/reacquire/abort with explicit reason codes, not implicit fallback target substitution.
- This prohibition is global and applies to all activities, including woodcutting and fishing.
- Treat this as project-wide policy for all runtime interaction systems (skilling, combat, login/logout, world-hop, banking, and shared utilities): no deterministic fallback anchors or deterministic retry substitutions.

## Anti-Repeat Dispatch Policy
- Never allow rapid repeat dispatches on the exact same actionable target (for example the same fishing NPC, same tree/rock, or same inventory slot) without an explicit cooldown guard.
- Repeated-target guards must live in the owning runtime/service for that behavior (for example drop runtime for inventory slots, fishing runtime/service for fishing NPCs).
- When a repeated target is detected inside the guard window, prefer reroute-to-alternate-target first; if no alternate exists, defer briefly rather than re-clicking the same target.
- Treat no-progress tolerances as safety stop limits only; they must not function as permission for rapid repeated clicks on the same target.

## Drop Traversal Continuity Policy
- Keep drop-cycle traversal contiguous and inventory-local: start at the first present slot for the target item and continue with same-row-first serpentine continuity.
- Do not use profile-driven slot hops, randomized start-slot anchors, or row/column jump traversal in live drop sessions.
- Do not allow "skip now, backfill at end" behavior for same-item drop cycles unless explicitly requested behind a clearly named feature gate.
- Any drop traversal edit must include tests that assert no contiguous-slot skip/backfill regressions under representative inventory layouts.

## Fishing Drop Traversal Policy
- For fishing drop sessions (`IdleSkillContext.FISHING`), enforce pure serpentine traversal only (`SERPENTINE`, stride `1`, hop chance `0`).
- Do not enable or sample `COLUMN_BIASED` / `WAVE_BIASED` traversal profiles while in fishing drop context.
- Do not advance to another row if same-row matching drop items are still present; consume same-row candidates first.
- Seed fishing drop-session start slot from the first present matching inventory slot (scan from slot `0`) to avoid mid-row skip/revisit loops.
- Keep explicit regression tests for:
  - fishing context traversal lock,
  - same-row continuity (no skip-3 then revisit-at-end behavior).

## Scoring Protocol
- Use delta-anchored rescoring against the most recent user-accepted baseline.
- Do not rescale or renormalize untouched categories during a scoped pass.
- Mark each category as `changed` or `unchanged` before assigning deltas.
- If a category is unchanged in code and tests, default delta is `0/0/0`.
- Any non-zero delta on unchanged categories requires explicit evidence and file references.

## Behavioral Realism Risk Scoring Protocol
- Apply this protocol to current and future logic changes that affect movement, targeting, and click behavior.
- Score each category from `0` to `5`:
  - `0-1` = low concern
  - `2` = mild concern
  - `3` = moderate concern
  - `4` = strong concern
  - `5` = very strong concern

### A. Temporal realism
- Purpose: evaluate whether timing has believable decision rhythm.
- Metrics:
  - inter-event interval variance
  - pause distribution
  - burstiness
  - pre-click dwell time
  - rhythm stability over long windows
- Low-concern signature:
  - uneven but coherent timing
  - pauses cluster around meaningful moments
  - short bursts mixed with hesitation
- High-concern signature:
  - very regular cadence
  - pauses detached from task complexity
  - overly smooth randomness with weak rhythm shifts
- Score guide:
  - `0`: timing clearly irregular in a believable way
  - `1`: mild regularity but still natural
  - `2`: some repeated cadence patterns
  - `3`: noticeable regular timing or synthetic pause structure
  - `4`: strong recurring cadence across many segments
  - `5`: near-mechanical timing profile
- Example feature formulas:
  - `dt_cv = std(dt) / mean(dt)`
  - `pause_rate = pauses_over_threshold / total_actions`
  - `burst_index = std(actions_per_2s_window) / mean(actions_per_2s_window)`

### B. Spatial path realism
- Purpose: evaluate whether cursor path behavior resembles believable motor control.
- Metrics:
  - path efficiency
  - curvature
  - heading change rate
  - overshoot/correction frequency
  - submovement count
  - velocity profile smoothness
- Low-concern signature:
  - imperfect but coherent travel
  - some corrections near targets
  - variable curvature by movement length
- High-concern signature:
  - too straight too often
  - repeated path geometry
  - fake-looking wobble
  - corrections that look injected rather than natural
- Score guide:
  - `0`: paths show natural variety
  - `1`: slightly direct but still believable
  - `2`: modest over-optimization
  - `3`: repeated directness or unnatural correction patterns
  - `4`: highly optimized or patterned pathing
  - `5`: strongly mechanical path signatures
- Example feature formulas:
  - `path_efficiency = straight_line_distance / traveled_distance`
  - `heading_var = variance(change_in_heading)`
  - `overshoot_rate = overshoots / target_entries`

### C. Target-relative behavior
- Purpose: evaluate behavior near clickable targets.
- Metrics:
  - entry angle variation
  - normalized click position within target
  - time spent slowing near target
  - hover time before click
  - miss-and-recover behavior
  - approach speed decay
- Low-concern signature:
  - click points vary within object bounds
  - approach angle changes naturally
  - small hesitation/correction near target
- High-concern signature:
  - repeated click offsets
  - repeated entry angles
  - instant arrival-to-click with little variation
  - same target class approached in the same style
- Score guide:
  - `0`: approach and click placement vary naturally
  - `1`: mild clustering
  - `2`: noticeable preferred approach style
  - `3`: repeated click-position or entry-angle patterns
  - `4`: highly stable target-relative behavior
  - `5`: near-template approach behavior
- Example feature formulas:
  - `center_offset_px = distance(click_point, target_center)`
  - `norm_click_x = (click_x - target_left) / target_width`
  - `norm_click_y = (click_y - target_top) / target_height`
  - `entry_angle_to_target = angle(last_segment, target_center_vector)`

### D. Session-level repetition
- Purpose: detect repetitive structure across loops and sessions.
- Metrics:
  - route similarity across repetitions
  - action order entropy
  - cross-session similarity
  - repeated movement archetypes
  - task loop regularity
  - fatigue drift or lack of drift
- Low-concern signature:
  - repeated tasks still vary
  - ordering and spacing shift over time
  - style drifts with fatigue/interruption/attention changes
- High-concern signature:
  - repeated routes remain too similar
  - loops repeat with low entropy
  - long sessions lack believable drift
- Score guide:
  - `0`: repeated tasks still show human variation
  - `1`: some stable habits
  - `2`: moderate route reuse
  - `3`: noticeable loop similarity
  - `4`: strong repetition across windows
  - `5`: highly templated session behavior
- Example feature formulas:
  - `route_similarity = mean(dynamic_time_warping_similarity over repeated tasks)`
  - `action_entropy = entropy(action_sequence)`
  - `cross_session_similarity = similarity(session_A_features, session_B_features)`

### E. Context coherence
- Purpose: evaluate whether behavior tracks on-screen/task context.
- Metrics:
  - pause length vs task complexity
  - movement length vs target difficulty
  - behavior change after interruptions
  - behavior shift by task type
  - response to unexpected state changes
- Low-concern signature:
  - harder actions cause distinct timing/motion
  - interruptions trigger believable recovery
  - behavior differs across task contexts
- High-concern signature:
  - same movement style regardless of task
  - same timing under easy and hard interactions
  - weak response to unexpected state changes
- Score guide:
  - `0`: strong context sensitivity
  - `1`: mostly coherent
  - `2`: some flattening across contexts
  - `3`: noticeable task-behavior mismatch
  - `4`: weak context linkage
  - `5`: nearly context-free behavior

### Final risk band
- Compute `category_scores = [A, B, C, D, E]` as integer scores in `[0, 5]`.
- Compute `mean_score = mean(category_scores)`.
- Compute `max_score = max(category_scores)`.
- Assign final risk band:
  - `LOW`: `mean_score < 1.5` and `max_score <= 2`
  - `MODERATE`: `1.5 <= mean_score < 2.5` and `max_score <= 3`
  - `ELEVATED`: `2.5 <= mean_score < 3.5` or `max_score == 4`
  - `HIGH`: `mean_score >= 3.5` or `max_score == 5`

### Implementation gating by band
- `LOW`: proceed normally.
- `MODERATE`: proceed with explicit mitigations listed in review notes.
- `ELEVATED`: require mitigation plan and verification evidence before merge.
- `HIGH`: do not merge behavior change without explicit user approval and a remediation pass.
