import 'dart:developer' as developer;

/// Register a VM-service extension idempotently.
///
/// A Flutter hot restart re-runs `main()` on the *same* isolate, so the
/// extensions registered on the previous run still exist and a plain
/// [developer.registerExtension] would throw "extension already registered",
/// aborting the rest of registration. Swallowing that keeps the existing
/// handler working and lets registration complete after a hot restart.
void registerExt(String name, developer.ServiceExtensionHandler handler) {
  try {
    developer.registerExtension(name, handler);
  } catch (_) {
    // Already registered (hot restart) — keep the existing handler.
  }
}
