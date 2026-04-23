package support

/**
 * MPS version used by integration tests that spin up an MPS instance (model check, generate, execute, migrate, ...).
 * Kept in a single place so Renovate can bump it.
 */
const val MPS_VERSION = "2025.1.2"

/**
 * The Java version to be used for [MPS_VERSION]. Updated manually.
 */
const val JAVA_VERSION_FOR_MPS = 21

/**
 * Version of the foojay-resolver-convention plugin applied in the settings scripts that tests generate.
 * Kept in a single place so Renovate can bump it.
 */
const val FOOJAY_RESOLVER_CONVENTION_VERSION = "1.0.0"
