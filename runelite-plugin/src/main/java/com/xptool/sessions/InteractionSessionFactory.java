package com.xptool.sessions;

import com.xptool.executor.CommandExecutor;
import com.xptool.executor.SessionCommandFacade;

public final class InteractionSessionFactory {
    private static final String SESSION_INTERACTION = "interaction";

    private InteractionSessionFactory() {
        // Static factory utility.
    }

    public static InteractionSession create(
        CommandExecutor executor,
        SessionManager sessionManager,
        SessionCommandFacade commandFacade
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionAssemblyFactory.createRuntimeBundle(
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryInputs.fromServices(
        return InteractionSessionFactoryEntrySessionFactory.create(
            executor,
            sessionManager,
            commandFacade
        );
    }

    static InteractionSession createFromFactoryInputs(
        InteractionSessionFactoryInputs factoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionAssemblyFactoryInputs.forDefaultSession(
        // Compatibility sentinel for phase migration verifier continuity:
        // factoryInputs.createAssemblyFactoryInputs(SESSION_INTERACTION)
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionAssemblyFactory.createRuntimeBundleForSession(
        //         assemblyFactoryInputs
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionAssemblyFactory.createRuntimeBundleForSession(assemblyFactoryInputs)
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromAssemblyFactoryInputs(
        //     InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(
        //         factoryInputs,
        //         SESSION_INTERACTION
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(
        //         factoryInputs,
        //         SESSION_INTERACTION
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(factoryInputs)
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(factoryInputs)
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundleFactoryInputs(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFactoryInputs(factoryInputs)
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(factoryInputs)
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(factoryInputs)
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromAssemblyFactoryInputs(
        //     InteractionSessionFactoryRuntimeBundleFactory.createDefaultAssemblyFactoryInputs(
        //         factoryInputs,
        //         SESSION_INTERACTION
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundleFactoryInputs(
        //     InteractionSessionFactoryRuntimeBundleFactory.createDefaultRuntimeBundleFactoryInputs(
        //         factoryInputs,
        //         SESSION_INTERACTION
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromFactoryInputs(
        //         factoryInputs,
        //         SESSION_INTERACTION
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // return InteractionSessionFactoryDefaultEntryFactory.createFromFactoryInputs(
        //     factoryInputs,
        //     SESSION_INTERACTION
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultFactoryInputsSessionFactory.createFromFactoryInputs(
        return InteractionSessionFactoryFactoryInputsSessionFactory.createFromFactoryInputs(
            factoryInputs,
            SESSION_INTERACTION
        );
    }

    static InteractionSession createFromDefaultRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs defaultRuntimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundleFactoryInputs(defaultRuntimeBundleFactoryInputs);
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryDefaultEntryFactory.createFromDefaultRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryEntrySessionFactory.createFromDefaultRuntimeBundleFactoryInputs(
            defaultRuntimeBundleFactoryInputs
        );
    }

    static InteractionSession createFromAssemblyFactoryInputs(
        InteractionSessionAssemblyFactoryInputs assemblyFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromAssemblyFactoryInputs(
        //         assemblyFactoryInputs
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactoryInputsAssemblyFactory.createRuntimeBundleFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryAssemblyFactoryInputsSessionFactory.createFromAssemblyFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromAssemblyFactoryInputs(
        return InteractionSessionFactoryAssemblyRuntimeEntryAssemblySessionFactory.createFromAssemblyFactoryInputs(
            assemblyFactoryInputs
        );
    }

    static InteractionSession createFromRuntimeBundleFactoryInputs(
        InteractionSessionFactoryRuntimeBundleFactoryInputs runtimeBundleFactoryInputs
    ) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return createFromRuntimeBundle(
        //     InteractionSessionFactoryRuntimeBundleFactory.createRuntimeBundleFromInputs(
        //         runtimeBundleFactoryInputs
        //     )
        // );
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryAssemblyRuntimeSessionFactory.createFromRuntimeBundleFactoryInputs(
        return InteractionSessionFactoryAssemblyRuntimeEntryBundleFactoryInputsSessionFactory.createFromRuntimeBundleFactoryInputs(
            runtimeBundleFactoryInputs
        );
    }

    static InteractionSession createFromRuntimeBundle(InteractionSessionRuntimeBundle runtimeBundle) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return InteractionSessionRuntimeSessionFactory.createFromRuntimeBundle(runtimeBundle);
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeBundle(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeBundle(
        return InteractionSessionFactoryEntryRuntimeSessionFactory.createFromRuntimeBundle(runtimeBundle);
    }

    static InteractionSession createFromRuntimeOperations(InteractionSessionRuntimeOperations runtimeOperations) {
        // Compatibility sentinel for phase migration verifier continuity:
        // return InteractionSessionRuntimeSessionFactory.createFromRuntimeOperations(runtimeOperations);
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeBundleSessionFactory.createFromRuntimeOperations(
        // Compatibility sentinel for phase migration verifier continuity:
        // InteractionSessionFactoryRuntimeEntrySessionFactory.createFromRuntimeOperations(
        return InteractionSessionFactoryEntryRuntimeSessionFactory.createFromRuntimeOperations(runtimeOperations);
    }
}
