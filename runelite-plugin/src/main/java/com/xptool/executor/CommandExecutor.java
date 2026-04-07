package com.xptool.executor;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xptool.activities.fishing.FishingCommandService;
import com.xptool.activities.fishing.FishingRuntime;
import com.xptool.activities.mining.MiningCommandService;
import com.xptool.activities.woodcutting.WoodcuttingCommandService;
import com.xptool.core.runtime.RuntimeDecision;
import com.xptool.executor.activity.ActivityRuntimeRegistry;
import com.xptool.executor.activity.DelegatingMiningActivityRuntime;
import com.xptool.executor.activity.DelegatingFishingActivityRuntime;
import com.xptool.executor.activity.DelegatingWoodcuttingActivityRuntime;
import com.xptool.executor.activity.CombatRuntimeTransitions;
import com.xptool.executor.activity.FishingActivityRuntime;
import com.xptool.executor.activity.FishingRuntimeService;
import com.xptool.executor.activity.FishingTargetStateService;
import com.xptool.executor.activity.MiningActivityRuntime;
import com.xptool.executor.activity.MiningRuntimeService;
import com.xptool.executor.activity.SkillingRuntimeTransitions;
import com.xptool.executor.activity.WoodcuttingActivityRuntime;
import com.xptool.executor.activity.WoodcuttingRuntimeService;
import com.xptool.executor.activity.WoodcuttingSelectionService;
import com.xptool.executor.activity.WoodcuttingTargetStateService;
import com.xptool.executor.activity.MiningTargetStateService;
import com.xptool.executor.activity.MiningSelectionService;
import com.xptool.executor.activity.SelectionWorldPointResolver;
import com.xptool.executor.activity.WoodcuttingSelectionController;
import com.xptool.executor.activity.MiningSelectionController;
import com.xptool.models.SceneCache;
import com.xptool.models.Snapshot;
import com.xptool.motor.CommandExecutorMotorEngine;
import com.xptool.motion.MotionProfile;
import com.xptool.motion.MotionProfile.ClickMotionSettings;
import com.xptool.motion.MotionProfile.MotorGestureMode;
import com.xptool.sessions.BankSession;
import com.xptool.sessions.DropSession;
import com.xptool.sessions.InteractionSession;
import com.xptool.sessions.SessionManager;
import com.xptool.sessions.idle.FishingIdleMode;
import com.xptool.sessions.idle.IdleSkillContext;
import com.xptool.systems.CommandIngestor;
import com.xptool.systems.CombatTargetPolicy;
import com.xptool.systems.CombatTargetResolver;
import com.xptool.systems.FishingTargetResolver;
import com.xptool.systems.MiningTargetResolver;
import com.xptool.systems.SceneCacheScanner;
import com.xptool.systems.TargetSelectionEngine;
import com.xptool.systems.WoodcuttingTargetResolver;
import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.HeadlessException;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import org.slf4j.Logger;
import javax.swing.SwingUtilities;

public final class CommandExecutor {
    private static final Gson GSON = new Gson();
    private static final ExecutionPayloadRedactor EXECUTION_PAYLOAD_REDACTOR = new ExecutionPayloadRedactor();
    private static final int MAX_SEEN_COMMAND_IDS = 8192;
    private static final int DEFAULT_MAX_LINES_PER_TICK = 32;
    private static final boolean MINIMAL_PLUGIN_RUNTIME = true;
    private static final int MINIMAL_MAX_LINES_PER_TICK = 8;
    private static final int MAX_MECHANICAL_DISPATCHES_PER_TICK = 3;
    private static final int EXECUTOR_WORK_BUDGET_PER_TICK = 3;
    private static final String IDLE_ARM_SOURCE_BOOTSTRAP = "bootstrap";
    private static final String IDLE_ARM_SOURCE_COMMAND_QUEUE = "command_queue";
    private static final String IDLE_ARM_SOURCE_FISHING_MODE_OVERRIDE = "fishing_mode_override";
    private static final int EXECUTOR_MAX_MOUSE_MUTATIONS_PER_TICK = 2;
    private static final int EXECUTOR_DEBUG_COUNTER_REPORT_INTERVAL_TICKS = 100;
    private static final double HOVER_TARGET_TOLERANCE_PX = 2.0;
    private static final int PENDING_MOVE_COMMIT_TIMEOUT_TICKS = 10;
    private static final int INGEST_POLL_INTERVAL_MS = 20;
    private static final int INGEST_QUEUE_CAPACITY = 256;
    private static final boolean VERBOSE_EXECUTION_LOGS =
        Boolean.parseBoolean(System.getProperty("xptool.verboseExecutionLogs", "false"));
    private static final boolean COMMAND_INGEST_DEBUG_ENABLED =
        ExecutionPrivacySettings.isCommandIngestDebugEnabled();
    private static final boolean CLICK_TELEMETRY_ENABLED =
        ExecutionPrivacySettings.isClickTelemetryEnabled();
    // Curved cursor motion is intentionally always-on.
    private static final boolean VISUAL_CURSOR_MOTION_ENABLED = true;
    private static final int CURSOR_PERCEPTION_DELAY_MIN_MS = 18;
    private static final int CURSOR_PERCEPTION_DELAY_MAX_MS = 34;
    private static final int DROP_CURSOR_PERCEPTION_DELAY_MIN_MS = 8;
    private static final int DROP_CURSOR_PERCEPTION_DELAY_MAX_MS = 16;
    private static final int DROP_SWEEP_REGION_PADDING_PX = 4;
    private static final int WOODCUT_WORLDPOINT_MATCH_RADIUS_TILES = 1;
    private static final int BANK_WORLDPOINT_MATCH_RADIUS_TILES = 1;
    private static final int SCENE_OBJECT_ACTION_RADIUS_TILES = 12;
    private static final int GROUND_ITEM_ACTION_RADIUS_TILES = 14;
    private static final int WOODCUT_HOVER_POINT_JITTER_MIN_PX = 2;
    private static final int WOODCUT_HOVER_POINT_JITTER_MAX_PX = 10;
    private static final int RANDOM_EVENT_NPC_CLICK_JITTER_MIN_PX = 1;
    private static final int RANDOM_EVENT_NPC_CLICK_JITTER_MAX_PX = 4;
    private static final int FISHING_NPC_CLICK_JITTER_MIN_PX = 2;
    private static final int FISHING_NPC_CLICK_JITTER_MAX_PX = 9;
    private static final int GROUND_ITEM_CLICK_JITTER_MIN_PX = 1;
    private static final int GROUND_ITEM_CLICK_JITTER_MAX_PX = 4;
    private static final int WOODCUT_TARGET_JITTER_MIN_PX = 1;
    private static final int WOODCUT_TARGET_JITTER_MAX_PX = 6;
    private static final double WOODCUT_TARGET_BOUNDS_INSET_RATIO = 0.18;
    private static final double COMBAT_CHATBOX_EXCLUDE_MAX_X_RATIO = 0.68;
    private static final double COMBAT_BOTTOM_UI_EXCLUDE_MIN_Y_RATIO = 0.94;
    private static final double COMBAT_HULL_UPPER_FALLBACK_Y_RATIO = 0.26;
    private static final double COMBAT_HULL_TOP_FALLBACK_Y_RATIO = 0.14;
    private static final int COMBAT_SMALL_HULL_MAX_WIDTH_PX = 30;
    private static final int COMBAT_SMALL_HULL_MAX_HEIGHT_PX = 26;
    private static final int COMBAT_HULL_CANDIDATE_SEARCH_RADIUS_PX = 3;
    private static final long COMBAT_POST_ATTEMPT_TARGET_SETTLE_GRACE_MS = 900L;
    private static final long COMBAT_CONTESTED_TARGET_SUPPRESSION_MS = 900L;
    private static final int COMBAT_ANCHOR_MAX_LOCAL_DRIFT_TILES = 96;
    private static final long COMBAT_EAT_DISPATCH_MIN_INTERVAL_MS = 700L;
    private static final long IDLE_INTERACTION_DELAY_MS = 700L;
    private static final long IDLE_SUPPRESS_AFTER_WALK_BASE_MS = 2900L;
    private static final long IDLE_SUPPRESS_AFTER_WALK_MIN_MS = 1800L;
    private static final long IDLE_SUPPRESS_AFTER_WALK_MAX_MS = 4600L;
    private static final long IDLE_SUPPRESS_AFTER_BANK_BASE_MS = 2400L;
    private static final long IDLE_SUPPRESS_AFTER_BANK_MIN_MS = 1400L;
    private static final long IDLE_SUPPRESS_AFTER_BANK_MAX_MS = 3800L;
    private static final long LOGIN_CLIENT_TICK_ADVANCE_MIN_INTERVAL_MS = 40L;
    private static final long LOGOUT_CLIENT_TICK_ADVANCE_MIN_INTERVAL_MS = 40L;
    private static final long LOGIN_IDLE_SUPPRESS_START_WINDOW_MS = 12000L;
    private static final long LOGIN_IDLE_SUPPRESS_RUNTIME_GRACE_MS = 2500L;
    private static final int MANUAL_METRICS_GATE_TELEMETRY_MIN_INTERVAL_TICKS = 24;
    private static final boolean IDLE_RUNTIME_DEFAULT_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.idleRuntimeEnabled", "true"));
    private static final boolean IDLE_ACTIVITY_GATE_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.idleActivityGateEnabled", "true"));
    private static final Set<String> IDLE_ACTIVITY_ALLOWLIST = parseIdleActivityAllowlist(
        System.getProperty("xptool.idleActivityAllowlist", "woodcutting,mining,fishing,combat")
    );
    private static final boolean RANDOM_EVENT_DISMISS_RUNTIME_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.randomEventDismissRuntimeEnabled", "true"));
    private static final boolean STRICT_FOREGROUND_WINDOW_GATING =
        Boolean.parseBoolean(System.getProperty("xptool.strictForegroundWindowGating", "true"));
    private static final boolean LOGIN_BREAK_RUNTIME_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.loginBreakRuntimeEnabled", "false"));
    private static final boolean LOGIN_BREAK_RUNTIME_AUTO_ARM =
        Boolean.parseBoolean(System.getProperty("xptool.loginBreakRuntimeAutoArm", "false"));
    private static final boolean COMMAND_EXECUTOR_SHADOW_ONLY =
        Boolean.parseBoolean(System.getProperty("xptool.commandExecutorShadowOnly", "false"));
    private static final String DEFAULT_COMMAND_FILE_PATH =
        "runelite-plugin/tools/command-bus.ndjson";
    private static final long DROP_CLIENT_ADVANCE_MIN_INTERVAL_MS = 44L;
    private static final long BASE_MOTOR_ARMED_CLICK_WINDOW_MS = 1200L;
    private static final double BASE_MOTOR_ARMED_CLICK_TOLERANCE_PX = 8.0;
    private static final FishingIdleMode DEFAULT_FISHING_IDLE_MODE = FishingIdleMode.STANDARD;
    private static final long FISHING_LEVEL_UP_CONTINUE_FISHING_SIGNAL_GRACE_MS = 2600L;

    private final Client client;
    private final Logger log;
    private final boolean shadowOnly;
    private final BridgeCommandDispatchModePolicy bridgeDispatchModePolicy;
    private final ExecutionTelemetryFileSink executionTelemetryFileSink;
    private final LinkedBlockingDeque<CommandRow> pendingCommands = new LinkedBlockingDeque<>(INGEST_QUEUE_CAPACITY);
    private final CommandFilePathResolver commandFilePathResolver;
    private final CommandIdDeduplicationService commandIdDeduplicationService;
    private final CommandIngestor commandIngestor;
    private final CommandEnvelopeVerifier commandEnvelopeVerifier;
    private final CommandEvaluationService commandEvaluationService;
    private final CommandQueueIdleArmingService commandQueueIdleArmingService;
    private final CommandQueueIngestService commandQueueIngestService;
    private final CommandIngestCallbackService commandIngestCallbackService;
    private final ManualMetricsGateTelemetryService manualMetricsGateTelemetryService;
    private static final ClickMotionSettings RANDOM_EVENT_CONTEXT_MENU_CLICK_MOTION =
        new ClickMotionSettings(1.2, 68L, 84L);
    private static final long WITHDRAW_X_PROMPT_WAIT_TIMEOUT_MS = 650L;
    private static final long WITHDRAW_X_PROMPT_POLL_INTERVAL_MS = 18L;
    private static final int BEGINNER_CLUE_SCROLL_ITEM_ID = 23182;
    private static final int BEGINNER_SCROLL_BOX_ITEM_ID = 24361;
    private static final String BEGINNER_CLUE_SCROLL_NAME = "Clue scroll (beginner)";
    private static final String BEGINNER_SCROLL_BOX_NAME = "Scroll box (beginner)";
    private static final boolean HUMANIZED_TIMING_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.humanizedTimingEnabled", "true"));
    private static final boolean HUMANIZED_BANK_WIDGET_ACTIONS_ENABLED =
        Boolean.parseBoolean(System.getProperty("xptool.humanizedBankWidgetActionsEnabled", "true"));
    private static final boolean DROP_ISOLATE_FROM_GLOBAL_COOLDOWN =
        Boolean.parseBoolean(System.getProperty("xptool.dropIsolateFromGlobalCooldown", "true"));
    private static final double SESSION_TIMING_SCALE_MIN = 0.92;
    private static final double SESSION_TIMING_SCALE_MAX = 1.08;
    private static final double SESSION_TIMING_SPREAD_NARROW = 0.08;
    private static final double SESSION_TIMING_SPREAD_DEFAULT = 0.12;
    private static final double SESSION_TIMING_SPREAD_WIDE = 0.18;
    private Robot sharedRobot = null;
    private int currentExecutorTick = Integer.MIN_VALUE;
    private int tickWorkBudgetRemaining = EXECUTOR_WORK_BUDGET_PER_TICK;
    private long sameTickCollapses = 0L;
    private long cursorReuseHits = 0L;
    private int consecutiveLocalInteractions = 0;
    private long deferredPendingMove = 0L;
    private long deferredHover = 0L;
    private long clicksDispatched = 0L;
    private final PendingMoveTelemetryService pendingMoveTelemetryService;
    private int debugTicksSinceLastReport = 0;
    private int sceneCacheTick = Integer.MIN_VALUE;
    private List<TileObject> cachedTreeObjects = List.of();
    private List<TileObject> cachedNormalTreeObjects = List.of();
    private List<TileObject> cachedOakTreeObjects = List.of();
    private List<TileObject> cachedWillowTreeObjects = List.of();
    private List<TileObject> cachedRockObjects = List.of();
    private List<TileObject> cachedBankObjects = List.of();
    private final MotorController<MotorProgram, PendingMouseMove> motorController = new MotorController<>();
    private Point motorCursorScreenPoint = null;
    private long motorCursorUpdatedAtMs = 0L;
    private final DropSweepSessionService dropSweepSessionService;
    private final DropSweepInventoryService dropSweepInventoryService;
    private long lastDropDebugEmitAtMs = 0L;
    private int pendingDropRepeatBlockedCount = 0;
    private long lastDropClientAdvanceAtMs = 0L;
    private Point baseMotorArmedClickScreenPoint = null;
    private long baseMotorArmedClickUntilMs = 0L;
    private final BankSession bankSession;
    private final DropSession dropSession;
    private final CombatRuntime combatRuntime = new CombatRuntime();
    private final FishingRuntime fishingRuntime = new FishingRuntime();
    private final MiningRuntime miningRuntime = new MiningRuntime();
    private final WoodcuttingRuntime woodcuttingRuntime = new WoodcuttingRuntime();
    private final SkillingRuntimeTransitions skillingRuntimeCoordinator =
        new SkillingRuntimeCoordinator(woodcuttingRuntime, miningRuntime, fishingRuntime);
    private final WoodcuttingActivityRuntime woodcuttingActivityRuntime =
        new DelegatingWoodcuttingActivityRuntime(skillingRuntimeCoordinator);
    private final MiningActivityRuntime miningActivityRuntime =
        new DelegatingMiningActivityRuntime(skillingRuntimeCoordinator);
    private final FishingActivityRuntime fishingActivityRuntime =
        new DelegatingFishingActivityRuntime(skillingRuntimeCoordinator);
    private final ActivityRuntimeRegistry activityRuntimeRegistry =
        ActivityRuntimeRegistry.of(woodcuttingActivityRuntime, miningActivityRuntime, fishingActivityRuntime);
    private final WoodcuttingSelectionService woodcuttingSelectionService =
        new WoodcuttingSelectionService(CommandExecutor::worldPointsMatch);
    private final MiningSelectionService miningSelectionService =
        new MiningSelectionService(CommandExecutor::worldPointsExactMatch);
    private final SelectionWorldPointResolver selectionWorldPointResolver;
    private final WoodcuttingSelectionController woodcuttingSelectionController;
    private final MiningSelectionController miningSelectionController;
    private final WoodcuttingTargetStateService woodcuttingTargetStateService = new WoodcuttingTargetStateService();
    private final MiningTargetStateService miningTargetStateService = new MiningTargetStateService();
    private final FishingTargetStateService fishingTargetStateService = new FishingTargetStateService();
    private final WoodcuttingRuntimeService woodcuttingRuntimeService;
    private final MiningRuntimeService miningRuntimeService;
    private final FishingRuntimeService fishingRuntimeService;
    private final CombatRuntimeTransitions combatRuntimeCoordinator =
        new CombatRuntimeCoordinator(combatRuntime);
    private final FatigueRuntime fatigueRuntime = new FatigueRuntime();
    private final ManualMetricsRuntimeTuning manualMetricsRuntimeTuning = new ManualMetricsRuntimeTuning();
    private final ActivityIdlePolicyRegistry activityIdlePolicyRegistry;
    private final CommandDispatchService commandDispatchService;
    private final IdleGateTelemetryService idleGateTelemetryService;
    private final RuntimeShutdownService runtimeShutdownService;
    private final AccountRuntimeTickCoordinator accountRuntimeTickCoordinator;
    private final LoginBreakRuntimeCoordinator loginBreakRuntimeCoordinator;
    private final IdleSuppressionService idleSuppressionService;
    private final CommandIngestLifecycleService commandIngestLifecycleService;
    private final RuntimeTickOrchestrator runtimeTickOrchestrator;
    private final LifecycleShutdownService lifecycleShutdownService;
    private final AccountRuntimeOrchestrator accountRuntimeOrchestrator;
    private final HumanTypingEngine humanTypingEngine;
    private final LoginRuntime loginRuntime;
    private final LogoutRuntime logoutRuntime;
    private final ResumePlanner resumePlanner;
    private final BreakRuntime breakRuntime;
    private final MenuEntryTargetMatcher menuEntryTargetMatcher;
    private final LoginInteractionController loginInteractionController;
    private final LogoutInteractionController logoutInteractionController;
    private final LoginSubmitTargetPlanner loginSubmitTargetPlanner;
    private final LoginSubmitTargetPlanner loginSubmitSecondaryTargetPlanner;
    private final LoginSubmitStagePlanner loginSubmitStagePlanner;
    private final WorldViewResolver worldViewResolver;
    private final ClientScreenBoundsResolver clientScreenBoundsResolver;
    private final IdleCursorTargetPlanner idleCursorTargetPlanner;
    private final IdleOffscreenMoveEngine idleOffscreenMoveEngine;
    private final CameraMotionService cameraMotionService;
    private final CombatTargetPolicy combatTargetPolicy;
    private final BrutusCombatSystem brutusCombatSystem;
    private final NpcClickPointResolver npcClickPointResolver;
    private final SceneQueryService sceneQueryService;
    private final ItemObjectNameResolver itemObjectNameResolver;
    private final SceneInteractionPointService sceneInteractionPointService;
    private final SceneCacheScanner sceneCacheScanner;
    private final SceneObjectCandidateClassifier sceneObjectCandidateClassifier;
    private final TargetSelectionEngine targetSelectionEngine;
    private final TargetPointVariationEngine targetPointVariationEngine;
    private final InventorySlotInteractionController inventorySlotInteractionController;
    private final InventorySlotPointPlanner inventorySlotPointPlanner;
    private final BankMenuInteractionController bankMenuInteractionController;
    private final FocusMenuInteractionController focusMenuInteractionController;
    private final InteractionClickEngine interactionClickEngine;
    private final InteractionSession interactionSession;
    private final SessionManager sessionManager;
    private final IdleRuntime idleRuntime;
    private final RandomEventDismissRuntime randomEventDismissRuntime;
    private final MotorDispatchAdmissionService motorDispatchAdmissionService;
    private final MotorDispatchContextService motorDispatchContextService;
    private final InteractionClickTelemetryService interactionClickTelemetryService;
    private final InteractionAnchorResolverService interactionAnchorResolverService;
    private final MotorCanvasMoveEngine motorCanvasMoveEngine;
    private final MotorProgramMoveEngine motorProgramMoveEngine;
    private final MotorProgramTerminalService motorProgramTerminalService;
    private final MotorRuntime motorRuntime;
    private final LayeredRuntimeRouter layeredRuntimeRouter;
    private final double sessionTimingScale;
    private final SkillTimingProfile sessionWoodcutTimingProfile;
    private final SkillTimingProfile sessionMiningTimingProfile;
    private final SkillTimingProfile sessionFishingTimingProfile;
    private final SkillTimingProfile sessionCombatTimingProfile;
    private final long sessionWoodcutApproachBaseWaitMs;
    private final long sessionWoodcutApproachWaitPerTileMs;
    private final long sessionWoodcutApproachMaxWaitMs;
    private final long sessionWoodcutApproachMinHoldMs;
    private final long sessionWoodcutSameTargetReclickCooldownMs;
    private final long sessionFishingApproachBaseWaitMs;
    private final long sessionFishingApproachWaitPerTileMs;
    private final long sessionFishingApproachMaxWaitMs;
    private final long sessionFishingSameTargetReclickCooldownMs;
    private final long sessionCombatPostAttemptTargetSettleGraceMs;
    private final long sessionCombatContestedTargetSuppressionMs;
    private final long sessionCombatEatDispatchMinIntervalMs;
    private final long sessionCombatRecenterMinCooldownMs;
    private final long sessionCombatRecenterMaxCooldownMs;
    private final long sessionIdleInteractionDelayMs;
    private volatile FishingIdleMode configuredFishingIdleMode = DEFAULT_FISHING_IDLE_MODE;
    private volatile boolean fishingIdleModeOverrideEnabled = false;
    private volatile IdleCadenceTuning activeIdleCadenceTuning = IdleCadenceTuning.none();
    private volatile Snapshot latestSnapshot = Snapshot.empty();
    private final IdleArmingService idleArmingService = new IdleArmingService();
    private SessionManager.Registration dropSweepSessionRegistration = null;
    private long lastInteractionTime = 0L;
    private long combatLastEatDispatchAtMs = 0L;
    private long suppressIdleForLoginUntilMs = Long.MIN_VALUE;
    private final IdleTraversalBankSuppressionGate idleTraversalBankSuppressionGate =
        new IdleTraversalBankSuppressionGate();
    private int processedCommandRowsThisTick = 0;
    private long lastLoginClientAdvanceAtMs = 0L;
    private int lastLoginRuntimeAdvanceTick = Integer.MIN_VALUE;
    private long lastLogoutClientAdvanceAtMs = 0L;
    private int lastLogoutRuntimeAdvanceTick = Integer.MIN_VALUE;
    public CommandExecutor(Client client, Logger log) {
        this.client = client;
        this.log = log;
        this.shadowOnly = COMMAND_EXECUTOR_SHADOW_ONLY;
        BridgeLiveDispatchPolicy bridgeLiveDispatchPolicy = new BridgeRuntimeStateLiveDispatchPolicy();
        this.bridgeDispatchModePolicy = new BridgeCommandDispatchModePolicy(bridgeLiveDispatchPolicy, shadowOnly);
        this.activityIdlePolicyRegistry = ActivityIdlePolicyRegistry.defaults();
        this.worldViewResolver = WorldViewResolver.fromClient(client);
        String configuredLeftClickDropItemIds = resolveConfiguredLeftClickDropItemIds(log);
        this.clientScreenBoundsResolver = new ClientScreenBoundsResolver(client);
        this.sceneObjectCandidateClassifier = new SceneObjectCandidateClassifier(client::getObjectDefinition);
        this.sceneQueryService = new SceneQueryService(
            client,
            worldViewResolver,
            sceneObjectCandidateClassifier,
            SCENE_OBJECT_ACTION_RADIUS_TILES,
            GROUND_ITEM_ACTION_RADIUS_TILES
        );
        this.itemObjectNameResolver = ItemObjectNameResolver.fromClient(client);
        this.dropSweepSessionService = new DropSweepSessionService();
        this.dropSweepInventoryService = new DropSweepInventoryService(
            BEGINNER_CLUE_SCROLL_ITEM_ID,
            BEGINNER_SCROLL_BOX_ITEM_ID,
            BEGINNER_CLUE_SCROLL_NAME,
            BEGINNER_SCROLL_BOX_NAME
        );
        this.pendingMoveTelemetryService = new PendingMoveTelemetryService(
            ExecutorValueParsers::details,
            this::emitIdleEvent,
            this::currentPointerLocationOr
        );
        this.executionTelemetryFileSink = new ExecutionTelemetryFileSink(log);
        this.commandEnvelopeVerifier = CommandEnvelopeVerifier.fromSystemProperties();
        this.commandFilePathResolver = new CommandFilePathResolver(
            () -> System.getenv("XPTOOL_COMMAND_FILE_PATH"),
            ExecutorValueParsers::safePath,
            DEFAULT_COMMAND_FILE_PATH
        );
        this.commandIdDeduplicationService = new CommandIdDeduplicationService(MAX_SEEN_COMMAND_IDS);
        this.manualMetricsGateTelemetryService = new ManualMetricsGateTelemetryService(
            () -> currentExecutorTick,
            this::hasManualMetricsRuntimeSignal,
            this::activeDropCadenceProfileKey,
            ExecutorValueParsers::details,
            (status, reason, details, eventType) -> emit(status, null, reason, details, eventType),
            MANUAL_METRICS_GATE_TELEMETRY_MIN_INTERVAL_TICKS
        );
        this.commandEvaluationService = new CommandEvaluationService(
            bridgeDispatchModePolicy,
            commandIdDeduplicationService::isDuplicateCommandId,
            commandEnvelopeVerifier,
            CommandExecutor::rejectedOutcomeFromReason,
            this::execute,
            this::rejectUnsupportedCommandType,
            CommandRowPlannerTagPolicy::resolvePlannerTag
        );
        this.commandQueueIdleArmingService = new CommandQueueIdleArmingService(
            idleArmingService,
            this::resolveFishingIdleMode,
            IDLE_ACTIVITY_GATE_ENABLED,
            IDLE_ACTIVITY_ALLOWLIST,
            IDLE_ARM_SOURCE_COMMAND_QUEUE
        );
        this.commandQueueIngestService = new CommandQueueIngestService(
            pendingCommands,
            commandQueueIdleArmingService
        );
        this.commandIngestCallbackService = new CommandIngestCallbackService(
            ExecutorValueParsers::details,
            details -> emit("accepted", null, "executor_config_updated", details),
            this::emitIngestDebug,
            reason -> emit("failed", null, reason, null),
            commandIdDeduplicationService::clearSeenCommandIds,
            commandQueueIngestService::onParsedCommandRow,
            commandEnvelopeVerifier,
            bridgeDispatchModePolicy,
            LOGIN_BREAK_RUNTIME_ENABLED,
            LOGIN_BREAK_RUNTIME_AUTO_ARM,
            COMMAND_INGEST_DEBUG_ENABLED,
            shadowOnly
        );
        this.selectionWorldPointResolver = new SelectionWorldPointResolver(worldViewResolver::resolveByIdOrTopLevel);
        this.woodcuttingSelectionController = new WoodcuttingSelectionController(
            selectionWorldPointResolver,
            woodcuttingSelectionService,
            woodcuttingTargetStateService,
            CommandExecutor::worldPointsMatch,
            this::clearWoodcutTargetLock
        );
        this.miningSelectionController = new MiningSelectionController(
            selectionWorldPointResolver,
            miningSelectionService,
            miningTargetStateService,
            CommandExecutor::worldPointsExactMatch,
            this::clearMiningTargetLock,
            this::clearMiningHoverPoint
        );
        this.sessionTimingScale = sampleSessionTimingScale();
        this.sessionWoodcutTimingProfile = scaleSkillTimingProfile(ExecutorSkillingTimingCatalog.WOODCUT_TIMING_PROFILE);
        this.sessionMiningTimingProfile = scaleSkillTimingProfile(ExecutorSkillingTimingCatalog.MINING_TIMING_PROFILE);
        this.sessionFishingTimingProfile = scaleSkillTimingProfile(ExecutorSkillingTimingCatalog.FISHING_TIMING_PROFILE);
        this.sessionCombatTimingProfile = scaleSkillTimingProfile(ExecutorSkillingTimingCatalog.COMBAT_TIMING_PROFILE);
        this.sessionWoodcutApproachBaseWaitMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.WOODCUT_APPROACH_BASE_WAIT_MS, 100L);
        this.sessionWoodcutApproachWaitPerTileMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.WOODCUT_APPROACH_WAIT_PER_TILE_MS, 20L);
        this.sessionWoodcutApproachMaxWaitMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.WOODCUT_APPROACH_MAX_WAIT_MS, 220L);
        this.sessionWoodcutApproachMinHoldMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.WOODCUT_APPROACH_MIN_HOLD_MS, 70L);
        this.sessionWoodcutSameTargetReclickCooldownMs = sessionScaledDurationMs(
            ExecutorSkillingTimingCatalog.WOODCUT_SAME_TARGET_RECLICK_COOLDOWN_MS,
            250L
        );
        this.sessionFishingApproachBaseWaitMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.FISHING_APPROACH_BASE_WAIT_MS, 250L);
        this.sessionFishingApproachWaitPerTileMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.FISHING_APPROACH_WAIT_PER_TILE_MS, 60L);
        this.sessionFishingApproachMaxWaitMs =
            sessionScaledDurationMs(ExecutorSkillingTimingCatalog.FISHING_APPROACH_MAX_WAIT_MS, 500L);
        this.sessionFishingSameTargetReclickCooldownMs = sessionScaledDurationMs(
            ExecutorSkillingTimingCatalog.FISHING_SAME_TARGET_RECLICK_COOLDOWN_MS,
            600L
        );
        this.sessionCombatPostAttemptTargetSettleGraceMs = sessionScaledDurationMs(
            COMBAT_POST_ATTEMPT_TARGET_SETTLE_GRACE_MS,
            200L
        );
        this.sessionCombatContestedTargetSuppressionMs = sessionScaledDurationMs(
            COMBAT_CONTESTED_TARGET_SUPPRESSION_MS,
            200L
        );
        this.sessionCombatEatDispatchMinIntervalMs = sessionScaledDurationMs(
            COMBAT_EAT_DISPATCH_MIN_INTERVAL_MS,
            250L
        );
        this.sessionCombatRecenterMinCooldownMs =
            sessionScaledDurationMs(ExecutorCombatProfile.COMBAT_RECENTER_MIN_COOLDOWN_MS, 300L);
        this.sessionCombatRecenterMaxCooldownMs =
            sessionScaledDurationMs(ExecutorCombatProfile.COMBAT_RECENTER_MAX_COOLDOWN_MS, 500L);
        this.sessionIdleInteractionDelayMs = sessionScaledDurationMs(IDLE_INTERACTION_DELAY_MS, 250L);
        String fishingIdleModeOverrideRaw = safeString(System.getProperty("xptool.fishingIdleMode"));
        this.fishingIdleModeOverrideEnabled = !fishingIdleModeOverrideRaw.isBlank();
        this.configuredFishingIdleMode = parseFishingIdleMode(
            this.fishingIdleModeOverrideEnabled
                ? fishingIdleModeOverrideRaw
                : DEFAULT_FISHING_IDLE_MODE.name()
        );
        this.woodcuttingRuntimeService = new WoodcuttingRuntimeService(
            resolveWoodcuttingActivityRuntime(),
            this::sessionJitteredDurationMs,
            sessionWoodcutTimingProfile.retryWindowMs,
            sessionWoodcutTimingProfile.outcomeWaitWindowMs,
            sessionWoodcutApproachBaseWaitMs,
            sessionWoodcutApproachWaitPerTileMs,
            sessionWoodcutApproachMaxWaitMs,
            sessionWoodcutApproachMinHoldMs,
            SESSION_TIMING_SPREAD_DEFAULT,
            SESSION_TIMING_SPREAD_NARROW,
            SESSION_TIMING_SPREAD_WIDE
        );
        this.miningRuntimeService = new MiningRuntimeService(
            resolveMiningActivityRuntime(),
            this::sessionJitteredDurationMs,
            sessionMiningTimingProfile.retryWindowMs,
            sessionMiningTimingProfile.outcomeWaitWindowMs,
            SESSION_TIMING_SPREAD_DEFAULT,
            SESSION_TIMING_SPREAD_NARROW
        );
        this.fishingRuntimeService = new FishingRuntimeService(
            fishingActivityRuntime,
            this::sessionJitteredDurationMs,
            sessionFishingTimingProfile.retryWindowMs,
            sessionFishingTimingProfile.outcomeWaitWindowMs,
            sessionFishingApproachBaseWaitMs,
            sessionFishingApproachWaitPerTileMs,
            sessionFishingApproachMaxWaitMs,
            SESSION_TIMING_SPREAD_DEFAULT,
            SESSION_TIMING_SPREAD_NARROW,
            SESSION_TIMING_SPREAD_WIDE
        );
        if (resolveFishingIdleMode(IdleSkillContext.FISHING) == FishingIdleMode.OFFSCREEN_BIASED) {
            idleArmingService.armActivity(
                ActivityIdlePolicyRegistry.ACTIVITY_FISHING,
                FishingIdleMode.OFFSCREEN_BIASED,
                IDLE_ARM_SOURCE_BOOTSTRAP
            );
        }
        if (resolveFishingIdleMode(IdleSkillContext.WOODCUTTING) == FishingIdleMode.OFFSCREEN_BIASED) {
            idleArmingService.armActivity(
                ActivityIdlePolicyRegistry.ACTIVITY_WOODCUTTING,
                FishingIdleMode.OFFSCREEN_BIASED,
                IDLE_ARM_SOURCE_BOOTSTRAP
            );
        }
        LoginSubmitTargetPlanner.Host loginSubmitPlannerHost =
            ExecutorLoginInteractionHostFactory.createLoginSubmitPlannerHost(
            client::getCanvasWidth,
            client::getCanvasHeight,
            this::isUsableCanvasPoint
        );
        this.loginSubmitTargetPlanner = new LoginSubmitTargetPlanner(
            loginSubmitPlannerHost,
            ExecutorEngineConfigCatalog.LOGIN_SUBMIT_PRIMARY_TARGET_PLANNER_CONFIG
        );
        this.loginSubmitSecondaryTargetPlanner = new LoginSubmitTargetPlanner(
            loginSubmitPlannerHost,
            ExecutorEngineConfigCatalog.LOGIN_SUBMIT_SECONDARY_TARGET_PLANNER_CONFIG
        );
        this.loginSubmitStagePlanner = new LoginSubmitStagePlanner(
            ExecutorLoginUiProfile.LOGIN_SUBMIT_SECONDARY_GRACE_WINDOW_MS
        );
        this.menuEntryTargetMatcher = new MenuEntryTargetMatcher(
            ExecutorLoginInteractionHostFactory.createMenuEntryTargetMatcherHost(
                () -> client.getMenu().getMenuEntries(),
                worldViewResolver::resolveByIdOrTopLevel
            ),
            WOODCUT_WORLDPOINT_MATCH_RADIUS_TILES
        );
        this.loginInteractionController = new LoginInteractionController(
            ExecutorLoginInteractionHostFactory.createLoginInteractionControllerHost(
                keywords -> CommandExecutor.this.findVisibleWidgetByKeywords(keywords),
                this::centerOfWidget,
                this::isUsableCanvasPoint,
                this::focusClientWindowAndCanvas,
                this::clickCanvasPoint,
                (reason, details) -> emit("executed", null, reason, details, "LOGIN"),
                ExecutorValueParsers::details,
                client::getGameState,
                client::getCanvasWidth,
                client::getCanvasHeight,
                this::getOrCreateRobot,
                CommandExecutor::sleepQuietly,
                this::noteMotorAction
            ),
            loginSubmitStagePlanner
        );
        this.logoutInteractionController = new LogoutInteractionController(
            ExecutorLoginInteractionHostFactory.createLogoutInteractionControllerHost(
                client::getWidget,
                keywords -> CommandExecutor.this.findVisibleWidgetByKeywords(keywords),
                this::centerOfWidget,
                this::isUsableCanvasPoint,
                this::focusClientWindowAndCanvas,
                this::clickCanvasPoint,
                (reason, details) -> emit("executed", null, reason, details, "LOGOUT"),
                ExecutorValueParsers::details,
                client::getGameState
            )
        );
        this.commandIngestor = ExecutorEngineWiring.createCommandIngestor(
            commandFilePathResolver::resolveCommandFilePath,
            () -> MINIMAL_PLUGIN_RUNTIME ? MINIMAL_MAX_LINES_PER_TICK : DEFAULT_MAX_LINES_PER_TICK,
            commandIngestCallbackService::onConfigPathUpdated,
            commandIngestCallbackService::onCommandFileAttached,
            commandIngestCallbackService::onCommandFileTruncated,
            commandIngestCallbackService::parseCommandLine,
            () -> GSON,
            commandIngestCallbackService::onParsedCommandRow,
            commandIngestCallbackService::onFailure,
            INGEST_POLL_INTERVAL_MS,
            "xptool-command-ingest"
        );
        this.interactionClickTelemetryService = new InteractionClickTelemetryService(
            new InteractionClickTelemetryService.Host() {
                @Override
                public boolean isUsableCanvasPoint(Point point) {
                    return CommandExecutor.this.isUsableCanvasPoint(point);
                }

                @Override
                public Point currentMouseCanvasPoint() {
                    return toAwtPoint(client.getMouseCanvasPosition());
                }

                @Override
                public int canvasWidth() {
                    return client.getCanvasWidth();
                }

                @Override
                public int canvasHeight() {
                    return client.getCanvasHeight();
                }

                @Override
                public String normalizedMotorOwnerName(String owner) {
                    return CommandExecutor.normalizedMotorOwnerName(owner);
                }

                @Override
                public String activeMotorOwnerContext() {
                    return CommandExecutor.this.activeMotorOwnerContext();
                }

                @Override
                public boolean isSettleEligibleClickType(String clickType) {
                    return CommandExecutor.this.isSettleEligibleClickType(clickType);
                }

                @Override
                public boolean targetVariationEnabled() {
                    return targetPointVariationEngine != null && targetPointVariationEngine.isEnabled();
                }

                @Override
                public int currentExecutorTick() {
                    return currentExecutorTick;
                }

                @Override
                public long motorActionSerial() {
                    return getMotorActionSerial();
                }

                @Override
                public void noteInteractionActivityNow() {
                    CommandExecutor.this.noteInteractionActivityNow();
                }

                @Override
                public void noteMotorAction() {
                    CommandExecutor.this.noteMotorAction();
                }

                @Override
                public void emitInteractionClickTelemetry(JsonObject telemetry) {
                    emit("executed", null, "interaction_click_telemetry", telemetry, "CLICK");
                }

                @Override
                public void onSettleEligibleInteractionClick(InteractionClickEvent clickEvent) {
                    if (interactionSession != null) {
                        interactionSession.onInteractionClickEvent(clickEvent);
                    }
                }
            },
            CLICK_TELEMETRY_ENABLED
        );
        this.interactionAnchorResolverService = new InteractionAnchorResolverService(
            this::rememberInteractionAnchor
        );
        ExecutorInteractionRuntimeBundle interactionRuntimeBundle = ExecutorInteractionRuntimeWiring.createBundle(
            client,
            clientScreenBoundsResolver,
            createInventorySlotInteractionControllerHost(),
            createBankMenuInteractionControllerHost(),
            ExecutorIdleHostFactory.createIdleOffscreenMoveHost(
                this::pendingMouseMove,
                this::setPendingMouseMove,
                this::clearPendingMouseMove,
                this::isPendingMouseMoveOwnerValid,
                this::currentPointerLocationOr,
                this::getOrCreateRobot,
                this::getCurrentExecutorTick,
                this::isCursorNearScreenPoint,
            this::emitIdleEvent
        ),
        () -> toAwtPoint(client.getMouseCanvasPosition()),
            this::resolveTileObjectClickPoint,
            candidate -> toAwtPoint(candidate == null ? null : candidate.getCanvasLocation()),
            this::isUsableCanvasPoint,
            CommandExecutor::pixelDistance,
            () -> consecutiveLocalInteractions,
            this::isBankOpen,
            this::clampPointToRectangle,
            this::canPerformMotorActionNow,
            this::motorMoveToCanvasPoint,
            this::resolveInventoryInteractionRegionCanvas,
            interactionClickTelemetryService::lastInteractionAnchorCenterCanvasPointOrNull,
            interactionClickTelemetryService::lastInteractionAnchorBoundsCanvasOrNull,
            this::isClientCanvasFocused,
            this::allowWindowRefocusForInteraction,
            this::focusClientWindowAndCanvas,
            this::toScreenPoint,
            this::getOrCreateRobot,
            this::clickCanvasActivationAnchor,
            this::noteInteractionClickSuccess,
            this::isCursorNearScreenPoint,
            CommandExecutor::moveMouseCurve,
            this::randomCanvasPointInRegion,
            this::currentPointerLocationOr,
            this::resolveIdleSkillContext,
            CommandExecutor::jitterWithinBounds,
            CommandExecutor::sleepCritical,
            CommandExecutor::sleepNoCooldown,
            CommandExecutor::sleepQuietly,
            this::noteMotorAction,
            (focusWindow, focusCanvas) -> this.focusClientWindowAndCanvas(focusWindow, focusCanvas),
            this::reserveMotorCooldown,
            this::activeIdleCadenceTuning,
            VISUAL_CURSOR_MOTION_ENABLED,
            HUMANIZED_TIMING_ENABLED,
            this::emitIdleEvent
        );
        this.targetSelectionEngine = interactionRuntimeBundle.targetSelectionEngine;
        this.targetPointVariationEngine = interactionRuntimeBundle.targetPointVariationEngine;
        this.sceneInteractionPointService = new SceneInteractionPointService(
            this::resolveTileObjectClickPoint,
            CommandExecutor::toAwtPoint,
            targetPointVariationEngine::varyForTileObject,
            WOODCUT_HOVER_POINT_JITTER_MIN_PX,
            WOODCUT_HOVER_POINT_JITTER_MAX_PX + 1,
            GROUND_ITEM_CLICK_JITTER_MIN_PX,
            GROUND_ITEM_CLICK_JITTER_MAX_PX
        );
        this.inventorySlotInteractionController = interactionRuntimeBundle.inventorySlotInteractionController;
        this.inventorySlotPointPlanner = interactionRuntimeBundle.inventorySlotPointPlanner;
        this.focusMenuInteractionController = interactionRuntimeBundle.focusMenuInteractionController;
        this.interactionClickEngine = interactionRuntimeBundle.interactionClickEngine;
        this.bankMenuInteractionController = interactionRuntimeBundle.bankMenuInteractionController;
        this.idleCursorTargetPlanner = interactionRuntimeBundle.idleCursorTargetPlanner;
        this.idleOffscreenMoveEngine = interactionRuntimeBundle.idleOffscreenMoveEngine;
        this.cameraMotionService = interactionRuntimeBundle.cameraMotionService;
        Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> woodcuttingCommandHostFactory =
            ExecutorSkillingHostFactories.createWoodcuttingCommandHostFactory(
                dropSweepSessionService::isSessionActive,
                this::endDropSweepSession,
                this::lastDropSweepSessionEndedAtMs,
                woodcuttingRuntimeService::extendRetryWindow,
                this::resolveClickMotion,
                this::currentPlayerAnimation,
                this::localPlayerWorldPoint,
                woodcuttingRuntimeService::clearOutcomeWaitWindow,
                woodcuttingRuntimeService::clearTargetAttempt,
                woodcuttingRuntimeService::clearDispatchAttempt,
                woodcuttingRuntime::outcomeWaitUntilMs,
                woodcuttingRuntime::lastAttemptWorldPoint,
                woodcuttingRuntime::approachWaitUntilMs,
                woodcuttingRuntime::lastDispatchWorldPoint,
                woodcuttingRuntime::lastDispatchAtMs,
                this::woodcutSameTargetReclickCooldownMs,
                this::updateWoodcutBoundary,
                this::clearWoodcutBoundary,
                this::lockWoodcutTarget,
                woodcuttingRuntimeService::clearInteractionWindows,
                woodcuttingSelectionService::size,
                this::resolveWoodcutHoverPoint,
                this::isUsableCanvasPoint,
                this::clearWoodcutTargetLock,
                this::clearWoodcutHoverPoint,
                this::rememberInteractionAnchorForTileObject,
                this::scheduleMotorGesture,
                this::buildWoodcutMoveAndClickProfile,
                this::noteInteractionActivityNow,
                targetObject -> woodcuttingRuntimeService.noteTargetAttempt(localPlayerWorldPoint(), targetObject),
                woodcuttingRuntimeService::noteDispatchAttempt,
                woodcuttingRuntimeService::beginOutcomeWaitWindow,
                this::incrementClicksDispatched,
                fatigueRuntime::snapshot,
                ExecutorValueParsers::details,
                ExecutorValueParsers::safeString,
                RuntimeDecision::accept,
                RuntimeDecision::reject
            );
        Function<MiningTargetResolver, MiningCommandService.Host> miningCommandHostFactory =
            ExecutorSkillingHostFactories.createMiningCommandHostFactory(
                dropSweepSessionService::isSessionActive,
                this::endDropSweepSession,
                miningRuntimeService::pruneRockSuppression,
                miningRuntimeService::extendRetryWindow,
                this::resolveClickMotion,
                this::currentPlayerAnimation,
                miningRuntimeService::clearOutcomeWaitWindow,
                miningRuntime::outcomeWaitUntilMs,
                miningTargetStateService::lockedWorldPoint,
                miningTargetStateService::lockedObjectId,
                miningTargetStateService::hasLockedTarget,
                this::lockMiningTarget,
                miningRuntimeService::clearInteractionWindows,
                miningSelectionService::size,
                this::resolveMiningHoverPoint,
                this::isUsableCanvasPoint,
                this::clearMiningTargetLock,
                this::clearMiningHoverPoint,
                this::rememberInteractionAnchorForTileObject,
                this::scheduleMotorGesture,
                this::buildMiningMoveAndClickProfile,
                this::noteInteractionActivityNow,
                miningRuntimeService::suppressRockTarget,
                this::miningTargetReclickCooldownMs,
                miningRuntimeService::beginOutcomeWaitWindow,
                this::incrementClicksDispatched,
                ExecutorValueParsers::details,
                ExecutorValueParsers::safeString,
                RuntimeDecision::accept,
                RuntimeDecision::reject
            );
        Function<FishingTargetResolver, FishingCommandService.Host> fishingCommandHostFactory =
            ExecutorSkillingHostFactories.createFishingCommandHostFactory(
                dropSweepSessionService::isSessionActive,
                this::endDropSweepSession,
                this::lastDropSweepSessionEndedAtMs,
                fishingRuntimeService::extendRetryWindow,
                this::resolveClickMotion,
                () -> client.getLocalPlayer(),
                this::isFishingLevelUpPromptVisible,
                this::dismissFishingLevelUpPrompt,
                fishingRuntimeService::clearOutcomeWaitWindow,
                fishingRuntimeService::clearTargetAttempt,
                ExecutorPayloadParsers::parsePreferredNpcIds,
                fishingRuntime::outcomeWaitUntilMs,
                fishingRuntime::lastAttemptNpcIndex,
                fishingRuntime::lastAttemptWorldPoint,
                fishingRuntime::approachWaitUntilMs,
                this::lockFishingTarget,
                fishingRuntimeService::clearInteractionWindows,
                fishingRuntimeService::clearInteractionWindowsPreserveDispatchSignal,
                this::resolveVariedFishingNpcClickPoint,
                this::isUsableCanvasPoint,
                this::clearFishingTargetLock,
                this::rememberInteractionAnchorForNpc,
                fishingRuntime::lastDispatchAtMs,
                fishingRuntime::lastDispatchWorldPoint,
                fishingRuntime::lastDispatchNpcIndex,
                this::fishingSameTargetReclickCooldownMs,
                this::scheduleMotorGesture,
                this::buildFishingMoveAndClickProfile,
                this::noteInteractionActivityNow,
                fishingRuntimeService::noteTargetAttempt,
                fishingRuntimeService::noteDispatchAttempt,
                fishingRuntimeService::beginOutcomeWaitWindow,
                this::incrementClicksDispatched,
                fatigueRuntime::snapshot,
                ExecutorValueParsers::details,
                ExecutorValueParsers::safeString,
                RuntimeDecision::accept,
                RuntimeDecision::reject
            );
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory =
            ExecutorCombatHostFactories.createBrutusCombatHostFactory(
                combatRuntime,
                this::isAttackableNpc,
                this::activeMotorProgram,
                this::clearActiveMotorProgram,
                CommandExecutor::normalizedMotorOwnerName,
                () -> ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                this::cancelMotorProgram,
                this::clearPendingMouseMove,
                this::isCombatCanvasPointUsable,
                this::scheduleMotorGesture,
                this::buildCombatDodgeMoveAndClickProfile,
                ExecutorValueParsers::safeString,
                () -> currentExecutorTick,
                this::clearCombatOutcomeWaitWindow,
                this::clearCombatTargetAttempt,
                this::noteInteractionActivityNow,
                this::incrementClicksDispatched,
                ExecutorValueParsers::details,
                this::hasCombatBoundary,
                this::combatRecenterMinCooldownMs,
                this::combatRecenterMaxCooldownMs,
                CommandExecutor::randomBetween,
                combatRuntime::setRecenterCooldownUntilMs,
                CommandDecision::accept,
                CommandDecision::reject
            );
        ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory =
            ExecutorCombatHostFactories.createCombatCommandHostFactory(
                client,
                dropSweepSessionService::isSessionActive,
                this::endDropSweepSession,
                this::extendCombatRetryWindow,
                this::resolveClickMotion,
                ExecutorPayloadParsers::parsePreferredNpcIds,
                this::resolvePreferredNpcNameHint,
                this::isCombatAnchorLikelyStale,
                this::updateCombatBoundary,
                this::pruneCombatNpcSuppression,
                combatRuntime::lastAttemptNpcIndex,
                combatRuntime::outcomeWaitUntilMs,
                this::isCombatPostOutcomeSettleGraceActive,
                this::suppressCombatNpcTarget,
                this::combatTargetReclickCooldownMs,
                this::clearCombatTargetAttempt,
                combatRuntime::resetTargetUnavailableStreak,
                this::isAttackableNpc,
                this::clearCombatOutcomeWaitWindow,
                this::resolveVariedNpcClickPoint,
                this::isCombatCanvasPointUsable,
                () -> ExecutorCombatProfile.COMBAT_TARGET_CLICK_FALLBACK_ATTEMPTS,
                combatRuntime::incrementTargetUnavailableStreak,
                this::combatPostAttemptTargetSettleGraceMs,
                this::clearCombatInteractionWindows,
                combatRuntime::suppressedNpcCount,
                this::rememberInteractionAnchorForNpc,
                this::scheduleMotorGesture,
                this::buildCombatMoveAndClickProfile,
                this::noteInteractionActivityNow,
                this::noteCombatTargetAttempt,
                this::beginCombatOutcomeWaitWindow,
                this::incrementClicksDispatched,
                this::combatContestedTargetSuppressionMs,
                CommandExecutor::randomBetween,
                fatigueRuntime::snapshot,
                ExecutorValueParsers::details,
                ExecutorValueParsers::safeString,
                CommandDecision::accept,
                CommandDecision::reject
            );
        ExecutorGameplayRuntimeInputs.ServiceHosts gameplayServiceHosts = createGameplayServiceHostsForRuntime(
            client,
            woodcuttingCommandHostFactory,
            miningCommandHostFactory,
            fishingCommandHostFactory,
            brutusCombatHostFactory,
            combatCommandHostFactory
        );
        this.motorDispatchContextService = new MotorDispatchContextService(
            new MotorDispatchContextService.Host() {
                @Override
                public String normalizedMotorOwnerName(String owner) {
                    return CommandExecutor.normalizedMotorOwnerName(owner);
                }

                @Override
                public String safeClickType(String clickType) {
                    return CommandExecutor.safeString(clickType);
                }
            },
            ExecutorMotorProfileCatalog.CLICK_TYPE_NONE
        );

        ExecutorGameplayRuntimeInputs.RuntimeInputs gameplayRuntimeInputs = createGameplayRuntimeInputs();

        ExecutorGameplayRuntimeBundle gameplayRuntimeBundle = ExecutorGameplayRuntimeWiring.createBundle(
            client,
            this,
            gameplayServiceHosts,
            gameplayRuntimeInputs
        );
        ExecutorServiceBundle serviceBundle = gameplayRuntimeBundle.serviceBundle;
        this.combatTargetPolicy = serviceBundle.combatTargetPolicy;
        this.npcClickPointResolver = serviceBundle.npcClickPointResolver;
        this.brutusCombatSystem = serviceBundle.brutusCombatSystem;
        this.sceneCacheScanner = serviceBundle.sceneCacheScanner;
        this.bankSession = serviceBundle.bankSession;
        this.dropSession = serviceBundle.dropSession;
        this.sessionManager = serviceBundle.sessionManager;
        this.interactionSession = serviceBundle.interactionSession;
        this.idleRuntime = serviceBundle.idleRuntime;
        this.randomEventDismissRuntime = gameplayRuntimeBundle.randomEventDismissRuntime;
        this.motorDispatchAdmissionService = new MotorDispatchAdmissionService(
            new MotorDispatchAdmissionService.Host() {
                @Override
                public boolean isUsableCanvasPoint(Point canvasPoint) {
                    return CommandExecutor.this.isUsableCanvasPoint(canvasPoint);
                }

                @Override
                public String normalizedMotorOwnerName(String owner) {
                    return CommandExecutor.normalizedMotorOwnerName(owner);
                }

                @Override
                public boolean isMotorOwner(String owner) {
                    return sessionManager.isMotorOwner(owner);
                }

                @Override
                public boolean acquireOrRenewMotorOwner(String owner, long leaseMs) {
                    if (sessionManager.isMotorOwner(owner)) {
                        return sessionManager.renewMotor(owner, leaseMs);
                    }
                    return sessionManager.tryAcquireMotor(owner, leaseMs);
                }

                @Override
                public long motorProgramLeaseMsForOwner(String owner) {
                    return CommandExecutor.this.motorProgramLeaseMsForOwner(owner);
                }

                @Override
                public MotorProgram activeMotorProgram() {
                    return CommandExecutor.this.activeMotorProgram();
                }

                @Override
                public void clearActiveMotorProgram() {
                    CommandExecutor.this.clearActiveMotorProgram();
                }

                @Override
                public long nextMotorProgramId() {
                    return CommandExecutor.this.nextMotorProgramId();
                }

                @Override
                public void setActiveMotorProgram(MotorProgram program) {
                    CommandExecutor.this.setActiveMotorProgram(program);
                }
            },
            EXECUTOR_MAX_MOUSE_MUTATIONS_PER_TICK
        );
        this.motorProgramTerminalService = new MotorProgramTerminalService(
            gameplayRuntimeBundle.motorProgramLifecycleEngine,
            new MotorProgramTerminalService.Host() {
                @Override
                public void releaseIdleMotorOwnership() {
                    sessionManager.releaseMotor(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE);
                }

                @Override
                public String normalizedMotorOwnerName(String owner) {
                    return CommandExecutor.normalizedMotorOwnerName(owner);
                }
            }
        );
        ExecutorMotorRuntimeInputs.Inputs motorRuntimeInputs = createMotorRuntimeInputs();
        ExecutorMotorRuntimeBundle motorRuntimeBundle = ExecutorMotorRuntimeWiring.createBundle(motorRuntimeInputs);
        this.motorCanvasMoveEngine = motorRuntimeBundle.motorCanvasMoveEngine;
        this.motorProgramMoveEngine = motorRuntimeBundle.motorProgramMoveEngine;
        this.motorRuntime = motorRuntimeBundle.motorRuntime;
        this.layeredRuntimeRouter = new LayeredRuntimeRouter(
            new CommandExecutorMotorEngine(this),
            configuredLeftClickDropItemIds,
            this::hasPendingCommandRows,
            log
        );
        ExecutorAccountRuntimeInputs.Inputs accountRuntimeInputs = ExecutorAccountRuntimeInputFactory.create(
            client::getGameState,
            keywords -> CommandExecutor.this.findVisibleWidgetByKeywords(keywords),
            this::isGameStateNamed,
            this::isPrimaryLoginSubmitPromptVisible,
            this::isSecondaryLoginSubmitPromptVisible,
            () -> focusClientWindowAndCanvas(false, false),
            this::pressLoginKeyChord,
            ExecutorValueParsers::details,
            () -> withMotorOwnerContextResult(
                ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                CommandExecutor.this::submitLoginAttempt
            ),
            () -> withMotorOwnerContextResult(
                ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                () -> clickVisibleLoginWidget(
                    "select a world",
                    "switch world",
                    "world"
                )
            ),
            () -> requestLogoutAttemptWithMotorOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION),
            () -> client.getGameState() == GameState.LOGGED_IN && !hasPendingCommandRows(),
            this::stopAllRuntimeForBreakStart,
            this::requestLogoutForBreakStart,
            this::startLoginRuntime,
            (reason, details) -> emit("executed", null, reason, details, "TYPING"),
            (reason, details) -> emit("executed", null, reason, details, "LOGIN"),
            (reason, details) -> emit("executed", null, reason, details, "LOGOUT"),
            (reason, details) -> emit("executed", null, reason, details, "RESUME"),
            (reason, details) -> emit("executed", null, reason, details, "BREAK")
        );
        ExecutorAccountRuntimeBundle accountRuntimeBundle =
            ExecutorAccountRuntimeWiring.createBundle(accountRuntimeInputs);
        this.humanTypingEngine = accountRuntimeBundle.humanTypingEngine;
        this.loginRuntime = accountRuntimeBundle.loginRuntime;
        this.logoutRuntime = accountRuntimeBundle.logoutRuntime;
        this.resumePlanner = accountRuntimeBundle.resumePlanner;
        this.breakRuntime = accountRuntimeBundle.breakRuntime;
        this.runtimeShutdownService = new RuntimeShutdownService(
            ExecutorRuntimeServiceHostFactory.createRuntimeShutdownHost(
            this::releasePendingIdleCameraDrag,
            pendingCommands::clear,
            this::clearPendingMouseMove,
            this::activeMotorProgram,
            this::cancelMotorProgram,
            this::clearActiveMotorProgram,
            this::endDropSweepSession,
            interactionSession::shutdown,
            sessionManager::releaseMotor,
            motorProgramTerminalService::releaseIdleMotorOwnershipForRuntimeTeardown,
            idleArmingService::disarmAll,
            idleTraversalBankSuppressionGate::clear,
            woodcuttingRuntimeService::clearInteractionWindows,
            miningRuntimeService::clearInteractionWindows,
            fishingRuntimeService::clearInteractionWindows,
            this::clearFishingTargetLock,
            this::clearCombatTargetAttempt,
            inventorySlotPointPlanner::resetDropPointHistory,
            fatigueRuntime::reset,
            () -> {
                loginSubmitTargetPlanner.reset();
                loginSubmitSecondaryTargetPlanner.reset();
                loginSubmitStagePlanner.reset();
            }
        ));
        this.accountRuntimeTickCoordinator = new AccountRuntimeTickCoordinator(
            ExecutorAccountRuntimeInputFactory.createAccountRuntimeTickHost(
            client::getGameState,
            loginRuntime::isActive,
            logoutRuntime::isActive,
            System::currentTimeMillis,
            () -> LOGIN_CLIENT_TICK_ADVANCE_MIN_INTERVAL_MS,
            () -> LOGOUT_CLIENT_TICK_ADVANCE_MIN_INTERVAL_MS,
            () -> lastLoginClientAdvanceAtMs,
            value -> lastLoginClientAdvanceAtMs = value,
            () -> lastLogoutClientAdvanceAtMs,
            value -> lastLogoutClientAdvanceAtMs = value,
            () -> lastLoginRuntimeAdvanceTick,
            value -> lastLoginRuntimeAdvanceTick = value,
            () -> lastLogoutRuntimeAdvanceTick,
            value -> lastLogoutRuntimeAdvanceTick = value,
            loginRuntime::onGameTick,
            logoutRuntime::onGameTick
        ));
        this.commandIngestLifecycleService = new CommandIngestLifecycleService(
            ExecutorRuntimeServiceHostFactory.createCommandIngestLifecycleHost(
            pendingCommands::peekFirst,
            pendingCommands::pollFirst,
            commandEvaluationService::evaluate,
            commandEvaluationService::evaluateShadow,
            this::maybeExtendIdleTraversalOrBankSuppression,
            bridgeDispatchModePolicy::isShadowWouldDispatchOutcome,
            () -> MAX_MECHANICAL_DISPATCHES_PER_TICK,
            ExecutorValueParsers::details,
            this::noteInteractionActivityNow,
            this::emit,
            client::getGameState
        ));
        this.commandDispatchService = new CommandDispatchService(
            ExecutorRuntimeServiceHostFactory.createCommandDispatchHost(
            () -> currentExecutorTick,
            CommandDecision::accept,
            CommandDecision::reject,
            ExecutorValueParsers::details,
            CommandExecutor::safeString,
            this::stopAllRuntime,
            () -> LOGIN_BREAK_RUNTIME_ENABLED,
            this::applyIdleCadenceTuningFromPayload,
            this::hasManualMetricsRuntimeSignalFor,
            client::getGameState,
            loginRuntime::isActive,
            () -> loginRuntime.state().name().toLowerCase(Locale.ROOT),
            this::startLoginRuntime,
            this::startLogoutRuntime,
            () -> accountRuntimeTickCoordinator.advanceLogoutRuntimeOnObservedTick(currentExecutorTick),
            () -> logoutRuntime.state() == LogoutRuntimeState.FAILED_HARD_STOP,
            logoutRuntime::isSuccessful,
            () -> logoutRuntime.state().name().toLowerCase(Locale.ROOT),
            logoutRuntime::isActive,
            logoutRuntime::lastFailureReason,
            CommandExecutor::tryParseFishingIdleMode,
            () -> configuredFishingIdleMode,
            mode -> configuredFishingIdleMode = mode,
            enabled -> fishingIdleModeOverrideEnabled = enabled,
            () -> IDLE_ACTIVITY_GATE_ENABLED,
            () -> IDLE_ACTIVITY_ALLOWLIST,
            () -> idleArmingService,
            () -> IDLE_ARM_SOURCE_FISHING_MODE_OVERRIDE,
            this::pushMotorOwnerContext,
            this::pushClickTypeContext,
            this::popMotorOwnerContext,
            this::popClickTypeContext,
            this::acquireOrRenewMotorOwner,
            this::motorLeaseMsForOwner,
            () -> bankSession,
            () -> dropSession,
            () -> interactionSession,
            this::rejectUnsupportedCommandType
        ));
        this.idleGateTelemetryService = new IdleGateTelemetryService(ExecutorIdleHostFactory.createIdleGateTelemetryHost(
            this::isIdleRuntimeEnabled,
            this::isIdleRuntimeArmedForCurrentContext,
            this::isIdleRuntimeArmedForContext,
            this::isClientWindowForegroundEligible,
            () -> logoutRuntime != null && logoutRuntime.isActive(),
            this::shouldSuppressIdleForLogin,
            client::getGameState,
            this::isBankOpen,
            this::shouldSuppressIdleForTraversalOrBank,
            this::isMouseMovePending,
            this::isIdleMenuBlockActive,
            client::isMenuOpen,
            this::shouldOwnInteractionSession,
            this::resolveIdleSkillContext,
            this::hasPendingCommandRows,
            this::isIdleAnimationActiveNow,
            this::hasActiveMotorProgramForOwner,
            this::isClientCanvasFocused,
            this::resolveFishingIdleMode,
            () -> fishingIdleModeOverrideEnabled,
            () -> idleArmingService,
            () -> STRICT_FOREGROUND_WINDOW_GATING,
            () -> suppressIdleForLoginUntilMs,
            () -> loginRuntime != null && loginRuntime.isActive(),
            this::isPrimaryLoginSubmitPromptVisible,
            this::isSecondaryLoginSubmitPromptVisible,
            System::currentTimeMillis,
            CommandExecutor::remainingFutureMs,
            () -> idleTraversalBankSuppressionGate,
            this::emit
        ));
        this.accountRuntimeOrchestrator = new AccountRuntimeOrchestrator(
            ExecutorAccountRuntimeInputFactory.createAccountRuntimeOrchestratorHost(
            client::getGameState,
            () -> currentExecutorTick,
            this::hasManualMetricsRuntimeSignalFor,
            this::resolveManualMetricsLogoutProfile,
            this::resolveManualMetricsLoginProfile,
            this::maybeEmitManualMetricsRuntimeGateEvent,
            ExecutorValueParsers::details,
            logoutRuntime::isActive,
            logoutRuntime::isSuccessful,
            () -> logoutRuntime.state() == LogoutRuntimeState.FAILED_HARD_STOP,
            logoutRuntime::requestStart,
            logoutRuntime::requestStop,
            observedTick -> accountRuntimeTickCoordinator.advanceLogoutRuntimeOnObservedTick(observedTick),
            () -> LOGIN_BREAK_RUNTIME_ENABLED,
            loginRuntime::isActive,
            loginRuntime::requestStop,
            (profile, typingProfile) -> loginRuntime.requestStart("", "", profile, typingProfile),
            System::currentTimeMillis,
            () -> LOGIN_IDLE_SUPPRESS_START_WINDOW_MS,
            untilMs -> suppressIdleForLoginUntilMs = Math.max(suppressIdleForLoginUntilMs, untilMs),
            this::suppressIdleMotionForLoginStart,
            () -> {
                loginSubmitTargetPlanner.reset();
                loginSubmitSecondaryTargetPlanner.reset();
                loginSubmitStagePlanner.reset();
            },
            breakRuntime::notifyStopAllRuntime,
            resumePlanner::cancel,
            humanTypingEngine::cancel,
            this::stopOperationalRuntimeState,
            CommandDecision::accept
        ));
        this.loginBreakRuntimeCoordinator = new LoginBreakRuntimeCoordinator(
            ExecutorRuntimeCoordinatorHostFactory.createLoginBreakRuntimeHost(
            this::hasManualMetricsRuntimeSignalFor,
            breakRuntime::state,
            breakRuntime::disarm,
            this::maybeEmitManualMetricsRuntimeGateEvent,
            ExecutorValueParsers::details,
            () -> LOGIN_BREAK_RUNTIME_ENABLED,
            () -> LOGIN_BREAK_RUNTIME_AUTO_ARM,
            this::resolveManualMetricsBreakProfile,
            breakRuntime::arm,
            breakRuntime::onGameTick
        ));
        this.idleSuppressionService = new IdleSuppressionService(ExecutorIdleHostFactory.createIdleSuppressionHost(
            sessionManager::hasActiveSession,
            sessionManager::hasActiveSessionOtherThan,
            this::resolveIdleSkillContext,
            this::pendingMouseMove,
            this::hasActiveDropSweepSession,
            this::isIdleOwnedOffscreenPendingMove,
            this::hasActiveMotorProgramForOwner,
            this::isBankOpen,
            this::hasPendingCommandRows,
            () -> sessionManager.isMotorOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE),
            this::releasePendingIdleCameraDrag,
            CommandExecutor::normalizedMotorOwnerName,
            this::clearPendingMouseMove,
            this::activeMotorProgram,
            this::cancelMotorProgram,
            motorProgramTerminalService::releaseIdleMotorOwnershipAfterSuppression
        ));
        this.runtimeTickOrchestrator = new RuntimeTickOrchestrator(
            ExecutorRuntimeCoordinatorHostFactory.createRuntimeTickOrchestratorHost(
            value -> currentExecutorTick = value,
            this::resetTickWorkState,
            bridgeDispatchModePolicy::isShadowQueueOnlyMode,
            this::processShadowCommandRows,
            this::refreshSceneCacheForTick,
            this::suppressIdleMotionIfCommandTrafficActive,
            motorRuntime::advancePendingMouseMove,
            motorRuntime::tickMotorProgram,
            value -> processedCommandRowsThisTick = value,
            commandIngestLifecycleService::processGameTickCommandRows,
            tick -> {
                if (RANDOM_EVENT_DISMISS_RUNTIME_ENABLED) {
                    withMotorOwnerContext(
                        ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                        () -> randomEventDismissRuntime.onGameTick(tick)
                    );
                }
            },
            tick -> withMotorOwnerContext(
                ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP,
                () -> dropSession.onGameTick(tick)
            ),
            tick -> withMotorOwnerContext(
                ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
                () -> interactionSession.onGameTick(tick)
            ),
            tick -> {
                if (isIdleRuntimeEnabled()) {
                    withMotorOwnerContext(
                        ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE,
                        () -> idleRuntime.onGameTick(tick)
                    );
                }
            },
            tick -> {
                if (LOGIN_BREAK_RUNTIME_ENABLED) {
                    loginBreakRuntimeCoordinator.advanceOnGameTick(tick);
                }
            },
            this::maybeEmitExecutorDebugCounters,
            this::resetClientPumpState,
            commandIngestLifecycleService::pumpPendingCommandOnClientTickWhenLoggedOut,
            tick -> {
                if (LOGIN_BREAK_RUNTIME_ENABLED) {
                    loginRuntime.onClientTick(tick);
                    accountRuntimeTickCoordinator.advanceLoginRuntimeOnClientTickWhenLoggedOut(tick);
                }
            },
            accountRuntimeTickCoordinator::advanceLogoutRuntimeOnClientTick,
            tick -> {
                if (!dropSweepSessionService.isSessionActive()) {
                    return;
                }
                long now = System.currentTimeMillis();
                if ((now - lastDropClientAdvanceAtMs) < DROP_CLIENT_ADVANCE_MIN_INTERVAL_MS) {
                    return;
                }
                lastDropClientAdvanceAtMs = now;
                withMotorOwnerContext(
                    ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP,
                    () -> dropSession.onGameTick(tick)
                );
            },
            () -> layeredRuntimeRouter.onGameTick(latestSnapshot)
        ));
        this.lifecycleShutdownService = new LifecycleShutdownService(
            ExecutorRuntimeCoordinatorHostFactory.createLifecycleShutdownHost(
            this::releasePendingIdleCameraDrag,
            commandIngestor::stop,
            pendingCommands::clear,
            this::clearDropSweepSessionRegistration,
            () -> logoutRuntime.isActive() || logoutRuntime.isSuccessful(),
            logoutRuntime::requestStop,
            () -> LOGIN_BREAK_RUNTIME_ENABLED,
            breakRuntime::disarm,
            loginRuntime::requestStop,
            resumePlanner::cancel,
            humanTypingEngine::cancel,
            executionTelemetryFileSink::emitPendingRollup,
            interactionSession::shutdown
        ));
    }

    private ExecutorGameplayRuntimeInputs.ServiceHosts createGameplayServiceHostsForRuntime(
        Client runtimeClient,
        Function<WoodcuttingTargetResolver, WoodcuttingCommandService.Host> woodcuttingCommandHostFactory,
        Function<MiningTargetResolver, MiningCommandService.Host> miningCommandHostFactory,
        Function<FishingTargetResolver, FishingCommandService.Host> fishingCommandHostFactory,
        BiFunction<CombatTargetPolicy, CombatTargetResolver, BrutusCombatSystem.Host> brutusCombatHostFactory,
        ExecutorServiceWiring.TriFunction<BrutusCombatSystem, CombatTargetPolicy, CombatTargetResolver, CombatCommandService.Host> combatCommandHostFactory
    ) {
        return ExecutorGameplayServiceHostsAssembler.create(
            runtimeClient,
            ExecutorGameplayServiceHostsAssembler.createBankCommandHostSupplier(
                CommandDecision::accept,
                CommandDecision::reject,
                ExecutorValueParsers::details,
                ExecutorValueParsers::safeString,
                this::isBankOpen,
                this::resolveClickMotion,
                this::resolveOpenBankTarget,
                this::resolveBankObjectClickPoint,
                this::isUsableCanvasPoint,
                this::rememberInteractionAnchorForTileObject,
                this::scheduleMotorGesture,
                this::buildBankMoveAndClickProfile,
                this::incrementClicksDispatched,
                this::findVisibleBankItemSlot,
                this::findBankItemSlot,
                this::resolveBankItemSlotWidget,
                () -> runtimeClient,
                this::slotCenter,
                this::prepareBankWidgetHover,
                WidgetActionResolver::chooseWidgetOpByKeywordPriority,
                this::tryConsumeWorkBudget,
                () -> HUMANIZED_BANK_WIDGET_ACTIONS_ENABLED,
                () -> ExecutorBankInteractionProfile.BANK_MOTOR_READY_WAIT_MAX_MS,
                this::waitForMotorActionReady,
                this::tryHumanizedBankWidgetAction,
                this::typeWithdrawQuantity,
                WidgetActionResolver::summarizeWidgetActions,
                this::findInventorySlot,
                this::resolveInventorySlotPoint,
                this::resolveInventorySlotWidget,
                ExecutorPayloadParsers::parseExcludeItemIds,
                this::findFirstInventoryItemNotIn,
                CommandExecutor::copyMotionFields,
                this::getOrCreateRobot,
                CommandExecutor::sleepQuietly,
                CommandExecutor::randomBetween,
                () -> ExecutorBankInteractionProfile.BANK_SEARCH_KEY_MIN_DELAY_MS,
                () -> ExecutorBankInteractionProfile.BANK_SEARCH_KEY_MAX_DELAY_MS,
                this::typeBankSearchChar,
                this::isBankPinPromptVisible,
                this::centerOfWidget,
                this::clickCanvasPoint,
                () -> MotionProfile.GENERIC_INTERACT.directClickSettings
            ),
            ExecutorGameplayServiceHostsAssembler.createWoodcuttingTargetResolverHostSupplier(
                this::localPlayerWorldPoint,
                woodcuttingTargetStateService::lockedWorldPoint,
                woodcuttingTargetStateService::preferredSelectedWorldPoint,
                woodcuttingSelectionService::size,
                () -> cachedTreeObjects,
                () -> cachedNormalTreeObjects,
                () -> cachedOakTreeObjects,
                () -> cachedWillowTreeObjects,
                woodcuttingSelectionService::hasTargetNear,
                CommandExecutor::worldPointsMatch,
                (candidates, worldDistanceProvider) -> {
                    if (worldDistanceProvider == null) {
                        return Optional.empty();
                    }
                    return this.selectBestCursorAwareTarget(candidates, worldDistanceProvider::worldDistance);
                }
            ),
            woodcuttingCommandHostFactory,
            ExecutorGameplayServiceHostsAssembler.createMiningTargetResolverHostSupplier(
                miningSelectionService::size,
                this::localPlayerWorldPoint,
                () -> cachedRockObjects,
                this::isRockObjectCandidate,
                miningRuntimeService::isRockSuppressed,
                miningSelectionService::hasTargetNear,
                (candidates, worldDistanceProvider) -> {
                    if (worldDistanceProvider == null) {
                        return Optional.empty();
                    }
                    return this.selectBestCursorAwareTarget(candidates, worldDistanceProvider::worldDistance);
                }
            ),
            miningCommandHostFactory,
            ExecutorGameplayServiceHostsAssembler.createFishingTargetResolverHostSupplier(this, runtimeClient),
            ExecutorGameplayServiceHostsAssembler.createCombatTargetPolicyHostSupplier(this, runtimeClient),
            ExecutorGameplayServiceHostsAssembler.createCombatTargetResolverHostFactory(this, runtimeClient),
            fishingCommandHostFactory,
            brutusCombatHostFactory,
            combatCommandHostFactory,
            ExecutorGameplayServiceHostsAssembler.createSceneCacheScannerHostSupplier(this, runtimeClient),
            this::isUsableCanvasPoint,
            CommandDecision::accept,
            CommandDecision::reject,
            ExecutorValueParsers::details,
            ExecutorValueParsers::safeString,
            this::resolveClickMotion,
            this::scheduleMotorGesture,
            this::buildWalkMoveAndClickProfile,
            this::noteInteractionActivityNow,
            this::incrementClicksDispatched,
            combatRuntime::targetUnavailableStreak,
            this::combatEatDispatchMinIntervalMs,
            value -> combatLastEatDispatchAtMs = value,
            () -> combatLastEatDispatchAtMs,
            () -> currentExecutorTick,
            this::findInventorySlot,
            this::canPerformMotorActionNow,
            this::clickInventorySlot,
            this::nudgeCameraYawLeft,
            this::nudgeCameraYawRight,
            this::nudgeCameraPitchUp,
            this::nudgeCameraPitchDown,
            this::cameraLastNudgeDetails,
            ExecutorValueParsers::asInt,
            sceneQueryService::currentNpcs,
            this::resolveVariedNpcClickPoint,
            this::moveInteractionCursorToCanvasPoint,
            this::isCursorNearRandomEventTarget,
            this::isTopMenuOptionOnNpc,
            this::clickNpcContextPrimaryAt,
            this::selectNpcContextMenuOptionAt,
            sceneQueryService::currentNearbySceneObjects,
            itemObjectNameResolver::resolveSceneObjectName,
            sceneInteractionPointService::resolveSceneObjectClickPoint,
            this::isTopMenuOptionOnObject,
            this::waitForMotorActionReady,
            () -> ExecutorBankInteractionProfile.RANDOM_EVENT_MOTOR_READY_WAIT_MAX_MS,
            this::clickCanvasPoint,
            sceneQueryService::currentNearbyGroundItems,
            itemObjectNameResolver::resolveGroundItemName,
            sceneInteractionPointService::resolveGroundItemClickPoint,
            this::isTopMenuOptionOnGroundItem,
            this::centerOfWidget,
            (focusWindow, focusCanvas) -> this.focusClientWindowAndCanvas(
                Boolean.TRUE.equals(focusWindow),
                Boolean.TRUE.equals(focusCanvas)
            ),
            () -> ExecutorBankInteractionProfile.BANK_MOTOR_READY_WAIT_MAX_MS,
            keywords -> CommandExecutor.this.findVisibleWidgetByKeywords(keywords),
            (focusWindow, focusCanvas) -> this.focusClientWindowAndCanvas(
                Boolean.TRUE.equals(focusWindow),
                Boolean.TRUE.equals(focusCanvas)
            ),
            this::isCombatCanvasPointUsable,
            COMBAT_HULL_UPPER_FALLBACK_Y_RATIO,
            COMBAT_HULL_TOP_FALLBACK_Y_RATIO,
            COMBAT_SMALL_HULL_MAX_WIDTH_PX,
            COMBAT_SMALL_HULL_MAX_HEIGHT_PX,
            COMBAT_HULL_CANDIDATE_SEARCH_RADIUS_PX,
            this::rejectUnsupportedCommandType
        );
    }

    private ExecutorGameplayRuntimeInputs.RuntimeInputs createGameplayRuntimeInputs() {
        return ExecutorGameplayRuntimeInputFactory.create(
            () -> currentExecutorTick,
            this::currentPlayerAnimation,
            CommandExecutor::isAnimationActive,
            dropSweepSessionService::isSessionActive,
            dropSweepSessionService::itemId,
            dropSweepSessionService::nextSlot,
            dropSweepSessionService::lastDispatchTick,
            dropSweepSessionService::dispatchFailStreak,
            dropSweepSessionService::awaitingFirstCursorSync,
            dropSweepSessionService::setNextSlot,
            dropSweepSessionService::setLastDispatchTick,
            dropSweepSessionService::setAwaitingFirstCursorSync,
            dropSweepSessionService::setProgressCheckPending,
            this::beginDropSweepSession,
            this::endDropSweepSession,
            this::updateDropSweepProgressState,
            this::noteDropSweepDispatchFailure,
            this::noteDropSweepDispatchSuccess,
            this::findInventorySlotFrom,
            this::resolveInventorySlotPoint,
            this::resolveInventorySlotBasePoint,
            this::centerOfDropSweepRegionCanvas,
            point -> isCursorNearCanvasPoint(point, ExecutorDropProfile.DROP_HOVER_TARGET_TOLERANCE_PX),
            this::scheduleDropMoveGesture,
            () -> acquireOrRenewMotorOwner(
                ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP,
                ExecutorMotorProfileCatalog.MOTOR_LEASE_DROP_SWEEP_MS
            ),
            () -> client.getGameState() == GameState.LOGGED_IN && !isBankOpen(),
            this::dispatchInventoryDropAction,
            this::applyDropPerceptionDelay,
            this::incrementClicksDispatched,
            fatigueRuntime::snapshot,
            this::setActiveDropCadenceProfileKey,
            this::setActiveIdleCadenceTuning,
            ExecutorValueParsers::details,
            this::emitDropDebug,
            CommandDecision::accept,
            CommandDecision::reject,
            this::hasActiveDropSweepSession,
            this::isIdleInterActionWindowOpen,
            this::resolveIdleSkillContext,
            this::isIdleActionWindowOpen,
            this::isIdleCameraWindowOpen,
            this::idleWindowGateSnapshot,
            this::isIdleAnimationActiveNow,
            this::isIdleInteractionDelaySatisfied,
            this::isIdleCameraInteractionDelaySatisfied,
            this::getLastInteractionClickSerial,
            this::isCursorOutsideClientWindow,
            () -> acquireOrRenewMotorOwner(
                ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE,
                ExecutorMotorProfileCatalog.MOTOR_LEASE_IDLE_MS
            ),
            this::canPerformIdleMotorActionNow,
            this::performIdleCameraMicroAdjust,
            this::resolveIdleHoverTargetCanvasPoint,
            this::performIdleCursorMove,
            this::resolveIdleDriftTargetCanvasPoint,
            this::resolveIdleOffscreenTargetScreenPoint,
            this::performIdleOffscreenCursorMove,
            this::resolveIdleParkingTargetCanvasPoint,
            this::resolveFishingIdleMode,
            this::resolveActivityIdlePolicy,
            this::activeIdleCadenceTuning,
            this::emitIdleEvent,
            this::isRandomEventRuntimeEnabled,
            this::isRandomEventRuntimeArmed,
            () -> client.getGameState() == GameState.LOGGED_IN,
            this::isBankOpen,
            () -> hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION),
            this::acquireOrRenewInteractionMotorOwnership,
            this::releaseInteractionMotorOwnership,
            client::getLocalPlayer,
            sceneQueryService::currentNpcs,
            this::resolveVariedNpcClickPoint,
            this::isUsableCanvasPoint,
            this::moveInteractionCursorToCanvasPoint,
            this::isCursorNearRandomEventTarget,
            this::selectRandomEventDismissMenuOptionAt,
            CommandExecutor::randomBetween,
            this::randomEventPreAttemptCooldownMinMs,
            this::randomEventPreAttemptCooldownMaxMs,
            this::randomEventSuccessCooldownMinMs,
            this::randomEventSuccessCooldownMaxMs,
            this::randomEventFailureRetryCooldownMinMs,
            this::randomEventFailureRetryCooldownMaxMs,
            this::randomEventCursorReadyHoldMs,
            this::emitRandomEventEvent,
            this::isTopMenuBankOnObject,
            this::isTopMenuChopOnTree,
            this::isTopMenuMineOnRock,
            this::hasAttackEntryOnNpc,
            this::reserveMotorCooldown
        );
    }

    private ExecutorMotorRuntimeInputs.Inputs createMotorRuntimeInputs() {
        return ExecutorMotorRuntimeInputFactory.create(
            sessionManager,
            this::isUsableCanvasPoint,
            this::canPerformMotorActionNow,
            allowActivationClick -> this.focusClientWindowAndCanvas(allowActivationClick),
            this::toScreenPoint,
            this::getOrCreateRobot,
            dropSweepSessionService::regionScreen,
            dropSweepSessionService::lastTargetScreen,
            dropSweepSessionService::setLastTargetScreen,
            dropSweepSessionService::awaitingFirstCursorSync,
            this::motorCursorLocationOr,
            this::isCursorNearScreenPoint,
            this::tryConsumeMouseMutationBudget,
            this::currentPointerLocationOr,
            CommandExecutor::moveMouseCurve,
            CommandExecutor::moveMouseCurve,
            this::moveMouseCurveIdle,
            () -> sessionManager.isMotorOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE),
            this::noteMouseMutation,
            this::noteMotorProgramFirstMouseMutation,
            this::updateMotorCursorState,
            this::clearPendingMouseMove,
            this::noteInteractionActivityNow,
            () -> motorCursorScreenPoint,
            this::failMotorProgram,
            this::completeMotorProgram,
            CommandExecutor::normalizedMotorOwnerName,
            CommandExecutor::isWoodcutWorldClickType,
            CommandExecutor::isFishingWorldClickType,
            this::noteInteractionClickSuccess,
            this::pendingMouseMove,
            this::isPendingMouseMoveOwnerValid,
            this::isMotorActionReadyNow,
            pendingMove -> pendingMoveTelemetryService.notePendingMoveAge(pendingMove, currentExecutorTick),
            this::pendingMoveHasExceededCommitTimeout,
            this::pendingMoveTargetInvalidated,
            pendingMoveTelemetryService::notePendingMoveRemainingDistance,
            (pendingMove, reason, tick) -> {
                if (isIdleOwnedOffscreenPendingMove(pendingMove)) {
                    pendingMoveTelemetryService.notePendingMoveBlocked(pendingMove, reason, tick);
                }
            },
            (pendingMove, tick, after) -> {
                if (isIdleOwnedOffscreenPendingMove(pendingMove)) {
                    pendingMoveTelemetryService.notePendingMoveAdvanced(pendingMove, tick, after);
                }
            },
            (pendingMove, reason, tick) -> {
                if (isIdleOwnedOffscreenPendingMove(pendingMove)) {
                    pendingMoveTelemetryService.notePendingMoveCleared(pendingMove, reason, tick);
                }
            },
            this::activeMotorProgram,
            this::motorProgramLeaseMsForOwner,
            this::cancelMotorProgram,
            this::pushMotorOwnerContext,
            this::pushClickTypeContext,
            this::advanceMotorProgramMove,
            this::validateMotorProgramMenu,
            this::popClickTypeContext,
            this::popMotorOwnerContext,
            HUMANIZED_TIMING_ENABLED
        );
    }

    private InventorySlotInteractionController.Host createInventorySlotInteractionControllerHost() {
        return ExecutorInteractionControllerHostFactories.createInventorySlotInteractionControllerHost(
            () -> client.getWidget(InterfaceID.Bankside.ITEMS),
            () -> client.getWidget(InterfaceID.Inventory.ITEMS),
            () -> client.getWidget(InterfaceID.Bankmain.ITEMS),
            this::isBankOpen,
            this::canPerformMotorActionNow,
            this::getOrCreateRobot,
            this::resolveInventorySlotPoint,
            this::toScreenPoint,
            (slot, basePoint, slotBounds) -> targetPointVariationEngine.varyForTarget(
                "inventory_slot:" + slot + ":" + (isBankOpen() ? "bank" : "inv"),
                basePoint,
                slotBounds,
                1,
                4
            ),
            this::noteInteractionClickSuccess
        );
    }

    private BankMenuInteractionController.Host createBankMenuInteractionControllerHost() {
        return ExecutorInteractionControllerHostFactories.createBankMenuInteractionControllerHost(
            this::rightClickCanvasPointBank,
            timeoutMs -> focusMenuInteractionController.waitForMenuOpen(timeoutMs),
            () -> client.getMenu().getMenuEntries(),
            () -> client.getMenu().getMenuX(),
            () -> client.getMenu().getMenuY(),
            () -> client.getMenu().getMenuWidth(),
            this::clickCanvasPointNoRefocus,
            CommandExecutor::sleepCritical,
            client::isMenuOpen,
            () -> MotionProfile.MENU_INTERACTION.directClickSettings,
            (option, target, matchedKeyword, row, menuX, menuY) -> emit(
                "accepted",
                null,
                "context_menu_option_clicked",
                ExecutorValueParsers.details(
                    "option", option,
                    "target", target,
                    "matchedKeyword", matchedKeyword,
                    "row", row,
                    "menuX", menuX,
                    "menuY", menuY
                )
            )
        );
    }

    public void start() {
        commandIngestor.start();
    }

    public void shutdown() {
        lifecycleShutdownService.shutdown();
    }

    public boolean isSelectedWoodcutTarget(int sceneX, int sceneY, int worldViewId) {
        return woodcuttingSelectionController.isSelectedTarget(sceneX, sceneY, worldViewId);
    }

    public boolean toggleSelectedWoodcutTarget(int sceneX, int sceneY, int worldViewId) {
        return woodcuttingSelectionController.toggleSelectedTarget(sceneX, sceneY, worldViewId);
    }

    public boolean isSelectedMiningTarget(int sceneX, int sceneY, int worldViewId) {
        return miningSelectionController.isSelectedTarget(sceneX, sceneY, worldViewId);
    }

    public boolean toggleSelectedMiningTarget(int sceneX, int sceneY, int worldViewId) {
        return miningSelectionController.toggleSelectedTarget(sceneX, sceneY, worldViewId);
    }

    public void onSnapshot(Snapshot snapshot) {
        latestSnapshot = snapshot == null ? Snapshot.empty() : snapshot;
    }

    public void onGameTick(int tick) {
        runtimeTickOrchestrator.onGameTick(tick);
    }

    public void onClientTick(int tick) {
        runtimeTickOrchestrator.onClientTick(tick);
    }

    public int baseMotorCanvasWidth() {
        return client.getCanvasWidth();
    }

    public int baseMotorCanvasHeight() {
        return client.getCanvasHeight();
    }

    public boolean baseMotorMoveMouse(int canvasX, int canvasY) {
        Point canvasPoint = new Point(canvasX, canvasY);
        Optional<Point> screenPoint = toScreenPoint(canvasPoint);
        if (screenPoint.isEmpty()) {
            return false;
        }
        Robot robot = getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        Point to = screenPoint.get();
        moveMouseCurve(robot, to);
        noteMouseMutation(to);
        armBaseMotorClick(to);
        return true;
    }

    public boolean baseMotorClick() {
        if (!consumeBaseMotorClickToken()) {
            return false;
        }
        Robot robot = getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        noteMouseMutation(currentPointerLocationOr(new Point(0, 0)));
        return true;
    }

    public boolean baseMotorRightClick() {
        if (!consumeBaseMotorClickToken()) {
            return false;
        }
        Robot robot = getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        noteMouseMutation(currentPointerLocationOr(new Point(0, 0)));
        return true;
    }

    public boolean baseMotorPressKey(int keyCode) {
        clearBaseMotorClickToken();
        Robot robot = getOrCreateRobot();
        if (robot == null || keyCode <= 0) {
            return false;
        }
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
        noteMotorAction();
        return true;
    }

    public boolean baseMotorDrag(int fromCanvasX, int fromCanvasY, int toCanvasX, int toCanvasY) {
        clearBaseMotorClickToken();
        Point from = new Point(fromCanvasX, fromCanvasY);
        Point to = new Point(toCanvasX, toCanvasY);
        Optional<Point> fromScreen = toScreenPoint(from);
        Optional<Point> toScreen = toScreenPoint(to);
        if (fromScreen.isEmpty() || toScreen.isEmpty()) {
            return false;
        }
        Robot robot = getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        Point start = fromScreen.get();
        Point end = toScreen.get();
        moveMouseCurve(robot, start);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        int steps = stepsForDistance(start, end);
        long stepDelay = stepDelayForDistance(start, end);
        moveMouseCurve(robot, start, end, steps, stepDelay);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        noteMouseMutation(end);
        return true;
    }

    private void armBaseMotorClick(Point movedToScreenPoint) {
        if (movedToScreenPoint == null) {
            clearBaseMotorClickToken();
            return;
        }
        baseMotorArmedClickScreenPoint = new Point(movedToScreenPoint);
        baseMotorArmedClickUntilMs = System.currentTimeMillis() + BASE_MOTOR_ARMED_CLICK_WINDOW_MS;
    }

    private void clearBaseMotorClickToken() {
        baseMotorArmedClickScreenPoint = null;
        baseMotorArmedClickUntilMs = 0L;
    }

    private boolean consumeBaseMotorClickToken() {
        Point armedPoint = baseMotorArmedClickScreenPoint == null
            ? null
            : new Point(baseMotorArmedClickScreenPoint);
        if (armedPoint == null) {
            clearBaseMotorClickToken();
            return false;
        }
        long now = System.currentTimeMillis();
        if (now > baseMotorArmedClickUntilMs) {
            clearBaseMotorClickToken();
            return false;
        }
        Point pointer = currentPointerLocationOr(null);
        if (pointer == null || pixelDistance(pointer, armedPoint) > BASE_MOTOR_ARMED_CLICK_TOLERANCE_PX) {
            clearBaseMotorClickToken();
            return false;
        }
        clearBaseMotorClickToken();
        return true;
    }

    public boolean hasActiveMotorProgram() {
        MotorProgram program = activeMotorProgram();
        return program != null && !program.toHandle().isTerminal();
    }

    public boolean hasPendingMouseMove() {
        return motorController.hasPendingMove();
    }

    public boolean hasActiveDropSweepSession() {
        return dropSweepSessionService.hasActiveSession();
    }

    public Set<Integer> activeDropSweepItemIds() {
        return dropSweepSessionService.activeItemIds();
    }

    public boolean isLogoutRuntimeActive() {
        return logoutRuntime != null && logoutRuntime.isActive();
    }

    public boolean hasCombatBoundary() {
        return combatRuntime.hasBoundary();
    }

    public int getCombatBoundaryCenterX() {
        return combatRuntime.boundaryCenterX();
    }

    public int getCombatBoundaryCenterY() {
        return combatRuntime.boundaryCenterY();
    }

    public int getCombatBoundaryRadiusTiles() {
        return combatRuntime.boundaryRadiusTiles();
    }

    private void withMotorOwnerContext(String owner, Runnable action) {
        if (action == null) {
            return;
        }
        String previous = pushMotorOwnerContext(owner);
        try {
            action.run();
        } finally {
            popMotorOwnerContext(previous);
        }
    }

    private boolean withMotorOwnerContextResult(String owner, java.util.function.BooleanSupplier action) {
        if (action == null) {
            return false;
        }
        String normalizedOwner = normalizedMotorOwnerName(owner);
        if (normalizedOwner.isEmpty()) {
            return action.getAsBoolean();
        }
        if (!acquireOrRenewMotorOwner(normalizedOwner, motorLeaseMsForOwner(normalizedOwner))) {
            return false;
        }
        String previous = pushMotorOwnerContext(normalizedOwner);
        try {
            return action.getAsBoolean();
        } finally {
            popMotorOwnerContext(previous);
        }
    }

    private void suppressIdleMotionIfCommandTrafficActive() {
        idleSuppressionService.suppressIdleMotionIfCommandTrafficActive();
    }

    private boolean isIdleOwnedOffscreenPendingMove(PendingMouseMove pending) {
        if (pending == null || pending.to == null) {
            return false;
        }
        if (!ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(normalizedMotorOwnerName(pending.owner))) {
            return false;
        }
        Optional<Rectangle> windowBoundsOpt = clientScreenBoundsResolver.resolveClientWindowBoundsScreen();
        if (windowBoundsOpt.isEmpty()) {
            return false;
        }
        Rectangle windowBounds = windowBoundsOpt.get();
        return !windowBounds.contains(pending.to);
    }

    private String pushMotorOwnerContext(String owner) {
        return motorDispatchContextService.pushMotorOwnerContext(owner);
    }

    private void popMotorOwnerContext(String previous) {
        motorDispatchContextService.popMotorOwnerContext(previous);
    }

    private String pushClickTypeContext(String clickType) {
        return motorDispatchContextService.pushClickTypeContext(clickType);
    }

    private void popClickTypeContext(String previous) {
        motorDispatchContextService.popClickTypeContext(previous);
    }

    private String activeMotorOwnerContext() {
        return motorDispatchContextService.activeMotorOwnerContext();
    }

    private String activeClickTypeContext() {
        return motorDispatchContextService.activeClickTypeContext();
    }

    private static String normalizedMotorOwnerName(String owner) {
        return safeString(owner).trim().toLowerCase(Locale.ROOT);
    }

    private long motorLeaseMsForOwner(String owner) {
        String normalized = normalizedMotorOwnerName(owner);
        if (ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK.equals(normalized)) {
            return ExecutorMotorProfileCatalog.MOTOR_LEASE_BANK_MS;
        }
        if (ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP.equals(normalized)) {
            return ExecutorMotorProfileCatalog.MOTOR_LEASE_DROP_SWEEP_MS;
        }
        if (ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(normalized)) {
            return ExecutorMotorProfileCatalog.MOTOR_LEASE_IDLE_MS;
        }
        return ExecutorMotorProfileCatalog.MOTOR_LEASE_INTERACTION_MS;
    }

    private long motorProgramLeaseMsForOwner(String owner) {
        return Math.max(ExecutorMotorProfileCatalog.MOTOR_PROGRAM_MIN_LEASE_MS, motorLeaseMsForOwner(owner));
    }

    private boolean acquireOrRenewMotorOwner(String owner, long leaseMs) {
        return motorDispatchAdmissionService.acquireOrRenewMotorOwner(owner, leaseMs);
    }

    private boolean canPerformMotorActionNow() {
        return motorDispatchAdmissionService.canPerformMotorActionNow(activeMotorOwnerContext());
    }

    private boolean isClientWindowForegroundEligible() {
        if (!STRICT_FOREGROUND_WINDOW_GATING) {
            return true;
        }
        return isClientCanvasFocused();
    }

    private boolean allowWindowRefocusForInteraction() {
        if (!STRICT_FOREGROUND_WINDOW_GATING) {
            return true;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return true;
        }
        return (loginRuntime != null && loginRuntime.isActive())
            || (logoutRuntime != null && logoutRuntime.isActive())
            || (breakRuntime != null && breakRuntime.isBreakActive());
    }

    private boolean isMotorActionReadyNow() {
        return motorDispatchAdmissionService.isMotorActionReadyNow();
    }

    private void reserveMotorCooldown(long delayMs) {
        motorDispatchAdmissionService.reserveMotorCooldown(delayMs);
    }

    private void noteMotorAction() {
        motorDispatchAdmissionService.noteMotorAction();
    }

    private boolean isIdleRuntimeEnabled() {
        return IDLE_RUNTIME_DEFAULT_ENABLED;
    }

    private boolean isIdleRuntimeArmedForContext(IdleSkillContext context) {
        return idleArmingService.isArmedForContext(context, resolveFishingIdleMode(context));
    }

    private boolean isIdleRuntimeArmedForCurrentContext() {
        return isIdleRuntimeArmedForContext(resolveIdleSkillContext());
    }

    public boolean isIdleActionWindowOpen() {
        return idleGateTelemetryService.isIdleActionWindowOpen();
    }

    public boolean isIdleCameraWindowOpen() {
        return idleGateTelemetryService.isIdleCameraWindowOpen();
    }

    private boolean isIdleMenuBlockActive() {
        if (!client.isMenuOpen()) {
            return false;
        }
        // Skilling idle is move-only and should keep running even if a menu is open,
        // otherwise transient menu-open states can starve idle movement entirely.
        IdleSkillContext context = resolveIdleSkillContext();
        return context != IdleSkillContext.FISHING && context != IdleSkillContext.WOODCUTTING;
    }

    private boolean shouldSuppressIdleForLogin() {
        long now = System.currentTimeMillis();
        boolean loginActive = loginRuntime != null && loginRuntime.isActive();
        boolean primaryPromptVisible = isPrimaryLoginSubmitPromptVisible();
        boolean secondaryPromptVisible = isSecondaryLoginSubmitPromptVisible();
        if (loginActive || primaryPromptVisible || secondaryPromptVisible) {
            suppressIdleForLoginUntilMs = Math.max(
                suppressIdleForLoginUntilMs,
                now + LOGIN_IDLE_SUPPRESS_RUNTIME_GRACE_MS
            );
            return true;
        }
        return now < suppressIdleForLoginUntilMs;
    }

    private boolean shouldSuppressIdleForTraversalOrBank() {
        boolean interactionOnlySessionActive =
            sessionManager.hasActiveSession()
                && !sessionManager.hasActiveSessionOtherThan(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
        IdleTraversalBankSuppressionGate.DirectState state = new IdleTraversalBankSuppressionGate.DirectState(
            isBankOpen(),
            hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK),
            hasActiveMotorProgramForOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION),
            hasPendingCommandRows(),
            interactionOnlySessionActive,
            shouldOwnInteractionSession()
        );
        return idleTraversalBankSuppressionGate.isSuppressedNow(state);
    }

    public JsonObject idleWindowGateSnapshot() {
        return idleGateTelemetryService.idleWindowGateSnapshot();
    }

    public boolean isIdleAnimationActiveNow() {
        return isAnimationActive(currentPlayerAnimation());
    }

    public boolean isIdleInteractionDelaySatisfied() {
        if (lastInteractionTime <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        return (now - lastInteractionTime) > idleInteractionDelayMs();
    }

    public boolean isIdleCameraInteractionDelaySatisfied() {
        if (lastInteractionTime <= 0L) {
            return true;
        }
        long now = System.currentTimeMillis();
        long relaxedDelayMs = Math.max(220L, Math.round(idleInteractionDelayMs() * 0.32));
        return (now - lastInteractionTime) > relaxedDelayMs;
    }

    public boolean isCursorOutsideClientWindow() {
        Point pointer = currentPointerLocationOr(null);
        if (pointer == null) {
            return false;
        }
        Optional<Rectangle> windowBoundsOpt = clientScreenBoundsResolver.resolveClientWindowBoundsScreen();
        if (windowBoundsOpt.isEmpty()) {
            return false;
        }
        return !windowBoundsOpt.get().contains(pointer);
    }

    public boolean canPerformIdleMotorActionNow() {
        return canPerformMotorActionNow();
    }

    public boolean isIdleInterActionWindowOpen() {
        return idleGateTelemetryService.isIdleInterActionWindowOpen();
    }

    private void ensureDropSweepSessionRegistered() {
        if (dropSweepSessionRegistration != null) {
            return;
        }
        dropSweepSessionRegistration = sessionManager.registerSession(ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP);
    }

    private void clearDropSweepSessionRegistration() {
        if (dropSweepSessionRegistration == null) {
            return;
        }
        dropSweepSessionRegistration.close();
        dropSweepSessionRegistration = null;
    }

    public boolean shouldOwnInteractionSession() {
        long now = System.currentTimeMillis();
        if (isAnimationActive(currentPlayerAnimation())) {
            return true;
        }
        if (now <= woodcuttingRuntime.retryWindowUntilMs()) {
            return true;
        }
        if (now <= miningRuntime.retryWindowUntilMs()) {
            return true;
        }
        if (now <= fishingRuntime.retryWindowUntilMs()) {
            return true;
        }
        if (now <= combatRuntime.retryWindowUntilMs()) {
            return true;
        }
        if (hasWoodcutSelectionOwnership()) {
            return true;
        }
        if (hasMiningSelectionOwnership()) {
            return true;
        }
        return now <= woodcuttingRuntime.outcomeWaitUntilMs()
            || now <= miningRuntime.outcomeWaitUntilMs()
            || now <= fishingRuntime.outcomeWaitUntilMs()
            || now <= combatRuntime.outcomeWaitUntilMs();
    }

    public IdleSkillContext resolveIdleSkillContext() {
        long now = System.currentTimeMillis();
        IdleSkillContext context = IdleSkillContext.GLOBAL;
        long strongestSignalUntil = now;

        long fishingSignalUntil = strongestFishingIdleSignalUntil(now);
        if (fishingSignalUntil > strongestSignalUntil) {
            strongestSignalUntil = fishingSignalUntil;
            context = IdleSkillContext.FISHING;
        }

        long woodcutSignalUntil = strongestWoodcuttingIdleSignalUntil(now);
        if (woodcutSignalUntil > strongestSignalUntil) {
            strongestSignalUntil = woodcutSignalUntil;
            context = IdleSkillContext.WOODCUTTING;
        }

        long miningSignalUntil = strongestMiningIdleSignalUntil(now);
        if (miningSignalUntil > strongestSignalUntil) {
            strongestSignalUntil = miningSignalUntil;
            context = IdleSkillContext.MINING;
        }

        long combatSignalUntil = strongestCombatIdleSignalUntil(now);
        if (combatSignalUntil > strongestSignalUntil) {
            context = IdleSkillContext.COMBAT;
        }

        return context;
    }

    public FishingIdleMode resolveFishingIdleMode() {
        return resolveFishingIdleMode(IdleSkillContext.FISHING);
    }

    public FishingIdleMode resolveFishingIdleMode(IdleSkillContext context) {
        IdleSkillContext idleContext = context == null ? IdleSkillContext.GLOBAL : context;
        if ((idleContext == IdleSkillContext.FISHING || idleContext == IdleSkillContext.WOODCUTTING)
            && fishingIdleModeOverrideEnabled) {
            FishingIdleMode mode = configuredFishingIdleMode;
            return mode == null ? DEFAULT_FISHING_IDLE_MODE : mode;
        }
        ActivityIdlePolicy policy = resolveActivityIdlePolicy(idleContext);
        FishingIdleMode policyMode = policy == null ? null : policy.fishingIdleMode();
        return policyMode == null ? DEFAULT_FISHING_IDLE_MODE : policyMode;
    }

    private String activeDropCadenceProfileKey() {
        ActivityIdlePolicy policy =
            activityIdlePolicyRegistry.resolveForActivity(ActivityIdlePolicyRegistry.ACTIVITY_FISHING);
        return policy == null ? "DB_PARITY" : policy.profileKey();
    }

    private ActivityIdlePolicy resolveActivityIdlePolicy(IdleSkillContext context) {
        return activityIdlePolicyRegistry.resolveForContext(context);
    }

    private IdleCadenceTuning activeIdleCadenceTuning() {
        IdleCadenceTuning tuning = activeIdleCadenceTuning;
        return tuning == null ? IdleCadenceTuning.none() : tuning;
    }

    private LoginProfile resolveManualMetricsLoginProfile() {
        return manualMetricsRuntimeTuning.resolveLoginProfile(activeIdleCadenceTuning());
    }

    private LogoutProfile resolveManualMetricsLogoutProfile() {
        return manualMetricsRuntimeTuning.resolveLogoutProfile(activeIdleCadenceTuning());
    }

    private BreakProfile resolveManualMetricsBreakProfile() {
        return manualMetricsRuntimeTuning.resolveBreakProfile(activeIdleCadenceTuning());
    }

    private long randomEventPreAttemptCooldownMinMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventPreAttemptCooldownMinMs(activeIdleCadenceTuning());
    }

    private long randomEventPreAttemptCooldownMaxMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventPreAttemptCooldownMaxMs(activeIdleCadenceTuning());
    }

    private long randomEventSuccessCooldownMinMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventSuccessCooldownMinMs(activeIdleCadenceTuning());
    }

    private long randomEventSuccessCooldownMaxMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventSuccessCooldownMaxMs(activeIdleCadenceTuning());
    }

    private long randomEventFailureRetryCooldownMinMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventFailureRetryCooldownMinMs(activeIdleCadenceTuning());
    }

    private long randomEventFailureRetryCooldownMaxMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventFailureRetryCooldownMaxMs(activeIdleCadenceTuning());
    }

    private long randomEventCursorReadyHoldMs() {
        return manualMetricsRuntimeTuning.resolveRandomEventCursorReadyHoldMs(activeIdleCadenceTuning());
    }

    private void setActiveDropCadenceProfileKey(String profileKey) {
        safeString(profileKey);
    }

    private void setActiveIdleCadenceTuning(IdleCadenceTuning tuning) {
        activeIdleCadenceTuning = tuning == null ? IdleCadenceTuning.none() : tuning;
    }

    private void applyIdleCadenceTuningFromPayload(JsonObject payload) {
        IdleCadenceTuning parsed = IdleCadenceTuning.fromPayload(payload);
        if (parsed != null && parsed.hasOverrides()) {
            setActiveIdleCadenceTuning(parsed);
        }
    }

    private static FishingIdleMode parseFishingIdleMode(String raw) {
        Optional<FishingIdleMode> parsed = tryParseFishingIdleMode(raw);
        if (parsed.isPresent()) {
            return parsed.get();
        }
        return DEFAULT_FISHING_IDLE_MODE;
    }

    private static Optional<FishingIdleMode> tryParseFishingIdleMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');
        if ("OFFSCREEN".equals(normalized)) {
            normalized = FishingIdleMode.OFFSCREEN_BIASED.name();
        } else if ("DISABLED".equals(normalized) || "NONE".equals(normalized)) {
            normalized = FishingIdleMode.OFF.name();
        }
        for (FishingIdleMode mode : FishingIdleMode.values()) {
            if (mode.name().equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }

    private boolean hasWoodcutSelectionOwnership() {
        return woodcuttingTargetStateService.preferredSelectedWorldPoint() != null
            || !woodcuttingSelectionService.isEmpty();
    }

    private boolean hasMiningSelectionOwnership() {
        return miningTargetStateService.preferredSelectedWorldPoint() != null
            || !miningSelectionService.isEmpty();
    }

    private long strongestFishingIdleSignalUntil(long now) {
        long signalUntil = Math.max(
            fishingRuntime.retryWindowUntilMs(),
            Math.max(
                fishingRuntime.outcomeWaitUntilMs(),
                fishingRuntime.approachWaitUntilMs()
            )
        );
        if (fishingTargetStateService.hasLockedTarget() || fishingRuntime.lastAttemptNpcIndex() >= 0) {
            signalUntil = Math.max(signalUntil, now + 1L);
        }
        return signalUntil;
    }

    private long strongestWoodcuttingIdleSignalUntil(long now) {
        long signalUntil = Math.max(
            woodcuttingRuntime.retryWindowUntilMs(),
            Math.max(
                woodcuttingRuntime.outcomeWaitUntilMs(),
                woodcuttingRuntime.approachWaitUntilMs()
            )
        );
        if (woodcuttingTargetStateService.hasLockedTarget()
            || hasWoodcutSelectionOwnership()
            || woodcuttingRuntime.lastAttemptWorldPoint() != null) {
            signalUntil = Math.max(signalUntil, now + 1L);
        }
        return signalUntil;
    }

    private long strongestMiningIdleSignalUntil(long now) {
        long signalUntil = Math.max(
            miningRuntime.retryWindowUntilMs(),
            miningRuntime.outcomeWaitUntilMs()
        );
        if (miningTargetStateService.hasLockedTarget() || hasMiningSelectionOwnership()) {
            signalUntil = Math.max(signalUntil, now + 1L);
        }
        return signalUntil;
    }

    private long strongestCombatIdleSignalUntil(long now) {
        long signalUntil = Math.max(
            combatRuntime.retryWindowUntilMs(),
            combatRuntime.outcomeWaitUntilMs()
        );
        if (combatRuntime.lastAttemptNpcIndex() >= 0 || isCombatPostOutcomeSettleGraceActive(now)) {
            signalUntil = Math.max(signalUntil, now + 1L);
        }
        return signalUntil;
    }

    void noteInteractionActivityNow() {
        lastInteractionTime = System.currentTimeMillis();
    }

    private void noteInteractionClickSuccess() {
        noteInteractionClickSuccess(activeClickTypeContext());
    }

    private void noteInteractionClickSuccess(String clickType) {
        interactionClickTelemetryService.noteInteractionClickSuccess(clickType);
    }

    private boolean isSettleEligibleClickType(String clickType) {
        return isFishingWorldClickType(clickType);
    }

    private static boolean isWoodcutWorldClickType(String clickType) {
        return ExecutorMotorProfileCatalog.CLICK_TYPE_WOODCUT_WORLD.equals(safeString(clickType));
    }

    private static boolean isFishingWorldClickType(String clickType) {
        return ExecutorMotorProfileCatalog.CLICK_TYPE_FISHING_WORLD.equals(safeString(clickType));
    }

    private void rememberInteractionAnchor(Point anchorCenterCanvasPoint, Rectangle anchorBoundsCanvas) {
        interactionClickTelemetryService.rememberInteractionAnchor(anchorCenterCanvasPoint, anchorBoundsCanvas);
    }

    private void rememberInteractionAnchorForTileObject(TileObject targetObject, Point fallbackCanvasPoint) {
        interactionAnchorResolverService.rememberInteractionAnchorForTileObject(targetObject, fallbackCanvasPoint);
    }

    public long getLastInteractionClickSerial() {
        return interactionClickTelemetryService.interactionClickSerial();
    }

    public Optional<Point> getLastInteractionClickCanvasPoint() {
        return interactionClickTelemetryService.lastInteractionClickCanvasPoint();
    }

    public boolean isInteractionClickFresh(long maxAgeMs) {
        return interactionClickTelemetryService.isInteractionClickFresh(maxAgeMs);
    }

    public int getProcessedCommandRowsThisTick() {
        return processedCommandRowsThisTick;
    }

    public boolean hasPendingCommandRows() {
        return !pendingCommands.isEmpty();
    }

    public boolean acquireOrRenewInteractionMotorOwnership() {
        return acquireOrRenewMotorOwner(
            ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION,
            ExecutorMotorProfileCatalog.MOTOR_LEASE_INTERACTION_MS
        );
    }

    public void releaseInteractionMotorOwnership() {
        sessionManager.releaseMotor(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
    }

    public int getCurrentExecutorTick() {
        return currentExecutorTick;
    }

    public long getMotorActionSerial() {
        return motorDispatchAdmissionService.actionSerial();
    }

    private MotorProgram activeMotorProgram() {
        return motorController.activeProgram();
    }

    private void setActiveMotorProgram(MotorProgram program) {
        motorController.setActiveProgram(program);
    }

    private void clearActiveMotorProgram() {
        motorController.clearActiveProgram();
    }

    private long nextMotorProgramId() {
        return motorController.nextProgramId();
    }

    private PendingMouseMove pendingMouseMove() {
        return motorController.pendingMove();
    }

    private void setPendingMouseMove(PendingMouseMove move) {
        motorController.setPendingMove(move);
    }

    private void clearPendingMouseMove() {
        motorController.clearPendingMove();
    }

    public boolean hasActiveMotorProgramForOwner(String owner) {
        String normalized = normalizedMotorOwnerName(owner);
        if (normalized.isEmpty()) {
            return false;
        }
        MotorProgram program = activeMotorProgram();
        if (program == null) {
            return false;
        }
        MotorHandle handle = program.toHandle();
        if (handle.isTerminal()) {
            return false;
        }
        return normalized.equals(normalizedMotorOwnerName(program.profile.owner));
    }

    public boolean performInteractionPostClickSettleMove(Point anchorCanvasPoint) {
        return interactionClickEngine.performInteractionPostClickSettleMove(anchorCanvasPoint);
    }

    private Point clampPointToRectangle(Point point, Rectangle bounds) {
        if (point == null || bounds == null) {
            return point;
        }
        int minX = bounds.x;
        int minY = bounds.y;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        return new Point(
            Math.max(minX, Math.min(maxX, point.x)),
            Math.max(minY, Math.min(maxY, point.y))
        );
    }

    private WoodcuttingActivityRuntime resolveWoodcuttingActivityRuntime() {
        return activityRuntimeRegistry.require("woodcutting", WoodcuttingActivityRuntime.class);
    }

    private MiningActivityRuntime resolveMiningActivityRuntime() {
        return activityRuntimeRegistry.require("mining", MiningActivityRuntime.class);
    }

    private boolean isFishingLevelUpPromptVisible() {
        Optional<Widget> continueWidget = findVisibleWidgetByKeywords("click here to continue", "continue");
        if (continueWidget.isEmpty()) {
            return false;
        }
        Optional<Widget> levelTextWidget = findVisibleWidgetByKeywords(
            "level is now",
            "you've just advanced",
            "you have just advanced",
            "advanced a level",
            "fishing level",
            "your fishing level",
            "levelled up",
            "leveled up",
            "congratulations"
        );
        if (levelTextWidget.isPresent()) {
            return true;
        }
        if (client.getGameState() != GameState.LOGGED_IN) {
            return false;
        }
        if (isAnimationActive(currentPlayerAnimation())) {
            return true;
        }
        long now = System.currentTimeMillis();
        long fishingSignalUntil = strongestFishingIdleSignalUntil(now);
        return now <= (fishingSignalUntil + FISHING_LEVEL_UP_CONTINUE_FISHING_SIGNAL_GRACE_MS);
    }

    private boolean dismissFishingLevelUpPrompt() {
        if (clickVisibleLoginWidget("click here to continue", "continue")) {
            return true;
        }
        int holdMs = (int) randomBetween(24L, 88L);
        return pressLoginKeyChord(KeyEvent.VK_SPACE, false, holdMs);
    }

    private void extendCombatRetryWindow() {
        combatRuntimeCoordinator.extendCombatRetryWindow(
            sessionJitteredDurationMs(sessionCombatTimingProfile.retryWindowMs, 180L, SESSION_TIMING_SPREAD_DEFAULT)
        );
    }

    private void beginCombatOutcomeWaitWindow() {
        combatRuntimeCoordinator.beginCombatOutcomeWaitWindow(
            sessionJitteredDurationMs(sessionCombatTimingProfile.outcomeWaitWindowMs, 260L, SESSION_TIMING_SPREAD_NARROW)
        );
    }

    private void clearCombatOutcomeWaitWindow() {
        combatRuntimeCoordinator.clearCombatOutcomeWaitWindow();
    }

    private void clearCombatInteractionWindows() {
        combatRuntimeCoordinator.clearCombatInteractionWindows();
    }

    private long miningTargetReclickCooldownMs() {
        return sessionMiningTimingProfile.targetReclickCooldownMs;
    }

    private long woodcutSameTargetReclickCooldownMs() {
        return sessionWoodcutSameTargetReclickCooldownMs;
    }

    private long fishingSameTargetReclickCooldownMs() {
        return sessionFishingSameTargetReclickCooldownMs;
    }

    private long combatTargetReclickCooldownMs() {
        return sessionCombatTimingProfile.targetReclickCooldownMs;
    }

    private long combatPostAttemptTargetSettleGraceMs() {
        return sessionCombatPostAttemptTargetSettleGraceMs;
    }

    private long combatContestedTargetSuppressionMs() {
        return sessionCombatContestedTargetSuppressionMs;
    }

    private long combatEatDispatchMinIntervalMs() {
        return sessionCombatEatDispatchMinIntervalMs;
    }

    private long combatRecenterMinCooldownMs() {
        return sessionCombatRecenterMinCooldownMs;
    }

    private long combatRecenterMaxCooldownMs() {
        return sessionCombatRecenterMaxCooldownMs;
    }

    private long idleInteractionDelayMs() {
        return sessionIdleInteractionDelayMs;
    }

    private SkillTimingProfile scaleSkillTimingProfile(SkillTimingProfile profile) {
        if (profile == null) {
            return new SkillTimingProfile(0L, 0L, 0L);
        }
        return new SkillTimingProfile(
            sessionScaledDurationMs(profile.retryWindowMs, 150L),
            sessionScaledDurationMs(profile.outcomeWaitWindowMs, 250L),
            sessionScaledDurationMs(profile.targetReclickCooldownMs, 0L)
        );
    }

    private long sessionScaledDurationMs(long baseMs, long minimumMs) {
        return scaleDuration(baseMs, sessionTimingScale, minimumMs);
    }

    private long sessionJitteredDurationMs(long baseMs, long minimumMs, double spreadFraction) {
        long scaled = sessionScaledDurationMs(baseMs, minimumMs);
        if (!HUMANIZED_TIMING_ENABLED) {
            return scaled;
        }
        double boundedSpread = Math.max(0.0, spreadFraction);
        long spread = Math.max(1L, Math.round((double) scaled * boundedSpread));
        long low = Math.max(minimumMs, scaled - spread);
        long high = Math.max(low, scaled + spread);
        return randomBetween(low, high);
    }

    private static long scaleDuration(long baseMs, double scale, long minimumMs) {
        long base = Math.max(minimumMs, baseMs);
        if (!HUMANIZED_TIMING_ENABLED) {
            return base;
        }
        double boundedScale = Math.max(0.10, scale);
        long scaled = Math.round((double) base * boundedScale);
        return Math.max(minimumMs, scaled);
    }

    private static double sampleSessionTimingScale() {
        if (!HUMANIZED_TIMING_ENABLED) {
            return 1.0;
        }
        double low = Math.min(SESSION_TIMING_SCALE_MIN, SESSION_TIMING_SCALE_MAX);
        double high = Math.max(SESSION_TIMING_SCALE_MIN, SESSION_TIMING_SCALE_MAX);
        return ThreadLocalRandom.current().nextDouble(low, high);
    }

    private void updateWoodcutBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
        combatRuntimeCoordinator.updateBoundary(targetWorldX, targetWorldY, targetMaxDistance);
    }

    private void clearWoodcutBoundary() {
        combatRuntimeCoordinator.updateBoundary(-1, -1, -1);
    }

    private void updateCombatBoundary(int targetWorldX, int targetWorldY, int targetMaxDistance) {
        combatRuntimeCoordinator.updateBoundary(targetWorldX, targetWorldY, targetMaxDistance);
    }

    private boolean isCombatAnchorLikelyStale(Player local, int targetWorldX, int targetWorldY, int targetMaxDistance) {
        if (local == null || targetWorldX <= 0 || targetWorldY <= 0) {
            return false;
        }
        WorldPoint localPos = local.getWorldLocation();
        if (localPos == null) {
            return false;
        }
        int drift = Math.max(
            Math.abs(localPos.getX() - targetWorldX),
            Math.abs(localPos.getY() - targetWorldY)
        );
        int toleratedDrift = Math.max(COMBAT_ANCHOR_MAX_LOCAL_DRIFT_TILES, Math.max(1, targetMaxDistance) * 4);
        return drift > toleratedDrift;
    }

    private void noteCombatTargetAttempt(NPC npc) {
        combatRuntimeCoordinator.noteCombatTargetAttempt(npc);
    }

    private void clearCombatTargetAttempt() {
        combatRuntimeCoordinator.clearCombatTargetAttempt();
    }

    private boolean isCombatPostOutcomeSettleGraceActive(long now) {
        return combatRuntimeCoordinator.isPostOutcomeSettleGraceActive(now, combatPostAttemptTargetSettleGraceMs());
    }

    private void suppressCombatNpcTarget(int npcIndex, long durationMs) {
        combatRuntimeCoordinator.suppressCombatNpcTarget(npcIndex, durationMs);
    }

    boolean isCombatNpcSuppressed(int npcIndex) {
        return combatRuntimeCoordinator.isCombatNpcSuppressed(npcIndex);
    }

    private void pruneCombatNpcSuppression() {
        combatRuntimeCoordinator.pruneCombatNpcSuppression();
    }

    public Optional<Point> resolveIdleHoverTargetCanvasPoint() {
        return idleCursorTargetPlanner.resolveIdleHoverTargetCanvasPoint();
    }

    public Optional<Point> resolveIdleDriftTargetCanvasPoint() {
        return idleCursorTargetPlanner.resolveIdleDriftTargetCanvasPoint();
    }

    public Optional<Point> resolveIdleParkingTargetCanvasPoint() {
        return idleCursorTargetPlanner.resolveIdleParkingTargetCanvasPoint();
    }

    public Optional<Point> resolveIdleOffscreenTargetScreenPoint() {
        return idleCursorTargetPlanner.resolveIdleOffscreenTargetScreenPoint();
    }

    public boolean performIdleCursorMove(Point canvasTarget) {
        if (canvasTarget == null || !isUsableCanvasPoint(canvasTarget)) {
            return false;
        }
        MotorHandle handle = scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(canvasTarget),
            MotorGestureType.MOVE_ONLY,
            buildIdleMoveProfile()
        );
        String reason = safeString(handle.reason);
        boolean moved = handle.status == MotorGestureStatus.COMPLETE
            || ((handle.status == MotorGestureStatus.SCHEDULED || handle.status == MotorGestureStatus.IN_FLIGHT)
                && !"motor_program_busy".equals(reason));
        if (moved) {
            boolean interactionOnlySessionActive = sessionManager.hasActiveSession()
                && !sessionManager.hasActiveSessionOtherThan(ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION);
            if (!interactionOnlySessionActive) {
                invalidateWoodcutSelectionContextFromIdle();
                invalidateMiningSelectionContextFromIdle();
                clearFishingTargetLock();
                fishingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
            }
        }
        return moved;
    }

    public boolean performIdleOffscreenCursorMove(Point screenTarget) {
        if (screenTarget == null) {
            emitIdleEvent(
                "offscreen_dispatch_rejected",
                ExecutorValueParsers.details("failureReason", "raw_target_null")
            );
            return false;
        }
        Rectangle windowBounds = clientScreenBoundsResolver.resolveClientWindowBoundsScreen().orElse(null);
        Rectangle canvasBounds = clientScreenBoundsResolver.resolveClientCanvasBoundsScreen().orElse(null);
        Point resolvedTarget = resolveIdleOffscreenScreenTarget(screenTarget);
        JsonObject dispatchDetails = offscreenDispatchDebugDetails(
            windowBounds,
            canvasBounds,
            screenTarget,
            resolvedTarget
        );
        if (log.isDebugEnabled()) {
            log.debug(
                "xptool.idle_offscreen_dispatch window={} rawTarget={} finalTarget={}",
                windowBounds,
                screenTarget,
                resolvedTarget
            );
        }
        if (resolvedTarget == null) {
            dispatchDetails.addProperty("failureReason", "resolved_target_null");
            dispatchDetails.addProperty("scheduleAccepted", false);
            emitIdleEvent("idle_offscreen_dispatch_debug", dispatchDetails);
            emitIdleEvent(
                "offscreen_dispatch_rejected",
                dispatchDetails
            );
            return false;
        }
        boolean finalOutsideWindow = windowBounds == null || !windowBounds.contains(resolvedTarget);
        if (!finalOutsideWindow) {
            dispatchDetails.addProperty("failureReason", "final_target_inside_window");
            dispatchDetails.addProperty("scheduleAccepted", false);
            emitIdleEvent("idle_offscreen_dispatch_debug", dispatchDetails);
            emitIdleEvent("offscreen_dispatch_rejected", dispatchDetails);
            return false;
        }
        boolean normalized = !resolvedTarget.equals(screenTarget);
        if (normalized) {
            emitIdleEvent(
                "idle_offscreen_target_normalized_to_screen",
                dispatchDetails
            );
        }
        boolean scheduled = idleOffscreenMoveEngine.scheduleIdleOffscreenMove(resolvedTarget);
        dispatchDetails.addProperty("scheduleAccepted", scheduled);
        emitIdleEvent("idle_offscreen_dispatch_debug", dispatchDetails);
        return scheduled;
    }

    public boolean performIdleCameraMicroAdjust() {
        return cameraMotionService.performIdleMicroAdjust();
    }

    public boolean nudgeCameraYawLeft() {
        return cameraMotionService.nudgeYawLeft();
    }

    public boolean nudgeCameraYawRight() {
        return cameraMotionService.nudgeYawRight();
    }

    public boolean nudgeCameraPitchUp() {
        return cameraMotionService.nudgePitchUp();
    }

    public boolean nudgeCameraPitchDown() {
        return cameraMotionService.nudgePitchDown();
    }

    public JsonObject cameraLastNudgeDetails() {
        return cameraMotionService.lastNudgeDetails();
    }

    private void releasePendingIdleCameraDrag() {
        cameraMotionService.releasePendingMotion();
    }

    private Optional<Point> randomCanvasPointInRegion(Rectangle region, int insetPx) {
        if (region == null || region.width <= 0 || region.height <= 0) {
            return Optional.empty();
        }
        int inset = Math.max(0, insetPx);
        int minX = region.x + inset;
        int minY = region.y + inset;
        int maxX = (region.x + region.width - 1) - inset;
        int maxY = (region.y + region.height - 1) - inset;
        if (maxX < minX || maxY < minY) {
            return Optional.empty();
        }
        Point p = new Point(
            ThreadLocalRandom.current().nextInt(minX, maxX + 1),
            ThreadLocalRandom.current().nextInt(minY, maxY + 1)
        );
        return isUsableCanvasPoint(p) ? Optional.of(p) : Optional.empty();
    }

    private void processShadowCommandRows(int tick) {
        processedCommandRowsThisTick = commandIngestLifecycleService.processShadowCommandRows(tick);
    }

    private boolean isRandomEventRuntimeArmed() {
        return idleArmingService.hasAnyArmedActivity();
    }

    private CommandDecision execute(CommandRow row) {
        return commandDispatchService.execute(row);
    }

    private static ExecutionOutcome rejectedOutcomeFromReason(String reason) {
        return CommandDecisionOutcomePolicy.outcomeFromDecision(CommandDecision.reject(reason));
    }

    public CommandDecision rejectUnsupportedCommandType() {
        return CommandDecision.reject("unsupported_command_type");
    }

    private boolean startLogoutRuntime() {
        return accountRuntimeOrchestrator.startLogoutRuntime();
    }

    private CommandDecision stopAllRuntime() {
        return accountRuntimeOrchestrator.stopAllRuntime();
    }

    private boolean stopAllRuntimeForBreakStart() {
        return accountRuntimeOrchestrator.stopAllRuntimeForBreakStart();
    }

    private void stopOperationalRuntimeState(String cancelReason) {
        runtimeShutdownService.stopOperationalRuntimeState(cancelReason);
    }

    private boolean requestLogoutForBreakStart() {
        return accountRuntimeOrchestrator.requestLogoutForBreakStart();
    }

    void incrementClicksDispatched() {
        clicksDispatched++;
        fatigueRuntime.noteActionDispatch();
    }

    private boolean startLoginRuntime() {
        return accountRuntimeOrchestrator.startLoginRuntime();
    }

    private boolean hasManualMetricsRuntimeSignal() {
        return manualMetricsRuntimeTuning.hasSignal(activeIdleCadenceTuning());
    }

    private boolean hasManualMetricsRuntimeSignalFor(String consumer, boolean emitWhenMissing) {
        return manualMetricsGateTelemetryService.hasSignalForConsumer(consumer, emitWhenMissing);
    }

    private void maybeEmitManualMetricsRuntimeGateEvent(String consumer, String reason, JsonObject details) {
        manualMetricsGateTelemetryService.emitGateEvent(consumer, reason, details);
    }

    private boolean isRandomEventRuntimeEnabled() {
        if (!RANDOM_EVENT_DISMISS_RUNTIME_ENABLED) {
            return false;
        }
        if (!isClientWindowForegroundEligible()) {
            maybeEmitManualMetricsRuntimeGateEvent(
                "random_event_runtime",
                "client_window_unfocused",
                ExecutorValueParsers.details("tick", currentExecutorTick)
            );
            return false;
        }
        return hasManualMetricsRuntimeSignalFor("random_event_runtime", true);
    }

    private void suppressIdleMotionForLoginStart() {
        boolean idleMotorOwnerActive = sessionManager.isMotorOwner(ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE);
        releasePendingIdleCameraDrag();
        PendingMouseMove pending = pendingMouseMove();
        boolean idlePendingMoveCleared = false;
        if (pending != null
            && ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(normalizedMotorOwnerName(pending.owner))) {
            clearPendingMouseMove();
            idlePendingMoveCleared = true;
        }
        boolean idleProgramCancelled = false;
        MotorProgram program = activeMotorProgram();
        if (program != null
            && !program.toHandle().isTerminal()
            && program.profile != null
            && ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE.equals(normalizedMotorOwnerName(program.profile.owner))) {
            cancelMotorProgram(program, "login_start_idle_suppressed");
            idleProgramCancelled = true;
        }
        boolean shouldReleaseIdleOwnership = IdleSuppressionReleasePolicy.shouldReleaseIdleOwnership(
            idleMotorOwnerActive,
            idlePendingMoveCleared,
            idleProgramCancelled
        );
        if (!shouldReleaseIdleOwnership) {
            return;
        }
        motorProgramTerminalService.releaseIdleMotorOwnershipAfterSuppression();
    }

    private int currentPlayerAnimation() {
        Player local = client.getLocalPlayer();
        return local == null ? -1 : local.getAnimation();
    }

    private static boolean isAnimationActive(int animation) {
        return animation != -1 && animation != 0;
    }

    private void beginDropSweepSession(int itemId, Set<Integer> itemIds) {
        Set<Integer> normalizedItemIds = dropSweepInventoryService.normalizeItemIds(itemId, itemIds);
        int initialObservedQty = countDropSweepTargetItems(itemId);
        boolean sessionChanged = dropSweepSessionService.beginSession(itemId, normalizedItemIds, initialObservedQty);
        if (sessionChanged) {
            pendingDropRepeatBlockedCount = 0;
            inventorySlotPointPlanner.consumeDropRepeatBlockedCount();
            lastDropClientAdvanceAtMs = 0L;
            // Dropping interrupts gather interaction flows; clear stale
            // approach/outcome/attempt windows so gathering can restart promptly after drop.
            woodcuttingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
            miningRuntimeService.clearInteractionWindows();
            fishingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
            clearFishingTargetLock();
        }
        if (dropSweepSessionService.isSessionActive()) {
            inventorySlotPointPlanner.beginDropSweepSession(dropSweepSessionService.sessionSerial());
        }
        dropSweepSessionService.refreshRegionScreen(resolveInventoryInteractionRegionScreen().orElse(null));
        ensureDropSweepSessionRegistered();
    }

    private void endDropSweepSession() {
        dropSweepSessionService.endSession(System.currentTimeMillis());
        pendingDropRepeatBlockedCount = 0;
        inventorySlotPointPlanner.consumeDropRepeatBlockedCount();
        inventorySlotPointPlanner.endDropSweepSession();
        lastDropClientAdvanceAtMs = 0L;
        // Ensure gather systems can re-dispatch after drop teardown while keeping
        // recent gather dispatch timing (woodcut/fishing) for post-action delay reliability.
        woodcuttingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
        miningRuntimeService.clearInteractionWindows();
        fishingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
        clearDropSweepSessionRegistration();
    }

    private long lastDropSweepSessionEndedAtMs() {
        return dropSweepSessionService.lastSessionEndedAtMs();
    }

    private Optional<Integer> findInventorySlotFrom(int itemId, int startSlot) {
        Optional<DropSweepInventoryService.InventoryView> inventoryViewOpt = resolveDropSweepInventoryView();
        if (inventoryViewOpt.isEmpty()) {
            return Optional.empty();
        }
        return dropSweepInventoryService.findInventorySlotFrom(
            inventoryViewOpt.get(),
            itemId,
            dropSweepSessionService.configuredItemIds(),
            startSlot,
            this::resolveDropSweepItemName
        );
    }

    private boolean updateDropSweepProgressState(int itemId) {
        if (itemId <= 0) {
            return true;
        }
        int currentQty = countDropSweepTargetItems(itemId);
        return dropSweepSessionService.updateProgressState(
            currentQty,
            currentExecutorTick,
            ExecutorDropProfile.DROP_SWEEP_NO_PROGRESS_LIMIT
        );
    }

    private int countDropSweepTargetItems(int sessionItemId) {
        Optional<DropSweepInventoryService.InventoryView> inventoryViewOpt = resolveDropSweepInventoryView();
        if (inventoryViewOpt.isEmpty()) {
            return 0;
        }
        return dropSweepInventoryService.countDropSweepTargetItems(
            inventoryViewOpt.get(),
            sessionItemId,
            dropSweepSessionService.configuredItemIds(),
            this::resolveDropSweepItemName
        );
    }

    private boolean noteDropSweepDispatchFailure() {
        return dropSweepSessionService.noteDispatchFailure(ExecutorDropProfile.DROP_SWEEP_DISPATCH_FAIL_LIMIT);
    }

    private void noteDropSweepDispatchSuccess() {
        dropSweepSessionService.clearDispatchFailureStreak();
    }

    private Optional<DropSweepInventoryService.InventoryView> resolveDropSweepInventoryView() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null || inventory.getItems() == null) {
            return Optional.empty();
        }
        Item[] items = inventory.getItems();
        if (items.length == 0) {
            return Optional.empty();
        }
        int slotCount = Math.max(0, Math.min(28, items.length));
        if (slotCount == 0) {
            return Optional.empty();
        }
        return Optional.of(new DropSweepInventoryService.InventoryView() {
            @Override
            public int slotCount() {
                return slotCount;
            }

            @Override
            public int itemIdAt(int slot) {
                if (slot < 0 || slot >= slotCount) {
                    return -1;
                }
                Item item = items[slot];
                return item == null ? -1 : item.getId();
            }

            @Override
            public int quantityAt(int slot) {
                if (slot < 0 || slot >= slotCount) {
                    return 0;
                }
                Item item = items[slot];
                return item == null ? 0 : item.getQuantity();
            }
        });
    }

    private String resolveDropSweepItemName(int itemId) {
        ItemComposition def = client.getItemDefinition(itemId);
        return def == null ? "" : safeString(def.getName());
    }

    private Optional<Point> centerOfDropSweepRegionCanvas() {
        Optional<Rectangle> regionOpt = resolveInventoryInteractionRegionCanvas();
        if (regionOpt.isEmpty()) {
            return Optional.empty();
        }
        Rectangle region = regionOpt.get();
        if (region.width <= 0 || region.height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Point(
            (int) Math.round(region.getCenterX()),
            (int) Math.round(region.getCenterY())
        ));
    }

    private Optional<Rectangle> resolveInventoryInteractionRegionCanvas() {
        Widget inv = null;
        Widget bankInv = client.getWidget(InterfaceID.Bankside.ITEMS);
        if (isBankOpen() && bankInv != null && !bankInv.isHidden()) {
            inv = bankInv;
        }
        if (inv == null) {
            Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
            if (inventory != null && !inventory.isHidden()) {
                inv = inventory;
            }
        }
        if (inv == null || inv.getBounds() == null) {
            return Optional.empty();
        }
        Rectangle b = inv.getBounds();
        if (b.width <= 0 || b.height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Rectangle(
            Math.max(0, b.x - DROP_SWEEP_REGION_PADDING_PX),
            Math.max(0, b.y - DROP_SWEEP_REGION_PADDING_PX),
            Math.max(1, b.width + (DROP_SWEEP_REGION_PADDING_PX * 2)),
            Math.max(1, b.height + (DROP_SWEEP_REGION_PADDING_PX * 2))
        ));
    }

    private Optional<Rectangle> resolveInventoryInteractionRegionScreen() {
        Optional<Rectangle> canvasRegionOpt = resolveInventoryInteractionRegionCanvas();
        if (canvasRegionOpt.isEmpty()) {
            return Optional.empty();
        }
        Canvas canvas = client.getCanvas();
        if (canvas == null) {
            return Optional.empty();
        }
        try {
            Point origin = canvas.getLocationOnScreen();
            Rectangle c = canvasRegionOpt.get();
            return Optional.of(new Rectangle(
                origin.x + c.x,
                origin.y + c.y,
                c.width,
                c.height
            ));
        } catch (IllegalComponentStateException ex) {
            return Optional.empty();
        }
    }

    private boolean dispatchInventoryDropAction(int slotIndex, int expectedItemId, Point preparedSlotCanvasPoint) {
        if (slotIndex < 0 || slotIndex >= 28) {
            return false;
        }
        Widget slotWidget = resolveInventorySlotWidget(slotIndex);
        if (slotWidget == null) {
            return false;
        }
        int itemId = slotWidget.getItemId() > 0 ? slotWidget.getItemId() : expectedItemId;
        if (itemId <= 0) {
            return false;
        }
        if (!focusClientWindowAndCanvas(false, false)) {
            return false;
        }
        Point slotHoverCanvas = isUsableCanvasPoint(preparedSlotCanvasPoint)
            ? new Point(preparedSlotCanvasPoint)
            : null;
        if (slotHoverCanvas == null) {
            Optional<Point> slotCanvasOpt = resolveInventorySlotPoint(slotIndex);
            if (slotCanvasOpt.isEmpty()) {
                return false;
            }
            slotHoverCanvas = slotCanvasOpt.get();
        }
        if (!isUsableCanvasPoint(slotHoverCanvas)) {
            return false;
        }
        ClickMotionSettings click = MotionProfile.DROP.resolveClickSettings(null);
        long settleMs = Math.max(2L, Math.round(click.preClickDelayMs * 0.74));
        long downMs = Math.max(3L, Math.round(click.postClickDelayMs * 0.78));
        if (HUMANIZED_TIMING_ENABLED) {
            settleMs += randomBetween(1L, 6L);
            downMs += randomBetween(1L, 5L);
            if (ThreadLocalRandom.current().nextInt(100) < 7) {
                settleMs += randomBetween(6L, 18L);
            }
        }
        ClickMotionSettings dropClickSettings = new ClickMotionSettings(
            click.driftRadiusPx,
            settleMs,
            downMs
        );
        if (!dispatchPrimaryClickMotorized(
            slotHoverCanvas,
            dropClickSettings,
            ExecutorMotorProfileCatalog.SESSION_DROP_SWEEP,
            ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD,
            MotionProfile.DROP
        )) {
            return false;
        }
        if (dropSweepSessionService.isSessionActive()) {
            inventorySlotPointPlanner.noteDropPointDispatched(dropSweepSessionService.sessionSerial(), slotHoverCanvas);
        }
        return true;
    }

    private void emitDropDebug(String reason, JsonObject details) {
        captureDropRepeatBlockedTelemetry();
        long now = System.currentTimeMillis();
        if ((now - lastDropDebugEmitAtMs) < ExecutorDropProfile.DROP_DEBUG_EMIT_MIN_INTERVAL_MS) {
            return;
        }
        JsonObject enriched = details == null ? new JsonObject() : details.deepCopy();
        if (pendingDropRepeatBlockedCount > 0) {
            enriched.addProperty("dropRepeatBlockedCount", pendingDropRepeatBlockedCount);
            pendingDropRepeatBlockedCount = 0;
        }
        JsonObject payloadDetails = enriched.size() == 0 ? null : enriched;
        lastDropDebugEmitAtMs = now;
        emit("executed", null, reason, payloadDetails, "DEFERRED");
    }

    private void captureDropRepeatBlockedTelemetry() {
        int blocked = inventorySlotPointPlanner.consumeDropRepeatBlockedCount();
        if (blocked <= 0) {
            return;
        }
        pendingDropRepeatBlockedCount = saturatingAdd(pendingDropRepeatBlockedCount, blocked);
    }

    private static int saturatingAdd(int left, int right) {
        if (right <= 0) {
            return left;
        }
        if (left >= Integer.MAX_VALUE - right) {
            return Integer.MAX_VALUE;
        }
        return left + right;
    }

    MotorHandle scheduleMotorGesture(CanvasPoint target, MotorGestureType type, MotorProfile profile) {
        return motorDispatchAdmissionService.scheduleMotorGesture(target, type, profile);
    }

    private MotorProfile buildBankMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject) {
        return buildMoveAndClickMotorProfile(ExecutorMotorProfileCatalog.BANK_MOTOR_PROFILE_SPEC, motion, targetObject);
    }

    private MotorProfile buildWoodcutMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject) {
        return buildMoveAndClickMotorProfile(
            ExecutorMotorProfileCatalog.WOODCUT_MOTOR_PROFILE_SPEC,
            motion,
            targetObject
        );
    }

    private MotorProfile buildMiningMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject) {
        return buildMoveAndClickMotorProfile(
            ExecutorMotorProfileCatalog.MINING_MOTOR_PROFILE_SPEC,
            motion,
            targetObject
        );
    }

    private MotorProfile buildFishingMoveAndClickProfile(ClickMotionSettings motion) {
        return buildMoveAndClickMotorProfile(ExecutorMotorProfileCatalog.FISHING_MOTOR_PROFILE_SPEC, motion, null);
    }

    MotorProfile buildAgilityMoveAndClickProfile(ClickMotionSettings motion, TileObject targetObject) {
        return buildMoveAndClickMotorProfile(
            ExecutorMotorProfileCatalog.AGILITY_MOTOR_PROFILE_SPEC,
            motion,
            targetObject
        );
    }

    private MotorProfile buildWalkMoveAndClickProfile(ClickMotionSettings motion) {
        return buildMoveAndClickMotorProfile(ExecutorMotorProfileCatalog.WALK_MOTOR_PROFILE_SPEC, motion, null);
    }

    private MotorProfile buildCombatMoveAndClickProfile(ClickMotionSettings motion) {
        return buildMoveAndClickMotorProfile(ExecutorMotorProfileCatalog.COMBAT_MOTOR_PROFILE_SPEC, motion, null);
    }

    private MotorProfile buildCombatDodgeMoveAndClickProfile(ClickMotionSettings motion) {
        return buildMoveAndClickMotorProfile(
            ExecutorMotorProfileCatalog.COMBAT_DODGE_MOTOR_PROFILE_SPEC,
            motion,
            null
        );
    }

    private MotorProfile buildDropMoveProfile() {
        return buildMoveOnlyMotorProfile(ExecutorMotorProfileCatalog.DROP_MOTOR_PROFILE_SPEC);
    }

    private MotorProfile buildPrimaryClickMotorProfile(
        String owner,
        String clickType,
        MotionProfile fallbackMotionProfile,
        ClickMotionSettings motion
    ) {
        MotionProfile resolvedFallback = fallbackMotionProfile == null ? MotionProfile.GENERIC_INTERACT : fallbackMotionProfile;
        ClickMotionSettings resolvedMotion = motion == null
            ? resolvedFallback.directClickSettings
            : motion;
        long cooldownMs = Math.max(1L, resolvedMotion.preClickDelayMs + resolvedMotion.postClickDelayMs);
        String resolvedOwner = normalizedMotorOwnerName(owner);
        if (resolvedOwner.isEmpty()) {
            resolvedOwner = ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION;
        }
        String resolvedClickType = safeString(clickType);
        if (resolvedClickType.isBlank()) {
            resolvedClickType = ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD;
        }
        return new MotorProfile(
            resolvedOwner,
            resolvedClickType,
            MotorGestureMode.GENERAL,
            false,
            true,
            resolvedMotion,
            MotorMenuValidationMode.NONE,
            null,
            0,
            1,
            3,
            cooldownMs
        );
    }

    private boolean isPrimaryClickDispatchAccepted(MotorHandle handle) {
        if (handle == null || handle.status == null) {
            return false;
        }
        if (handle.status == MotorGestureStatus.COMPLETE || handle.status == MotorGestureStatus.SCHEDULED) {
            return true;
        }
        if (handle.status == MotorGestureStatus.IN_FLIGHT) {
            return !"motor_program_busy".equals(safeString(handle.reason));
        }
        return false;
    }

    private boolean dispatchPrimaryClickMotorized(
        Point canvasPoint,
        ClickMotionSettings motion,
        String owner,
        String clickType,
        MotionProfile fallbackMotionProfile
    ) {
        if (canvasPoint == null || !isUsableCanvasPoint(canvasPoint)) {
            return false;
        }
        MotorHandle handle = scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(canvasPoint),
            MotorGestureType.MOVE_AND_CLICK,
            buildPrimaryClickMotorProfile(owner, clickType, fallbackMotionProfile, motion)
        );
        return isPrimaryClickDispatchAccepted(handle);
    }

    private MotorProfile buildIdleMoveProfile() {
        return new MotorProfile(
            ExecutorMotorProfileCatalog.MOTOR_OWNER_IDLE,
            ExecutorMotorProfileCatalog.CLICK_TYPE_NONE,
            MotorGestureMode.GENERAL,
            false,
            true,
            MotionProfile.GENERIC_INTERACT.directClickSettings,
            MotorMenuValidationMode.NONE,
            null,
            0,
            1,
            2,
            1L
        );
    }

    private MotorProfile buildMoveAndClickMotorProfile(
        MoveAndClickMotorProfileSpec spec,
        ClickMotionSettings motion,
        TileObject targetObject
    ) {
        ClickMotionSettings resolvedMotion = motion == null
            ? spec.fallbackMotionProfile.directClickSettings
            : motion;
        long cooldownMs = Math.max(1L, resolvedMotion.preClickDelayMs + resolvedMotion.postClickDelayMs);
        return new MotorProfile(
            spec.owner,
            spec.clickType,
            MotorGestureMode.GENERAL,
            spec.requireHoverSettleBeforeClick,
            true,
            resolvedMotion,
            spec.menuValidationMode,
            targetObject,
            spec.hoverSettleTicks,
            spec.maxMenuValidationTicks,
            spec.maxStepsPerTick,
            cooldownMs
        );
    }

    private MotorProfile buildMoveOnlyMotorProfile(MoveOnlyMotorProfileSpec spec) {
        return new MotorProfile(
            spec.owner,
            spec.clickType,
            MotorGestureMode.DROP_SWEEP,
            false,
            true,
            spec.fallbackMotionProfile.directClickSettings,
            spec.menuValidationMode,
            null,
            0,
            1,
            spec.maxStepsPerTick,
            spec.postGestureCooldownMs
        );
    }

    private MotorHandle scheduleDropMoveGesture(Point canvasPoint) {
        if (canvasPoint == null || !isUsableCanvasPoint(canvasPoint)) {
            return MotorHandle.failed(0L, MotorGestureType.MOVE_ONLY, "drop_move_target_unusable");
        }
        return scheduleMotorGesture(
            CanvasPoint.fromAwtPoint(canvasPoint),
            MotorGestureType.MOVE_ONLY,
            buildDropMoveProfile()
        );
    }

    private void advanceMotorProgramMove(MotorProgram program) {
        motorProgramMoveEngine.advanceMotorProgramMove(program);
    }

    private boolean validateMotorProgramMenu(MotorProgram program) {
        return motorProgramTerminalService.validateMotorProgramMenu(program);
    }

    private void completeMotorProgram(MotorProgram program, String reason) {
        motorProgramTerminalService.completeMotorProgram(program, reason);
    }

    private void cancelMotorProgram(MotorProgram program, String reason) {
        motorProgramTerminalService.cancelMotorProgram(program, reason);
    }

    private void failMotorProgram(MotorProgram program, String reason) {
        motorProgramTerminalService.failMotorProgram(program, reason);
    }

    private static void sleepNoCooldown(long ms) {
        if (ms <= 0L) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean moveToCanvasPointForProfile(
        Point canvasPoint,
        MotionProfile motionProfile,
        boolean enforceMutationBudget
    ) {
        MotionProfile profile = motionProfile == null ? MotionProfile.GENERIC_INTERACT : motionProfile;
        return motorMoveToCanvasPoint(
            canvasPoint,
            profile.motorGestureMode,
            enforceMutationBudget,
            profile.allowActivationClickForMotorMove,
            true
        );
    }

    private boolean motorMoveToCanvasPoint(
        Point canvasPoint,
        MotorGestureMode mode,
        boolean enforceMutationBudget,
        boolean allowActivationClick,
        boolean recordInteraction
    ) {
        return motorCanvasMoveEngine.motorMoveToCanvasPoint(
            canvasPoint,
            mode,
            enforceMutationBudget,
            allowActivationClick,
            recordInteraction
        );
    }

    private void moveMouseCurveIdle(Robot robot, Point from, Point to) {
        ExecutorCursorMotion.moveMouseCurveIdle(robot, from, to, HUMANIZED_TIMING_ENABLED);
    }

    private Point motorCursorLocationOr(Point fallback) {
        long now = System.currentTimeMillis();
        if (motorCursorScreenPoint != null
            && (now - motorCursorUpdatedAtMs) <= ExecutorEngineConfigCatalog.CURSOR_MOTOR_STATE_STALE_MS) {
            return new Point(motorCursorScreenPoint);
        }
        return currentPointerLocationOr(fallback);
    }

    private void updateMotorCursorState(Point screenPoint) {
        if (screenPoint == null) {
            return;
        }
        motorCursorScreenPoint = new Point(screenPoint);
        motorCursorUpdatedAtMs = System.currentTimeMillis();
    }

    private boolean isPendingMouseMoveOwnerValid(PendingMouseMove pendingMove) {
        if (pendingMove == null) {
            return false;
        }
        String owner = normalizedMotorOwnerName(pendingMove.owner);
        if (owner.isEmpty()) {
            return false;
        }
        if (!sessionManager.isMotorOwner(owner)) {
            return false;
        }
        return sessionManager.renewMotor(owner, motorLeaseMsForOwner(owner));
    }

    private boolean pendingMoveHasExceededCommitTimeout(PendingMouseMove pendingMove) {
        return pendingMove != null && pendingMove.ticksAlive >= PENDING_MOVE_COMMIT_TIMEOUT_TICKS;
    }

    private boolean pendingMoveTargetInvalidated(PendingMouseMove pendingMove) {
        if (pendingMove == null || pendingMove.committedTargetCanvasPoint == null) {
            return false;
        }
        return toScreenPoint(pendingMove.committedTargetCanvasPoint).isEmpty();
    }

    private boolean isCursorNearCanvasPoint(Point canvasPoint, double tolerancePx) {
        if (canvasPoint == null) {
            return false;
        }
        Optional<Point> screen = toScreenPoint(canvasPoint);
        if (screen.isEmpty()) {
            return false;
        }
        return isCursorNearScreenPoint(screen.get(), tolerancePx);
    }

    private boolean isCursorNearScreenPoint(Point screenPoint, double tolerancePx) {
        if (screenPoint == null) {
            return false;
        }
        PointerInfo info = MouseInfo.getPointerInfo();
        Point pointer = info == null ? null : info.getLocation();
        if (pointer == null) {
            pointer = currentPointerLocationFromClientCanvas();
        }
        if (pointer == null) {
            return false;
        }
        return pixelDistance(pointer, screenPoint) <= Math.max(0.0, tolerancePx);
    }

    private Point currentPointerLocationOr(Point fallback) {
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer != null && pointer.getLocation() != null) {
            return pointer.getLocation();
        }
        Point fromCanvas = currentPointerLocationFromClientCanvas();
        if (fromCanvas != null) {
            return fromCanvas;
        }
        return fallback;
    }

    private Point currentPointerLocationFromClientCanvas() {
        Point canvasPointer = toAwtPoint(client.getMouseCanvasPosition());
        if (!isUsableCanvasPoint(canvasPointer)) {
            return null;
        }
        Optional<Point> screenPointer = toScreenPoint(canvasPointer);
        return screenPointer.orElse(null);
    }

    private Point resolveIdleOffscreenScreenTarget(Point rawTarget) {
        if (rawTarget == null) {
            return null;
        }
        Point raw = new Point(rawTarget);
        Optional<Rectangle> windowBoundsOpt = clientScreenBoundsResolver.resolveClientWindowBoundsScreen();
        Optional<Rectangle> canvasBoundsOpt = clientScreenBoundsResolver.resolveClientCanvasBoundsScreen();
        Rectangle windowBounds = windowBoundsOpt.orElse(null);
        Rectangle canvasBounds = canvasBoundsOpt.orElse(null);
        boolean rawOnScreen = isPointOnAnyScreen(raw);
        boolean rawOutsideWindow = windowBounds == null || !windowBounds.contains(raw);
        if (rawOnScreen && rawOutsideWindow) {
            return raw;
        }

        Point best = null;
        int bestGapOutsideWindow = -1;
        if (windowBounds != null && isClearlyRelativeToBounds(raw, windowBounds)) {
            Point converted = new Point(windowBounds.x + raw.x, windowBounds.y + raw.y);
            if (isPointOnAnyScreen(converted)) {
                int gap = gapOutsideBoundsPx(converted, windowBounds);
                if (gap > bestGapOutsideWindow) {
                    best = converted;
                    bestGapOutsideWindow = gap;
                }
            }
        }
        if (canvasBounds != null && isClearlyRelativeToBounds(raw, canvasBounds)) {
            Point converted = new Point(canvasBounds.x + raw.x, canvasBounds.y + raw.y);
            if (isPointOnAnyScreen(converted)) {
                int gap = gapOutsideBoundsPx(converted, windowBounds);
                if (gap > bestGapOutsideWindow) {
                    best = converted;
                    bestGapOutsideWindow = gap;
                }
            }
        }
        if (best != null && (windowBounds == null || !windowBounds.contains(best))) {
            return best;
        }
        if (rawOnScreen && rawOutsideWindow) {
            return raw;
        }
        return null;
    }

    private JsonObject offscreenDispatchDebugDetails(
        Rectangle windowBounds,
        Rectangle canvasBounds,
        Point rawTarget,
        Point finalTarget
    ) {
        int windowX = windowBounds == null ? -1 : windowBounds.x;
        int windowY = windowBounds == null ? -1 : windowBounds.y;
        int windowWidth = windowBounds == null ? -1 : windowBounds.width;
        int windowHeight = windowBounds == null ? -1 : windowBounds.height;
        int canvasX = canvasBounds == null ? -1 : canvasBounds.x;
        int canvasY = canvasBounds == null ? -1 : canvasBounds.y;
        int canvasWidth = canvasBounds == null ? -1 : canvasBounds.width;
        int canvasHeight = canvasBounds == null ? -1 : canvasBounds.height;
        int rawX = rawTarget == null ? -1 : rawTarget.x;
        int rawY = rawTarget == null ? -1 : rawTarget.y;
        int finalX = finalTarget == null ? -1 : finalTarget.x;
        int finalY = finalTarget == null ? -1 : finalTarget.y;
        boolean normalized = rawTarget != null && finalTarget != null && !rawTarget.equals(finalTarget);
        boolean rawOnScreen = rawTarget != null && isPointOnAnyScreen(rawTarget);
        boolean finalOnScreen = finalTarget != null && isPointOnAnyScreen(finalTarget);
        boolean rawOutsideWindow = windowBounds == null || (rawTarget != null && !windowBounds.contains(rawTarget));
        boolean finalOutsideWindow =
            windowBounds == null || (finalTarget != null && !windowBounds.contains(finalTarget));
        String normalizationType = classifyIdleOffscreenNormalization(rawTarget, finalTarget, windowBounds, canvasBounds);
        int finalGapOutsideWindow = gapOutsideBoundsPx(finalTarget, windowBounds);
        return ExecutorValueParsers.details(
            "windowPresent", windowBounds != null,
            "windowX", windowX,
            "windowY", windowY,
            "windowWidth", windowWidth,
            "windowHeight", windowHeight,
            "canvasPresent", canvasBounds != null,
            "canvasX", canvasX,
            "canvasY", canvasY,
            "canvasWidth", canvasWidth,
            "canvasHeight", canvasHeight,
            "rawX", rawX,
            "rawY", rawY,
            "finalX", finalX,
            "finalY", finalY,
            "normalizedToScreen", normalized,
            "normalizationType", normalizationType,
            "rawOnScreen", rawOnScreen,
            "finalOnScreen", finalOnScreen,
            "rawOutsideWindow", rawOutsideWindow,
            "finalOutsideWindow", finalOutsideWindow,
            "finalGapOutsideWindow", finalGapOutsideWindow
        );
    }

    private String classifyIdleOffscreenNormalization(
        Point rawTarget,
        Point finalTarget,
        Rectangle windowBounds,
        Rectangle canvasBounds
    ) {
        if (rawTarget == null || finalTarget == null || rawTarget.equals(finalTarget)) {
            return "none";
        }
        if (windowBounds != null) {
            Point windowRelative = new Point(windowBounds.x + rawTarget.x, windowBounds.y + rawTarget.y);
            if (windowRelative.equals(finalTarget)) {
                return "window_relative";
            }
        }
        if (canvasBounds != null) {
            Point canvasRelative = new Point(canvasBounds.x + rawTarget.x, canvasBounds.y + rawTarget.y);
            if (canvasRelative.equals(finalTarget)) {
                return "canvas_relative";
            }
        }
        return "other";
    }

    private boolean isClearlyRelativeToBounds(Point candidate, Rectangle bounds) {
        if (candidate == null || bounds == null) {
            return false;
        }
        int tolerancePx = 32;
        int width = Math.max(1, bounds.width);
        int height = Math.max(1, bounds.height);
        return candidate.x >= -tolerancePx
            && candidate.x <= (width + tolerancePx)
            && candidate.y >= -tolerancePx
            && candidate.y <= (height + tolerancePx);
    }

    private boolean isPointOnAnyScreen(Point screenPoint) {
        if (screenPoint == null) {
            return false;
        }
        Optional<Rectangle> screenBoundsOpt = clientScreenBoundsResolver.resolveScreenBoundsForPoint(screenPoint);
        return screenBoundsOpt.isPresent() && screenBoundsOpt.get().contains(screenPoint);
    }

    private int gapOutsideBoundsPx(Point candidate, Rectangle bounds) {
        if (candidate == null || bounds == null) {
            return 0;
        }
        int minX = bounds.x;
        int maxX = bounds.x + Math.max(0, bounds.width - 1);
        int minY = bounds.y;
        int maxY = bounds.y + Math.max(0, bounds.height - 1);
        int gapX = candidate.x < minX ? (minX - candidate.x) : Math.max(0, candidate.x - maxX);
        int gapY = candidate.y < minY ? (minY - candidate.y) : Math.max(0, candidate.y - maxY);
        return Math.max(gapX, gapY);
    }

    private boolean isMouseMovePending() {
        return motorController.hasPendingMove();
    }

    private void applyPerceptionDelay(Robot robot) {
        if (robot == null) {
            return;
        }
        applyPerceptionDelayRange(CURSOR_PERCEPTION_DELAY_MIN_MS, CURSOR_PERCEPTION_DELAY_MAX_MS);
    }

    private void applyPerceptionDelayRange(long minMs, long maxMs) {
        long lo = Math.max(0L, Math.min(minMs, maxMs));
        long hi = Math.max(lo, Math.max(minMs, maxMs));
        int delayMs = (int) randomBetween(lo, hi);
        reserveMotorCooldown(Math.max(0, delayMs));
    }

    private ClickMotionSettings resolveClickMotion(JsonObject payload, MotionProfile motionProfile) {
        MotionProfile profile = motionProfile == null ? MotionProfile.GENERIC_INTERACT : motionProfile;
        return profile.resolveClickSettings(payload);
    }

    private static void copyMotionFields(JsonObject source, JsonObject destination) {
        if (source == null || destination == null) {
            return;
        }
        if (source.has("motionProfile")) {
            destination.add("motionProfile", source.get("motionProfile"));
        }
        if (source.has("mouseDriftRadius")) {
            destination.add("mouseDriftRadius", source.get("mouseDriftRadius"));
        }
        if (source.has("preClickDelayMs")) {
            destination.add("preClickDelayMs", source.get("preClickDelayMs"));
        }
        if (source.has("postClickDelayMs")) {
            destination.add("postClickDelayMs", source.get("postClickDelayMs"));
        }
        if (source.has("moveAccelPercent")) {
            destination.add("moveAccelPercent", source.get("moveAccelPercent"));
        }
        if (source.has("moveDecelPercent")) {
            destination.add("moveDecelPercent", source.get("moveDecelPercent"));
        }
        if (source.has("terminalSlowdownRadiusPx")) {
            destination.add("terminalSlowdownRadiusPx", source.get("terminalSlowdownRadiusPx"));
        }
    }

    private boolean isTopMenuChopOnTree(TileObject targetObject) {
        return menuEntryTargetMatcher.isTopMenuChopOnTree(targetObject);
    }

    private boolean isTopMenuMineOnRock(TileObject targetObject) {
        return menuEntryTargetMatcher.isTopMenuMineOnRock(targetObject);
    }

    private boolean isTopMenuBankOnObject() {
        return menuEntryTargetMatcher.isTopMenuBankOnObject();
    }

    private boolean hasAttackEntryOnNpc() {
        return menuEntryTargetMatcher.hasAttackEntryOnNpc();
    }

    private static boolean worldPointsMatch(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        int dist = a.distanceTo(b);
        return dist >= 0 && dist <= WOODCUT_WORLDPOINT_MATCH_RADIUS_TILES;
    }

    private static boolean worldPointsExactMatch(WorldPoint a, WorldPoint b) {
        return a != null && b != null && a.equals(b);
    }

    private void lockWoodcutTarget(TileObject obj) {
        if (woodcuttingTargetStateService.lockTarget(obj)) {
            clearWoodcutHoverPoint();
        }
    }

    private void clearWoodcutTargetLock() {
        woodcuttingTargetStateService.clearTargetLock();
        clearWoodcutHoverPoint();
    }

    private void invalidateWoodcutSelectionContextFromIdle() {
        woodcuttingTargetStateService.clearSelectionContext();
        clearWoodcutHoverPoint();
        woodcuttingRuntimeService.clearInteractionWindowsPreserveDispatchSignal();
    }

    private void invalidateMiningSelectionContextFromIdle() {
        miningTargetStateService.clearSelectionContext();
        clearMiningHoverPoint();
        miningRuntimeService.clearInteractionWindows();
    }

    private void clearWoodcutHoverPoint() {
        // Hover point caching removed; variability path resolves fresh points each attempt.
    }

    private void lockMiningTarget(TileObject obj) {
        if (miningTargetStateService.lockTarget(obj)) {
            clearMiningHoverPoint();
        }
    }

    private void clearMiningTargetLock() {
        miningTargetStateService.clearTargetLock();
        clearMiningHoverPoint();
    }

    private void clearMiningHoverPoint() {
        // Hover point caching removed; variability path resolves fresh points each attempt.
    }

    private void lockFishingTarget(NPC npc) {
        fishingTargetStateService.lockTarget(npc);
    }

    void clearFishingTargetLock() {
        fishingTargetStateService.clearTargetLock();
    }

    int lockedFishingNpcIndexValue() {
        return fishingTargetStateService.lockedNpcIndex();
    }

    WorldPoint lockedFishingWorldPointValue() {
        return fishingTargetStateService.lockedWorldPoint();
    }

    boolean worldPointsExactMatchForHost(WorldPoint a, WorldPoint b) {
        return worldPointsExactMatch(a, b);
    }

    private Point resolveMiningHoverPoint(TileObject targetObject) {
        if (targetObject == null || targetObject.getWorldLocation() == null) {
            return null;
        }

        Point resolved = resolveTileObjectClickPoint(targetObject);
        if (resolved == null || !isUsableCanvasPoint(resolved)) {
            clearMiningHoverPoint();
            return null;
        }
        resolved = targetPointVariationEngine.varyForTileObject(
            targetObject,
            resolved,
            "mining_hover",
            WOODCUT_HOVER_POINT_JITTER_MIN_PX,
            WOODCUT_HOVER_POINT_JITTER_MAX_PX
        );
        if (resolved == null || !isUsableCanvasPoint(resolved)) {
            clearMiningHoverPoint();
            return null;
        }
        return resolved;
    }

    private Point resolveWoodcutHoverPoint(TileObject targetObject) {
        if (targetObject == null || targetObject.getWorldLocation() == null) {
            return null;
        }

        Point resolved = resolveTileObjectClickPoint(targetObject);
        if (resolved == null || !isUsableCanvasPoint(resolved)) {
            clearWoodcutHoverPoint();
            return null;
        }
        resolved = jitterWoodcutHoverPoint(targetObject, resolved);
        if (resolved == null || !isUsableCanvasPoint(resolved)) {
            clearWoodcutHoverPoint();
            return null;
        }
        return resolved;
    }

    private Point jitterWoodcutHoverPoint(TileObject targetObject, Point basePoint) {
        if (basePoint == null) {
            return null;
        }
        try {
            Rectangle bounds = resolveWoodcutVariationBounds(targetObject);
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                int dynamicMax = Math.min(
                    WOODCUT_TARGET_JITTER_MAX_PX,
                    Math.max(
                        WOODCUT_TARGET_JITTER_MIN_PX,
                        (Math.min(bounds.width, bounds.height) / 7) + 1
                    )
                );
                int radius = HUMANIZED_TIMING_ENABLED
                    ? randomIntInclusive(WOODCUT_TARGET_JITTER_MIN_PX, dynamicMax)
                    : dynamicMax;
                Point anchor = clampPointToRectangle(basePoint, bounds);
                Point jittered = targetPointVariationEngine.varyForTileObjectInBounds(
                    targetObject,
                    anchor,
                    bounds,
                    "woodcut_hover",
                    WOODCUT_TARGET_JITTER_MIN_PX,
                    radius
                );
                return isUsableCanvasPoint(jittered) ? jittered : null;
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        int fallbackRadius = HUMANIZED_TIMING_ENABLED
            ? randomIntInclusive(WOODCUT_TARGET_JITTER_MIN_PX, Math.min(WOODCUT_TARGET_JITTER_MAX_PX, WOODCUT_TARGET_JITTER_MIN_PX + 2))
            : WOODCUT_TARGET_JITTER_MIN_PX;
        Point jittered = targetPointVariationEngine.varyForTileObject(
            targetObject,
            basePoint,
            "woodcut_hover",
            WOODCUT_TARGET_JITTER_MIN_PX,
            fallbackRadius
        );
        return isUsableCanvasPoint(jittered) ? jittered : null;
    }

    private Optional<CommandDecision> prepareBankWidgetHover(
        boolean inventorySide,
        int slot,
        Point targetCanvas,
        MotionProfile motionProfile,
        String reasonPrefix
    ) {
        String scope = inventorySide ? "inventory" : "bank";
        Point hoverTarget = targetPointVariationEngine.varyForTarget(
            "bank_widget_hover:" + scope + ":" + slot,
            targetCanvas,
            null,
            1,
            3
        );
        boolean cursorOnTarget = isCursorNearCanvasPoint(hoverTarget, HOVER_TARGET_TOLERANCE_PX);
        if (!cursorOnTarget && !moveToCanvasPointForProfile(hoverTarget, motionProfile, true)) {
            if (isMouseMovePending()) {
                deferredPendingMove++;
                return Optional.of(
                    CommandDecision.accept(
                        reasonPrefix + "_hover_move_pending",
                        ExecutorValueParsers.details("slot", slot, "scope", scope)
                    )
                );
            }
            return Optional.of(CommandDecision.reject(scope + "_slot_hover_move_failed"));
        }
        if (!isCursorNearCanvasPoint(hoverTarget, HOVER_TARGET_TOLERANCE_PX)) {
            deferredHover++;
            return Optional.of(
                CommandDecision.accept(
                    reasonPrefix + "_hover_settling",
                    ExecutorValueParsers.details("slot", slot, "scope", scope)
                )
            );
        }
        return Optional.empty();
    }

    private String resolvePreferredNpcNameHint(int preferredNpcId) {
        if (preferredNpcId <= 0) {
            return "";
        }
        WorldView view = worldViewResolver.topLevel();
        if (view == null) {
            return "";
        }
        for (NPC npc : view.npcs()) {
            if (npc == null) {
                continue;
            }
            NPCComposition comp = npc.getTransformedComposition();
            if (comp == null) {
                comp = npc.getComposition();
            }
            boolean idMatches = npc.getId() == preferredNpcId || (comp != null && comp.getId() == preferredNpcId);
            if (!idMatches) {
                continue;
            }
            String name = normalizedNpcName(comp);
            if (!name.isEmpty()) {
                return name;
            }
        }
        return "";
    }

    private static String normalizedNpcName(NPCComposition comp) {
        return CombatTargetPolicy.normalizedNpcName(comp);
    }

    boolean isAttackableNpc(NPC npc) {
        return combatTargetPolicy.isAttackableNpc(npc);
    }

    private WorldPoint localPlayerWorldPoint() {
        Player local = client.getLocalPlayer();
        return local == null ? null : local.getWorldLocation();
    }

    boolean isBrutusNpcFromCombatSystem(NPC npc) {
        return brutusCombatSystem.isBrutusNpc(npc);
    }

    private void rememberInteractionAnchorForNpc(NPC npc, Point fallbackCanvasPoint) {
        Rectangle hullBounds = null;
        if (npc != null) {
            try {
                Shape hull = npc.getConvexHull();
                hullBounds = hull == null ? null : hull.getBounds();
            } catch (Exception ignored) {
                hullBounds = null;
            }
        }
        Point center = fallbackCanvasPoint == null ? null : new Point(fallbackCanvasPoint);
        if (hullBounds != null && hullBounds.width > 0 && hullBounds.height > 0) {
            center = new Point(
                (int) Math.round(hullBounds.getCenterX()),
                (int) Math.round(hullBounds.getCenterY())
            );
        }
        rememberInteractionAnchor(center, hullBounds);
    }

    private void refreshSceneCacheForTick(int tick) {
        if (sceneCacheTick == tick) {
            return;
        }
        sceneCacheTick = tick;
        SceneCache sceneCache = sceneCacheScanner.scanTopLevelPlane();
        cachedTreeObjects = sceneCache.treeObjects();
        cachedNormalTreeObjects = sceneCache.normalTreeObjects();
        cachedOakTreeObjects = sceneCache.oakTreeObjects();
        cachedWillowTreeObjects = sceneCache.willowTreeObjects();
        cachedRockObjects = sceneCache.rockObjects();
        cachedBankObjects = sceneCache.bankObjects();
    }

    private boolean clickCanvasPoint(Point canvasPoint, ClickMotionSettings motion) {
        String owner = normalizedMotorOwnerName(activeMotorOwnerContext());
        if (owner.isEmpty()) {
            owner = ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION;
        }
        String clickType = safeString(activeClickTypeContext());
        return dispatchPrimaryClickMotorized(
            canvasPoint,
            motion,
            owner,
            clickType,
            MotionProfile.GENERIC_INTERACT
        );
    }

    private boolean rightClickCanvasPointBank(Point canvasPoint, ClickMotionSettings motion) {
        return interactionClickEngine.rightClickCanvasPointBank(canvasPoint, motion);
    }

    private boolean waitForMotorActionReady(long timeoutMs) {
        long budgetMs = Math.max(0L, timeoutMs);
        long deadlineMs = System.currentTimeMillis() + budgetMs;
        while (System.currentTimeMillis() <= deadlineMs) {
            if (canPerformMotorActionNow()) {
                return true;
            }
            sleepNoCooldown(1L);
        }
        return canPerformMotorActionNow();
    }

    private boolean selectBankContextMenuOptionAt(Point canvasPoint, ClickMotionSettings motion, String... optionKeywords) {
        return bankMenuInteractionController.selectBankContextMenuOptionAt(canvasPoint, motion, optionKeywords);
    }

    private boolean moveInteractionCursorToCanvasPoint(Point canvasPoint) {
        return moveToCanvasPointForProfile(canvasPoint, MotionProfile.GENERIC_INTERACT, true);
    }

    private boolean isCursorNearRandomEventTarget(Point canvasPoint) {
        return isCursorNearCanvasPoint(canvasPoint, HOVER_TARGET_TOLERANCE_PX);
    }

    private boolean isTopMenuOptionOnNpc(NPC npc, String... optionKeywords) {
        return menuEntryTargetMatcher.isTopMenuOptionOnNpc(npc, optionKeywords);
    }

    private boolean isTopMenuOptionOnObject(TileObject targetObject, String... optionKeywords) {
        return menuEntryTargetMatcher.isTopMenuOptionOnObject(targetObject, optionKeywords);
    }

    private boolean isTopMenuOptionOnGroundItem(GroundItemRef targetItem, String... optionKeywords) {
        return menuEntryTargetMatcher.isTopMenuOptionOnGroundItem(targetItem, optionKeywords);
    }

    private boolean clickNpcContextPrimaryAt(Point canvasPoint) {
        String previousClickType = pushClickTypeContext(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        try {
            if (!waitForMotorActionReady(ExecutorBankInteractionProfile.RANDOM_EVENT_MOTOR_READY_WAIT_MAX_MS)) {
                return false;
            }
            return clickCanvasPoint(canvasPoint, RANDOM_EVENT_CONTEXT_MENU_CLICK_MOTION);
        } finally {
            popClickTypeContext(previousClickType);
        }
    }

    private boolean selectNpcContextMenuOptionAt(Point canvasPoint, String... optionKeywords) {
        String previousClickType = pushClickTypeContext(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        try {
            String[] keywords = optionKeywords == null || optionKeywords.length == 0
                ? new String[] {"dismiss"}
                : optionKeywords;
            return selectBankContextMenuOptionAt(
                canvasPoint,
                RANDOM_EVENT_CONTEXT_MENU_CLICK_MOTION,
                keywords
            );
        } finally {
            popClickTypeContext(previousClickType);
        }
    }

    private boolean selectRandomEventDismissMenuOptionAt(Point canvasPoint) {
        String previousClickType = pushClickTypeContext(ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD);
        try {
            return selectBankContextMenuOptionAt(
                canvasPoint,
                RANDOM_EVENT_CONTEXT_MENU_CLICK_MOTION,
                "dismiss"
            );
        } finally {
            popClickTypeContext(previousClickType);
        }
    }

    private boolean clickCanvasPointNoRefocus(Point canvasPoint, int clickSettleMs, int clickDownMs) {
        ClickMotionSettings defaults = MotionProfile.MENU_INTERACTION.directClickSettings;
        ClickMotionSettings noRefocusMotion = new ClickMotionSettings(
            defaults.driftRadiusPx,
            Math.max(0L, (long) clickSettleMs),
            Math.max(0L, (long) clickDownMs),
            defaults.moveAccelPercent,
            defaults.moveDecelPercent,
            defaults.terminalSlowdownRadiusPx
        );
        String owner = normalizedMotorOwnerName(activeMotorOwnerContext());
        if (owner.isEmpty()) {
            owner = ExecutorMotorProfileCatalog.MOTOR_OWNER_BANK;
        }
        String clickType = safeString(activeClickTypeContext());
        return dispatchPrimaryClickMotorized(
            canvasPoint,
            noRefocusMotion,
            owner,
            clickType,
            MotionProfile.MENU_INTERACTION
        );
    }

    private boolean focusClientWindowAndCanvas() {
        return focusMenuInteractionController.focusClientWindowAndCanvas();
    }

    private boolean focusClientWindowAndCanvas(boolean allowActivationClick) {
        return focusMenuInteractionController.focusClientWindowAndCanvas(allowActivationClick);
    }

    private boolean focusClientWindowAndCanvas(boolean allowActivationClick, boolean reserveGlobalCooldown) {
        return focusMenuInteractionController.focusClientWindowAndCanvas(allowActivationClick, reserveGlobalCooldown);
    }

    private boolean isClientCanvasFocused() {
        return focusMenuInteractionController.isClientCanvasFocused();
    }

    private boolean clickCanvasActivationAnchor(Robot robot) {
        return focusMenuInteractionController.clickCanvasActivationAnchor(robot);
    }

    private boolean isBankPinPromptVisible() {
        return findVisibleWidgetByKeywords(
            "bank pin",
            "please enter your pin",
            "first click the",
            "second click the",
            "third click the",
            "fourth click the"
        ).isPresent();
    }

    private boolean clickInventorySlot(int slot) {
        return inventorySlotInteractionController.clickInventorySlot(slot);
    }

    private Optional<Integer> findInventorySlot(int itemId) {
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv == null || inv.getItems() == null) {
            return Optional.empty();
        }
        Item[] items = inv.getItems();
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (item != null && item.getId() == itemId && item.getQuantity() > 0) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private void resetTickWorkState() {
        tickWorkBudgetRemaining = EXECUTOR_WORK_BUDGET_PER_TICK;
        motorDispatchAdmissionService.resetMouseMutationBudget();
    }

    private void resetClientPumpState() {
        // ClientTick drives in-flight motor gestures. Reset only the mouse
        // mutation budget here so move steps are not throttled to GameTick cadence.
        motorDispatchAdmissionService.resetMouseMutationBudget();
    }

    private void maybeEmitExecutorDebugCounters() {
        debugTicksSinceLastReport++;
        if (debugTicksSinceLastReport < EXECUTOR_DEBUG_COUNTER_REPORT_INTERVAL_TICKS) {
            return;
        }
        if (log != null && (VERBOSE_EXECUTION_LOGS || log.isDebugEnabled())) {
            emitExecutorTelemetry(
                "[EXECUTOR] sameTick={} reuseHits={} localReuseStreak={} deferredMove={} deferredHover={} clicks={}",
                sameTickCollapses,
                cursorReuseHits,
                consecutiveLocalInteractions,
                deferredPendingMove,
                deferredHover,
                clicksDispatched
            );
            double reuseRatio =
                clicksDispatched == 0 ? 0.0 :
                (double) cursorReuseHits / (double) clicksDispatched;
            if (reuseRatio > 0.60) {
                emitExecutorTelemetry("[CURSOR_MODEL] OVER_REUSING");
            } else if (reuseRatio > 0.20) {
                emitExecutorTelemetry("[CURSOR_MODEL] HEALTHY_REUSE");
            } else {
                emitExecutorTelemetry("[CURSOR_MODEL] LOW_REUSE");
            }
            double avgRemainingDistPx = pendingMoveTelemetryService.averageRemainingDistancePx();
            double minRemainingDistPx = pendingMoveTelemetryService.minRemainingDistancePx();
            double maxRemainingDistPx = pendingMoveTelemetryService.maxRemainingDistancePx();
            emitExecutorTelemetry(
                "[PENDING_MOVE] avgRemainingDistPx={} minRemainingDistPx={} maxRemainingDistPx={} pendingMoveTicksAliveMax={}",
                avgRemainingDistPx,
                minRemainingDistPx,
                maxRemainingDistPx,
                pendingMoveTelemetryService.pendingMoveTicksAliveMax()
            );
            long targetVariationSamples = targetPointVariationEngine.sampleCount();
            long targetVariationFallbacks = targetPointVariationEngine.fallbackCount();
            double fallbackRatio = targetVariationSamples == 0L
                ? 0.0
                : (double) targetVariationFallbacks / (double) targetVariationSamples;
            emitExecutorTelemetry(
                "[TARGET_VARIATION] enabled={} samples={} fallbacks={} fallbackRatio={}",
                targetPointVariationEngine.isEnabled(),
                targetVariationSamples,
                targetVariationFallbacks,
                fallbackRatio
            );
            if (sameTickCollapses > (deferredPendingMove * 2L)
                && sameTickCollapses > (deferredHover * 2L)) {
                emitExecutorTelemetry("[EXECUTOR_STATE] FAST_PATH_HEALTHY");
            } else if (deferredPendingMove > sameTickCollapses) {
                emitExecutorTelemetry("[EXECUTOR_STATE] MOVE_BOUND");
            } else if (deferredHover > sameTickCollapses) {
                emitExecutorTelemetry("[EXECUTOR_STATE] HOVER_BOUND");
            } else {
                emitExecutorTelemetry("[EXECUTOR_STATE] MIXED");
            }
        }
        sameTickCollapses = 0L;
        cursorReuseHits = 0L;
        deferredPendingMove = 0L;
        deferredHover = 0L;
        clicksDispatched = 0L;
        pendingMoveTelemetryService.resetDebugCounters();
        targetPointVariationEngine.resetTelemetryCounters();
        debugTicksSinceLastReport = 0;
    }

    private void emitExecutorTelemetry(String message, Object... args) {
        if (log == null) {
            return;
        }
        if (VERBOSE_EXECUTION_LOGS) {
            log.info(message, args);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(message, args);
        }
    }

    private boolean tryConsumeWorkBudget() {
        if (tickWorkBudgetRemaining <= 0) {
            return false;
        }
        tickWorkBudgetRemaining--;
        return true;
    }

    private boolean tryConsumeMouseMutationBudget() {
        return motorDispatchAdmissionService.tryConsumeMouseMutationBudget();
    }

    private void noteMouseMutation(Point after) {
        updateMotorCursorState(after);
        noteMotorAction();
    }

    private void noteMotorProgramFirstMouseMutation(MotorProgram program) {
        if (program == null || program.profile == null || program.type != MotorGestureType.MOVE_AND_CLICK) {
            return;
        }
        if (loginRuntime == null || !loginRuntime.isActive()) {
            return;
        }
        String owner = normalizedMotorOwnerName(program.profile.owner);
        String clickType = safeString(program.profile.clickType);
        if (!ExecutorMotorProfileCatalog.MOTOR_OWNER_INTERACTION.equals(owner)
            || !ExecutorMotorProfileCatalog.CLICK_TYPE_NON_WORLD.equals(clickType)) {
            return;
        }
        long dispatchAtMs = program.createdAtMs;
        long firstMutationAtMs = program.firstMouseMutationAtMs;
        if (dispatchAtMs <= 0L || firstMutationAtMs <= 0L) {
            return;
        }
        long dispatchToFirstMutationMs = Math.max(0L, firstMutationAtMs - dispatchAtMs);
        Point targetCanvas = program.targetCanvas == null ? null : program.targetCanvas.toAwtPoint();
        emit(
            "executed",
            null,
            "login_click_first_mouse_mutation",
            ExecutorValueParsers.details(
                "programId", program.id,
                "owner", owner,
                "clickType", clickType,
                "dispatchAtMs", dispatchAtMs,
                "firstMouseMutationAtMs", firstMutationAtMs,
                "dispatchToFirstMouseMutationMs", dispatchToFirstMutationMs,
                "stepIndex", program.stepIndex,
                "totalSteps", program.totalSteps,
                "targetCanvasX", targetCanvas == null ? -1 : targetCanvas.x,
                "targetCanvasY", targetCanvas == null ? -1 : targetCanvas.y,
                "gameState", safeString(client.getGameState() == null ? "" : client.getGameState().name())
            ),
            "LOGIN"
        );
    }

    private Optional<Widget> findVisibleWidgetByKeywords(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return Optional.empty();
        }

        Set<Integer> seenIds = new HashSet<>();
        Widget[] roots = client.getWidgetRoots();
        if (roots == null || roots.length == 0) {
            return Optional.empty();
        }

        Widget best = null;
        long bestScore = Long.MIN_VALUE;
        for (Widget root : roots) {
            if (root == null) {
                continue;
            }
            Widget candidate = findBestWidgetRecursive(root, keywords, seenIds);
            if (candidate == null) {
                continue;
            }
            long score = LoginWidgetHeuristics.scoreWidget(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private Widget findBestWidgetRecursive(Widget widget, String[] keywords, Set<Integer> seenIds) {
        if (widget == null) {
            return null;
        }
        int id = widget.getId();
        if (id != -1 && seenIds.contains(id)) {
            return null;
        }
        if (id != -1) {
            seenIds.add(id);
        }

        Widget best = null;
        long bestScore = Long.MIN_VALUE;
        if (LoginWidgetHeuristics.matchesKeywords(widget, keywords)) {
            best = widget;
            bestScore = LoginWidgetHeuristics.scoreWidget(widget);
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null) {
            for (Widget child : dynamicChildren) {
                Widget found = findBestWidgetRecursive(child, keywords, seenIds);
                if (found == null) {
                    continue;
                }
                long score = LoginWidgetHeuristics.scoreWidget(found);
                if (score > bestScore) {
                    bestScore = score;
                    best = found;
                }
            }
        }
        Widget[] children = widget.getChildren();
        if (children != null) {
            for (Widget child : children) {
                Widget found = findBestWidgetRecursive(child, keywords, seenIds);
                if (found == null) {
                    continue;
                }
                long score = LoginWidgetHeuristics.scoreWidget(found);
                if (score > bestScore) {
                    bestScore = score;
                    best = found;
                }
            }
        }
        return best;
    }

    private boolean isGameStateNamed(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        GameState gameState = client.getGameState();
        String stateName = gameState == null ? "" : safeString(gameState.name());
        return stateName.toUpperCase(Locale.ROOT).contains(token.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isPrimaryLoginSubmitPromptVisible() {
        return loginInteractionController.isPrimaryLoginSubmitPromptVisible();
    }

    private boolean isSecondaryLoginSubmitPromptVisible() {
        return loginInteractionController.isSecondaryLoginSubmitPromptVisible();
    }

    private boolean submitLoginAttempt() {
        return loginInteractionController.submitLoginAttempt();
    }

    private LogoutInteractionController.AttemptStatus requestLogoutAttempt() {
        return logoutInteractionController.requestLogoutAttempt();
    }

    private LogoutInteractionController.AttemptStatus requestLogoutAttemptWithMotorOwner(String owner) {
        String normalizedOwner = normalizedMotorOwnerName(owner);
        if (normalizedOwner.isEmpty()) {
            return requestLogoutAttempt();
        }
        if (!acquireOrRenewMotorOwner(normalizedOwner, motorLeaseMsForOwner(normalizedOwner))) {
            return LogoutInteractionController.AttemptStatus.FAILED;
        }
        String previous = pushMotorOwnerContext(normalizedOwner);
        try {
            return requestLogoutAttempt();
        } finally {
            popMotorOwnerContext(previous);
        }
    }

    private boolean clickVisibleLoginWidget(String... keywords) {
        return loginInteractionController.clickVisibleLoginWidget(keywords);
    }

    private boolean pressLoginKeyChord(int keyCode, boolean holdShift, int holdMs) {
        return loginInteractionController.pressLoginKeyChord(keyCode, holdShift, holdMs);
    }

    private Optional<TileObject> resolveOpenBankTarget(JsonObject payload) {
        int targetWorldX = payload == null ? -1 : ExecutorValueParsers.asInt(payload.get("targetWorldX"), -1);
        int targetWorldY = payload == null ? -1 : ExecutorValueParsers.asInt(payload.get("targetWorldY"), -1);
        if (targetWorldX > 0 && targetWorldY > 0) {
            Optional<TileObject> preferred = findBestBankObject(targetWorldX, targetWorldY);
            if (preferred.isPresent()) {
                return preferred;
            }
        }
        return resolveNearestBankTarget();
    }

    private Optional<TileObject> findBestBankObject(int preferredWorldX, int preferredWorldY) {
        return selectBestCursorAwareTarget(
            cachedBankObjects,
            candidate -> {
                if (candidate == null || candidate.getWorldLocation() == null) {
                    return -1;
                }
                WorldPoint wp = candidate.getWorldLocation();
                int distance = Math.abs(wp.getX() - preferredWorldX) + Math.abs(wp.getY() - preferredWorldY);
                if (distance > BANK_WORLDPOINT_MATCH_RADIUS_TILES) {
                    return -1;
                }
                return distance;
            }
        );
    }

    private Optional<TileObject> resolveNearestBankTarget() {
        Player local = client.getLocalPlayer();
        WorldPoint localPos = local == null ? null : local.getWorldLocation();
        if (localPos == null) {
            return selectBestCursorAwareTarget(
                cachedBankObjects,
                candidate -> (candidate == null || candidate.getWorldLocation() == null) ? -1 : 0
            );
        }
        return selectBestCursorAwareTarget(
            cachedBankObjects,
            candidate -> {
                if (candidate == null || candidate.getWorldLocation() == null) {
                    return -1;
                }
                return localPos.distanceTo(candidate.getWorldLocation());
            }
        );
    }

    private Optional<TileObject> selectBestCursorAwareTarget(
        Iterable<TileObject> candidates,
        TargetSelectionEngine.WorldDistanceProvider worldDistanceProvider
    ) {
        return targetSelectionEngine.selectBestCursorAwareTarget(candidates, worldDistanceProvider);
    }

    private Point resolveBankObjectClickPoint(TileObject targetObject) {
        if (targetObject == null) {
            return null;
        }
        Point point = resolveTileObjectClickPoint(targetObject);
        if (point == null) {
            point = toAwtPoint(targetObject.getCanvasLocation());
        }
        return targetPointVariationEngine.varyForTileObject(
            targetObject,
            point,
            "bank_object",
            WOODCUT_HOVER_POINT_JITTER_MIN_PX,
            WOODCUT_HOVER_POINT_JITTER_MAX_PX + 2
        );
    }

    TileObject[] collectBankObjectCandidates(Tile tile) {
        return sceneObjectCandidateClassifier.collectBankObjectCandidates(tile);
    }

    TileObject[] collectTreeObjectCandidates(Tile tile) {
        return sceneObjectCandidateClassifier.collectTreeObjectCandidates(tile);
    }

    TileObject[] collectRockObjectCandidates(Tile tile) {
        return sceneObjectCandidateClassifier.collectRockObjectCandidates(tile);
    }

    private Point resolveTileObjectClickPoint(TileObject obj) {
        if (obj == null) {
            return null;
        }
        try {
            Shape clickbox = obj.getClickbox();
            if (clickbox != null) {
                Point sampled = samplePointInsideShape(clickbox, 18);
                if (sampled != null) {
                    return sampled;
                }
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        Point canvas = toAwtPoint(obj.getCanvasLocation());
        if (canvas == null) {
            return null;
        }
        // Keep fallback stochastic so failed clickbox sampling does not collapse to repeatable anchors.
        int dx = randomIntInclusive(-6, 6);
        int dy = -randomIntInclusive(8, 16);
        return new Point(canvas.x + dx, canvas.y + dy);
    }

    private Point samplePointInsideShape(Shape shape, int maxAttempts) {
        if (shape == null) {
            return null;
        }
        Rectangle b = shape.getBounds();
        if (b == null || b.width <= 1 || b.height <= 1) {
            return null;
        }
        int attempts = Math.max(4, maxAttempts);
        for (int i = 0; i < attempts; i++) {
            int x = ThreadLocalRandom.current().nextInt(b.x, b.x + b.width);
            int y = ThreadLocalRandom.current().nextInt(b.y, b.y + b.height);
            if (shape.contains(x, y)) {
                return new Point(x, y);
            }
        }
        int centerRetryAttempts = Math.max(6, attempts / 3);
        for (int i = 0; i < centerRetryAttempts; i++) {
            int dx = randomIntInclusive(-3, 3);
            int dy = randomIntInclusive(-3, 3);
            int cx = (int) Math.round(b.getCenterX()) + dx;
            int cy = (int) Math.round(b.getCenterY()) + dy;
            if (shape.contains(cx, cy)) {
                return new Point(cx, cy);
            }
        }
        return null;
    }

    private Point resolveVariedNpcClickPoint(NPC npc) {
        Point base = npcClickPointResolver.resolve(npc);
        if (!isUsableCanvasPoint(base)) {
            return base;
        }
        Point varied = targetPointVariationEngine.varyForNpc(
            npc,
            base,
            "npc_click",
            RANDOM_EVENT_NPC_CLICK_JITTER_MIN_PX,
            RANDOM_EVENT_NPC_CLICK_JITTER_MAX_PX
        );
        return isUsableCanvasPoint(varied) ? varied : null;
    }

    private Point resolveVariedFishingNpcClickPoint(NPC npc) {
        Point base = npcClickPointResolver.resolve(npc);
        if (!isUsableCanvasPoint(base)) {
            return base;
        }
        int maxJitter = FISHING_NPC_CLICK_JITTER_MAX_PX;
        try {
            Shape hull = npc == null ? null : npc.getConvexHull();
            Rectangle bounds = hull == null ? null : hull.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                int minDim = Math.min(bounds.width, bounds.height);
                int dynamic = Math.max(
                    FISHING_NPC_CLICK_JITTER_MIN_PX,
                    Math.min(FISHING_NPC_CLICK_JITTER_MAX_PX, (minDim / 6) + 1)
                );
                maxJitter = dynamic;
            }
        } catch (Exception ignored) {
            // Keep default jitter radius.
        }
        Point varied = targetPointVariationEngine.varyForNpc(
            npc,
            base,
            "fishing_npc_click",
            FISHING_NPC_CLICK_JITTER_MIN_PX,
            maxJitter
        );
        if (!isUsableCanvasPoint(varied)) {
            return null;
        }
        Point humanized = applyFishingClickMicroVariation(npc, varied, maxJitter);
        return isUsableCanvasPoint(humanized) ? humanized : varied;
    }

    private Point applyFishingClickMicroVariation(NPC npc, Point point, int maxJitter) {
        if (point == null) {
            return null;
        }
        int radius = sampleFishingJitterRadiusPx(maxJitter);
        if (radius <= 0) {
            return point;
        }
        int dx = sampleTriangularOffset(radius);
        int dy = sampleTriangularOffset(radius);
        Point candidate = new Point(point.x + dx, point.y + dy);
        try {
            Shape hull = npc == null ? null : npc.getConvexHull();
            Rectangle bounds = hull == null ? null : hull.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                candidate = clampPointToRectangle(candidate, bounds);
            }
        } catch (Exception ignored) {
            // Keep sampled point when hull bounds are unavailable.
        }
        return candidate;
    }

    private static int sampleFishingJitterRadiusPx(int maxJitter) {
        int max = Math.max(1, maxJitter);
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 58) {
            return randomIntInclusive(1, Math.max(1, max / 3));
        }
        if (roll < 90) {
            return randomIntInclusive(Math.max(1, max / 4), Math.max(1, (max * 2) / 3));
        }
        return randomIntInclusive(Math.max(1, (max * 2) / 3), max);
    }

    private static int sampleTriangularOffset(int radiusPx) {
        int radius = Math.max(1, radiusPx);
        double a = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        double b = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        double c = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        double weighted = (a * 0.50) + (b * 0.35) + (c * 0.15);
        return (int) Math.round(weighted * (double) radius);
    }

    private Rectangle resolveTileObjectClickBounds(TileObject targetObject) {
        if (targetObject == null) {
            return null;
        }
        try {
            Shape clickbox = targetObject.getClickbox();
            Rectangle bounds = clickbox == null ? null : clickbox.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                return bounds;
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return null;
    }

    private Rectangle resolveWoodcutVariationBounds(TileObject targetObject) {
        Rectangle bounds = resolveTileObjectClickBounds(targetObject);
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        int insetX = Math.max(
            1,
            Math.min(8, (int) Math.round(bounds.width * WOODCUT_TARGET_BOUNDS_INSET_RATIO))
        );
        int insetY = Math.max(
            1,
            Math.min(7, (int) Math.round(bounds.height * (WOODCUT_TARGET_BOUNDS_INSET_RATIO + 0.04)))
        );
        int innerWidth = bounds.width - (insetX * 2);
        int innerHeight = bounds.height - (insetY * 2);
        if (innerWidth >= 5 && innerHeight >= 5) {
            return new Rectangle(bounds.x + insetX, bounds.y + insetY, innerWidth, innerHeight);
        }
        return bounds;
    }

    boolean isNormalTreeObjectCandidate(TileObject obj) {
        return sceneObjectCandidateClassifier.isNormalTreeObjectCandidate(obj);
    }

    boolean isOakTreeObjectCandidate(TileObject obj) {
        return sceneObjectCandidateClassifier.isOakTreeObjectCandidate(obj);
    }

    boolean isWillowTreeObjectCandidate(TileObject obj) {
        return sceneObjectCandidateClassifier.isWillowTreeObjectCandidate(obj);
    }

    private boolean isRockObjectCandidate(TileObject obj) {
        return sceneObjectCandidateClassifier.isRockObjectCandidate(obj);
    }

    boolean isFishingSpotNpcCandidate(NPC npc) {
        if (npc == null) {
            return false;
        }
        NPCComposition comp = npc.getTransformedComposition();
        if (comp == null) {
            comp = npc.getComposition();
        }
        if (comp == null || !comp.isInteractible()) {
            return false;
        }
        return MenuActionHeuristics.hasFishingAction(comp.getActions());
    }

    private Optional<Integer> findBankItemSlot(int itemId) {
        if (itemId <= 0) {
            return Optional.empty();
        }
        Optional<Integer> visibleSlot = findVisibleBankItemSlot(itemId);
        if (visibleSlot.isPresent()) {
            return visibleSlot;
        }
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null || bank.getItems() == null) {
            return Optional.empty();
        }
        Item[] items = bank.getItems();
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (item != null && item.getId() == itemId && item.getQuantity() > 0) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private Optional<Integer> findVisibleBankItemSlot(int itemId) {
        Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        if (bankItems == null || bankItems.isHidden()) {
            return Optional.empty();
        }

        Widget[] dynamicChildren = bankItems.getDynamicChildren();
        if (dynamicChildren != null && dynamicChildren.length > 0) {
            for (int i = 0; i < dynamicChildren.length; i++) {
                Widget child = dynamicChildren[i];
                if (child == null || child.isHidden()) {
                    continue;
                }
                if (child.getItemId() == itemId && child.getItemQuantity() > 0) {
                    return Optional.of(i);
                }
            }
        }

        Widget[] children = bankItems.getChildren();
        if (children != null && children.length > 0) {
            for (int i = 0; i < children.length; i++) {
                Widget child = children[i];
                if (child == null || child.isHidden()) {
                    continue;
                }
                if (child.getItemId() == itemId && child.getItemQuantity() > 0) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Point> resolveInventorySlotPoint(int slot) {
        Optional<Point> slotPoint = inventorySlotPointPlanner.resolveInventorySlotPoint(
            slot,
            dropSweepSessionService.isSessionActive(),
            dropSweepSessionService.sessionSerial(),
            interactionClickTelemetryService.lastInteractionClickCanvasPointOrNull()
        );
        captureDropRepeatBlockedTelemetry();
        if (dropSweepSessionService.isSessionActive() && pendingDropRepeatBlockedCount > 0) {
            emitDropDebug("drop_repeat_blocked", ExecutorValueParsers.details("slot", slot));
        }
        return slotPoint;
    }

    private Optional<Point> resolveInventorySlotBasePoint(int slot) {
        Widget bankInv = client.getWidget(InterfaceID.Bankside.ITEMS);
        if (isBankOpen() && bankInv != null && !bankInv.isHidden()) {
            Optional<Point> point = slotCenter(bankInv, slot);
            if (point.isPresent()) {
                return point;
            }
        }
        Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
        if (inventory == null || inventory.isHidden()) {
            return Optional.empty();
        }
        return slotCenter(inventory, slot);
    }

    private Widget resolveInventorySlotWidget(int slot) {
        return inventorySlotInteractionController.resolveInventorySlotWidget(slot);
    }

    private Widget resolveBankItemSlotWidget(int slot) {
        return inventorySlotInteractionController.resolveBankItemSlotWidget(slot);
    }

    private Optional<Point> slotCenter(Widget container, int slot) {
        return inventorySlotInteractionController.slotCenter(container, slot);
    }

    private Optional<Point> centerOfWidget(Widget widget) {
        return inventorySlotInteractionController.centerOfWidget(widget);
    }

    private void maybeExtendIdleTraversalOrBankSuppression(CommandRow row, ExecutionOutcome outcome) {
        if (row == null || outcome == null) {
            return;
        }
        String normalizedCommandType = safeString(row.commandType).trim().toUpperCase(Locale.ROOT);
        long walkSuppressDurationMs = 0L;
        long bankSuppressDurationMs = 0L;
        if ("WALK_TO_WORLDPOINT_SAFE".equals(normalizedCommandType)) {
            walkSuppressDurationMs = sessionJitteredDurationMs(
                IDLE_SUPPRESS_AFTER_WALK_BASE_MS,
                IDLE_SUPPRESS_AFTER_WALK_MIN_MS,
                IDLE_SUPPRESS_AFTER_WALK_MAX_MS
            );
        } else if (IdleTraversalBankSuppressionGate.isBankCommandType(normalizedCommandType)) {
            bankSuppressDurationMs = sessionJitteredDurationMs(
                IDLE_SUPPRESS_AFTER_BANK_BASE_MS,
                IDLE_SUPPRESS_AFTER_BANK_MIN_MS,
                IDLE_SUPPRESS_AFTER_BANK_MAX_MS
            );
        }
        idleTraversalBankSuppressionGate.noteCommandOutcome(
            row,
            outcome,
            walkSuppressDurationMs,
            bankSuppressDurationMs
        );
    }

    private void emit(String status, CommandRow row, String reason, JsonObject details) {
        emit(status, row, reason, details, null);
    }

    public void emitIdleEvent(String reason, JsonObject details) {
        idleGateTelemetryService.emitIdleEvent(reason, details);
    }

    private void emitRandomEventEvent(String reason, JsonObject details) {
        emit("executed", null, reason, details, "RANDOM_EVENT");
    }

    private void emitIngestDebug(String reason, JsonObject details) {
        if (!COMMAND_INGEST_DEBUG_ENABLED) {
            return;
        }
        emit("executed", null, reason, details, "INGEST");
    }

    private void emit(String status, CommandRow row, String reason, JsonObject details, String eventType) {
        // Command-scoped outcomes must always be logged so core can reconcile lifecycle.
        if (row == null && !ExecutionEventLogPolicy.shouldLogExecutionStatus(status, reason, VERBOSE_EXECUTION_LOGS)) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("status", status);
        payload.addProperty("reason", reason);
        if (eventType != null && !eventType.isBlank()) {
            payload.addProperty("eventType", eventType);
        }
        if (row != null) {
            payload.addProperty("commandId", row.commandId);
            payload.addProperty("commandType", row.commandType);
            payload.addProperty("source", row.source);
            payload.addProperty("commandTick", row.tick);
            payload.addProperty("commandReason", row.reason);
        }
        if (details != null) {
            payload.add("details", details);
        }
        if (bridgeDispatchModePolicy.isBridgeRuntimeEnabled()) {
            payload.addProperty(
                "bridgeMode",
                bridgeDispatchModePolicy.telemetryMode(row == null ? "" : row.commandType, reason)
            );
        }
        JsonObject redactedPayload = EXECUTION_PAYLOAD_REDACTOR.redact(payload);
        log.info("xptool.execution {}", GSON.toJson(redactedPayload));
        executionTelemetryFileSink.publish(redactedPayload);
    }

    private boolean isBankOpen() {
        if (client.getItemContainer(InventoryID.BANK) != null) {
            return true;
        }
        Widget container = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        if (container != null && !container.isHidden()) {
            return true;
        }
        Widget itemContainer = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return itemContainer != null && !itemContainer.isHidden();
    }

    private Optional<Integer> findFirstInventoryItemNotIn(Set<Integer> excludeItemIds) {
        ItemContainer inv = client.getItemContainer(InventoryID.INV);
        if (inv == null) {
            return Optional.empty();
        }
        for (Item item : inv.getItems()) {
            if (item == null) {
                continue;
            }
            int id = item.getId();
            int qty = item.getQuantity();
            if (id <= 0 || qty <= 0) {
                continue;
            }
            if (excludeItemIds.contains(id)) {
                continue;
            }
            return Optional.of(id);
        }
        return Optional.empty();
    }

    private boolean tryHumanizedBankWidgetAction(Point targetPoint, String... optionKeywords) {
        if (!HUMANIZED_BANK_WIDGET_ACTIONS_ENABLED) {
            return false;
        }
        if (targetPoint == null || optionKeywords == null || optionKeywords.length == 0) {
            return false;
        }
        if (!selectBankContextMenuOptionAt(
            targetPoint,
            MotionProfile.MENU_INTERACTION.directClickSettings,
            optionKeywords
        )) {
            return false;
        }
        noteInteractionActivityNow();
        return true;
    }

    private static long randomBetween(long minInclusive, long maxInclusive) {
        long lo = Math.min(minInclusive, maxInclusive);
        long hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        if (!HUMANIZED_TIMING_ENABLED) {
            return lo;
        }
        if (hi == Long.MAX_VALUE) {
            long sampled = ThreadLocalRandom.current().nextLong(lo, hi);
            return ThreadLocalRandom.current().nextBoolean() ? sampled : hi;
        }
        return ThreadLocalRandom.current().nextLong(lo, hi + 1L);
    }

    private static int randomIntInclusive(int minInclusive, int maxInclusive) {
        int lo = Math.min(minInclusive, maxInclusive);
        int hi = Math.max(minInclusive, maxInclusive);
        if (hi <= lo) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String resolveConfiguredLeftClickDropItemIds(Logger log) {
        String configured = safeString(System.getProperty("xptool.leftClickDropItemIds"));
        if (!configured.isBlank() && log != null) {
            log.info("xptool.executor left_click_drop_ids_source=system_property");
        }
        return configured;
    }

    private static long remainingFutureMs(long untilMs, long nowMs) {
        if (untilMs <= 0L || untilMs == Long.MIN_VALUE) {
            return 0L;
        }
        if (untilMs <= nowMs) {
            return 0L;
        }
        return untilMs - nowMs;
    }

    private static Set<String> parseIdleActivityAllowlist(String csv) {
        Set<String> allowlist = new HashSet<>();
        String raw = safeString(csv);
        for (String token : raw.split(",")) {
            String normalized = safeString(token).trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                allowlist.add(normalized);
            }
        }
        return allowlist;
    }

    static Point toAwtPoint(net.runelite.api.Point p) {
        if (p == null) {
            return null;
        }
        return new Point(p.getX(), p.getY());
    }

    private static boolean isPositiveInteger(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean typeWithdrawQuantity(String quantityRaw) {
        String quantity = quantityRaw == null ? "" : quantityRaw.trim();
        if (!isPositiveInteger(quantity)) {
            return false;
        }
        if (!waitForWithdrawQuantityPrompt()) {
            return false;
        }
        Robot robot = getOrCreateRobot();
        if (robot == null) {
            return false;
        }
        Canvas canvas = client.getCanvas();
        if (canvas != null) {
            Window window = SwingUtilities.getWindowAncestor(canvas);
            if (window != null) {
                window.toFront();
                window.requestFocus();
                sleepQuietly(10L);
            }
            canvas.requestFocus();
            canvas.requestFocusInWindow();
            sleepQuietly(10L);
        }
        for (int i = 0; i < quantity.length(); i++) {
            int keyCode = KeyEvent.VK_0 + (quantity.charAt(i) - '0');
            robot.keyPress(keyCode);
            sleepQuietly(10L);
            robot.keyRelease(keyCode);
            sleepQuietly(18L);
        }
        robot.keyPress(KeyEvent.VK_ENTER);
        sleepQuietly(10L);
        robot.keyRelease(KeyEvent.VK_ENTER);
        return true;
    }

    private boolean waitForWithdrawQuantityPrompt() {
        long deadlineMs = System.currentTimeMillis() + WITHDRAW_X_PROMPT_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() <= deadlineMs) {
            if (isWithdrawQuantityPromptVisible()) {
                return true;
            }
            sleepQuietly(WITHDRAW_X_PROMPT_POLL_INTERVAL_MS);
        }
        return isWithdrawQuantityPromptVisible();
    }

    private boolean isWithdrawQuantityPromptVisible() {
        return findVisibleWidgetByKeywords(
            "enter amount",
            "how many",
            "amount to withdraw"
        ).isPresent();
    }

    private void typeBankSearchChar(Robot robot, char ch) {
        if (robot == null) {
            return;
        }
        char c = Character.toLowerCase(ch);
        if (c >= 'a' && c <= 'z') {
            int keyCode = KeyEvent.VK_A + (c - 'a');
            robot.keyPress(keyCode);
            sleepQuietly(
                randomBetween(
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MIN_DELAY_MS,
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MAX_DELAY_MS
                )
            );
            robot.keyRelease(keyCode);
            return;
        }
        if (c >= '0' && c <= '9') {
            int keyCode = KeyEvent.VK_0 + (c - '0');
            robot.keyPress(keyCode);
            sleepQuietly(
                randomBetween(
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MIN_DELAY_MS,
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MAX_DELAY_MS
                )
            );
            robot.keyRelease(keyCode);
            return;
        }
        if (c == ' ') {
            robot.keyPress(KeyEvent.VK_SPACE);
            sleepQuietly(
                randomBetween(
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MIN_DELAY_MS,
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MAX_DELAY_MS
                )
            );
            robot.keyRelease(KeyEvent.VK_SPACE);
            return;
        }
        if (c == '-') {
            robot.keyPress(KeyEvent.VK_MINUS);
            sleepQuietly(
                randomBetween(
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MIN_DELAY_MS,
                    ExecutorBankInteractionProfile.BANK_SEARCH_KEY_HOLD_MAX_DELAY_MS
                )
            );
            robot.keyRelease(KeyEvent.VK_MINUS);
        }
    }

    private Optional<Point> toScreenPoint(Point canvasPoint) {
        if (!isUsableCanvasPoint(canvasPoint)) {
            return Optional.empty();
        }
        Canvas canvas = client.getCanvas();
        if (canvas == null) {
            return Optional.empty();
        }
        try {
            Point origin = canvas.getLocationOnScreen();
            return Optional.of(new Point(origin.x + canvasPoint.x, origin.y + canvasPoint.y));
        } catch (IllegalComponentStateException ex) {
            return Optional.empty();
        }
    }

    private Robot getOrCreateRobot() {
        if (sharedRobot != null) {
            return sharedRobot;
        }
        try {
            sharedRobot = new Robot();
            return sharedRobot;
        } catch (AWTException | HeadlessException ex) {
            return null;
        }
    }

    private static void moveMouseCurve(Robot robot, Point to) {
        ExecutorCursorMotion.moveMouseCurve(robot, to);
    }

    private static void moveMouseCurve(Robot robot, Point from, Point to, int steps, long stepDelayMs) {
        ExecutorCursorMotion.moveMouseCurve(robot, from, to, steps, stepDelayMs);
    }

    private static int stepsForDistance(Point from, Point to) {
        return ExecutorCursorMotion.stepsForDistance(from, to);
    }

    private static long stepDelayForDistance(Point from, Point to) {
        return ExecutorCursorMotion.stepDelayForDistance(from, to);
    }

    static Point curveControlPoint(Point from, Point to) {
        return ExecutorCursorMotion.curveControlPoint(from, to);
    }

    static Point quadraticBezier(Point p0, Point p1, Point p2, double t) {
        return ExecutorCursorMotion.quadraticBezier(p0, p1, p2, t);
    }

    static Point linearInterpolate(Point from, Point to, double t) {
        return ExecutorCursorMotion.linearInterpolate(from, to, t);
    }

    static double smoothstep(double t) {
        return ExecutorCursorMotion.smoothstep(t);
    }

    static double pixelDistance(Point a, Point b) {
        return ExecutorCursorMotion.pixelDistance(a, b);
    }

    private static Point jitterWithinBounds(Point base, Rectangle bounds, int radiusPx) {
        return ExecutorCursorMotion.jitterWithinBounds(base, bounds, radiusPx);
    }

    private boolean isUsableCanvasPoint(Point p) {
        if (p == null) {
            return false;
        }
        int width = client.getCanvasWidth();
        int height = client.getCanvasHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        return p.x >= 1 && p.x < (width - 1) && p.y >= 1 && p.y < (height - 1);
    }

    private boolean isCombatCanvasPointUsable(Point p) {
        if (!isUsableCanvasPoint(p)) {
            return false;
        }
        int width = client.getCanvasWidth();
        int height = client.getCanvasHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        int chatboxMinY = (int) Math.round(height * ExecutorCombatProfile.COMBAT_CHATBOX_EXCLUDE_MIN_Y_RATIO);
        int chatboxMaxX = (int) Math.round(width * COMBAT_CHATBOX_EXCLUDE_MAX_X_RATIO);
        int bottomUiMinY = (int) Math.round(height * COMBAT_BOTTOM_UI_EXCLUDE_MIN_Y_RATIO);
        if (p.y >= bottomUiMinY) {
            return false;
        }
        return !(p.y >= chatboxMinY && p.x <= chatboxMaxX);
    }

    private static void sleepQuietly(long ms) {
        MotorActionGate.reserveGlobalCooldownOnly(ms);
    }

    private static void sleepCritical(long ms) {
        MotorActionGate.reserveGlobalCooldownOnly(ms);
    }

    private void applyDropPerceptionDelay() {
        if (DROP_ISOLATE_FROM_GLOBAL_COOLDOWN) {
            int delayMs = (int) randomBetween(DROP_CURSOR_PERCEPTION_DELAY_MIN_MS, DROP_CURSOR_PERCEPTION_DELAY_MAX_MS);
            reserveMotorCooldown(Math.max(0, delayMs));
            return;
        }
        applyPerceptionDelay(getOrCreateRobot());
    }

    public static final class CommandDecision {
        private final boolean accepted;
        private final String reason;
        private final JsonObject details;

        private CommandDecision(boolean accepted, String reason, JsonObject details) {
            this.accepted = accepted;
            this.reason = reason;
            this.details = details;
        }

        private static CommandDecision accept(String reason, JsonObject details) {
            return new CommandDecision(true, reason, details);
        }

        private static CommandDecision reject(String reason) {
            return new CommandDecision(false, reason, null);
        }

        static CommandDecision fromRuntimeDecision(RuntimeDecision decision) {
            if (decision == null) {
                return reject("runtime_decision_missing");
            }
            if (decision.isAccepted()) {
                return accept(decision.getReason(), decision.getDetails());
            }
            return reject(decision.getReason());
        }

        public boolean isAccepted() {
            return accepted;
        }

        public String getReason() {
            return reason;
        }

        public JsonObject getDetails() {
            return details;
        }
    }
}


